package xryusha.onlinedebug.config.conditions;

import xryusha.onlinedebug.util.Util;

import javax.xml.bind.annotation.*;


/**
 * Specifies condition base
 */
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({GroupConditionSpec.class, RelationConditionSpec.class, IsNullConditionSpec.class})
public abstract class AbstractConditionSpec implements Util
{
    /**
     * predicate inversion (NOT)
     */
    @XmlAttribute(name="inverse", required = false)
    protected Boolean inverse = false;

    public boolean isInverse()
    {
        return valueOf(inverse, false);
    }

    public void setInverse(boolean inverse)
    {
        this.inverse = inverse;
    }
}
