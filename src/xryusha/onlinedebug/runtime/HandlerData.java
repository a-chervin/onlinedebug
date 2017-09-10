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

import java.util.*;
import java.util.concurrent.atomic.*;

import xryusha.onlinedebug.config.ConfigEntry;
import xryusha.onlinedebug.runtime.actions.Action;



/**
 * Describes all configurations of particular event type, used by event specific handler
 */
public class HandlerData
{
    private final List<RuntimeConfig> configs = new ArrayList<>();
    private final List<RuntimeConfig> asReadOnly = Collections.unmodifiableList(configs);
    private final RemoteJVM jvm;

    public  HandlerData(RemoteJVM jvm)
    {
        this.jvm = jvm;
    }

    public List<RuntimeConfig> getConfig()
    {
        return asReadOnly;
    }

    public RemoteJVM getJvm()
    {
        return jvm;
    }

    public void addConfig(ConfigEntry configEntry)
    {
        RuntimeConfig runconf = new RuntimeConfig(configEntry);
        configs.add(runconf);
    }


    // Runtime data regarding one configuration entry (config itself and parsed and cached action objects)
    public class RuntimeConfig
    {
        private final ConfigEntry configEntry;
        private AtomicReference<List<Action>> actionsRef = new AtomicReference<>();
        private AtomicLong conditionCheckCounter = new AtomicLong(0);
        private AtomicLong actionUseCounter = new AtomicLong(0);


        private RuntimeConfig(ConfigEntry configEntry)
        {
            this.configEntry = configEntry;
        }

        public RemoteJVM getJvm()
        {
            return HandlerData.this.jvm;
        }

        public ConfigEntry getConfigEntry()
        {
            return configEntry;
        }

        public List<Action> getActions()
        {
            return actionsRef.get();
        }

        public boolean setActions(List<Action> actions)
        {
            List<Action> safe =  actions != null ?
                                   Collections.unmodifiableList(actions) : null;
            boolean replaced = actionsRef.compareAndSet(null, safe);
            return replaced;
        }

        public void conditionChecked()
        { conditionCheckCounter.incrementAndGet(); }

        public long  getConditionChecks()
        { return conditionCheckCounter.longValue(); }

        public void actionUsed()
        { actionUseCounter.incrementAndGet(); }

        public long getActionUse()
        { return actionUseCounter.longValue(); }

    }
} // HandlerData
