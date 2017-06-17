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

package xryusha.onlinedebug.runtime.handlers.optimize;

import com.sun.jdi.ThreadReference;
import xryusha.onlinedebug.runtime.HandlerData;

public class OptimizerBase implements Optimizer
{
    protected int optimizationTreshold = 20;

    @Override
    public void condition(ThreadReference thread, HandlerData.RuntimeConfig runtimeConfig) throws Exception
    {
        if (  runtimeConfig.getConfigEntry().getCondition() == null ||
               runtimeConfig.getConditionChecks() < optimizationTreshold ||
                 runtimeConfig.getConfigEntry().getCondition() instanceof AbstractOptimizedCondition)
        return;
        _condition(thread, runtimeConfig);
        runtimeConfig.conditionChecked();
    }

    @Override
    public void actions(ThreadReference thread, HandlerData.RuntimeConfig runtimeConfig) throws Exception
    {
        if ( runtimeConfig.getActionUse() < optimizationTreshold ||
                runtimeConfig.getActions() == null ||
                runtimeConfig.getActions().size() == 0 ||
                runtimeConfig.getActions().get(0) instanceof AbstractOptimizedAction)
            return;

        _actions(thread, runtimeConfig);
        runtimeConfig.actionUsed();
    }

    // NOOP. Implemented in extenders
    protected void _condition(ThreadReference thread, HandlerData.RuntimeConfig runtimeConfig)
    {
    }

    // NOOP. Implemented in extenders
    protected void _actions(ThreadReference thread, HandlerData.RuntimeConfig runtimeConfig)
    {
    }
}
