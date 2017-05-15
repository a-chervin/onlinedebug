//package xryusha.onlinedebug.runtime.actions.print;
//
//import java.io.ByteArrayOutputStream;
//import java.io.PrintStream;
//import java.util.concurrent.Future;
//
//public class VerifyingTask implements Runnable
//{
//    private final Future future;
//
//    public VerifyingTask(Future future)
//    {
//        this.future = future;
//    }
//
//    @Override
//    public void run()
//    {
//        try {
//            future.get();
//        } catch (Throwable th) {
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            PrintStream ps = new PrintStream(baos);
//            ps.println("Append task failed: ");
//            th.printStackTrace(ps);
//            ps.flush();
//            String str = baos.toString();
//            System.err.println(str);
//        }
//    }
//}
