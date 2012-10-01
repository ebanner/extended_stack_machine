import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.io.Reader;
import java.io.BufferedReader;
import java.util.regex.Pattern;
import java.util.Random;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;

// TODO: Figure out why unsax.sxx isn't working.

public class Driver {

    // this is *the* master stack in the stack machine emulator
    public static final Stack stack = new Stack(16000);
    // values used to help compute the values of instructions
    public static int temp, t1, t2;
    // TRACE mode can either be ON of OFF
    public static boolean TRACE;
    // Scanner on STDIN is used for READ and READC commands
    public static Scanner in = null;
    // if oldStyle is true, then we are using the legacy opcode numbering
    // system
    public static boolean oldStyle;
    public static int length;  // this is only around for DEBUGging purposes
    public static int DEBUG = 1;

    public static void main(String[] args) throws IOException {
        // main loads the SXX machine code into memory and executes it

        // base address starts somewhere between 15 and 1000 exclusive
        //int baseAddr = new Random().nextInt(984) + 16;
        int baseAddr = 16;
        String file = args[0];
        if (DEBUG == 1) {  System.out.println("Base address: " + baseAddr); }

        // insert opcodes and data into the stack machine and perform
        // relocation process
        int entryPoint = initializeStack(baseAddr, file);
        if (DEBUG == 1) { System.out.println("Entry point: " + entryPoint); }
        int PC = baseAddr + entryPoint;
        
        // open up a scanner to read future input
        in = new Scanner(System.in); 
        while (true) { // execute opcodes
            PC = executeInstruction(PC);
        }
    }

    public static int initializeStack(int nextFreeAddr, String file) {
        // This method [1] parses the header of the SXX program and then 
        // [2] loads opcodes and data into memory.
        // The return value is the entry point provided by the SXX header.
        
        int baseAddr = nextFreeAddr;  // keep the base address around
        Scanner sc = null;
        try {  // open up a new scanner on the source file
            sc = new Scanner(new BufferedReader(new FileReader(file)));
        } catch (Exception e) {
            errorAndExit("ERROR: " + e.getMessage());
        }
                                                                          
        // ############## BEGIN PARSE HEADER ##################
        // a Header object's purpose is to parse the header of the SXX
        // program and hold useful information
        Header header = new Header(sc); 
        // parse the header and give us back the scanner where the header ends
        sc = header.parseHeader();
        // the header will tell us if we're using old style opcode numbering
        oldStyle = header.oldStyle;

        if (0 > header.length || 
                header.length > stack.height-baseAddr) {
            // make sure the length is in range
            System.err.println("Illegal length: Out of range");
            System.err.println("  " + header.length);
            System.exit(1);
        }
        length = header.length;  // make header global for DEBUGging
        if (DEBUG == 1) { System.out.println("Length: " + length); }
        // ################ END PARSE HEADER #################
        
        // ############ BEGIN INSERTING OPCODES ##############
        String opcode = null;
        // keep track of how many words have been inserted into memory
        int words = 0; 
        int currFreeAddr;
        while (sc.hasNextLine() && words <= header.length) {
            // read the next opcode and trim off whitespace
            opcode = sc.nextLine().trim();

            if (isCommentOrBlankLine(opcode)) {
                continue;
            } else if (opcode.charAt(0) == '%') {
                break;  // move onto the relocation process
            } else {
                // keep current address around so we can tell after how many
                // memory cells we consumed
                currFreeAddr = nextFreeAddr;
                // insert the opcode/data into the stack machine
                // or skip over memory cells if the opcode begins with `:'
                nextFreeAddr = insertOpcode(nextFreeAddr, opcode);
                // update the number of words that have been inserted into
                // memory
                words += nextFreeAddr - currFreeAddr;
            }
        } 
        // ############### END INSERTING OPCODES ################

        // ############# BEGIN RELOCATION PROCESS ###############
        while (sc.hasNextLine()) {  
            opcode = sc.nextLine().trim();

            if (isCommentOrBlankLine(opcode)) {
                continue;
            } else if (opcode.charAt(0) == '%') {
                break;  // we're done reading the SXX program
            } else{
                insertRelocation(baseAddr, opcode);
            }
        } // ############## END RELOCATION PROCESS ################

        try { }
        finally {  // close the scanner
            if (sc != null)
                sc.close();
        }
        return header.entry;
    }
    
    public static int insertOpcode(int nextFreeAddr, String instr) {
        // inserts the opcode of the next instruction or skips over a number of
        // memory cells if a line staring with `:' is encountered
                                                                                
        String number = "^-?(\\d)+$";
        String colonInstruction = "^:(\\d)+$";
                                                                                
        if (Pattern.matches(number, instr)) {
            // we now know that we have a digit
            stack.putContents(nextFreeAddr, Integer.parseInt(instr));
            nextFreeAddr++;
        } else if (Pattern.matches(colonInstruction, instr)) {
            nextFreeAddr += Integer.parseInt(instr.split(":")[1]);
        } else {
            System.err.println("ERROR: Not a valid line:");
            System.err.println("  " + instr);
            System.exit(1);
        }

        return nextFreeAddr;
    }

    public static void insertRelocation(int baseAddr, String address) {
        // insert into the current memory address the following:
        //     *(baseAddr+addresss) + baseAddr
        
        String number = "^-?(\\d)+$";
        if (! Pattern.matches(number, address)) {
            System.err.println("ERROR: Not a relocation address:");
            System.err.println("  " + address);
            System.exit(1);
        } else {
            int addr = Integer.parseInt(address) + baseAddr;
            stack.putContents(addr, stack.getContents(addr)+baseAddr);
        }
    }

    public static int executeInstruction(int PC) {
        int opcode = stack.getContents(PC);
        PC++;  // increment PC immediately
        if (oldStyle == true && opcode > 22) {
            // support legacy opcode numbering convention
            opcode++;
        }
        
        // for DEBUGGING!
        //printDEBUG(opcode, length, PC, temp);  
        
        int addr, value, num; 
        addr = value = num = 0;

        if (instructionRequiresParameter(opcode)) {
            // if it's an instruction that needs an `addr' or `value'
            // parameter, save that argument
            addr = value = stack.getContents(PC);
            PC++;  // increment PC again
        }

        switch(opcode) {
            case BKPT:   // 0
                /* unconditionally enter the sxx DEBUGger */
                System.err.println("Unimplemented operation");
                System.exit(1);
                break;
            case PUSH:   // 1
                /* push(*addr); */
                stack.push(stack.getContents(addr));
                if (DEBUG == 1) { System.out.println("PUSH " + addr); }
                break;
            case PUSHV:  // 2
                /* push(value); */
                stack.push(value);
                if (DEBUG == 1) { System.out.println("PUSHV " + value); }
                break;
            case PUSHS:  // 3
                /* push(*pop()); */
                num = stack.pop();
                stack.push(stack.getContents(num));
                if (DEBUG == 1) { System.out.println("PUSHS "); }
                break;
            case PUSHX:  // 4
                /* push(*(pop()+addr)); */
                num = stack.pop()+addr;
                stack.push(stack.getContents(num));
                if (DEBUG == 1) { System.out.println("PUSHX " + addr); }
                break;
            case POP:    // 5
                /* *addr=pop(); */
                stack.putContents(addr, stack.pop());
                if (DEBUG == 1) { System.out.println("POP " + addr); }
                break;
            case POPS:   // 6
                /* temp=pop(); *pop()=temp; */
                temp = stack.pop();
                num = stack.pop();
                stack.putContents(num, temp);
                if (DEBUG == 1) { System.out.println("POPS"); }
                break;
            case POPX:   // 7
                /* temp=pop(); *(pop()+addr)=temp; */
                temp = stack.pop();
                num = stack.pop()+addr;
                stack.putContents(num, temp);
                if (DEBUG == 1) { System.out.println("POPX " + addr); }
                break;
            case DUPL:   // 8
                /* push(*SP); */
                stack.push(stack.getContents(stack.SP));
                if (DEBUG == 1) { System.out.println("DUPL"); }
                break;
            case SWAP:   // 9
                /* temp=*SP; *SP=*(SP+1); *(SP+1)=temp; */
                temp = stack.getContents(stack.SP);
                stack.putContents(stack.SP, stack.getContents(stack.SP+1));
                stack.putContents(stack.SP+1, temp);
                if (DEBUG == 1) { System.out.println("SWAP"); }
                break;
            case OVER:   // 10
                /* push(*(SP+1)); */
                stack.push(stack.getContents(stack.SP+1));
                if (DEBUG == 1) { System.out.println("OVER"); }
                break;
            case DROP:   // 11
                /* SP++; */
                stack.SP++;
                if (DEBUG == 1) { System.out.println("DROP"); }
                break;
            case ROT:    // 12
                /* temp=*SP; *SP=*(SP+2); *(SP+2)=*(SP+1); *(SP+1)=temp; */
                temp = stack.getContents(stack.SP);
                stack.putContents(stack.SP, stack.getContents(stack.SP+2));
                stack.putContents(stack.SP+2, stack.getContents(stack.SP+1));
                stack.putContents(stack.SP+1, temp);
                if (DEBUG == 1) { System.out.println("ROT"); }
                break;
            case TSTLT:  // 13
                /* TSTLT       --> temp=pop(); push((temp<0)?1:0); */
                temp = stack.pop();
                stack.push( (temp < 0) ? 1 : 0 );
                if (DEBUG == 1) { System.out.println("TSTLT"); }
                break;
            case TSTLE:  // 14
                /* TSTLE       --> temp=pop(); push((temp<=0)?1:0); */
                temp = stack.pop();
                stack.push( (temp <= 0) ? 1 : 0 );
                if (DEBUG == 1) { System.out.println("TSTLE"); }
                break;
            case TSTGT:  // 15
                /* temp=pop(); push((temp>0)?1:0); */
                temp = stack.pop();
                stack.push( (temp > 0) ? 1 : 0 );
                if (DEBUG == 1) { System.out.println("TSTGT"); }
                break;
            case TSTGE:  // 16
                /* temp=pop(); push((temp>=0)?1:0); */
                temp = stack.pop();
                stack.push( (temp >= 0) ? 1 : 0 );
                if (DEBUG == 1) { System.out.println("TSTGE"); }
                break;
            case TSTEQ:  // 17
                /* temp=pop(); push((temp==0)?1:0); */
                temp = stack.pop();
                stack.push( (temp == 0) ? 1 : 0);
                if (DEBUG == 1) { System.out.println("TSTEQ"); }
                break;
            case TSTNE:  // 18
                /* temp=pop(); push((temp!=0)?1:0); */
                temp = stack.pop();
                stack.push( (temp != 0) ? 1 : 0 );
                if (DEBUG == 1) { System.out.println("TSTNE"); }
                break;
            case BNE:    // 19
                /* if (pop()!=0) PC=addr; */
                if (stack.pop() != 0) {
                    PC = addr;
                }
                if (DEBUG == 1) { System.out.println("BNE " + addr); }
                break;
            case BEQ:    // 20
                /* if (pop()==0) PC=addr; */
                if (stack.pop() == 0) {
                    PC = addr;
                }
                if (DEBUG == 1) { System.out.println("BEQ " + addr); }
                break;
            case BR:     // 21
                /* PC=addr; */
                PC = addr;
                if (DEBUG == 1) { System.out.println("BR " + addr); }
                break;
            case CALL:   // 22
                /* push(PC); PC=addr; */
                stack.push(PC);
                PC = addr;
                if (DEBUG == 1) { System.out.println("CALL " + addr); }
                break;
            case CALLS:  // 23
                /* temp=pop(); push(PC); PC=temp; */
                temp = stack.pop();
                stack.push(PC);
                PC = temp;
                if (DEBUG == 1) { System.out.println("CALLS"); }
                break;
            case RETURN: // 24
                /* PC=pop(); */
                PC = stack.pop();
                if (DEBUG == 1) { System.out.println("RETURN"); }
                break;
            case RETN:   // 25
                /* temp=pop(); SP += value; PC=temp; */
                temp = stack.pop();
                stack.SP += value;
                PC = temp;
                if (DEBUG == 1) { System.out.println("RETN " + value); }
                break;
            case HALT:   // 26
                /* halt program execution */
                //System.out.println("Halting program execution");
                //stack.reveal();
                //System.out.format("%nPC: %d  temp: %d%n", PC-1, temp);
                if (DEBUG == 1) { System.out.println("HALT"); }
                System.exit(0);
                break;
            case ADD:    // 27
                /* temp=pop(); push( pop() + temp ); */
                temp = stack.pop();
                stack.push(stack.pop()+temp);
                if (DEBUG == 1) { System.out.println("ADD"); }
                break;
            case SUB:    // 28
                /* temp=pop(); push( pop() - temp ); */
                temp = stack.pop();
                stack.push(stack.pop()-temp);
                if (DEBUG == 1) { System.out.println("SUB"); }
                break;
            case MUL:    // 29
                /* temp=pop(); push( pop() * temp ); */
                temp = stack.pop();
                stack.push(stack.pop() * temp);
                if (DEBUG == 1) { System.out.println("MUL"); }
                break;
            case DIV:    // 30
                /* temp=pop(); push( pop() / temp ); */
                temp = stack.pop();
                try {
                    stack.push(stack.pop() / temp);
                } catch (ArithmeticException e) {
                    errorAndExit("ERROR: Attempt to divide by zero");
                }
                break;
            case MOD:    // 31
                /* temp=pop(); push( pop() % temp ); */
                temp = stack.pop();
                try {
                    stack.push(stack.pop() % temp);
                } catch (ArithmeticException e) {
                    errorAndExit("ERROR: Attempt to mod by zero");
                }
                if (DEBUG == 1) { System.out.println("DIV"); }
                break;
            case OR:     // 32
                /* temp=pop(); push( pop() || temp ); */
                temp = stack.pop();
                stack.push( (stack.pop() != 0 || temp != 0) ? 1 : 0 );
                if (DEBUG == 1) { System.out.println("OR"); }
                break;
            case AND:    // 33
                /* temp=pop(); push( pop() && temp ); */
                temp = stack.pop();
                stack.push( (stack.pop() != 0 && temp != 0) ? 1 : 0 );
                if (DEBUG == 1) { System.out.println("AND"); }
                break;
            case XOR:    // 34
                /* temp=pop(); push( pop() xor temp ); [see below] */
                temp = stack.pop();
                stack.push( (stack.pop() != 0 ^ temp != 0) ? 1 : 0 );
                //stack.push( (!(t1 != 0 && t2 != 0) 
                //            && (t1 != 0 || t2 != 0)) ? 1 : 0 );
                if (DEBUG == 1) { System.out.println("XOR"); }
                break;
            case NOT:    // 35
                /* push( !pop() ); */
                stack.push( !(stack.pop() != 0) ? 1 : 0 );
                if (DEBUG == 1) { System.out.println("NOT"); }
                break;
            case NEG:    // 36
                /* push( -pop() ); */
                stack.push( (-1)*stack.pop());
                if (DEBUG == 1) { System.out.println("NEG"); }
                break;
            case ADDX:   // 37
                /* push( pop()+addr ); */
                stack.push(stack.pop() + addr);
                if (DEBUG == 1) { System.out.println("ADDX " + addr); }
                break;
            case ADDSP:  // 38
                /* SP += value; */
                stack.SP += value;
                if (DEBUG == 1) { System.out.println("ADDSP " + value); }
                break;
            case READ:   // 39
                temp = Read.READ(in);
                stack.push(temp);
                if (DEBUG == 1) { System.out.println("READ"); }
                break;
            case PRINT:  // 40
                /* print pop() in %d format */
                System.out.print(stack.pop());
                if (DEBUG == 1) { System.out.println("PRINT"); }
                break;
            case READC:  // 41
                /* read temp in %c format; push(temp); */
                temp = Read.READC(in);
                stack.push(temp);
                if (DEBUG == 1) { System.out.println("READC"); }
                break;
            case PRINTC: // 42
                /* print pop() in %c format */
                System.out.print((char)stack.pop());
                if (DEBUG == 1) { System.out.println("PRINTC"); }
                break;
            case TRON:   // 43
                /* turn on trace feature */
                TRACE = true;
                if (DEBUG == 1) { System.out.println("TRON"); }
                break;
            case TROFF:  // 44
                /* turn off trace feature */
                TRACE = false;
                if (DEBUG == 1) { System.out.println("TROFF"); }
                break;
            case DUMP:   // 45
                /* temp=pop(); dump memory from pop() to temp; */
                temp = stack.pop();
                dump(stack.pop(), temp);
                if (DEBUG == 1) { System.out.println("DUMP"); }
                break;
            default:
                if (DEBUG == 1) { printDebug(opcode, length, PC, temp); }
                System.err.println("ERROR: Invalid opcode");
                System.err.println("  " + opcode);
                System.exit(1);
        }

        // update SP 
        stack.putContents(0, stack.SP);

        // return the new PC
        return PC;
    }

    public static void dump(int pop, int temp) {
        // dumps memory in descending order
        // pop MUST be greater than or equal to temp
        if (pop < temp || 0 > pop || pop > stack.height-1
                || 0 > temp || temp > stack.height-1) {
            errorAndExit("ERROR: Illegal dump range");
        }
        for (; pop >= temp; pop--)
            System.out.println(stack.getContents(pop));
    }

    public static void putTestValuesOnStack() {
        for (int i = 25; i < 45; i++)
            stack.push(i-4);
        //stack.putContents(31, 49);
        //stack.putContents(30, 30);
    }

    public static boolean instructionRequiresParameter(int opcode) {
        // informs the caller whether the current opcode needs to look at the
        // next location in memory for an argument to the instruction
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

    public static void printInstructions(int length, int PC) {
        for (int pointer = 16+length-1; pointer >= 16; pointer--)
            System.out.format("%2d| %2s%s\n", pointer, stack.getContents(pointer), 
                    (pointer == PC) ? " <-- PC" : "");
        System.out.println();
    }

    public static boolean isCommentOrBlankLine(String line) {
        if (line.isEmpty() || line.charAt(0) == '#')
            return true;
        else
            return false;
    }

    public static void errorAndExit(String error) {
        System.err.println(error);
        System.exit(1);
    }

    public static void printDebug(int opcode, int length, int PC, int temp) {
        System.out.println("About to execute opcode: " + opcode);
        stack.reveal();
        System.out.println("\nInstructions:");
        printInstructions(length, PC-1);
        System.out.format("PC: %d  temp: %d%n%n", PC-1, temp); 
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
