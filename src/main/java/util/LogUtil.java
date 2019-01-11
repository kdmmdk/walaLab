package util;

public class LogUtil {
    private static int DEBUG_SWITCH = 1; // 0:关闭调试 1:打开调试

    public static void i(String tag, Object Content) {
        System.out.println(tag + ": " + Content.toString());

    }


}
