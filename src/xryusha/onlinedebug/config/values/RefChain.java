/**
 * Licensed to the a-chervin (ax.chervin@gmail.com) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * a-chervin licenses this file under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
