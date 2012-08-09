import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

/* TODO: Support command line arguments:
 *           - Base address
 *       Test some actual programs
 *       Return correct error codes
 *       GUI (!!)
 */

public class Driver {

    /* this is *the* master stack in the stack machine emulator */
    public static final Stack stack = new Stack(50);  
    /* values used to help compute the values of instructions */
    public static int addr, value, temp, t1, t2;
    /* TRACE mode can either be ON of OFF */
    public static boolean TRACE;
    /* scanner is used for READ and READC commands */
    public static Scanner in = null;
    /* map is used to map opcode addresses to their arguement */
    public static Map<Integer, Integer> opAddrToArg = 
        new HashMap<Integer, Integer>();

    public static void main(String[] args) throws IOException {

        /* assume for now that opcodes start at position 16 */
        int baseAddr = 16;

        /* throw the opcodes onto the stack */
        initializeStack(baseAddr);
        putTestValuesOnStack();  /* DEBUGGING */

        int PC = baseAddr;
        while (stack.getContents(PC) != HALT) {
            /* keep executing instructions until a HALT command is reached */
            PC = executeInstruction(PC);
        }

        /* reveal the stack at the end for a great surprise! */
        stack.reveal();
        System.out.format("%nPC: %d  temp: %d%n", PC, temp);
        //printMapping();
        executeInstruction(PC);
    }

    public static void initializeStack(int baseAddr) throws IOException {
        /* place opcodes onto the stack starting at location 16 for now */

        /* keep track of where the next position an opcode needs to go on the
         * stack */
        int nextFreeAddr = baseAddr;  /* just 16 until we support CLI options */
        Scanner sc = null;
        String instruction = null;

        try {  /* open up a scanner and start reading all lines of input */
            sc = new Scanner(new BufferedReader(new FileReader("test.esm")));
            in = new Scanner(System.in);

            while (sc.hasNextLine()) {  /* grab the next instruction */
                /* trim off whitespace to the left and right */
                instruction = sc.nextLine().trim();

                if (Pattern.matches(".*;.*", instruction)) {
                    /* grab the part of the string before the `;' and trim off
                     * excess whitespce */
                    instruction = instruction.split(";")[0].trim();
                    if (instruction.equals(""))
                        continue;
                }

                /* put the instruction's opcode on the stack so PC to read it
                 * later */
                insertOpcode(nextFreeAddr, instruction);
                System.out.println(instruction);

                /* create a mapping between the location of the instruction and
                 * its possible argument so we can recall it later.
                 * when it comes time to read the next instruction, all we have
                 * to look at is an opcode.  we need some way to remember that
                 * some of these opcodes have arguments we provided as users.
                 * if an opcode doesn't have an arguement, its arg is null */
                opAddrToArg.put(nextFreeAddr, getArg(instruction));

                /* point nextFreeAddr to the next free position on the stack to stick
                 * the next opcode */
                nextFreeAddr++;
            }
        } finally {
            if (sc != null)
                sc.close();
        }
    }

    public static void insertOpcode(int nextFreeAddr, String instr) {
        /* inserts the opcode of the next instruction in the user's program
         * into the next available free loaction (growing upwards) */

        if (instr.equals("BKPT")) {
            stack.putContents(nextFreeAddr, BKPT);
        } else if (Pattern.matches("PUSH -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, PUSH);
        } else if (Pattern.matches("PUSHV -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, PUSHV);
        } else if (instr.equals("PUSHS")) {
            stack.putContents(nextFreeAddr, PUSHS);
        } else if (instr.equals("INDIR")) {
            stack.putContents(nextFreeAddr, INDIR);
        } else if (Pattern.matches("PUSHX -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, PUSHX);
        } else if (Pattern.matches("POP -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, POP);
        } else if (instr.equals("POPS")) {
            stack.putContents(nextFreeAddr, POPS);
        } else if (Pattern.matches("POPX -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, POPX);
        } else if (instr.equals("DUPL")) {
            stack.putContents(nextFreeAddr, DUPL);
        } else if (instr.equals("SWAP")) {
            stack.putContents(nextFreeAddr, SWAP);
        } else if (instr.equals("OVER")) {
            stack.putContents(nextFreeAddr, OVER);
        } else if (instr.equals("DROP")) {
            stack.putContents(nextFreeAddr, DROP);
        } else if (instr.equals("ROT")) {
            stack.putContents(nextFreeAddr, ROT);
        } else if (instr.equals("TSTLT")) {
            stack.putContents(nextFreeAddr, TSTLT);
        } else if (instr.equals("TSTLE")) {
            stack.putContents(nextFreeAddr, TSTLE);
        } else if (instr.equals("TSTGT")) {
            stack.putContents(nextFreeAddr, TSTGT);
        } else if (instr.equals("TSTGE")) {
            stack.putContents(nextFreeAddr, TSTGE);
        } else if (instr.equals("TSTEQ")) {
            stack.putContents(nextFreeAddr, TSTEQ);
        } else if (instr.equals("TSTNE")) {
            stack.putContents(nextFreeAddr, TSTNE);
        } else if (Pattern.matches("BNE -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, BNE);
        } else if (Pattern.matches("BT -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, BT);
        } else if (Pattern.matches("BEQ -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, BEQ);
        } else if (Pattern.matches("BF -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, BF);
        } else if (Pattern.matches("BR -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, BR);
        } else if (Pattern.matches("CALL -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, CALL);
        } else if (instr.equals("CALLS")) {
            stack.putContents(nextFreeAddr, CALLS);
        } else if (instr.equals("RETURN")) {
            stack.putContents(nextFreeAddr, RETURN);
        } else if (instr.equals("POPPC")) {
            stack.putContents(nextFreeAddr, POPPC);
        } else if (Pattern.matches("RETN -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, RETN);
        } else if (instr.equals("HALT")) {
            stack.putContents(nextFreeAddr, HALT);
        } else if (instr.equals("ADD")) {
            stack.putContents(nextFreeAddr, ADD);
        } else if (instr.equals("SUB")) {
            stack.putContents(nextFreeAddr, SUB);
        } else if (instr.equals("MUL")) {
            stack.putContents(nextFreeAddr, MUL);
        } else if (instr.equals("DIV")) {
            stack.putContents(nextFreeAddr, DIV);
        } else if (instr.equals("MOD")) {
            stack.putContents(nextFreeAddr, MOD);
        } else if (instr.equals("OR")) {
            stack.putContents(nextFreeAddr, OR);
        } else if (instr.equals("AND")) {
            stack.putContents(nextFreeAddr, AND);
        } else if (instr.equals("XOR")) {
            stack.putContents(nextFreeAddr, XOR);
        } else if (instr.equals("NOT")) {
            stack.putContents(nextFreeAddr, NOT);
        } else if (instr.equals("NEG")) {
            stack.putContents(nextFreeAddr, NEG);
        } else if (Pattern.matches("ADDX -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, ADDX);
        } else if (Pattern.matches("ADDSP -?(\\d+)", instr)) {
            stack.putContents(nextFreeAddr, ADDSP);
        } else if (instr.equals("READ")) {
            stack.putContents(nextFreeAddr, READ);
        } else if (instr.equals("PRINT")) {
            stack.putContents(nextFreeAddr, PRINT);
        } else if (instr.equals("READC")) {
            stack.putContents(nextFreeAddr, READC);
        } else if (instr.equals("PRINTC")) {
            stack.putContents(nextFreeAddr, PRINTC);
        } else if (instr.equals("TRON")) {
            stack.putContents(nextFreeAddr, TRON);
        } else if (instr.equals("TROFF")) {
            stack.putContents(nextFreeAddr, TROFF);
        } else if (instr.equals("DUMP")) {
            stack.putContents(nextFreeAddr, DUMP);
        } else {
            System.err.println("Invalid instruction--exiting.");
            System.err.println(instr);
            System.exit(1);
        }
    }

    public static int executeInstruction(int PC) {
        /* executes the instruction that PC is pointing to */
        boolean pcModified = false;
        int opcode = stack.getContents(PC);

        switch(opcode) {
            case BKPT:   // 0
                 /* unconditionally enter the sxx debugger */
                pass(BKPT);
                break;
            case PUSH:   // 1
                /* push(*addr); */
                addr = opAddrToArg.get(PC);
                stack.push(stack.getContents(addr));
                break;
            case PUSHV:  // 2
                /* push(value); */
                value = opAddrToArg.get(PC);
                stack.push(value);
                break;
            case PUSHS:  // 3
                 /* push(*pop()); */
                stack.push(stack.getContents(stack.pop()));
                break;
            case PUSHX:  // 4
                /* push(*(pop()+addr)); */
                addr = opAddrToArg.get(PC);
                stack.push(stack.getContents(stack.pop()+addr));
                break;
            case POP:    // 5
                /* *addr=pop(); */
                addr = opAddrToArg.get(PC);
                stack.putContents(addr, stack.pop());
                break;
            case POPS:   // 6
                /* temp=pop(); *pop()=temp; */
                temp = stack.pop();
                stack.putContents(stack.pop(), temp);
                break;
            case POPX:   // 7
                /* temp=pop(); *(pop()+addr)=temp; */
                temp = stack.pop();
                addr = opAddrToArg.get(PC);
                stack.putContents(stack.pop()+addr, temp);
                break;
            case DUPL:   // 8
                /* push(*SP); */
                stack.push(stack.getContents(stack.SP));
                break;
            case SWAP:   // 9
                /* temp=*SP; *SP=*(SP+1); *(SP+1)=temp; */
                temp = stack.getContents(stack.SP);
                stack.putContents(stack.SP, stack.getContents(stack.SP+1));
                stack.putContents(stack.SP+1, temp);
                break;
            case OVER:   // 10
                /* push(*(SP+1)); */
                stack.push(stack.getContents(stack.SP+1));
                break;
            case DROP:   // 11
                /* SP++; */
                stack.SP++;
                break;
            case ROT:    // 12
                /* temp=*SP; *SP=*(SP+2); *(SP+2)=*(SP+1); *(SP+1)=temp; */
                temp = stack.getContents(stack.SP);
                stack.putContents(stack.SP, stack.getContents(stack.SP+2));
                stack.putContents(stack.SP+2, stack.getContents(stack.SP+1));
                stack.putContents(stack.SP+1, temp);
                break;
            case TSTLT:  // 13
                /* TSTLT       --> temp=pop(); push((temp<0)?1:0); */
                temp = stack.pop();
                stack.push( (temp < 0) ? 1 : 0 );
                break;
            case TSTLE:  // 14
                /* TSTLE       --> temp=pop(); push((temp<=0)?1:0); */
                temp = stack.pop();
                stack.push( (temp <= 0) ? 1 : 0 );
                break;
            case TSTGT:  // 15
                /* temp=pop(); push((temp>0)?1:0); */
                temp = stack.pop();
                stack.push( (temp > 0) ? 1 : 0 );
                break;
            case TSTGE:  // 16
                /* temp=pop(); push((temp>=0)?1:0); */
                temp = stack.pop();
                stack.push( (temp >= 0) ? 1 : 0 );
                break;
            case TSTEQ:  // 17
                /* temp=pop(); push((temp==0)?1:0); */
                temp = stack.pop();
                stack.push( (temp == 0) ? 1 : 0);
                break;
            case TSTNE:  // 18
                /* temp=pop(); push((temp!=0)?1:0); */
                temp = stack.pop();
                stack.push( (temp != 0) ? 1 : 0 );
                break;
            case BNE:    // 19
                /* if (pop()!=0) PC=addr; */
                addr = opAddrToArg.get(PC);
                if (stack.pop() != 0) {
                    PC = addr;
                    pcModified = true;
                }
                break;
            case BEQ:    // 20
                /* if (pop()==0) PC=addr; */
                addr = opAddrToArg.get(PC);
                if (stack.pop() == 0) {
                    PC = addr;
                    pcModified = true;
                }
                break;
            case BR:     // 21
                /* PC=addr; */
                addr = opAddrToArg.get(PC);
                PC = addr;
                pcModified = true;
                break;
            case CALL:   // 22
                /* push(PC); PC=addr; */
                stack.push(PC);
                addr = opAddrToArg.get(PC);
                PC = addr;
                pcModified = true;
                break;
            case CALLS:  // 23
                /* temp=pop(); push(PC); PC=temp; */
                temp = stack.pop();
                stack.push(PC);
                PC = temp;
                pcModified = true;
                break;
            case RETURN: // 24
                /* PC=pop(); */
                PC = stack.pop();
                pcModified = true;
                break;
            case RETN:   // 25
                /* temp=pop(); SP += value; PC=temp; */
                temp = stack.pop();
                value = opAddrToArg.get(PC);
                stack.SP += value;
                PC = temp;
                pcModified = true;
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
                break;
            case SUB:    // 28
                /* temp=pop(); push( pop() - temp ); */
                temp = stack.pop();
                stack.push(stack.pop()-temp);
                break;
            case MUL:    // 29
                /* temp=pop(); push( pop() * temp ); */
                temp = stack.pop();
                stack.push(stack.pop() * temp);
                break;
            case DIV:    // 30
                /* temp=pop(); push( pop() / temp ); */
                temp = stack.pop();
                stack.push(stack.pop() / temp);
                break;
            case MOD:    // 31
                /* temp=pop(); push( pop() % temp ); */
                temp = stack.pop();
                stack.push(stack.pop() % temp);
                break;
            case OR:     // 32
                /* temp=pop(); push( pop() || temp ); */
                temp = stack.pop();
                stack.push( (stack.pop() != 0 || temp != 0) ? 1 : 0 );
                break;
            case AND:    // 33
                /* temp=pop(); push( pop() && temp ); */
                temp = stack.pop();
                stack.push( (stack.pop() != 0 && temp != 0) ? 1 : 0 );
                break;
            case XOR:    // 34
                /* temp=pop(); push( pop() xor temp ); [see below] */
                t1 = stack.pop();
                t2 = stack.pop();
                stack.push( (!(t1 != 0 && t2 != 0) 
                            && (t1 != 0 || t2 != 0)) ? 1 : 0 );
                break;
            case NOT:    // 35
                /* push( !pop() ); */
                stack.push( !(stack.pop() != 0) ? 1 : 0 );
                break;
            case NEG:    // 36
                /* push( -pop() ); */
                stack.push( -1*stack.pop());
                break;
            case ADDX:   // 37
                /* push( pop()+addr ); */
                addr = opAddrToArg.get(PC);
                stack.push(stack.pop() + addr);
                break;
            case ADDSP:  // 38
                /* SP += value; */
                value = opAddrToArg.get(PC);
                stack.SP += value;
                break;
            case READ:   // 39
                /* read temp in %d format; push(temp); */
                temp = in.nextInt();
                stack.push(temp);
                break;
            case PRINT:  // 40
                /* print pop() in %d format */
                System.out.println(stack.pop());
                break;
            case READC:  // 41
                /* read temp in %c format; push(temp); */
                temp = in.nextLine().charAt(0);
                stack.push(temp);
                break;
            case PRINTC: // 42
                /* print pop() in %c format */
                System.out.println((char)stack.pop());
                break;
            case TRON:   // 43
                /* turn on trace feature */
                TRACE = true;
                break;
            case TROFF:  // 44
                /* turn off trace feature */
                TRACE = false;
                break;
            case DUMP:   // 45
                /* temp=pop(); dump memory from pop() to temp; */
                temp = stack.pop();
                dump(stack.pop(), temp);
                break;
        }

        /* update SP */
        stack.putContents(0, stack.SP);

        /* increment PC by one unless it has already been modified */
        return pcModified ? PC : PC+1;
    }

    public static void pass(int op) {
        System.out.println("/\\/" + op + "\\/\\");
    }

    public static Integer getArg(String instr) {
        String[] args = instr.split(" ");
        if (args.length == 1)
            return null;
        else
            return Integer.parseInt(instr.split(" ")[1]);
    }

    public static void dump(int pop, int temp) {
        for (; pop >= temp; pop--)
            System.out.println(stack.getContents(pop));
    }

    public static void putTestValuesOnStack() {
        for (int i = 25; i < 45; i++)
            stack.push(i-4);
        //stack.putContents(31, 1);
        //stack.putContents(30, 35);
    }

    public static void printMapping() {
        /* test and see if the addresses ARE mapped to the values 
         * THANKS to rogerdpack from stackoverflow.com for this one! */
        for (Map.Entry<Integer, Integer> entry : opAddrToArg.entrySet()) {
            System.out.println("Address: " + entry.getKey() + "  Argument: " +
                    entry.getValue());
        }
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
