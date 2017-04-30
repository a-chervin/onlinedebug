package xryusha.onlinedebug.testcases.automatic;

import xryusha.onlinedebug.testcases.AutomaticTestcaseBase;
import xryusha.onlinedebug.testcases.nested.FromAnonymousMethodInner;
import xryusha.onlinedebug.testcases.nested.FromStaticInner;
import xryusha.onlinedebug.testcases.nested.FromInstanceInner;
import xryusha.onlinedebug.testcases.nested.FromMethodInner;
import org.junit.Test;

public class AccessEncloserTest extends AutomaticTestcaseBase
{
    @Test
    public void accessEncloserFromInnerClass() throws Exception
    {
        runTest(FromInstanceInner.class);
    }

    @Test
    public void accessEncloserFromInnerStaticClass() throws Exception
    {
        runTest(FromStaticInner.class);
    }

    @Test
    public void accessEncloserFromMethodInnerClass() throws Exception
    {
        runTest(FromMethodInner.class);
    }

    @Test
    public void accessFromAnonymousMethodInner() throws Exception
    {
        runTest(FromAnonymousMethodInner.class);
    }

    @Test
    public void accessFromAnonymousMethodInnerCallMethod() throws Exception
    {
        runTest(FromAnonymousMethodInner.class,
                "FromAnonymousMethodInner_accessMethod.xml",
                "FromAnonymousMethodInner_accessMethod.txt");
    }

}
