public class Switch {

    public static void main(String[] args) {

        int num = 5;

        switch (num) {
            case 1:
            case 2:
                System.out.println("Yes!");
                break;
            case 4:
            case 5:
                System.out.println("Foo!");
                break;
        }
    }
}
