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


import javax.xml.bind.annotation.*;
import java.util.List;


/**
 * Specifies complex predicate (OR/AND) base
 */
@XmlType
@XmlSeeAlso({AndGroupSpec.class, OrGroupSpec.class})
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class GroupConditionSpec extends AbstractConditionSpec
{
    /**
     * Related predicates
     */
    @XmlElementRef
    protected List<AbstractConditionSpec> conditions;


    public List<AbstractConditionSpec> getConditions()
    {
        return conditions;
    }

    public void setConditions(List<AbstractConditionSpec> conditions)
    {
        this.conditions = conditions;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        if ( inverse )
            sb.append(":inversed");
        sb.append(":{conditions=").append(conditions);
        sb.append('}');
        return sb.toString();
    }
}
