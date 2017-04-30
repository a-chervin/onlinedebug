package xryusha.onlinedebug.exceptions;

public class RemoteMethodNotFoundException extends RemoteExceptionBase
{
    private String method;
    private String clazz;

    public RemoteMethodNotFoundException(String message, String method, String clazz)
    {
        super(message);
        this.method = method;
        this.clazz = clazz;
    }

    public RemoteMethodNotFoundException(String message, Throwable cause, String method, String clazz)
    {
        super(message, cause);
        this.method = method;
        this.clazz = clazz;
    }

    @Override
    public String toString()
    {
        return "RemoteMethodNotFoundException{" +
                "method='" + method + '\'' +
                ", clazz='" + clazz + '\'' +
                '}';
    }
}
