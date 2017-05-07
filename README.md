utility which may be useful for other developers. This utility allows configuration-based non-intrusive monitoring and manipulation of remote jvm. Upon specified event type (method exit/entry, thrown exception, field modification or arriving to specific source code line) required actions (data logging, assigning value to variables, aritrary method invocation or enforcing immediate return from currently executed method with configured return value) may be performed in conditional way.

Most obvious use cases - troubleshooting, code behavior investigation, including 3rd party libraries compiled without debug information or reproducing of different scenarios (assigning to variables requried values and modifying method return values).
Various configuration examples may be found in /xryusha/onlinedebug/testcases/**.xml files, detailed configuration elements explanation provided in example_breakpoints.xml, example_actions.xml, example_rvalues.xml and example_condition.xml.

As utility is based on java debug interface, remote jvm must be launched with remote debug flags (-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address={PORT}
