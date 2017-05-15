//package xryusha.onlinedebug.runtime.actions.print;
//
//import java.io.PrintStream;
//import java.util.concurrent.Callable;
//
//public class PrintingTask implements Callable<Void>
//{
//    private final PrintStream target;
//    private final String message;
//
//
//    public PrintingTask(PrintStream target, String message)
//    {
//        this.target = target;
//        this.message = message;
//    }
//
//    @Override
//    public Void call() throws Exception
//    {
//        target.println(message) ;
//        return null;
//    }
//}
