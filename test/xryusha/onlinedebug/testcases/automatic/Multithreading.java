package xryusha.onlinedebug.testcases.automatic;

import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.runtime.EventsProcessor;
import xryusha.onlinedebug.runtime.RemoteJVM;
import xryusha.onlinedebug.testcases.AutomaticTestcaseBase;
import xryusha.onlinedebug.testcases.other.MultithreadingPrintTest;
import org.junit.Test;

import java.security.SecureRandom;

public class Multithreading extends AutomaticTestcaseBase
{
    @Test
    public void multithreadedPrinting() throws Exception
    {
        runTest(MultithreadingPrintTest.class);
    }

    @Test
    public void asyncTask() throws Exception
    {
/*
        long start = System.currentTimeMillis();
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "SUN");
        String src = "Twas brillig, and the slithy toves. "+
                     "Did gyre and gimble in the wabe";
        sr.setSeed(src.getBytes());
        byte[] arr = new byte[64*1024*1024];
        sr.nextBytes(arr);
        long end = System.currentTimeMillis();
        System.out.println("Elapsed: " + (end-start));
*/
    }

    @Override
    protected void onStart(EventsProcessor handler, RemoteJVM jvm, Configuration config)
    {
        super.onStart(handler, jvm, config);
    }
}
