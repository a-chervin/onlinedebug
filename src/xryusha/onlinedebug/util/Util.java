package xryusha.onlinedebug.util;

public interface Util
{
    default boolean valueOf(Boolean bool)
    {
        return valueOf(bool, false);
    }

    default boolean valueOf(Boolean bool, boolean defaultValue)
    {
        return bool == null ? defaultValue : bool.booleanValue();
    }
}
