//package onlinedebug.exceptions;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class AggregatedException extends Exception
//{
//    private ArrayList<Throwable> aggregation = new ArrayList<>();
//
//    public AggregatedException(String message)
//    {
//        super(message);
//    }
//
//    public AggregatedException(String message, Throwable ... exceptions )
//    {
//        super(message);
//        for ( int inx = 0; exceptions != null && inx < exceptions.length ; inx++)
//            aggregation.add(exceptions[inx]);
//    }
//
//    public AggregatedException(String message, List<Throwable> exceptions )
//    {
//        super(message);
//        if ( exceptions != null )
//            aggregation.addAll(exceptions);
//    }
//
//    public void add(Throwable th)
//    {
//        aggregation.add(th);
//    }
//
//    @Override
//    public String toString()
//    {
//        final StringBuilder sb = new StringBuilder("AggregatedException{");
//        sb.append("aggregation=").append(aggregation);
//        sb.append('}');
//        return sb.toString();
//    }
//}
