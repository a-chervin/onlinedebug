package xryusha.onlinedebug.testcases.breakpoints.conditional;

import xryusha.onlinedebug.testcases.Flow;

public class OverlappingPoints extends Flow
{
    @Override
    public Object call() throws Exception
    {
        int v1 = 1;
        int v2 = 2;
        int v3 = 3;
        String BP = "23423";
        BP = "fgf";
        BP = "67uh";
        return null;
    }
}
