package xryusha.onlinedebug.exceptions;

import xryusha.onlinedebug.config.values.RValue;

import java.util.ArrayList;
import java.util.List;

public class RemoteDataAccessException extends RemoteExceptionBase
{
//    private RValue dataPath;
    List<RValue> path = new ArrayList<>();

    public RemoteDataAccessException(String message, RValue dataPath)
    {
        super(message);
        path.add(dataPath);
    }

    public RemoteDataAccessException(String message, List<RValue> dataPath)
    {
        super(message);
        path.addAll(dataPath);
    }

    public RemoteDataAccessException(String message, Throwable cause, RValue dataPath)
    {
        super(message, cause);
        path.add(dataPath);
    }

    @Override
    public String toString()
    {
        return "RemoteDataAccessException{" +
                "dataPath=" + path +
                '}';
    }
}
