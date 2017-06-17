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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.*;
import xryusha.onlinedebug.config.Configuration;


/**
 * Specifies array entry reference
 */
@XmlRootElement(name= Configuration.Elements.VALUE_ARRAYINDEX)
@XmlType(name="arrayIndex")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArrayIndex extends Ref
{
    /**
     * Required element fixedIndex.
     * TODO: consider specifying as element of RValue type to make fixedIndex value dynamic
     */
    @XmlAttribute(name="index", required = false)
    private Integer fixedIndex;

    @XmlElementRef(required = false)
    private RValue dynamicIndex;

    public int getFixedIndex()
    {
        return fixedIndex;
    }

    public void setFixedIndex(int fixedIndex)
    {
        this.fixedIndex = fixedIndex;
    }

    public void setFixedIndex(Integer fixedIndex)
    {
        this.fixedIndex = fixedIndex;
    }

    public RValue getDynamicIndex()
    {
        return dynamicIndex;
    }

    public void setDynamicIndex(RValue dynamicIndex)
    {
        this.dynamicIndex = dynamicIndex;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("ArrayIndex{");
        sb.append("fixedIndex=").append(fixedIndex);
        sb.append('}');
        return sb.toString();
    }
}
