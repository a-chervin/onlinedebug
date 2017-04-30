package xryusha.onlinedebug.testcases.automatic;

import xryusha.onlinedebug.testcases.AutomaticTestcaseBase;
import org.junit.Test;
import xryusha.onlinedebug.testcases.specialvalues.ReturnValue;
import xryusha.onlinedebug.testcases.specialvalues.ThreadDump;
import xryusha.onlinedebug.testcases.specialvalues.VarsDump;

public class AccessSpecialTest extends AutomaticTestcaseBase
{
    @Test
    public void accessThreadDump() throws Exception
    {
        runTest(ThreadDump.class);
    }

    @Test
    public void accessArgValsDump() throws Exception
    {
        runTest(VarsDump.class, "VarsDump_Arg.xml", "VarsDump_Arg.txt");
    }

    @Test
    public void accessVisibleVarsDump() throws Exception
    {
        runTest(VarsDump.class, "VarsDump_Visible.xml", "VarsDump_Visible.txt");
    }

    @Test
    public void accessReturnValue() throws Exception
    {
        runTest(ReturnValue.class, "ReturnValue.xml", "ReturnValue.txt");
    }
}
