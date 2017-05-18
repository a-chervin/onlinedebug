//package xryusha.onlinedebug.runtime.func;
//
//import java.util.function.Function;
//
//
//public class ErrorproneFunctionWrapper<T,R> implements Function<T,R>
//{
//    private ErrorproneFunction<T,R> func;
//
//    public ErrorproneFunctionWrapper(ErrorproneFunction<T, R> func)
//    {
//        this.func = func;
//    }
//
//    @Override
//    public R apply(T t)
//    {
//        try {
//            return func.apply(t);
//        } catch (Exception ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//}
