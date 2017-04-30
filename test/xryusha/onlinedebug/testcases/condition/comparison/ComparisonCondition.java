package xryusha.onlinedebug.testcases.condition.comparison;
import xryusha.onlinedebug.testcases.Flow;
import java.util.Date;

public class ComparisonCondition extends Flow
{
    @Override
    public Object call() throws Exception
    {
        int val=0;
        String BP = "34534636";
        val=1;
        BP = "34534636";

        String strval="AB";
        BP = "34534636";
        strval="BC";
        BP = "34534637";

        long now=System.currentTimeMillis();
        long before=now-1000*3600*24;

        Date nowDate = new Date(now);
        Date beforeDate = new Date(before);
        BP = "34534636";
        Date now2Date = new Date(nowDate.getTime());
        BP = "34534637";

        return null;
    }
}
