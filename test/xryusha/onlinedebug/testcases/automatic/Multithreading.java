package xryusha.onlinedebug.testcases.automatic;

import xryusha.onlinedebug.testcases.AutomaticTestcaseBase;
import xryusha.onlinedebug.testcases.other.MultithreadingPrintTest;
import org.junit.Test;

public class Multithreading extends AutomaticTestcaseBase
{
    @Test
    public void multithreadedPrinting() throws Exception
    {
        runTest(MultithreadingPrintTest.class);
    }

    private void f()
    {

    }
    class C1 {

    }
}
