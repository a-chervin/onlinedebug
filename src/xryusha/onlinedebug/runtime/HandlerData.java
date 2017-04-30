package xryusha.onlinedebug.runtime;

import xryusha.onlinedebug.config.ConfigEntry;
import xryusha.onlinedebug.runtime.actions.Action;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Describes all configurations of particular event type, used by event specific handler
 */
public class HandlerData
{
    private final List<RuntimeConfig> configs = new ArrayList<>();
    private final List<RuntimeConfig> asReadOnly = Collections.unmodifiableList(configs);
    private final RemoteJVM jvm;

    public HandlerData(RemoteJVM jvm)
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


    // Runtime data regarding one configuration entry (config itself and parsed and chached action objects)
    public class RuntimeConfig
    {
        private final ConfigEntry configEntry;
        private AtomicReference<List<Action>> actionsRef = new AtomicReference<>();

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
    }
} // HandlerData
