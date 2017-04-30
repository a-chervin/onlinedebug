package xryusha.onlinedebug.testcases.automatic;

import xryusha.onlinedebug.testcases.AutomaticTestcaseBase;
import xryusha.onlinedebug.testcases.condition.comparison.ComparisonCondition;
import xryusha.onlinedebug.testcases.condition.grouping.ComplexCondition;
import org.junit.Test;


public class ConditionTest extends AutomaticTestcaseBase
{
    @Test
    public void equalsTest() throws Exception
    {
        runTest(ComparisonCondition.class,
                "EqualsCondition.xml", "EqualsCondition.txt");
    }

    @Test
    public void equalsInverseTest() throws Exception
    {
        runTest(ComparisonCondition.class,
                "EqualsCondition_inverse.xml", "EqualsCondition_inverse.txt");
    }

    @Test
    public void lessTest() throws Exception
    {
        runTest(ComparisonCondition.class,
                "LessCondition.xml", "LessCondition.txt");
    }

    @Test
    public void equalStringsTest() throws Exception
    {
        runTest(ComparisonCondition.class,
                "EqualsStringCondition.xml", "EqualsStringCondition.txt");
    }

    @Test
    public void lessStringTest() throws Exception
    {
        runTest(ComparisonCondition.class,
                "LessStringCondition.xml", "LessStringCondition.txt");
    }

    @Test
    public void equalDatesTest() throws Exception
    {
        runTest(ComparisonCondition.class,
                "EqualDatesCondition.xml", "EqualDatesCondition.txt");
    }

    @Test
    public void lessDatesTest() throws Exception
    {
        runTest(ComparisonCondition.class,
                "LessDatesCondition.xml", "LessDatesCondition.txt");
    }

    @Test
    public void andConditionTest() throws Exception
    {
        runTest(ComplexCondition.class,
                "AndCondition.xml", "AndCondition.txt");
    }

    @Test
    public void orConditionTest() throws Exception
    {
        runTest(ComplexCondition.class,
                "OrCondition.xml", "OrCondition.txt");
    }

    @Test
    public void nestedConditionTest() throws Exception
    {
        runTest(ComplexCondition.class,
                "NestedGroupCondition.xml", "NestedGroupCondition.txt");
    } //
}
