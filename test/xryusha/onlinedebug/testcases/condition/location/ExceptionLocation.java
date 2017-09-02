package xryusha.onlinedebug.testcases.condition.location;

import xryusha.onlinedebug.testcases.Flow;

import java.sql.SQLTimeoutException;

public class ExceptionLocation extends Flow
{
    static int counter = 0;

    @Override
    public void reset()
    {
        counter = 0;
    }

    @Override
    public Object call() throws Exception
    {
        String BP = "wetertr";
        f();
        BP = "wetertr";
        reset();
        ExceptionIrrelevantLocation.f();
        BP = "wetertr";
        reset();
        BP = "wetertr";
        f2();
        BP = "wetertr";
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

    void f2()
    {
        String BP = "wetertr";
        try {
            throw new SQLTimeoutException("");
        } catch (Exception ex) {
        }
        BP = "wetertr";
    }
}
