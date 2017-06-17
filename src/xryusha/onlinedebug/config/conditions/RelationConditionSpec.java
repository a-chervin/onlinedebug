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

package xryusha.onlinedebug.config.conditions;

import xryusha.onlinedebug.config.values.RValue;

import javax.xml.bind.annotation.*;

/**
 * Base type of relational predicaes (equals,less)
 * @see EqualsConditionSpec
 * @see LessConditionSpec
 */
@XmlType
@XmlSeeAlso({EqualsConditionSpec.class, LessConditionSpec.class})
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class RelationConditionSpec extends AbstractConditionSpec
{
    @XmlElementRef(required = true)
    private RValue[] vals = new RValue[2];

    public RValue getLeft()
    {
        return vals == null ? null : vals[0];
    }

    public void setLeft(RValue left)
    {
        if ( vals == null )
            vals = new RValue[2];
        vals[0] = left;
    }

    public RValue getRight()
    {
        return vals == null ? null : vals[1];
    }

    public void setRight(RValue right)
    {
        if ( vals == null )
            vals = new RValue[2];
        vals[1] = right;
    }

    @Override
    public String toString()
    {
        StringBuilder sb =
                new StringBuilder(getClass().getSimpleName());
        if ( inverse )
            sb.append(":inverse");
        sb.append(":{left=").append(getLeft());
        sb.append(", right=").append(getRight());
        sb.append('}');
        return sb.toString();
    }
}
