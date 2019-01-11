package Test.example;


public class Test {
    String data;

    public Test(String d) {
        data = d;
    }

    public String processData(boolean b) {
        String data = this.data;
        Test test = new Test("test" + b);
        StringBuilder builder = new StringBuilder("Data = ");
        if (b) {
            builder.append(data);
        } else {
            builder.append("[data is not visible]");
        }
        builder.append("<DONE>");
        return builder.toString();
    }

    public static void main(String[] args) {
        String data = System.getProperty(args[0]);  // SOURCE
        Test test = new Test(data);
//        String tt = c.data;
        String d1 = test.processData(Boolean.parseBoolean(args[1]));
        System.out.println(d1);                     // SINK
//        System.out.println(tt);                     // NOT A SINK
        System.out.println(data);
    }
}
