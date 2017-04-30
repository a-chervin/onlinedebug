package xryusha.onlinedebug.config.conditions;

import xryusha.onlinedebug.config.values.RValue;

import javax.xml.bind.annotation.*;

/**
 * Base type of relational predicaes (equals,less)
 * @see EqualsConditionSpec
 * @see LessConditionSpec
 */
@XmlType
@XmlSeeAlso({EqualsConditionSpec.class, LessConditionSpec.class})
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class RelationConditionSpec extends AbstractConditionSpec
{
    @XmlElementRef(required = true)
    private RValue[] vals = new RValue[2];

    public RValue getLeft()
    {
        return vals == null ? null : vals[0];
    }

    public void setLeft(RValue left)
    {
        if ( vals == null )
            vals = new RValue[2];
        vals[0] = left;
    }

    public RValue getRight()
    {
        return vals == null ? null : vals[1];
    }

    public void setRight(RValue right)
    {
        if ( vals == null )
            vals = new RValue[2];
        vals[1] = right;
    }

    @Override
    public String toString()
    {
        StringBuilder sb =
                new StringBuilder(getClass().getSimpleName());
        if ( inverse )
            sb.append(":inverse");
        sb.append(":{left=").append(getLeft());
        sb.append(", right=").append(getRight());
        sb.append('}');
        return sb.toString();
    }
}
