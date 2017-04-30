package xryusha.onlinedebug.config.conditions;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Specifies "equal" relation
 */
@XmlType
@XmlRootElement(name= Configuration.Elements.CONDITION_EQUAL)
@XmlAccessorType(XmlAccessType.FIELD)
public class EqualsConditionSpec extends RelationConditionSpec
{
}
