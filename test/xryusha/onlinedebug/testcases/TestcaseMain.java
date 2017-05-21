package xryusha.onlinedebug.testcases;

public class TestcaseMain
{
    public static void main(String[] args) throws Exception
    {
        String clazz = args[0];
        Class<? extends Flow> flowclazz = (Class<? extends Flow>) Class.forName(clazz);
        Flow flw = flowclazz.newInstance();
        while(true) {
            try {
                System.out.println("----");
                flw.reset();
                flw.call();
            } catch (Throwable th) {
                th.printStackTrace();
            }
            Thread.sleep(3000);
        }
    }
}
