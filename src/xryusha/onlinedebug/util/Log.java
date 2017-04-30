package xryusha.onlinedebug.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

public class Log
{
    private static Logger logger = Logger.getLogger("onlinedebug");

    static void setLogger(Logger logger)
    {
        Log.logger = logger;
    }

    public static Logger getLogger()
    {
        return logger;
    }

    public static String toString(String descr, Throwable th)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(descr + ":");
        th.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
