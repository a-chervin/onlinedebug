package xryusha.onlinedebug.config.conditions;


import javax.xml.bind.annotation.*;
import java.util.List;


/**
 * Specifies complex predicate (OR/AND) base
 */
@XmlType
@XmlSeeAlso({AndGroupSpec.class, OrGroupSpec.class})
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class GroupConditionSpec extends AbstractConditionSpec
{
    /**
     * Related predicates
     */
    @XmlElementRef
    protected List<AbstractConditionSpec> conditions;


    public List<AbstractConditionSpec> getConditions()
    {
        return conditions;
    }

    public void setConditions(List<AbstractConditionSpec> conditions)
    {
        this.conditions = conditions;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        if ( inverse )
            sb.append(":inversed");
        sb.append(":{conditions=").append(conditions);
        sb.append('}');
        return sb.toString();
    }
}
