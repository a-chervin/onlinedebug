package xryusha.onlinedebug.testcases.automatic;

import xryusha.onlinedebug.testcases.AutomaticTestcaseBase;
import xryusha.onlinedebug.testcases.local.AssignInstanceVars;
import xryusha.onlinedebug.testcases.local.AssignLocalVars;
import org.junit.Test;

public class AssignTest extends AutomaticTestcaseBase
{
    @Test
    public void assignLocalVars() throws Exception
    {
        runTest(AssignLocalVars.class);
    }

    @Test
    public void assignInstanceVars() throws Exception
    {
        runTest(AssignInstanceVars.class);
    }

}
