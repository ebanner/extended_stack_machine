import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;

/* TODO: Figure out how to execute instructions
 *       Support `:' skipping over memory cells
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

    public static void main(String[] args) throws IOException {

        /* assume for now that opcodes start at position 16 */
        int baseAddr = 16;

        /* throw the opcodes onto the stack */
        initializeStack(baseAddr);
        putTestValuesOnStack();  /* DEBUGGING */

        int PC = baseAddr;
        /*while (stack.getContents(PC) != HALT) {
            // keep executing instructions until a HALT command is reached
            PC = executeInstruction(PC);
        }*/

        /* reveal the stack at the end for a great surprise! */
        stack.reveal();
        System.out.format("%nPC: %d  temp: %d%n", PC, temp);
        //printMapping();
        //executeInstruction(PC);
    }

    public static void initializeStack(int nextFreeAddr) throws IOException {
        /* place opcodes onto the stack starting at location 16 for now */

        /* keep track of where the next position an opcode needs to go on the
         * stack */
        Scanner sc = null;
        String instruction = null;

        try {  /* open up a scanner and start reading all lines of input */
            sc = new Scanner(new BufferedReader(new FileReader("test.esm")));
            in = new Scanner(System.in);

            while (sc.hasNextLine()) {  /* grab the next instruction */
                /* trim off whitespace to the left and right */
                instruction = sc.nextLine().trim();

                if (Pattern.matches(".*;.*", instruction)) {
                    /* if the line contains a comment, 
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

    public static Integer getArg(String instr) {
        String[] args = instr.split(" ");
        if (args.length == 1)
            return null;
        else
            return Integer.parseInt(instr.split(" ")[1]);
    }

    public static void dump(int pop, int temp) {
        if (pop < temp || 0 > pop || pop > stack.height-1
                || 0 < temp || temp > stack.height-1) {
            System.err.println("ERROR 4: Illegal dump range");
            System.exit(4);
        }
        for (; pop >= temp; pop--)
            System.out.println(stack.getContents(pop));
    }

    public static void putTestValuesOnStack() {
        for (int i = 25; i < 45; i++)
            stack.push(i-4);
        //stack.putContents(31, 1);
        //stack.putContents(30, 0);
    }

    /*public static void printMapping() {
        // test and see if the addresses ARE mapped to the values 
        // THANKS to rogerdpack from stackoverflow.com for this one!
        for (Map.Entry<Integer, Integer> entry : opAddrToArg.entrySet()) {
            System.out.println("Address: " + entry.getKey() + "  Argument: " +
                    entry.getValue());
        }
    }*/

    public static void ensureValidity(Integer addr) {
        if (0 <= addr && addr <= stack.height-1)
            return;
        else {
            System.err.println("ERROR 2: Address out of range");
            System.exit(2);
        }
    }

    public static void ensureValidity(int pointer, String identifier) {
        if (0 <= pointer && pointer <= stack.height-1)
            return;
        else {  /* pointer is out of range */
            if (identifier.equals("SP")) {
                System.err.println("ERROR 3: SP out of range");
                System.exit(3);
            } else {
                System.err.println("ERROR 5: Invalid PC");
                System.exit(5);
            }
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
