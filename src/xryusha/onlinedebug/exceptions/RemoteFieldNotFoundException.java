package xryusha.onlinedebug.exceptions;

public class RemoteFieldNotFoundException extends RemoteExceptionBase
{
    private String field;
    private String clazz;

    public RemoteFieldNotFoundException(String message, String field, String clazz)
    {
        super(message);
        this.field = this.field;
        this.clazz = clazz;
    }

    public RemoteFieldNotFoundException(String message, Throwable cause, String method, String clazz)
    {
        super(message, cause);
        this.field = method;
        this.clazz = clazz;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("RemoteFieldNotFoundException{");
        sb.append("field='").append(field).append('\'');
        sb.append(", clazz='").append(clazz).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
