package xryusha.onlinedebug.testcases.automatic;


import xryusha.onlinedebug.testcases.AutomaticTestcaseBase;
import org.junit.Test;
import xryusha.onlinedebug.testcases.nested.*;


public class AccessNestedTest extends AutomaticTestcaseBase
{
    @Test
    public void accessNestedLocal() throws Exception
    {
        runTest(NestedLocal.class);
    }

    @Test
    public void accessNestedInstance() throws Exception
    {
        runTest(NestedInstance.class);
    }

    @Test
    public void accessNestedStatic() throws Exception
    {
        runTest(NestedStatic.class);
    }

    @Test
    public void accessNestedInstanceFirstCall() throws Exception
    {
        runTest(NestedInstanceFirstCall.class);
    }

    @Test
    public void accessNestedStaticFirstCall() throws Exception
    {
        runTest(NestedStaticFirstCall.class);
    }

    @Test
    public void accessNestedCallChain() throws Exception
    {
        runTest(NestedCallChain.class);
    }

    @Test
    public void accessNestedCallChainWithArg() throws Exception
    {
        runTest(NestedCallChainWithArg.class);
    }

    @Test
    public void accessNestedCallChainPrematureEnd() throws Exception
    {
        runTest(NestedCallChainPrematureEnd.class);
    }

/*
    public static void main(String[] args) throws Exception
    {
        AccessLocalTest at = new AccessLocalTest();
        at.accessLocalVars();
        Map<Thread, StackTraceElement[]> thrm = Thread.getAllStackTraces();
        System.out.println("--thlist--");
        thrm.keySet().stream().filter(t->!t.isDaemon())
                              .forEach(t-> {
                                  System.out.println("Thread: " + t);
                                  StackTraceElement[] stacks = thrm.get(t);
                                  for(StackTraceElement stack: stacks) {
                                      System.out.println("   " + stack);
                                  }
                                        });
        System.out.println("--end--");
    }
*/
}
