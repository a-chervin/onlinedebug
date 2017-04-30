package xryusha.onlinedebug.exceptions;

public class RemoteExceptionBase extends Exception
{
    public RemoteExceptionBase()
    {
    }

    public RemoteExceptionBase(String message)
    {
        super(message);
    }

    public RemoteExceptionBase(String message, Throwable cause)
    {
        super(message, cause);
    }

    public RemoteExceptionBase(Throwable cause)
    {
        super(cause);
    }
}
