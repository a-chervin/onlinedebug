package xryusha.onlinedebug.testcases.automatic;


import org.junit.Test;
import xryusha.onlinedebug.testcases.AutomaticTestcaseBase;
import xryusha.onlinedebug.testcases.local.*;


public class AccessLocalTest extends AutomaticTestcaseBase
{
    @Test
    public void accessLocalVars() throws Exception
    {
        runTest(LocalVars.class);
    }

    @Test
    public void accessInstanceVars() throws Exception
    {
        runTest(InstanceVars.class);
    }

    @Test
    public void accessLocalArrayVar() throws Exception
    {
        runTest(LocalArrayVar.class);
    }

    @Test
    public void accessLocalArrayWithDynamicIndexVar() throws Exception
    {
        runTest(LocalArrayVar.class,
                "LocalArrayDynamicIndex.xml", "LocalArrayDynamicIndex.txt");
    }

    @Test
    public void accessLocalMethodsNoArgs() throws Exception
    {
        runTest(LocalMethodsNoArgs.class);
    }

    @Test
    public void accessLocalMethodsWithArgs() throws Exception
    {
        runTest(LocalMethodsWithArgs.class);
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
