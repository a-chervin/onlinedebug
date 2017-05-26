<h4>The project is aimed to be useful for java developer. The most obvious use case of this utility<br>
    is simplification of troubleshooting process and code investigation in places where regular debugging
    is impossible or problematic:</h4>
    
<ul>
  <li><strong>Q</strong>A/system tests etc environments  <!--<img src="images/qa_env_2.png" />-->
  <img src="images/very_far_4.png"/>
  </li>
  <li>Investigation of sporadically happening problems or long running (say, overnight) data collection</li>
  <li>Running code is too far and networking overhead becomes significant 
      <img src="images/very_far_4.png"/>
  </li>
  <li>
   <strong>R</strong>eproducing complicated scenario: for example, just to check smsEngine.sendMessage() tons  of irrelevant configurations required:
   <pre>
        void alert(String customerId, String storeId, String productId)
        {
           Customer customer;
           Store store;
           if (  (customer=customerRepository.getCustomer(customerId)) != null
                  &&  productVerifier.isValid(productId, customer)
                  && (store=storeRepository.getStore(storeId)) != null
                  &&  zoneService.isSameArea(customer, store)
                  &&  discountEngine.getDiscount(customer, store, productId) > 0 ) {
                      smsEngine.sendMessage(customer, 
                                           "Don't miss! we have something interesting..");
           } // if
        } // alert
   </pre>
 </li>
 <li>
    “<strong>f</strong>ast and dirty” workaround for problems which can’t be fixed currently but blocks working on something else:
     <pre>
        void ourMethod()
        {
            //// something
            if ( !valid() )
               throw new IllegalArgumentException("Ooops..");
            //// something
        }

         boolean valid()
         { 
            return false; 
         }     
 </pre>
 </li>
 <li><strong>o</strong>r just library source code is not available but still must be investigated in different cases </li>
</ul>

Without modification/recompilation of applicative code this utility allows configuration-based non-intrusive monitoring and manipulation of remote jvm. Upon specified event type (arriving to specific source code line, method entry/exit, throwing exception or field modification) it performs required actions, as:
<ul>
<li><strong>c</strong>ollecting and logging to console or file information of:
      <ul>
          <li>specified variables values</li>
          <li>all visible local variables values</li>
          <li>evaluated expression value</li>
          <li>method arguments</li>
          <li>method return value</li>
          <li>stacktrace</li>
          <li>old/new values in case of field value modification</li>
          <li>etc</li>
      </ul>
   </li>
   <li>
   <strong>a</strong>ssigning value to variables where value may by 
      <ul>
          <li>constant value</li>
          <li>value of another variable</li>
          <li>result of evaluated expression</li>
          <li>new instance from arbitrary constructor signature</li>
       </ul>
    </li>
    <li><strong>a</strong>rbitrary method invocation </li>
    <li>
    <strong>e</strong>nforcing return from currently executed method with configured return value: 
      <pre>
          boolean valid()
          {
             System.out.println("so bad luck..");
             return false;  ⇐ without code changing returned value may be enforced to true
          }      
      </pre>
    </li>    
</ul>

   All specified may be performed in conditional way (<i>equals(..,..)</i>, <i>less/greater</i>, <i>isnull</i>, <i>not(...)</i>, nested <i>or(...)</i> / <i>and(...)</i>)
 
The utility is based on java debug interface and  no application bytecode modifications involved, as a result  when it disconnects from monitored application the last returns to its initial state. 
 
 
   Various configuration examples may be found in <i>/xryusha/onlinedebug/testcases/**.xml</i> files, detailed configuration elements explanation are provided in <i>example_breakpoints.xml</i>, <i>example_actions.xml</i>, <i>example_rvalues.xml</i> and <i>example_condition.xml</i>.
