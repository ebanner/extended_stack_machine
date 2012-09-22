import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.InputMismatchException;

// This development code is used to test a possible way to read individual
// characters and integers from a Scanner.  The program will read three
// characters, then an integer (with possible leading whitespace), then
// three more characters.  The integer may be followed immediately by any
// non-digit characters, so that an input line of
//     ab1234de
// will read the three characters 'a', 'b', and '1',
// then the integer '234', then the three characters 'd', 'e', and a newline.
// Note that newlines (and, more generally, any whitespace) are included
// in the individual character matches.
public class Read {

    public static Pattern anyCh = Pattern.compile(".", Pattern.DOTALL);
    public static Pattern whiteSpace = Pattern.compile("\\s");
    public static Pattern sign = Pattern.compile("[-]");
    public static Pattern nonDigit = Pattern.compile("[^\\d]");
    public static Pattern digit = Pattern.compile("\\d");
    public static Pattern digits = Pattern.compile("\\d+");

    public static int READC(Scanner in) {
        char ch = '?';
        try {
            in.useDelimiter("");
            ch = in.next(anyCh).charAt(0);
        } catch (Exception e) { // end of input
            return -1;
        }
        return ch;
    }

    public static int READ(Scanner in) {
        int n = 0;
        String m = "";
        try {
            in.useDelimiter("");
            while (in.hasNext(whiteSpace))
                in.next(whiteSpace);
            if (in.hasNext(sign))
                m = in.next(sign);
            if (in.hasNext(digit)) {
                in.useDelimiter(nonDigit);
                if (in.hasNext(digits)) {
                    String s = m + in.next(digits);
                    n = Integer.parseInt(s);
                } else
                    throw new InputMismatchException();
            } else {
                throw new InputMismatchException();
            }
        } catch (Exception e) {
            System.out.println(e); // only for diagnostics
            System.exit(1);
        }
        return n;
    }

    public static void main(String [] args) {
        Scanner in = new Scanner(System.in);
        // read three individual characters
        for (int i=0 ; i<3 ; i++) {
            System.out.println("." + READC(in));
        }
        // read an integer (possibly negative)
        System.out.println(":" + READ(in));
        // read three more individual characters
        for (int i=0 ; i<3 ; i++) {
            System.out.println("." + READC(in));
        }
        System.out.println();
    }
}
