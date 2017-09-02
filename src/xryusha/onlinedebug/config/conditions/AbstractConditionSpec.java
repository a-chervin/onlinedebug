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

import xryusha.onlinedebug.util.Util;

import javax.xml.bind.annotation.*;


/**
 * Specifies condition base
 */
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({GroupConditionSpec.class,
             RelationConditionSpec.class,
             IsNullConditionSpec.class,
             AtLocationConditionSpec.class})
public abstract class AbstractConditionSpec implements Util
{
    /**
     * predicate inversion (NOT)
     */
    @XmlAttribute(name="inverse", required = false)
    protected Boolean inverse = false;

    public boolean isInverse()
    {
        return valueOf(inverse, false);
    }

    public void setInverse(boolean inverse)
    {
        this.inverse = inverse;
    }
}
