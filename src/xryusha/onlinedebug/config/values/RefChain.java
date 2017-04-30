package xryusha.onlinedebug.config.values;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * specifies reference chain, e.g. a.x.d.f or a.d().d etc
 */
@XmlRootElement(name= Configuration.Elements.VALUE_CHAIN)
@XmlType(name="ref-chain")
@XmlAccessorType(XmlAccessType.FIELD)
public class RefChain extends Ref
{
    /**
     * chain elements
     */
    @XmlElementRef
    protected List<Ref> ref;

    public RefChain()
    {
        ref = new ArrayList<>();
    }

    public RefChain(List<Ref> nestings)
    {
        this();
        if ( nestings != null )
            ref.addAll(nestings);
    }

    public List<Ref> getRef()
    {
        return ref;
    }

    public void setRef(List<Ref> ref)
    {
        this.ref = ref;
    }


    @Override
    public String uniqueID()
    {
        StringBuilder sb = new StringBuilder("chain(");
        boolean first = true;
        for(Iterator<Ref> itr = ref.iterator(); itr.hasNext();) {
            Ref ref = itr.next();
            String nest = ref.uniqueID();
            if ( !first )
              sb.append('.');
            first = false;
            sb.append(nest);
        }
        sb.append(')');

        return sb.toString();
    }

    @Override
    public String toString()
    {
        return "RefChain{" +
                "ref=" + ref +
                '}';
    }
}
