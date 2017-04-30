package xryusha.onlinedebug.testcases.automatic;

import xryusha.onlinedebug.testcases.AutomaticTestcaseBase;
import xryusha.onlinedebug.testcases.breakpoints.conditional.OverlappingPoints;
import xryusha.onlinedebug.testcases.breakpoints.exception.ExceptionBreakpoint;
import xryusha.onlinedebug.testcases.breakpoints.methodrelated.ConstructorMethod;
import xryusha.onlinedebug.testcases.breakpoints.methodrelated.InnerMethodBreakpoint;
import xryusha.onlinedebug.testcases.breakpoints.methodrelated.MethodBreakpoint;
import xryusha.onlinedebug.testcases.breakpoints.modification.ModificationBreakpoint;
import org.junit.Test;

public class BreakPointTest extends AutomaticTestcaseBase
{
    @Test
    public void exactExceptionBreakPointTest() throws Exception
    {
        runTest(ExceptionBreakpoint.class,
                "ExceptionBreakpoint_ExactException.xml", "ExceptionBreakpoint_ExactException.txt");
    }

    @Test
    public void multipleExceptionBreakPointTest() throws Exception
    {
        runTest(ExceptionBreakpoint.class,
                "ExceptionBreakpoint_MultipleExceptions.xml", "ExceptionBreakpoint_ExactException.txt");
    }

    @Test
    public void baseExceptionBreakPointTest() throws Exception
    {
        runTest(ExceptionBreakpoint.class,
                "ExceptionBreakpoint_ExtendingException.xml", "ExceptionBreakpoint_ExtendingException.txt");
    }

    @Test
    public void nonrelevantExceptionBreakPointTest() throws Exception
    {
        runTest(ExceptionBreakpoint.class,
                "ExceptionBreakpoint_DifferentException.xml", "ExceptionBreakpoint_DifferentException.txt");
    }

    @Test
    public void simpleMethodEntryBreakPointTest() throws Exception
    {
        runTest(MethodBreakpoint.class,
                "MethodEntryBreakpoint.xml", "MethodEntryBreakpoint.txt");
    }

    @Test
    public void simpleMethodEntryBreakAnySignaturePointTest() throws Exception
    {
        runTest(MethodBreakpoint.class,
                "MethodEntryBreakpoint_anySignature.xml", "MethodEntryBreakpoint_anySignature.txt");
    }

    @Test
    public void simpleMethodEntryWrongSignatureBreakPointTest() throws Exception
    {
        runTest(MethodBreakpoint.class,
                "MethodEntryBreakpoint_wrongSignature.xml", "MethodEntryBreakpoint_wrongSignature.txt");
    }

    @Test
    public void innerMethodEntryBreakPointTest() throws Exception
    {
        runTest(InnerMethodBreakpoint.class,
                "InnerMethodEntryBreakpoint.xml", "InnerMethodEntryBreakpoint.txt");
    }

    @Test
    public void simpleMethodExitBreakPointTest() throws Exception
    {
        runTest(MethodBreakpoint.class,
                "MethodExitBreakpoint.xml", "MethodExitBreakpoint.txt");
    }

    @Test
    public void simpleOverloadedMethodExitBreakPointTest() throws Exception
    {
        runTest(MethodBreakpoint.class,
                "MethodExitBreakpoint_overloading.xml", "MethodExitBreakpoint_overloading.txt");
    }

    @Test
    public void constructorEntryBreakPointTest() throws Exception
    {
        runTest(ConstructorMethod.class,
                "ConstructorMethodEntry.xml", "ConstructorMethodEntry.txt");
    }

    @Test
    public void overlappingBreakPointTest() throws Exception
    {
        runTest(OverlappingPoints.class,
                "OverlappingPoints.xml", "OverlappingPoints.txt");
    }

    @Test
    public void fieldModificationBreakPointTest() throws Exception
    {
        runTest(ModificationBreakpoint.class,
                "ModificationBreakpoint.xml", "ModificationBreakpoint.txt");
    }
}
