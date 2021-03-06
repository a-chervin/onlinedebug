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

package xryusha.onlinedebug.config.actions;


import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.values.RValue;
import xryusha.onlinedebug.config.values.Ref;
import xryusha.onlinedebug.config.values.RefChain;
import xryusha.onlinedebug.config.values.RefPath;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Specifies action of assigning value to specific variable
 */
@XmlRootElement(name= Configuration.Elements.ACTION_ASSIGN)
@XmlType(name="assign")
@XmlAccessorType(XmlAccessType.FIELD)
public class AssignSpec implements ActionSpec
{
    /**
     * variable reference should be updated
     */
    @XmlElementRef(required = true)
    private Ref target;

    /**
     * new variable value
     */
    @XmlElement(name = "value", required = true)
    private ValueHolder value;

    public AssignSpec()
    {
        this.value = new ValueHolder();
    }

    public AssignSpec(Ref target, RValue value)
    {
        this();
        setTarget(target);
        this.value.value = value;
    }

    public Ref getTarget()
    {
        return target;
    }

    public void setTarget(Ref target)
    {
        // basic validation. last link must be ref
        if ( target instanceof RefPath) { // it's fine;
            this.target = target;
            return;
        }
        if ( target instanceof RefChain) {
            List<Ref> chain = ((RefChain) target).getRef();
            if ( chain.size() == 0 )
                throw new IllegalArgumentException("target chain path is empty");
            Ref last = chain.get(chain.size()-1);
            setTarget(last);
        }
        else
            throw new IllegalArgumentException("illegal target: " + target);
    }

    public RValue getValue()
    {
        return value != null ? value.value : null;
    }

    public void setValue(RValue value)
    {
        if( this.value == null )
            this.value = new ValueHolder();
        this.value.value = value;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("AssignSpec{");
        sb.append("target=").append(target);
        sb.append(", value=").append(value!=null?value.value:null);
        sb.append('}');
        return sb.toString();
    }

    @XmlType
    public static class ValueHolder
    {
        @XmlElementRef
        public RValue value;
    }
}
