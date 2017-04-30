package xryusha.onlinedebug.testcases.local;

import xryusha.onlinedebug.testcases.Flow;

public class LocalMethodsNoArgs extends Flow
{
    int ii_inst = 0;
    static int ii_stat = 0;

    @Override
    public void reset()
    {
        ii_inst = 0;
        ii_stat = 0;
    }

    @Override
    public Object call() throws Exception
    {
        func();
        return null;
    }

    void func()
    {
        String BP = "aa";
        ii_inst++;
        ii_stat++;

        BP = "aa";
        ii_inst++;
        ii_stat++;

        BP = "aa";
    }

    int instFunc()
    {
        return ii_inst;
    }

    static int staticFunc()
    {
        return ii_stat;
    }
}
