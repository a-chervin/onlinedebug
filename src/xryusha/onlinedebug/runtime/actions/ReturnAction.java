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

package xryusha.onlinedebug.runtime.actions;

import com.sun.jdi.Method;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.actions.ReturnSpec;
import xryusha.onlinedebug.config.values.Const;
import xryusha.onlinedebug.config.values.RValue;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.PrimitiveValueFactory;


public class ReturnAction extends Action<ReturnSpec>
{
    public ReturnAction(ThreadReference thread, ReturnSpec spec) throws Exception
    {
        super(spec);
        if ( !thread.virtualMachine().canForceEarlyReturn() )
            throw new UnsupportedOperationException("Remove JVM does not support earlyReturn operation");
    }

    @Override
    public void execute(LocatableEvent event, ExecutionContext ctx) throws Exception
    {
        ThreadReference thread = event.thread();
        RValue rvalSpec = spec.getReturnValue();
        if ( rvalSpec instanceof Const ) {
            Method m = event.location().method();
            Const cnst = (Const) rvalSpec;
            if ( !m.returnType().name().equals(cnst.getType()) &&
                    PrimitiveValueFactory.canConvert(m.returnType()))
                cnst.setType(m.returnType().name());
        }

        Value returnValue = getValue(thread, rvalSpec);
        thread.forceEarlyReturn(returnValue);
    }
}
