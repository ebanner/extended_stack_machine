import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;

/* TODO: Support `:' for skipping over memory cells
 *       Support `%' to denote the different sections in a program
 */

public class Driver {

    /* this is *the* master stack in the stack machine emulator */
    public static final Stack stack = new Stack(50);  
    /* values used to help compute the values of instructions */
    public static int temp, t1, t2;
    /* TRACE mode can either be ON of OFF */
    public static boolean TRACE;
    /* scanner is used for READ and READC commands */
    public static Scanner in = null;
    // scanner used for parsing the source file
    public static Scanner sc = null;

    public static void main(String[] args) throws IOException {
        /* assume for now that opcodes start at position 16 */
        int baseAddr = 16;
        String file = args[0];

        /* throw the opcodes onto the stack */
        initializeStack(baseAddr, file);
        putTestValuesOnStack();  /* DEBUGGING */

        int PC = baseAddr;
        stack.reveal();
        System.out.format("%nPC: %d  temp: %d%n%n", PC, temp);
        while (stack.getContents(PC) != HALT) {
            // keep executing instructions until a HALT command is reached
            PC = executeInstruction(PC);
            stack.reveal();
            System.out.format("%nPC: %d  temp: %d%n", PC, temp);
        }

        /* reveal the stack at the end for a great surprise! */
        //stack.reveal();
        //System.out.format("%nPC: %d  temp: %d%n", PC, temp);
        //printMapping();
        //executeInstruction(PC);
    }

    public static void initializeStack(int nextFreeAddr, String file) throws IOException {
        // this method
        //   1. parses the header
        //   2. inserts opcodes and data into memory cells in the stack machine
        //   3. 

        file = "tests/" + file;
        try {  // open up a new scanner on the source file
            sc = new Scanner(new BufferedReader(new FileReader(file)));
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
                                                                          
        // a `Header' object's sole purpose is to parse a header and hold
        // information about a header that we find useful
        Header header = new Header(sc); 
        sc = header.parseHeader();

        String instruction = null;

        try {  /* open up a scanner and start reading all lines of input */
            in = new Scanner(System.in);

            while (sc.hasNextLine()) {  /* grab the next instruction */
                /* trim off whitespace to the left and right */
                instruction = sc.nextLine().trim();

                if (instruction.isEmpty()) {
                    // skip blank lines
                    continue;
                } else if (Pattern.matches(".*#.*", instruction)) {
                    /* if the line contains a comment, 
                    /* grab the part of the string before the `;' and trim off
                     * excess whitespce */
                    instruction = instruction.split("#")[0].trim();
                    if (instruction.isEmpty())
                        continue;
                }

                // put the instruction's opcode on the stack so PC to read it
                // later
                insertOpcode(nextFreeAddr, instruction);
                System.out.println(instruction);

                /* point nextFreeAddr to the next free position on the stack to stick
                 * the next opcode */
                nextFreeAddr++;
            }
        } finally {
            // close the scanners
            if (sc != null)
                sc.close();
        }
    }

    public static int executeInstruction(int PC) {
        int opcode = stack.getContents(PC);
        int addr, value, num; 
        addr = value = num = 0;

        if (instructionRequiresParameter(opcode)) {
            // if it's an instruction that needs an `addr' or `value'
            // parameter, save that argument
            addr = value = stack.getContents(PC+1);
        }

        switch(opcode) {
            case BKPT:   // 0
                /* unconditionally enter the sxx debugger */
                pass(BKPT);
                PC++;
                break;
            case PUSH:   // 1
                /* push(*addr); */
                ensureValidity(addr);
                stack.push(stack.getContents(addr));
                PC += 2;
                break;
            case PUSHV:  // 2
                /* push(value); */
                stack.push(value);
                PC += 2;
                break;
            case PUSHS:  // 3
                /* push(*pop()); */
                num = stack.pop();
                ensureValidity(num);
                stack.push(stack.getContents(num));
                PC++;
                break;
            case PUSHX:  // 4
                /* push(*(pop()+addr)); */
                num = stack.pop()+addr;
                ensureValidity(num);
                stack.push(stack.getContents(num));
                PC += 2;
                break;
            case POP:    // 5
                /* *addr=pop(); */
                ensureValidity(addr);
                stack.putContents(addr, stack.pop());
                PC += 2;
                break;
            case POPS:   // 6
                /* temp=pop(); *pop()=temp; */
                temp = stack.pop();
                num = stack.pop();
                ensureValidity(num);
                stack.putContents(num, temp);
                PC++;
                break;
            case POPX:   // 7
                /* temp=pop(); *(pop()+addr)=temp; */
                temp = stack.pop();
                num = stack.pop()+addr;
                ensureValidity(num);
                stack.putContents(num, temp);
                PC += 2;
                break;
            case DUPL:   // 8
                /* push(*SP); */
                ensureValidity(stack.SP, "SP");
                stack.push(stack.getContents(stack.SP));
                PC++;
                break;
            case SWAP:   // 9
                /* temp=*SP; *SP=*(SP+1); *(SP+1)=temp; */
                ensureValidity(stack.SP, "SP");
                temp = stack.getContents(stack.SP);
                ensureValidity(stack.SP+1);
                stack.putContents(stack.SP, stack.getContents(stack.SP+1));
                stack.putContents(stack.SP+1, temp);
                PC++;
                break;
            case OVER:   // 10
                /* push(*(SP+1)); */
                ensureValidity(stack.SP+1);
                stack.push(stack.getContents(stack.SP+1));
                PC++;
                break;
            case DROP:   // 11
                /* SP++; */
                stack.SP++;
                PC++;
                break;
            case ROT:    // 12
                /* temp=*SP; *SP=*(SP+2); *(SP+2)=*(SP+1); *(SP+1)=temp; */
                ensureValidity(stack.SP, "SP");
                temp = stack.getContents(stack.SP);
                ensureValidity(stack.SP+2);
                stack.putContents(stack.SP, stack.getContents(stack.SP+2));
                ensureValidity(stack.SP+1);
                stack.putContents(stack.SP+2, stack.getContents(stack.SP+1));
                stack.putContents(stack.SP+1, temp);
                PC++;
                break;
            case TSTLT:  // 13
                /* TSTLT       --> temp=pop(); push((temp<0)?1:0); */
                temp = stack.pop();
                stack.push( (temp < 0) ? 1 : 0 );
                PC++;
                break;
            case TSTLE:  // 14
                /* TSTLE       --> temp=pop(); push((temp<=0)?1:0); */
                temp = stack.pop();
                stack.push( (temp <= 0) ? 1 : 0 );
                PC++;
                break;
            case TSTGT:  // 15
                /* temp=pop(); push((temp>0)?1:0); */
                temp = stack.pop();
                stack.push( (temp > 0) ? 1 : 0 );
                PC++;
                break;
            case TSTGE:  // 16
                /* temp=pop(); push((temp>=0)?1:0); */
                temp = stack.pop();
                stack.push( (temp >= 0) ? 1 : 0 );
                PC++;
                break;
            case TSTEQ:  // 17
                /* temp=pop(); push((temp==0)?1:0); */
                temp = stack.pop();
                stack.push( (temp == 0) ? 1 : 0);
                PC++;
                break;
            case TSTNE:  // 18
                /* temp=pop(); push((temp!=0)?1:0); */
                temp = stack.pop();
                stack.push( (temp != 0) ? 1 : 0 );
                PC++;
                break;
            case BNE:    // 19
                /* if (pop()!=0) PC=addr; */
                ensureValidity(addr, "PC");
                if (stack.pop() != 0) {
                    PC = addr;
                } else {
                    PC++;
                }
                break;
            case BEQ:    // 20
                /* if (pop()==0) PC=addr; */
                ensureValidity(addr, "PC");
                if (stack.pop() == 0) {
                    PC = addr;
                } else {
                    PC++;
                }
                break;
            case BR:     // 21
                /* PC=addr; */
                ensureValidity(addr, "PC");
                PC = addr;
                break;
            case CALL:   // 22
                /* push(PC); PC=addr; */
                stack.push(PC);
                ensureValidity(addr, "PC");
                PC = addr;
                break;
            case CALLS:  // 23
                /* temp=pop(); push(PC); PC=temp; */
                temp = stack.pop();
                stack.push(PC);
                ensureValidity(temp, "PC");
                PC = temp;
                break;
            case RETURN: // 24
                /* PC=pop(); */
                num = stack.pop();
                ensureValidity(num, "PC");
                PC = num;
                break;
            case RETN:   // 25
                /* temp=pop(); SP += value; PC=temp; */
                temp = stack.pop();
                stack.SP += value;
                ensureValidity(temp, "PC");
                PC = temp;
                break;
            case HALT:   // 26
                /* halt program execution */
                System.out.println("Halting program execution");
                System.exit(0);
                break;
            case ADD:    // 27
                /* temp=pop(); push( pop() + temp ); */
                temp = stack.pop();
                stack.push(stack.pop()+temp);
                PC++;
                break;
            case SUB:    // 28
                /* temp=pop(); push( pop() - temp ); */
                temp = stack.pop();
                stack.push(stack.pop()-temp);
                PC++;
                break;
            case MUL:    // 29
                /* temp=pop(); push( pop() * temp ); */
                temp = stack.pop();
                stack.push(stack.pop() * temp);
                PC++;
                break;
            case DIV:    // 30
                /* temp=pop(); push( pop() / temp ); */
                temp = stack.pop();
                try {
                    stack.push(stack.pop() / temp);
                } catch (ArithmeticException e) {
                    System.err.println("ERROR 1: Attempt to divide by zero");
                    System.exit(1);
                }
                PC++;
                break;
            case MOD:    // 31
                /* temp=pop(); push( pop() % temp ); */
                temp = stack.pop();
                try {
                    stack.push(stack.pop() % temp);
                } catch (ArithmeticException e) {
                    System.err.println("ERROR 1: Attempt to mod by zero");
                    System.exit(1);
                }
                PC++;
                break;
            case OR:     // 32
                /* temp=pop(); push( pop() || temp ); */
                temp = stack.pop();
                stack.push( (stack.pop() != 0 || temp != 0) ? 1 : 0 );
                PC++;
                break;
            case AND:    // 33
                /* temp=pop(); push( pop() && temp ); */
                temp = stack.pop();
                stack.push( (stack.pop() != 0 && temp != 0) ? 1 : 0 );
                PC++;
                break;
            case XOR:    // 34
                /* temp=pop(); push( pop() xor temp ); [see below] */
                t1 = stack.pop();
                t2 = stack.pop();
                stack.push( (!(t1 != 0 && t2 != 0) 
                            && (t1 != 0 || t2 != 0)) ? 1 : 0 );
                PC++;
                break;
            case NOT:    // 35
                /* push( !pop() ); */
                stack.push( !(stack.pop() != 0) ? 1 : 0 );
                PC++;
                break;
            case NEG:    // 36
                /* push( -pop() ); */
                stack.push( -1*stack.pop());
                PC++;
                break;
            case ADDX:   // 37
                /* push( pop()+addr ); */
                stack.push(stack.pop() + addr);
                PC += 2;
                break;
            case ADDSP:  // 38
                /* SP += value; */
                stack.SP += value;
                PC += 2;
                break;
            case READ:   // 39
                /* read temp in %d format; push(temp); */
                try {
                    temp = in.nextInt();
                } catch (Exception e) {
                    if (e instanceof InputMismatchException) {
                        System.err.println("ERROR 8: Illegal integer on READ");
                        System.exit(8);
                    } else if (e instanceof NoSuchElementException) {
                        System.err.println("ERROR 7: Attempted to READ past end of file");
                        System.exit(7);
                    } else {
                        System.err.println("Didn't account for this excpetion.");  
                        System.err.println("Please report this bug to Edward ");
                        System.err.println("Banner at edward.banner@gmail.com");
                        System.exit(42);
                    }
                }
                stack.push(temp);
                PC++;
                break;
            case PRINT:  // 40
                /* print pop() in %d format */
                System.out.println(stack.pop());
                PC++;
                break;
            case READC:  // 41
                /* read temp in %c format; push(temp); */
                temp = in.nextLine().charAt(0);
                stack.push(temp);
                PC++;
                break;
            case PRINTC: // 42
                /* print pop() in %c format */
                System.out.println((char)stack.pop());
                PC++;
                break;
            case TRON:   // 43
                /* turn on trace feature */
                TRACE = true;
                PC++;
                break;
            case TROFF:  // 44
                /* turn off trace feature */
                TRACE = false;
                PC++;
                break;
            case DUMP:   // 45
                /* temp=pop(); dump memory from pop() to temp; */
                temp = stack.pop();
                dump(stack.pop(), temp);
                PC++;
                break;
        }

        /* update SP */
        stack.putContents(0, stack.SP);
        ensureValidity(stack.SP, "SP");

        ensureValidity(PC, "PC");
        // return the new PC
        return PC;
    }

    public static void insertOpcode(int nextFreeAddr, String instr) {
        /* inserts the opcode of the next instruction in the user's program
         * into the next available free loaction (growing upwards) */

        if (! Pattern.matches("^-?(\\d)+$", instr)) {
            /* test to see if the next value is a digit */
            System.out.println("Not a digit:");
            System.out.println("  " + instr);
            System.exit(1);
        }

        /* we now know that we have a digit */
        stack.putContents(nextFreeAddr, Integer.parseInt(instr));
    }

    public static void pass(int op) {
        System.out.println("/\\/" + op + "\\/\\");
    }

    public static void dump(int pop, int temp) {
        // dumps memory in descending order
        // pop MUST be greater than or equal to temp
        if (pop < temp || 0 > pop || pop > stack.height-1
                || 0 > temp || temp > stack.height-1) {
            System.err.println("ERROR 4: Illegal dump range");
            System.exit(4);
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

    public static void ensureValidity(Integer addr) {
        // check to see if addr is in range
        if (0 <= addr && addr <= stack.height-1)
            return;
        else {
            System.err.println("ERROR 2: Address out of range: " + addr);
            System.exit(2);
        }
    }

    public static void ensureValidity(int pointer, String identifier) {
        // this method can be used to ensure either SP and/or PC are in range
        if (0 <= pointer && pointer <= stack.height-1)
            return;
        else {  /* pointer is out of range */
            if (identifier.equals("SP")) {
                System.err.println("ERROR 3: SP out of range: " + pointer);
                System.exit(3);
            } else {
                System.err.println("ERROR 5: Invalid PC: " + pointer);
                System.exit(5);
            }
        }
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
