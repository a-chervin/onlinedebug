package xryusha.onlinedebug.testcases.automatic;


import xryusha.onlinedebug.testcases.AutomaticTestcaseBase;
import xryusha.onlinedebug.testcases.actions.InvokeAction;
import xryusha.onlinedebug.testcases.actions.ReturnValue;
import org.junit.Test;


public class ActionsTest extends AutomaticTestcaseBase
{
    @Test
    public void returnAction() throws Exception
    {
        runTest(ReturnValue.class);
    }

    @Test
    public void invokeAction() throws Exception
    {
        runTest(InvokeAction.class);
    }
}
