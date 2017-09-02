package xryusha.onlinedebug.config.conditions;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Specifies location condition (e.g. onException if fired in ...)
 */
@XmlType(name="locationCondition")
@XmlRootElement(name= Configuration.Elements.CONDITION_LOCATION)
public class AtLocationConditionSpec extends AbstractConditionSpec
{
    @XmlAttribute(name="class", required = true)
    private String inClass;

    @XmlAttribute(name="method", required = false)
    private String method;

    @XmlAttribute(name="line", required = false)
    private Integer line;


    public String getInClass()
    {
        return inClass;
    }

    public void setInClass(String inClass)
    {
        this.inClass = inClass;
    }

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method)
    {
        this.method = method;
    }

    public Integer getLine()
    {
        return line;
    }

    public void setLine(Integer line)
    {
        this.line = line;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("AtLocationConditionSpec{");
        sb.append("inClass='").append(inClass).append('\'');
        sb.append(", method='").append(method).append('\'');
        sb.append(", line=").append(line);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AtLocationConditionSpec that = (AtLocationConditionSpec) o;

        if (!inClass.equals(that.inClass)) return false;
        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        return !(line != null ? !line.equals(that.line) : that.line != null);

    }

    @Override
    public int hashCode()
    {
        int result = inClass.hashCode();
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (line != null ? line.hashCode() : 0);
        return result;
    }
}
