package xryusha.onlinedebug.testcases;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class AutomaticTestcaseDriver
{
    static boolean debug = false;
    
    public static void main(String[] args) throws Exception
    {
        int shift = 0;
        if ( "-debug".equals(args[0])) {
            debug = true;
            shift=1;
        }
        long wait = Integer.parseInt(args[0+shift]);
        String clazz = args[1+shift];
        log("process started");

        Class<? extends Flow> flowclazz = (Class<? extends Flow>) Class.forName(clazz);
        Flow flw = flowclazz.newInstance();
        long start = System.currentTimeMillis();
        int available;

        while((available=System.in.available()) == 0) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
            if ( System.currentTimeMillis() - start > wait ) {
                break;
            }
        }
        log("waiting completed");
        if ( available == 0 ) {
            log("terminating without flow");
            System.exit(0);
        }

        flw.reset();
        log("test flow start");
        flw.call();
        log("test flow completed");

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000));
//        System.out.println("Pizdec from " + clazz);
        System.exit(0);
    }
    
    static void log(String msg)
    {
        if ( debug )
            System.out.println("TestDriver:>>"+msg);
    }
}
