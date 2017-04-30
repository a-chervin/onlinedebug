package xryusha.onlinedebug.config.conditions;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Spcifies "less" relation
 */
@XmlType(name="less")
@XmlRootElement(name= Configuration.Elements.CONDITION_LESS)
public class LessConditionSpec extends RelationConditionSpec
{
}
