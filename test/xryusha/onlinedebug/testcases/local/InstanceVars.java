package xryusha.onlinedebug.testcases.local;

import xryusha.onlinedebug.testcases.Flow;

public class InstanceVars extends Flow
{
    static int ii_stat = 0;
    int ii_inst = 0;

    @Override
    public void reset()
    {
        ii_stat = 0;
        ii_inst = 0;
    }

    @Override
    public Object call() throws Exception
    {
        func(0);
        return null;
    }

    void func(int arg)
    {
        String BP = "aa"; // expected: ii_stat: 0  ii_inst:0

        ii_stat++;
        ii_inst++;
        BP = "aa"; // expected: ii_stat: 1  ii_inst:1

        ii_stat++;
        ii_inst++;
        BP = "aa"; // expected: ii_stat: 2  ii_inst:2
    }
}
