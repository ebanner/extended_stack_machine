public class Stack {

    public int sp = 0;  /* assume stack is full for now and that sp is not
                           contained on the stack */
    public int pc;  /* program counter */
    public int[] stack;
    public int size;

    public Stack(int size) {
        this.stack = new int[size];  /* create the stack with specified size */
        this.size = size;
    }

    public void push(int val) {
        this.stack[--sp] = val;
    }

    public int pop() {
        return this.stack[sp++];
    }

    public String toString() {
        /* prints out the stack like one might imagine a stack might look like */
        String s = new String();
        String temp;

        while (sp < this.size) {
            temp = "" + this.pop() + '\n';
            s += temp;
        }

        return s;
    }
}
