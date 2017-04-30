package xryusha.onlinedebug.config;

import xryusha.onlinedebug.config.actions.*;
import xryusha.onlinedebug.config.breakpoints.AbstractBreakPointSpec;
import xryusha.onlinedebug.config.conditions.AbstractConditionSpec;


import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single configuration entry.
 * The entry contains definition of
 * <ul>
 *  <li> when (i.e. specific code line, exception throwing, method entry/exit)</li>
 *  <li> optionally: conditions</li>
 *  <li> what must be performed</li>
 *  </ul>
 */
@XmlType(name="entry")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConfigEntry implements Cloneable
{
    /**
     * Disabling particular entry if necessary, simpler than commenting out
     */
    @XmlAttribute(name="enabled" , required = false)
    private Boolean enabled = true;

    @XmlElementRef(required = true)
    private AbstractBreakPointSpec breakPoint;

    @XmlElement(name="if")
    private ConditionWrapper conditionHolder;

    @XmlElements({
        @XmlElement(name = Configuration.Elements.ACTION_ASSIGN, type = AssignSpec.class),
        @XmlElement(name = Configuration.Elements.ACTION_PRINT, type = PrintSpec.class),
        @XmlElement(name = Configuration.Elements.ACTION_RETURN, type = ReturnSpec.class),
        @XmlElement(name = Configuration.Elements.ACTION_INVOKE, type = InvokeSpec.class),
    })
    private List<ActionSpec> actionList = new ArrayList<>();



    public ConfigEntry()
    {
    }

    public ConfigEntry(AbstractBreakPointSpec breakPoint, ActionSpec actionSpec)
    {
        this.breakPoint = breakPoint;
        if ( actionSpec != null )
          this.actionList.add(actionSpec);
    }


    @Override
    public ConfigEntry clone() throws CloneNotSupportedException
    {
        return (ConfigEntry)super.clone();
    }

    public AbstractBreakPointSpec getBreakPoint()
    {
        return breakPoint;
    }

    public void setBreakPoint(AbstractBreakPointSpec breakPoint)
    {
        this.breakPoint = breakPoint;
    }

    public AbstractConditionSpec getCondition()
    {
        return conditionHolder != null ? conditionHolder.condition : null;
    }

    public void setCondition(AbstractConditionSpec condition)
    {
        if ( conditionHolder == null )
            conditionHolder = new ConditionWrapper();
        this.conditionHolder.condition = condition;
//        this.condition = condition;
    }

    public List<ActionSpec> getActionList()
    {
        return actionList;
    }

    public boolean isEnabled()
    {
        return enabled == null ? true : enabled.booleanValue();
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @XmlType
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ConditionWrapper
    {
        @XmlElementRef
        private AbstractConditionSpec condition;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("ConfigEntry{");
        sb.append("enabled=").append(enabled);
        sb.append(", breakPoint=").append(breakPoint);
        sb.append(", condition=").append(conditionHolder!=null?conditionHolder.condition:null);
        sb.append(", actionList=").append(actionList);
        sb.append('}');
        return sb.toString();
    }
}
