package xryusha.onlinedebug.exceptions;

public class RemoteDataConvertionException extends RemoteExceptionBase
{
    private String type;

    public RemoteDataConvertionException(String message, String type)
    {
        super(message);
        this.type = type;
    }

    public RemoteDataConvertionException(String message, Throwable cause, String type)
    {
        super(message, cause);
        this.type = type;
    }

    @Override
    public String toString()
    {
        return "RemoteDataConvertionException{" +
                "type='" + type + '\'' +
                '}';
    }
}
