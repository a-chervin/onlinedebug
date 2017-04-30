package xryusha.onlinedebug.config.values.eventspecific;

import xryusha.onlinedebug.config.values.Ref;

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * Tag interface specifying values related to specific event type (method return value, thrown exception etc)
 */
@XmlType(name="eventSpecificValue")
@XmlSeeAlso({CurrentException.class, ModificationCurrent.class, ModificationNew.class, ReturnValue.class})
public abstract class BaseEventSpecificValue extends Ref
{
}
