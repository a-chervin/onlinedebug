package xryusha.onlinedebug.testcases.nested;

public class Nested_T1
{
    static int stat_ii;
    int inst_ii = 1;
    Nested_T2 inst_t2 = new Nested_T2();

    public Nested_T2 getInst_t2()
    {
        return inst_t2;
    }

    public Nested_T2 getInst_t2WithAdd(int add)
    {
        inst_t2.inst_ii +=add;
        return inst_t2;
    }

    public Nested_T2 conditionalNotNull(boolean notnull)
    {
        Nested_T2 res = notnull ? inst_t2 : null;
        return res;
    }
}
