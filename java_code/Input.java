import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Input {

    public static void main(String[] args) throws IOException {

        Scanner sc = null;
        String instruction = null;

        try {
            sc = new Scanner(new BufferedReader(new FileReader("prog.esm")));

            while (sc.hasNextLine()) {
                instruction = sc.nextLine();
                executeInstruction(instruction);
            }
        } finally {
            sc.close();
        }
    }

    public static void executeInstruction(String instr) {
        if (instr.equals("BKPT")) {
            pass(instr);
        } else if (Pattern.matches("PUSH (\\d+)", instr)) {
            pass(instr);
        } else if (Pattern.matches("PUSHV (\\d+)", instr)) {
            pass(instr);
        } else if (instr.equals("PUSHS")) {
            pass(instr);
        } else if (instr.equals("INDIR")) {
            pass(instr);
        } else if (Pattern.matches("PUSHX (\\d+)", instr)) {
            pass(instr);
        } else if (Pattern.matches("POP (\\d+)", instr)) {
            pass(instr);
        } else if (instr.equals("POPS")) {
            pass(instr);
        } else if (Pattern.matches("POPX (\\d+)", instr)) {
            pass(instr);
        } else if (instr.equals("DUPL")) {
            pass(instr);
        } else if (instr.equals("SWAP")) {
            pass(instr);
        } else if (instr.equals("OVER")) {
            pass(instr);
        } else if (instr.equals("DROP")) {
            pass(instr);
        } else if (instr.equals("ROT")) {
            pass(instr);
        } else if (instr.equals("TSTLT")) {
            pass(instr);
        } else if (instr.equals("TSTLE")) {
            pass(instr);
        } else if (instr.equals("TSTGT")) {
            pass(instr);
        } else if (instr.equals("TSTGE")) {
            pass(instr);
        } else if (instr.equals("TSTEQ")) {
            pass(instr);
        } else if (instr.equals("TSTNE")) {
            pass(instr);
        } else if (Pattern.matches("BNE (\\d+)", instr)) {
            pass(instr);
        } else if (Pattern.matches("BT (\\d+)", instr)) {
            pass(instr);
        } else if (Pattern.matches("BEQ (\\d+)", instr)) {
            pass(instr);
        } else if (Pattern.matches("BF (\\d+)", instr)) {
            pass(instr);
        } else if (Pattern.matches("BR (\\d+)", instr)) {
            pass(instr);
        } else if (Pattern.matches("CALL (\\d+)", instr)) {
            pass(instr);
        } else if (instr.equals("CALLS")) {
            pass(instr);
        } else if (instr.equals("RETURN")) {
            pass(instr);
        } else if (instr.equals("POPPC")) {
            pass(instr);
        } else if (Pattern.matches("RETN (\\d+)", instr)) {
            pass(instr);
        } else if (instr.equals("HALT")) {
            pass(instr);
        } else if (instr.equals("ADD")) {
            pass(instr);
        } else if (instr.equals("SUB")) {
            pass(instr);
        } else if (instr.equals("MUL")) {
            pass(instr);
        } else if (instr.equals("DIV")) {
            pass(instr);
        } else if (instr.equals("MOD")) {
            pass(instr);
        } else if (instr.equals("OR")) {
            pass(instr);
        } else if (instr.equals("AND")) {
            pass(instr);
        } else if (instr.equals("XOR")) {
            pass(instr);
        } else if (instr.equals("NOT")) {
            pass(instr);
        } else if (instr.equals("NEG")) {
            pass(instr);
        } else if (Pattern.matches("ADDX (\\d+)", instr)) {
            pass(instr);
        } else if (Pattern.matches("ADDSP (\\d+)", instr)) {
            pass(instr);
        } else if (instr.equals("READ")) {
            pass(instr);
        } else if (instr.equals("PRINT")) {
            pass(instr);
        } else if (instr.equals("READC")) {
            pass(instr);
        } else if (instr.equals("PRINTC")) {
            pass(instr);
        } else if (instr.equals("TRON")) {
            pass(instr);
        } else if (instr.equals("TROFF")) {
            pass(instr);
        } else if (instr.equals("DUMP")) {
            pass(instr);
        }
    }

    public static void pass(String instr) {
        System.out.println("/\\/" + instr + "\\/\\");
    }
}
