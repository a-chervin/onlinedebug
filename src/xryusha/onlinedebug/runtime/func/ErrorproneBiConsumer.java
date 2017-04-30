package xryusha.onlinedebug.runtime.func;

public interface ErrorproneBiConsumer<T1,T2>
{
    void apply(T1 t1, T2 t2) throws Exception;
}
