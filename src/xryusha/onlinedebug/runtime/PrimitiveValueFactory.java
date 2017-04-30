package xryusha.onlinedebug.runtime;


import java.util.*;
import java.util.logging.Logger;

import com.sun.jdi.*;
import xryusha.onlinedebug.util.Log;
import xryusha.onlinedebug.exceptions.RemoteDataConvertionException;

/**
 * Utility class used to convert remote to local primitive types and vice versa
 */
public class PrimitiveValueFactory
{
    private static Logger log = Log.getLogger();//Logger.getGlobal();

    private final static Map<String, ValueFactory> factories =
            new HashMap<String, ValueFactory>() {{
                        put(String.class.getName(), new StringValueFactory());
                        put(boolean.class.getName(), new BooleanValueFactory());
                        put(char.class.getName(), new CharValueFactory());
                        put(byte.class.getName(), new BooleanValueFactory());
                        put(short.class.getName(), new ShortValueFactory());
                        put(int.class.getName(), new IntegerValueFactory());
                        put(long.class.getName(), new LongValueFactory());
                        put(float.class.getName(), new FloatValueFactory());
                        put(double.class.getName(), new DoubleValueFactory());
                    }};

    public static boolean canConvert(Type type)
    {
        return canConvert(type.name());
    }

    public static boolean canConvert(String type)
    {
        boolean res = factories.containsKey(type) ||
                String.class.getName().equals(type);
        return res;
    }

    public static Value convert(ThreadReference thread, Type type, String value) throws Exception
    {
        return convert(thread, type.name(), value);
    }

    public static Value convert(ThreadReference thread, String type, String value) throws Exception
    {
        ValueFactory factory = factories.get(type);
        if ( factory == null ) {
            if ( String.class.getName().equals(type) )
                factory = factories.get(String.class);
        }
        if ( factory == null ) {
            RemoteDataConvertionException dcex =
                    new RemoteDataConvertionException("unsupported", type);
            log.throwing("PrimitiveValueFactory", "convert", dcex);
            throw dcex;
        }

        Value res = factory.fromString(thread, value);
        return res;
    } // fromString

    public static Object convert(Value value) throws Exception
    {
        if ( value == null )
            return null;
        Type type = value.type();
        ValueFactory factory = factories.get(type.name());
        if ( factory == null ) {
            if ( type.equals(ClassType.class) && type.name().equals(String.class))
                factory = factories.get(String.class);
        }
        if ( factory == null ) {
            RemoteDataConvertionException dcex =
                    new RemoteDataConvertionException("unsupported",  type.name());
            log.throwing("PrimitiveValueFactory", "convert", dcex);
            throw dcex;

        }

        Object res = factory.toLocalObject(value);
        return res;
    } // fromString

    private abstract static class ValueFactory
    {
        abstract Value fromString(ThreadReference thread, String value) throws Exception;
        abstract Object toLocalObject(Value value) throws Exception;
    }

    static class StringValueFactory extends ValueFactory
    {
        @Override
        Value fromString(ThreadReference thread, String value) throws Exception
        {
            return thread.virtualMachine().mirrorOf(value);
        }

        @Override
        Object toLocalObject(Value value) throws Exception
        {
            return value == null ? null : ((StringReference)value).value();
        }
    } // StringValueFactory


    static class BooleanValueFactory extends ValueFactory
    {
        @Override
        Value fromString(ThreadReference thread, String value) throws Exception
        {
            return thread.virtualMachine().mirrorOf(Boolean.valueOf(value));
        }

        @Override
        Object toLocalObject(Value value) throws Exception
        {
            return ((BooleanValue)value).value();
        }
    } // StringValueFactory

    static class CharValueFactory extends ValueFactory
    {
        @Override
        Value fromString(ThreadReference thread, String value) throws Exception
        {
            if ( value.length() != 1 ) {
                RemoteDataConvertionException dcex =
                        new RemoteDataConvertionException("unsupported",
                                                          "value length more than 1("
                                                           + value.length()+")");

                log.throwing("CharValueFactory", "fromString", dcex);
            }
            return thread.virtualMachine().mirrorOf(value.charAt(0));
        }

        @Override
        Object toLocalObject(Value value) throws Exception
        {
            return ((CharValue)value).value();
        }
    } // CharValueFactory

    static class ShortValueFactory extends ValueFactory
    {
        @Override
        Value fromString(ThreadReference thread, String value) throws Exception
        {
            return thread.virtualMachine().mirrorOf(Short.valueOf(value));
        }

        @Override
        Object toLocalObject(Value value) throws Exception
        {
            return ((ShortValue)value).value();
        }
    } // ShortValueFactory

    static class IntegerValueFactory extends ValueFactory
    {
        @Override
        Value fromString(ThreadReference thread, String value) throws Exception
        {
            return thread.virtualMachine().mirrorOf(Integer.valueOf(value));
        }

        @Override
        Object toLocalObject(Value value) throws Exception
        {
            return ((IntegerValue)value).value();
        }
    } // IntegerValueFactory

    static class LongValueFactory extends ValueFactory
    {
        @Override
        Value fromString(ThreadReference thread, String value) throws Exception
        {
            return thread.virtualMachine().mirrorOf(Integer.valueOf(value));
        }

        @Override
        Object toLocalObject(Value value) throws Exception
        {
            return ((LongValue)value).value();
        }
    } // LongValueFactory

    static class FloatValueFactory extends ValueFactory
    {
        @Override
        Value fromString(ThreadReference thread, String value) throws Exception
        {
            return thread.virtualMachine().mirrorOf(Float.valueOf(value));
        }

        @Override
        Object toLocalObject(Value value) throws Exception
        {
            return ((FloatValue)value).value();
        }
    } // FloatValueFactory

    static class DoubleValueFactory extends ValueFactory
    {
        @Override
        Value fromString(ThreadReference thread, String value) throws Exception
        {
            return thread.virtualMachine().mirrorOf(Float.valueOf(value));
        }

        @Override
        Object toLocalObject(Value value) throws Exception
        {
            return ((DoubleValue)value).value();
        }
    } // DoubleValueFactory
}
