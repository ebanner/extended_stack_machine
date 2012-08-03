import java.util.EmptyStackException;

public class Stack {

    public int sp;  /* assume stack is full for now and that sp is not
                           contained on the stack */
    public int pc;  /* program counter */
    public int[] stack;
    public int height;

    public Stack(int height) {
        this.sp = height;
        this.stack = new int[height];  /* create the stack with specified
                                          height */
        this.height = height;
    }

    public void push(int val) {
        if (sp <= 0) {  /* full stack */
            System.err.println("Can't push another value--full stack.");
            System.exit(1);
        } else {
            this.stack[--sp] = val;
        }
    }

    public int pop() {
        if (sp >= height) {  /* empty stack */
            throw new EmptyStackException();  /* need to throw an exception
                                                 because Java complains that
                                                 we're not returning anything
                                                 if we just quit */
        } else {
            return this.stack[sp++];
        }
    }

    public int getContents(int addr) {  /* get the contents at a specific
                                           address */
        return stack[addr];
    }

    public void putContents(int val, int addr) {
        /* insert val at addr on the stack */
        stack[addr] = val;
    }

    public String toString() { /* prints out the stack much like how our brains
                                  imagine what a stack looks like */
        String s = new String();
        String temp;

        for (int pointer = height-1; pointer > 0; pointer--) { 
            /* start from the top of the stack and don't stop until you get
             * past sp */
            temp = "" + stack[pointer] + '\n';
            s += temp;
        }

        return s;
    }
}
