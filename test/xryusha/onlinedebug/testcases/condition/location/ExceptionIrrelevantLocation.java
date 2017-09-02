package xryusha.onlinedebug.testcases.condition.location;

import java.sql.SQLTimeoutException;

public class ExceptionIrrelevantLocation
{
    static void f()
    {
        String BP = "wetertr";
        try {
            throw new SQLTimeoutException("");
        } catch (Exception ex) {
        }
        BP = "wetertr";
    }
}
