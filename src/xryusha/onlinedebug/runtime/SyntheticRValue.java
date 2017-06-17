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

package xryusha.onlinedebug.runtime;

import com.sun.jdi.Value;
import xryusha.onlinedebug.config.values.RValue;
import xryusha.onlinedebug.config.values.Ref;

/**
 * {@link RValue} type created during run-time and not based on configuration,
 *                used to keep remote object values
 */
public class SyntheticRValue extends Ref
{
    private ThreadLocal<Value> threadLocal = new ThreadLocal<Value>();
    private Value value;
    private boolean shared;

    public SyntheticRValue()
    {
        this(false);
    }

    public SyntheticRValue(Value value)
    {
        this(false);
        setValue(value);
    }

    public SyntheticRValue(boolean shared)
    {
        this.shared = shared;
    }

    public SyntheticRValue(Value value, boolean shared)
    {
        this.shared = shared;
        setValue(value);
    }

    public void setValue(Value value)
    {
        if ( shared )
           this.value = value;
        else
           threadLocal.set(value);
    }

    public Value getValue()
    {
        if ( shared )
            return value;
        return threadLocal.get();
    }
} // SyntheticRValue
