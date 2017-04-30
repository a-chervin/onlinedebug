package xryusha.onlinedebug.exceptions;

public class RemoteClassNotFoundException extends RemoteExceptionBase
{
    private String clazz;

    public RemoteClassNotFoundException(String message, String clazz)
    {
        super(message);
        this.clazz = clazz;
    }

    public RemoteClassNotFoundException(String message, Throwable cause, String clazz)
    {
        super(message, cause);
        this.clazz = clazz;
    }

    @Override
    public String toString()
    {
        return "RemoteClassNotFoundException{" +
                "message=" + getMessage()+
                '}';
    }
}
