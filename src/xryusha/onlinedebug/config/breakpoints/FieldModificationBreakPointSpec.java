package xryusha.onlinedebug.config.breakpoints;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.*;

/**
 * Field modification event
 */
@XmlRootElement(name= Configuration.Elements.BREAKPOINT_FIELDMODIFICATION)
@XmlType(name="fieldModification-breakpoint")
@XmlAccessorType(XmlAccessType.FIELD)
public class FieldModificationBreakPointSpec extends  AbstractBreakPointSpec
{
    @XmlAttribute(name="class", required = true)
    private String targetClass;

    @XmlAttribute(name="field", required = true)
    private String targetField;

    public String getTargetClass()
    {
        return targetClass;
    }

    public void setTargetClass(String targetClass)
    {
        this.targetClass = targetClass;
    }

    public String getTargetField()
    {
        return targetField;
    }

    public void setTargetField(String targetField)
    {
        this.targetField = targetField;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("FieldModificationBreakPointSpec{");
        sb.append("targetClass='").append(targetClass).append('\'');
        sb.append(", targetField='").append(targetField).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
