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
            this.type = sc.nextLine();
            System.out.println("Type: " + this.type);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
        if (! type.substring(0,6).equals("%SXX-E")) {
            // first line of the executable MUST start with `%SXX-E'
            System.err.println("ERROR:  The first line in any sxx executable file must start with `%SXX-E':");
            System.err.println("  " + this.type);
            System.exit(1);
        }

        try {
            this.length = sc.nextInt();
            skipToEOL();
            System.out.println("Length: " + this.length);
            // the first thing on this line should be an integer containing
            // the length of the program
        } catch (Exception e) {
            System.err.println("ERROR: The first thing in this line must be the length of the program:");
            System.err.println("  " + this.length);
            System.exit(1);
        }
        try {
            this.entry = sc.nextInt();
            skipToEOL();
            System.out.println("Entry: " + this.entry);
            // the first thing on this line should be an integer containing
            // the entry point for the program counter
        } catch (Exception e) {
            System.err.println("ERROR: The first thing in the line must be the entry point:");
            System.err.println("  " + this.length);
            System.exit(1);
        }

        String text = sc.nextLine();
        System.out.println("Text: " + text);
        // this line should be a line that starts with a `%' sign
        if (text.charAt(0) != '%') {
            System.err.println("ERROR: The line must start with a percent sign:");
            System.err.println("  " + text);
            System.exit(1);
        }

        return sc;
    }

    public void skipToEOL() {
        sc.nextLine();
    }
}
