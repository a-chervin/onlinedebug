package xryusha.onlinedebug.testcases.breakpoints.methodrelated;

import xryusha.onlinedebug.testcases.Flow;

public class ConstructorMethod extends Flow
{
    @Override
    public void reset()
    {
        CtorTest.counter = 0;
    }

    @Override
    public Object call() throws Exception
    {
        f();
        return null;
    }

    void f()
    {
        String BP = "sdsfs";
        CtorTest ct = new CtorTest();
        BP = "sdsfs";
        ct = new CtorTest("asdsad");
        BP = "sdsfs";
    }

    static class CtorTest
    {
        static int counter = 0;

        CtorTest()
        {
            String BP="sadfs";
        }

        CtorTest(String vvv)
        {
            String BP="sadfs";
        }
    }

}
