package xryusha.onlinedebug.testcases.specialvalues;

import xryusha.onlinedebug.testcases.Flow;

public class ThreadDump extends Flow
{
    @Override
    public Object call() throws Exception
    {
        root(3);
        return null;
    }

    void root(int inx)
    {
        String BP = "sdsds-ddd";
        depth_1("FF", false);
    }

    private void depth_1(String ff, boolean b)
    {
        String BP = "sdsds-ddd";
        depth_2(ff, b, 6);
    }

    private void depth_2(String ff, boolean b, int i)
    {
        String BP = "sdsds-ddd";
        int x = 24 % 7;
        String vv = Integer.toString(x);
    }
}
