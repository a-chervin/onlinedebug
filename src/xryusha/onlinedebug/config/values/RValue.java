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

import javax.xml.bind.annotation.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base value type
 */
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({ Const.class, Ref.class})
public abstract class RValue
{
    private static AtomicInteger counter = new AtomicInteger(0);

    /**
     * base evaluated element class
     */
    @XmlAttribute(name="class", required = false)
    protected String type;

    @XmlTransient
    private final String id;

    public RValue()
    {
        id = this.getClass().getSimpleName() + "-" + counter.incrementAndGet();
    }

    public String uniqueID()
    {
        return id;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
