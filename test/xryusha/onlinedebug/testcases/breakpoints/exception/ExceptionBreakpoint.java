package xryusha.onlinedebug.testcases.breakpoints.exception;

import xryusha.onlinedebug.testcases.Flow;

import java.sql.SQLTimeoutException;

public class ExceptionBreakpoint extends Flow
{
    int counter = 0;

    @Override
    public void reset()
    {
        counter = 0;
    }

    @Override
    public Object call() throws Exception
    {
        f();
        return null;
    }

    void f()
    {
        String BP = "wetertr";
        try {
            throw new SQLTimeoutException("");
        } catch (Exception ex) {
        }
        BP = "wetertr";
    }
}
