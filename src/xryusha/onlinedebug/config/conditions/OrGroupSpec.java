package xryusha.onlinedebug.config.conditions;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="or")
@XmlRootElement(name=Configuration.Elements.CONDITION_OR)
public class OrGroupSpec extends GroupConditionSpec
{
}
