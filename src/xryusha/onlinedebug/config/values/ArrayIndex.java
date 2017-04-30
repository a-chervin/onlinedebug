package xryusha.onlinedebug.config.values;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.*;
import xryusha.onlinedebug.config.Configuration;


/**
 * Specifies array entry reference
 */
@XmlRootElement(name= Configuration.Elements.VALUE_ARRAYINDEX)
@XmlType(name="arrayIndex")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArrayIndex extends Ref
{
    /**
     * Required element fixedIndex.
     * TODO: consider specifying as element of RValue type to make fixedIndex value dynamic
     */
    @XmlAttribute(name="index", required = false)
    private Integer fixedIndex;

    @XmlElementRef(required = false)
    private RValue dynamicIndex;

    public int getFixedIndex()
    {
        return fixedIndex;
    }

    public void setFixedIndex(int fixedIndex)
    {
        this.fixedIndex = fixedIndex;
    }

    public void setFixedIndex(Integer fixedIndex)
    {
        this.fixedIndex = fixedIndex;
    }

    public RValue getDynamicIndex()
    {
        return dynamicIndex;
    }

    public void setDynamicIndex(RValue dynamicIndex)
    {
        this.dynamicIndex = dynamicIndex;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("ArrayIndex{");
        sb.append("fixedIndex=").append(fixedIndex);
        sb.append('}');
        return sb.toString();
    }
}
