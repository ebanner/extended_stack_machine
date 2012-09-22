import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Header {

    public Scanner sc;
    public String type;  // e.g. SXX-E for Executable
    public int length;   // length of program
    public int entry;    // how far off the offset to start PC
    public boolean oldStyle;  // program is `New Style' is type is `%SXX+E'

    public Header(Scanner sc) {
        this.sc = sc;
    }

    public Scanner parseHeader() {
        // this method endows this Header object with all of its instance
        // variables by parsing the header of the source code file

        String line;

        if (sc.hasNextLine()) {
            this.type = sc.nextLine().substring(0,6);
            System.out.println("Type: " + this.type);
            // read the FUCKING manual next time
            if (Pattern.matches("^%SXX[+-]E", this.type)) {
                this.oldStyle = (this.type.charAt(4) == '-') ? true : false;
            } else {
                System.err.println("ERROR:  The first line in any SXX " +
                        "executable file must start with `%SXX[+-]E':");
                System.err.println("  " + this.type);
                System.exit(1);
            }
        } else {
            System.err.println("ERROR: Empty file");
            System.exit(1);
        }

        // get the length and entry values of the header
        this.length = returnFirstElement(sc, "length", "int" );
        this.entry  = returnFirstElement(sc, "entry",  "int" );

        if (0 > this.entry || this.entry >= this.length) {
            System.err.println("ERROR: Illegal entry point");
            System.err.println("  " + this.entry);
            System.exit(1);
        }

        char text = (char)returnFirstElement(sc, "text", "char");

        if (text != '%') {
            System.err.println("ERROR: `%' sign indicating start of text " +
                    "missing:");
            System.err.println("  " + text);
            System.exit(1);
        }

        // return the scanner so the rest of the program can pick up where the
        // header leaves off
        return sc;
    }

    /**
     * Returns the first element in a line.
     *
     * @param sc the scanner which points to the line you want the first
     * element of.
     *
     * @param value can either be `length', `entry', or `text'.  this is used
     * to indicate what type of value you are looking for
     *
     * @param type the return type (int or char)
     *
     */
    public int returnFirstElement(Scanner sc, String value, String type) {
        String line = null;

        while (sc.hasNextLine()) {
            line = sc.nextLine();
            if (isCommentOrBlank(line)) {
                continue;
            } else {
                try {
                    if (type.equals("int")) {
                        System.out.format("%s: %s%n", value, 
                            Integer.parseInt(line.split("\\D")[0]));
                    } else if (type.equals("char")) {
                        return line.charAt((int)0);
                    }
                    return Integer.parseInt(line.split("\\D")[0]);
                } catch (Exception e) {
                    System.err.format("ERROR: The first thing in this line " +
                            "must be the %s of the program:%n", value);
                    System.err.println("  " + line);
                    System.exit(1);
                }
            }
        }
        // didn't find an integer, so quit
        System.err.format("No %s provided%n", value);
        System.exit(1);

        return -1;
    }

    public boolean isCommentOrBlank(String line) {
        line = line.trim();

        if (line.isEmpty() || line.charAt(0) == '#') {
            return true;
        } else {
            return false;
        }
    }
}
