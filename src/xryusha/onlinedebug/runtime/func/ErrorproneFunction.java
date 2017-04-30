package xryusha.onlinedebug.runtime.func;

public interface ErrorproneFunction<T,R>
{
    R apply(T t) throws Exception;
}
