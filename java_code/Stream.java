import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Stream {

    public static void main(String[] args) throws IOException {

        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(System.in));
            int c;

            System.out.println(Integer.parseInt(br.readLine()));
            //System.out.println(br.read());

        } finally {
            if (br != null) {
                br.close();
            }
        }
    }
}
