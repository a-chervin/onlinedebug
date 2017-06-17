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

import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.actions.InvokeSpec;
import xryusha.onlinedebug.config.values.CallSpec;
import xryusha.onlinedebug.config.values.Ref;
import xryusha.onlinedebug.config.values.RefChain;
import xryusha.onlinedebug.runtime.ExecutionContext;

public class InvokeAction extends Action<InvokeSpec>
{
    private Ref invocationPath;

    public InvokeAction(InvokeSpec spec)
    {
        super(spec);
        CallSpec call = new CallSpec(spec.getType(), spec.getMethod());
        call.getParams().addAll(spec.getParams());
        Ref target = spec.getAccessPath();
        if ( target != null ) {
            RefChain chain = new RefChain();
            chain.getRef().add(target);
            chain.getRef().add(call);
            invocationPath = chain;
        } else
            invocationPath = call;
    }

    @Override
    public void execute(LocatableEvent event, ExecutionContext ctx) throws Exception
    {
        ThreadReference thread = event.thread();
        Value retval = super.getValue(thread, invocationPath);
    }


}
