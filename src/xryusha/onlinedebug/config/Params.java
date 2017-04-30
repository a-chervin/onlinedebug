//package onlinedebug.config;
//
//import onlinedebug.config.values.*;
//
//import javax.xml.bind.annotation.XmlElement;
//import javax.xml.bind.annotation.XmlElements;
//import javax.xml.bind.annotation.XmlType;
//import java.util.List;
//
//@XmlType
//public class Params
//{
//    @XmlElements({
//            @XmlElement(name = "const", type = Const.class),
//            @XmlElement(name = "path", type = RefPath.class),
//            @XmlElement(name = "ref-chain", type = RefChain.class),
//            @XmlElement(name = "call", type = CallSpec.class)
//    })
//    protected List<RValue> params;
//
//    public List<RValue> getParams()
//    {
//        return params;
//    }
//
//    public void setParams(List<RValue> params)
//    {
//        this.params = params;
//    }
//
//    @Override
//    public String toString()
//    {
//        return "Params{" +
//                "params=" + params +
//                '}';
//    }
//}
