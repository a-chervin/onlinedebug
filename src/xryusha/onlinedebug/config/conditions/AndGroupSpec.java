package xryusha.onlinedebug.config.conditions;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Specifies AND relation
 * @see GroupConditionSpec
 */
@XmlType(name="and")
@XmlRootElement(name= Configuration.Elements.CONDITION_AND)
public class AndGroupSpec extends GroupConditionSpec
{
}
