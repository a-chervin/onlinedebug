package xryusha.onlinedebug.testcases.breakpoints.modification;

import xryusha.onlinedebug.testcases.Flow;

public class ModificationBreakpoint extends Flow
{
    @Override
    public Object call() throws Exception
    {
        f();
        return null;
    }

    void f()
    {
        int ii = 0;
        BaseType bt = new BaseType();

        ii = 1;
        String fff = "AA"+bt.str1;

        ii = 2;
        bt.str1 = "B1";

        ii = 3;
        ExtenderType ext = new ExtenderType();
        ii = 4;
        ext.str1 = "B2";
    }

    protected static class BaseType
    {
        protected String str1 = "B";
    }

    protected static class ExtenderType extends BaseType
    {
        protected String str2 = "B";
    }

}
