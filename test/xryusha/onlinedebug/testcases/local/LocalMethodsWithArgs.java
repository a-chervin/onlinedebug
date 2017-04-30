package xryusha.onlinedebug.testcases.local;

import xryusha.onlinedebug.testcases.Flow;

public class LocalMethodsWithArgs extends Flow
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

    String instFunc(String str, int val)
    {
        return ""+str+val;
    }

    String staticFunc(String str, int val)
    {
        return ""+str+val;
    }
}
