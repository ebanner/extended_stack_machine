/* This class represents a stack, although stack purists will disagree.  Let me
 * explain:  A true stack is a data structure in which it is only possible to
 * push and pop values.  This stack supports pushing and popping, but also
 * allows the user to access the stack pointer (SP) value, along with providing
 * the user the ability to insert values wherever they would like
 * (through the putContents() method).  The user is also free to grab values from
 * anywhere on the stack (through the getContents() method).  These methods made
 * implementing the Stack Machine Emulator far easier. This stack class also
 * provides a reveal() method, in which each element in the stack is printed
 * alongside its location.  The stack grows downward, with the top location
 * identified as position `height-1' and the bottom position `0'.*/

import java.util.EmptyStackException;

public class Stack {

    public int[] stack;
    public int SP;  
    public int height;

    public Stack(int height) {
        /* create the stack with specified height */
        this.stack = new int[height];
        /* point SP to one position above the stack */
        this.SP = this.stack.length;
        this.height = height;
    }

    public void push(int val) {
        if (SP <= 0) {  /* full stack */
            System.err.println("Can't push another value--full stack.");
            System.exit(1);
        } else {
            this.stack[--this.SP] = val;
        }
    }

    public int pop() {
        if (this.SP >= this.height) {  /* empty stack */
            throw new EmptyStackException();  
        } else {
            return this.stack[this.SP++];
        }
    }

    public int getContents(int addr) {  
        /* get the contents at a specific address */
        if (0 > addr || addr > this.height-1) {
            System.err.println("ERROR: Attempt to get a value from an address out of range");
            System.err.println("  Illegal Address: " + addr);
            System.exit(1);
        }

        return this.stack[addr];
    }

    public void putContents(int addr, int value) {
        /* *addr = val */
        if (0 > addr || addr > this.height-1) {
            System.err.println("ERROR: Attempt to put a value into an address out of range");
            System.err.println("  Illegal Address: " + addr);
            System.exit(1);
        } 

        this.stack[addr] = value;
    }

    public void reveal() { 
        /* prints out the stack much like how our brains imagine what a stack
         * looks like */
        for (int pointer = height-1; pointer >= 0; pointer--) { 
            /* start from the top of the stack and print adresses & value pairs
             * all the way down to SP -- also print out an arrow to where SP
             * points */
            System.out.format("%2d| %2s%s\n", pointer, this.stack[pointer], 
                    (pointer == this.SP) ? " <-- SP" : "");
        }
    }
    public static void main(String[] args) {
        System.out.println("Hello, world!");
    }
}
