import java.util.Scanner;

public class Header {

    public Scanner sc;
    public String type;  // e.g. SXX-E for Executable
    public int length;   // length of program
    public int entry;    // how far off the offset to start PC

    public Header(Scanner sc) {
        this.sc = sc;
    }

    public Scanner parseHeader() {
        // this method endows this Header object with all of its instance
        // variables by parsing the header of the source code file

        try {
            // this is the first line in the source code file
            // it must be of the form `%SXX-E...'
            this.type = sc.nextLine();
            System.out.println("Type: " + this.type);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
        if (! type.substring(0,6).equals("%SXX-E")) {
            System.err.println("ERROR:  The first line in any sxx executable file must start with `%SXX-E':");
            System.err.println("  " + this.type);
            System.exit(1);
        }

        try {
            // the first thing on next line must be an integer containing the
            // number of words that will be inserted into the stack machine
            this.length = sc.nextInt();
            skipToEOL();
            System.out.println("Length: " + this.length);
        } catch (Exception e) {
            System.err.println("ERROR: The first thing in this line must be the length of the program:");
            System.err.println("  " + this.length);
            System.exit(1);
        }
        try {
            // the first thing on the next line must be an integer containing
            // the entry point for the program counter
            this.entry = sc.nextInt();
            skipToEOL();
            System.out.println("Entry: " + this.entry);
        } catch (Exception e) {
            System.err.println("ERROR: The first thing in the line must be the entry point:");
            System.err.println("  " + this.length);
            System.exit(1);
        }

        // the next line must start with a `%'
        String text = sc.nextLine();
        System.out.println("Text: " + text);
        if (text.charAt(0) != '%') {
            System.err.println("ERROR: The line must start with a percent sign:");
            System.err.println("  " + text);
            System.exit(1);
        }

        // return the scanner so the rest of the program can pick up where the
        // header leaves off
        return sc;
    }

    public void skipToEOL() {
        sc.nextLine();
    }
}
