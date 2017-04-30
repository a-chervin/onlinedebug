package xryusha.onlinedebug.testcases.specialvalues;

import xryusha.onlinedebug.testcases.Flow;

public class VarsDump extends Flow
{
    int instInt = 4;
    String instStr = "InstStr";
    static int statInt = 5;
    String statStr = "StatStr";

    @Override
    public Object call() throws Exception
    {
        root(6);
        return null;
    }

    void root(int intarg)
    {
        String BP = "as" + 12 + "---";
        depth_1(intarg, "zhop", true);
    }

    void depth_1(int intarf, String strarg, boolean booarg)
    {
        int dep_1locint = 13 % 3;
        String BP = "dep1_qw" + 12 + "---";
        String dep1_locstr = "xerp" + dep_1locint;
        System.setProperty("HAX-1",dep1_locstr);
        depth_2(12, "dep12_zhop", null, true);
    }

    void depth_2(int intarg, String strarg_1, String strarg_2, boolean booarg)
    {
        int dep2_locint = 13 % 3;
        String BP = "dep2_as" + 12 + "---";
        String dep2_locstr = "xerp";
        System.setProperty("HAX-2",dep2_locstr);
    }
}
