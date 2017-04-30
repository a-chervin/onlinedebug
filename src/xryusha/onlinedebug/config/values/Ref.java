package xryusha.onlinedebug.config.values;

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * Specifies base for references to remote VM's references (vars, methods etc)
 */
@XmlType(name = "abstractRef")
@XmlSeeAlso({CallSpec.class, RefChain.class, RefPath.class, ArrayIndex.class})
public abstract class Ref extends RValue
{
    @Override
    public String uniqueID()
    {
        return super.uniqueID()+"<" + reference()+">";
    }

    protected String reference()
    {
        return "";
    };
}
