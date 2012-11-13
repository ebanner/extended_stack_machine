/**
 * This class defines an Swing Worker that executes in the background and does
 * not interfere with the action events that the GUI must catch.  Its main
 * purpose is to execute instructions on a stack machine that has already been
 * populated with opcodes and data.
 */

package wkr;

import java.awt.EventQueue;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gui.HaltException;
import gui.SM;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import cli.Memory;
import cli.Read;

public class ExecuteInstructionWorker extends SwingWorker<Void, Void> {
	
	private SM sm;
	private Memory mem;
	public static int PC;
	boolean oldStyle;
	private StringBuilder instructions;
	private String inputLine;
	private UpdateTableRunnable utr;
	private UpdateSPRunnable uspr;
	private UpdateRegistersRunnable urr;
	private final int DEBUG;
	private static Pattern digit;
	private static Matcher m;
	private boolean TRACE;
	
	public ExecuteInstructionWorker(SM sm) {
		this.sm = sm;
		this.mem = sm.mem;
		this.PC = sm.PC;
		this.instructions = new StringBuilder("");
		this.inputLine = "";
		utr = new UpdateTableRunnable(sm);
		uspr = new UpdateSPRunnable(sm);
		urr = new UpdateRegistersRunnable(sm);
		this.DEBUG = 0;
		digit = Pattern.compile("^[-+]?\\d+");
	}
	
	// for when we load in a new file in the GUI
	public void reset(SM sm) {
		this.sm = sm;
		this.mem = sm.mem;
		this.PC = sm.PC;
		this.instructions = new StringBuilder("");
		this.inputLine = "";
		utr = new UpdateTableRunnable(sm);
		uspr = new UpdateSPRunnable(sm);
		urr = new UpdateRegistersRunnable(sm);
	}

	@Override
	/**
	 * Run instructions depending on whether the user has clicked the ``Run" or
	 * ``Single Step" button.
	 */
	protected Void doInBackground() {

		while (true) {
			// keep executing the instructions
			while (sm.singleStep || sm.keepExecuting) {
				/* Only execute if we've hit the ``Run" button or the ``Single
				 * Step" button. */
				try {
					executeInstruction();
					sm.singleStep = false;  // only execute the instruction once
				} catch (HaltException e) {  // we're done
					return null;
				}
				try {  // sleep for the amount of time specified by the slider
					Thread.sleep(sm.speed);
				} catch (Exception e) { }
			}
			try { // cut down on CPU usage
				Thread.sleep(50);
			} catch (Exception e) { }
		}
	}
	
	@Override
	/**
	 * Updates the table and the stack when execution finishes.
	 */
	protected void done() {
		uspr.setPC(PC);
		EventQueue.invokeLater(uspr);

		// update registers in the northeast text field
		EventQueue.invokeLater(urr);
	}
	
	/**
	 * Execute a single instruction and update the tables if we're not at the
	 * maximum speed.
	 * 
	 * @throws HaltException  when a halt opcode is reached
	 */
	public void executeInstruction() throws HaltException {
		/**
		 * Execute the opcode pointed at by PC.
		 *
		 * @param PC  the program counter
		 */
		int opcode = mem.getContents(PC);
		PC++;  // increment PC immediately
		if (sm.oldStyle == true && opcode > 22) {
			opcode++; // support legacy opcode numbering convention
		}

		// values to be used in opcode computations
		int addr, value, num, temp; 
		addr = value = num = 0;

		if (opcodeRequiresParameter(opcode)) {
			/* If it's an instruction that needs an `addr' or `value' parameter,
			 * save that argument. */
			addr = value = mem.getContents(PC);
			PC++;  // increment PC again
		}
	
		switch(opcode) {  // find out the opcode and execute it
		case BKPT:   // 0
			/* unconditionally enter the sxx debugger */
			instructions.append("BKPT not implemented\n");
			System.exit(1);
			break;
		case PUSH:   // 1
			/* push(*addr); */
			if (DEBUG == 1) { System.out.println("PUSH " + addr); }
			instructions.append("PUSH " + addr + "\n");
			mem.push(mem.getContents(addr));
			// update tables
			utr.setVal(mem.getSP());
			EventQueue.invokeLater(utr);
			break;
		case PUSHV:  // 2
			/* push(value); */
			if (DEBUG == 1) { System.out.println("PUSHV " + value); }
			instructions.append("PUSHV " + value + "\n");
			mem.push(value);
			// update tables
			utr.setVal(mem.getSP());
			EventQueue.invokeLater(utr);
			break;
		case PUSHS:  // 3
			/* push(*pop()); */
			if (DEBUG == 1) { System.out.println("PUSHS"); }
			instructions.append("PUSHS\n");
			num = mem.pop();
			mem.push(mem.getContents(num));
			// update tables
			utr.setVal(mem.getSP());
			;;
			break;
		case PUSHX:  // 4
			/* push(*(pop()+addr)); */
			if (DEBUG == 1) { System.out.println("PUSHX " + addr); }
			instructions.append("PUSHX\n");
			num = mem.pop()+addr;
			mem.push(mem.getContents(num));
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 1000) { EventQueue.invokeLater(utr); }
			break;
		case POP:    // 5
			/* *addr=pop(); */
			if (DEBUG == 1) { System.out.println("POP " + addr); }
			instructions.append("POP " + addr + "\n");
			temp = mem.pop();
			mem.putContents(addr, temp);
			// update tables
			utr.setVal(addr);
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case POPS:   // 6
			/* temp=pop(); *pop()=temp; */
			if (DEBUG == 1) { System.out.println("POPS"); }
			instructions.append("POPS\n");
			temp = mem.pop();
			num = mem.pop();
			mem.putContents(num, temp);
			// update tables
			utr.setVal(num);
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case POPX:   // 7
			/* temp=pop(); *(pop()+addr)=temp; */
			if (DEBUG == 1) { System.out.println("POPX " + addr); }
			instructions.append("POPX " + addr + "\n");
			temp = mem.pop();
			num = mem.pop()+addr;
			mem.putContents(num, temp);
			// update tables
			utr.setVal(num);
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case DUPL:   // 8
			/* push(*SP); */
			if (DEBUG == 1) { System.out.println("DUPL"); }
			instructions.append("DUPL\n");
			mem.push(mem.getContents(mem.getSP()));
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case SWAP:   // 9
			/* temp=*SP; *SP=*(SP+1); *(SP+1)=temp; */
			if (DEBUG == 1) { System.out.println("SWAP"); }
			instructions.append("SWAP\n");
			temp = mem.getContents(mem.getSP());
			mem.putContents(mem.getSP(), mem.getContents(mem.getSP()+1));
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			mem.putContents(mem.getSP()+1, temp);
			// update tables
			utr.setVal(mem.getSP()+1);
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case OVER:   // 10
			/* push(*(SP+1)); */
			if (DEBUG == 1) { System.out.println("OVER"); }
			instructions.append("OVER\n");
			mem.push(mem.getContents(mem.getSP()+1));
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case DROP:   // 11
			/* SP++; */
			if (DEBUG == 1) { System.out.println("DROP"); }
			instructions.append("DROP\n");
			mem.setSP(mem.getSP()+1);
			break;
		case ROT:    // 12
			/* temp=*SP; *SP=*(SP+2); *(SP+2)=*(SP+1); *(SP+1)=temp; */
			if (DEBUG == 1) { System.out.println("ROT"); }
			instructions.append("ROT\n");
			temp = mem.getContents(mem.getSP());
			mem.putContents(mem.getSP(), mem.getContents(mem.getSP()+2));
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			mem.putContents(mem.getSP()+2, mem.getContents(mem.getSP()+1));
			// update tables
			utr.setVal(mem.getSP()+2);
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			mem.putContents(mem.getSP()+1, temp);
			// update tables
			utr.setVal(mem.getSP()+1);
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case TSTLT:  // 13
			/* TSTLT       --> temp=pop(); push((temp<0)?1:0); */
			if (DEBUG == 1) { System.out.println("TSTLT"); }
			instructions.append("TSTLT\n");
			temp = mem.pop();
			mem.push( (temp < 0) ? 1 : 0 );
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case TSTLE:  // 14
			/* TSTLE       --> temp=pop(); push((temp<=0)?1:0); */
			if (DEBUG == 1) { System.out.println("TSTLE"); }
			instructions.append("TSTLE\n");
			temp = mem.pop();
			mem.push( (temp <= 0) ? 1 : 0 );
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case TSTGT:  // 15
			/* temp=pop(); push((temp>0)?1:0); */
			instructions.append("TSTGT\n");
			if (DEBUG == 1) { System.out.println("TSTGT"); }
			temp = mem.pop();
			mem.push( (temp > 0) ? 1 : 0 );
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case TSTGE:  // 16
			/* temp=pop(); push((temp>=0)?1:0); */
			if (DEBUG == 1) { System.out.println("TSTGE"); }
			instructions.append("TSTGE\n");
			temp = mem.pop();
			mem.push( (temp >= 0) ? 1 : 0 );
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case TSTEQ:  // 17
			/* temp=pop(); push((temp==0)?1:0); */
			if (DEBUG == 1) { System.out.println("TSTEQ"); }
			instructions.append("TSTEQ\n");
			temp = mem.pop();
			mem.push( (temp == 0) ? 1 : 0);
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case TSTNE:  // 18
			/* temp=pop(); push((temp!=0)?1:0); */
			if (DEBUG == 1) { System.out.println("TSTNE"); }
			instructions.append("TSTNE\n");
			temp = mem.pop();
			mem.push( (temp != 0) ? 1 : 0 );
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case BNE:    // 19
			/* if (pop()!=0) PC=addr; */
			if (DEBUG == 1) { System.out.println("BNE " + addr); }
			instructions.append("BNE " + addr + "\n");
			if (mem.pop() != 0) {
				PC = addr;
			}
			break;
		case BEQ:    // 20
			/* if (pop()==0) PC=addr; */
			if (DEBUG == 1) { System.out.println("BEQ " + addr); }
			instructions.append("BEQ " + addr + "\n");
			if (mem.pop() == 0) {
				PC = addr;
			}
			break;
		case BR:     // 21
			/* PC=addr; */
			if (DEBUG == 1) { System.out.println("BR " + addr); }
			instructions.append("BR " + addr + "\n");
			PC = addr;
			break;
		case CALL:   // 22
			/* push(PC); PC=addr; */
			if (DEBUG == 1) { System.out.println("CALL " + addr); }
			instructions.append("CALL " + addr + "\n");
			mem.push(PC);
			PC = addr;
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case CALLS:  // 23
			/* temp=pop(); push(PC); PC=temp; */
			if (DEBUG == 1) { System.out.println("CALLS"); }
			instructions.append("CALLS\n");
			temp = mem.pop();
			mem.push(PC);
			PC = temp;
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case RETURN: // 24
			/* PC=pop(); */
			if (DEBUG == 1) { System.out.println("RETURN"); }
			instructions.append("RETURN\n");
			PC = mem.pop();
			break;
		case RETN:   // 25
			/* temp=pop(); SP += value; PC=temp; */
			if (DEBUG == 1) { System.out.println("RETN " + value); }
			instructions.append("RETURN " + value + "\n");
			temp = mem.pop();
			//if (DEBUG == 1) { System.out.println("RETN:temp = " + temp); }
			mem.setSP(mem.getSP()+value);
			PC = temp;
			break;
		case HALT:   // 26
			/* halt program execution */
			if (DEBUG == 1) { System.out.println("HALT"); }
			instructions.append("HALT\n");
			throw new HaltException();
			//break;
		case ADD:    // 27
			/* temp=pop(); push( pop() + temp ); */
			if (DEBUG == 1) { System.out.println("ADD"); }
			instructions.append("ADD\n");
			temp = mem.pop();
			mem.push(mem.pop()+temp);
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case SUB:    // 28
			/* temp=pop(); push( pop() - temp ); */
			if (DEBUG == 1) { System.out.println("SUB"); }
			instructions.append("SUB\n");
			temp = mem.pop();
			mem.push(mem.pop()-temp);
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case MUL:    // 29
			/* temp=pop(); push( pop() * temp ); */
			if (DEBUG == 1) { System.out.println("MUL"); }
			instructions.append("MUL\n");
			temp = mem.pop();
			mem.push(mem.pop() * temp);
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case DIV:    // 30
			/* temp=pop(); push( pop() / temp ); */
			if (DEBUG == 1) { System.out.println("DIV"); }
			instructions.append("DIV\n");
			temp = mem.pop();
			try {
				mem.push(mem.pop() / temp);
			} catch (ArithmeticException e) {
				errorAndExit("ERROR: Attempt to divide by zero");
			}
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case MOD:    // 31
			/* temp=pop(); push( pop() % temp ); */
			if (DEBUG == 1) { System.out.println("DIV"); }
			instructions.append("MOD\n");
			temp = mem.pop();
			try {
				mem.push(mem.pop() % temp);
			} catch (ArithmeticException e) {
				errorAndExit("ERROR: Attempt to mod by zero");
			}
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case OR:     // 32
			/* temp=pop(); push( pop() || temp ); */
			if (DEBUG == 1) { System.out.println("OR"); }
			instructions.append("OR\n");
			temp = mem.pop();
			mem.push( (mem.pop() != 0 || temp != 0) ? 1 : 0 );
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case AND:    // 33
			/* temp=pop(); push( pop() && temp ); */
			if (DEBUG == 1) { System.out.println("AND"); }
			instructions.append("AND\n");
			temp = mem.pop();
			mem.push( (mem.pop() != 0 && temp != 0) ? 1 : 0 );
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case XOR:    // 34
			/* temp=pop(); push( pop() xor temp ); [see below] */
			if (DEBUG == 1) { System.out.println("XOR"); }
			instructions.append("XOR\n");
			temp = mem.pop();
			mem.push( (mem.pop() != 0 ^ temp != 0) ? 1 : 0 );
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case NOT:    // 35
			/* push( !pop() ); */
			if (DEBUG == 1) { System.out.println("NOT"); }
			instructions.append("NOT\n");
			mem.push( !(mem.pop() != 0) ? 1 : 0 );
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case NEG:    // 36
			/* push( -pop() ); */
			if (DEBUG == 1) { System.out.println("NEG"); }
			instructions.append("NEG\n");
			mem.push( (-1)*mem.pop());
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case ADDX:   // 37
			/* push( pop()+addr ); */
			if (DEBUG == 1) { System.out.println("ADDX " + addr); }
			instructions.append("ADDX " + addr + "\n");
			mem.push(mem.pop() + addr);
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case ADDSP:  // 38
			/* SP += value; */
			if (DEBUG == 1) { System.out.println("ADDSP " + value); }
			instructions.append("ADDSP " + value + "\n");
			mem.setSP(mem.getSP()+value);
			break;
		case READ:   // 39
			/* Beware of the hackiness that lies here. */
			
			// clear out leading whitespace
			inputLine = inputLine.replaceAll("^\\s*", "");
			while (inputLine.equals("")) {
				inputLine = JOptionPane.showInputDialog(null, "SXX");
				// user clicks ``cancel"
				if (inputLine == null) {
					throw new InputMismatchException();
				}
				inputLine += "\n";
				// simulate the user actually typing this in
				System.out.print(inputLine);
				inputLine = inputLine.replaceAll("^\\s*", "");
			}
			m = digit.matcher(inputLine);
			if (! m.find()) {
				throw new InputMismatchException();
			}
			String digits = m.group();
			temp = Integer.parseInt(digits);
			if (DEBUG == 1) { System.out.println("READ ["+temp+"]"); }
			instructions.append("READ ["+temp+"]\n");
			inputLine = inputLine.replaceAll("^[+-]?\\d+", "");
			mem.push(temp);
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case PRINT:  // 40
			/* print pop() in %d format */
			temp = mem.pop();
			if (DEBUG == 1) { System.out.println("PRINT ["+temp+"]"); }
			instructions.append("PRINT ["+temp+"]\n");
			System.out.print(temp);
			break;
		case READC:  // 41
			/* read temp in %c format; push(temp); */
			if (inputLine.equals("")) {
				// collect line from the user
				inputLine = JOptionPane.showInputDialog(null, "SXX");			
				// user clicks ``cancel"
				if (inputLine == null) {
					temp = -1;
					break;
				}
				// stick on a newline to mimic STDIN
				inputLine += "\n";
				System.out.print(inputLine);
			}
			// get the next character
			temp = Read.READC(new Scanner(inputLine));
			if (DEBUG == 1) { System.out.println("READC ["+temp+"]"); }
			instructions.append("READC ["+temp+"]\n");
			mem.push(temp);
			// the character is consumed
			inputLine = inputLine.substring(1);
			// update tables
			utr.setVal(mem.getSP());
			if (sm.speed != 0) { EventQueue.invokeLater(utr); }
			break;
		case PRINTC: // 42
			/* print pop() in %c format */
			temp = mem.pop();
			if (DEBUG == 1) { System.out.println("PRINTC ["+temp+"]"); }
			instructions.append("PRINTC ["+temp+"]\n");
			System.out.print((char)temp);
			break;
		case TRON:   // 43
			/* turn on trace feature */
			if (DEBUG == 1) { System.out.println("TRON"); }
			instructions.append("TRON\n");
			TRACE = true;
			break;
		case TROFF:  // 44
			/* turn off trace feature */
			if (DEBUG == 1) { System.out.println("TROFF"); }
			instructions.append("TROFF\n");
			TRACE = false;
			break;
		case DUMP:   // 45
			/* temp=pop(); dump memory from pop() to temp; */
			if (DEBUG == 1) { System.out.println("DUMP"); }
			instructions.append("DUMP\n");
			temp = mem.pop();
			dump(mem.pop(), temp);
			break;
		default:  // not a valid opcode
			System.err.println("ERROR: Invalid opcode");
			System.err.println("  " + opcode);
			System.exit(1);
		}

		// only update tables if we are not at the maximum speed
		if (sm.speed != 0) {
			// update more table entries and the SP/PC label
			uspr.setPC(PC);
			EventQueue.invokeLater(uspr);

			// update registers in the northeast text field
			EventQueue.invokeLater(urr);
		}
	}

	/**
	 * Returns true if opcode requires a parameter.
	 *
	 * @param opcode  the opcode you wish to determine if requres a parameter
	 *
	 */
	public boolean opcodeRequiresParameter(int opcode) {
		/* Informs the caller whether the current opcode needs to look at the
		 * next location in memory for an argument to the instruction. */
		boolean requiresParameter;

		switch (opcode) {
		case PUSH:
		case PUSHV:
		case PUSHX:
		case POP:
		case POPX:
		case BNE:
		case BEQ:
		case BR:
		case CALL:
		case RETN:
		case ADDX:
		case ADDSP:
			requiresParameter = true;
			break;
		default:
			requiresParameter = false;
			break;
		}

		return requiresParameter;
	}

	/**
	 * Dump memory from pop to temp.
	 *
	 * @param pop  beginning address
	 * @param temp ending address
	 *
	 */
	public void dump(int pop, int temp) {
		if (pop < temp || 0 > pop || pop > mem.memorySize-1
				|| 0 > temp || temp > mem.memorySize-1) {
			errorAndExit("ERROR: Illegal dump range");
		}
		for (; pop >= temp; pop--)
			System.out.println(mem.getContents(pop));
	}

	/**
	 * Print error message and exit unsuccessfully.
	 * 
	 * @param error  the error message that is sent to STDERR
	 */
	private void errorAndExit(String error) {
		System.err.println(error);
		System.exit(1);
	}
	
	/* mneumonics and their opcodes */
	public static final int BKPT   =   0;
	public static final int PUSH   =   1;
	public static final int PUSHV  =   2;
	public static final int PUSHS  =   3;
	public static final int INDIR  =   3;
	public static final int PUSHX  =   4;
	public static final int POP    =   5;
	public static final int POPS   =   6;
	public static final int POPX   =   7;
	public static final int DUPL   =   8;
	public static final int SWAP   =   9;
	public static final int OVER   =  10;
	public static final int DROP   =  11;
	public static final int ROT    =  12;
	public static final int TSTLT  =  13;
	public static final int TSTLE  =  14;
	public static final int TSTGT  =  15;
	public static final int TSTGE  =  16;
	public static final int TSTEQ  =  17;
	public static final int TSTNE  =  18;
	public static final int BNE    =  19;
	public static final int BT     =  19;
	public static final int BEQ    =  20;
	public static final int BF     =  20;
	public static final int BR     =  21;
	public static final int CALL   =  22;
	public static final int CALLS  =  23;
	public static final int RETURN =  24;
	public static final int POPPC  =  24;
	public static final int RETN   =  25;
	public static final int HALT   =  26;
	public static final int ADD    =  27;
	public static final int SUB    =  28;
	public static final int MUL    =  29;
	public static final int DIV    =  30;
	public static final int MOD    =  31;
	public static final int OR     =  32;
	public static final int AND    =  33;
	public static final int XOR    =  34;
	public static final int NOT    =  35;
	public static final int NEG    =  36;
	public static final int ADDX   =  37;
	public static final int ADDSP  =  38;
	public static final int READ   =  39;
	public static final int PRINT  =  40;
	public static final int READC  =  41;
	public static final int PRINTC =  42;
	public static final int TRON   =  43;
	public static final int TROFF  =  44;
	public static final int DUMP   =  45;
	
}