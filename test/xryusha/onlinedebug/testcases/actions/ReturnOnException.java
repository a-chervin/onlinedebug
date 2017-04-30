package xryusha.onlinedebug.testcases.actions;

import xryusha.onlinedebug.testcases.Flow;

public class ReturnOnException extends Flow
{
    @Override
    public Object call() throws Exception
    {
        f();
        return null;
    }

    void f() throws Exception
    {
        int ii = d();
        String BP = "asdasds";
        System.out.println("--------------------");
    }

    private int d() throws Exception
    {
        System.out.println("asdasds");
        throw new java.net.BindException("sdfs");
    }
}
