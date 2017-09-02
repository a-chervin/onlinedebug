/**
 * Licensed to the a-chervin (ax.chervin@gmail.com) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * a-chervin licenses this file under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xryusha.onlinedebug.config;


import xryusha.onlinedebug.config.breakpoints.ExceptionBreakpointSpec;
import xryusha.onlinedebug.config.breakpoints.LineBreakpointSpec;
import xryusha.onlinedebug.config.breakpoints.MethodTargetingBreakPoint;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Represents configuration document.
 * Verified agains schema file {@link #SCHEMA_FILE}
 */
@XmlRootElement(name="configuration")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({ExceptionBreakpointSpec.class, LineBreakpointSpec.class, MethodTargetingBreakPoint.class})
public class Configuration
{
    /**
     * root element names
     */
    public static class Elements
    {
        public static final String ACTION_PRINT = "print";
        public static final String ACTION_ASSIGN = "assign";
        public static final String ACTION_RETURN = "return";
        public static final String ACTION_INVOKE = "invoke";

        public static final String BREAKPOINT_LOCATION = "atLocation";
        public static final String BREAKPOINT_EXCEPTION = "onException";
        public static final String BREAKPOINT_METHODENTRY = "methodEntry";
        public static final String BREAKPOINT_METHODEXIT = "methodExit";
        public static final String BREAKPOINT_FIELDMODIFICATION = "fieldModification";

        public static final String CONDITION_AND = "and";
        public static final String CONDITION_OR = "or";
        public static final String CONDITION_EQUAL = "equal";
        public static final String CONDITION_ISNULL = "isnull";
        public static final String CONDITION_LESS = "less";
        public static final String CONDITION_LOCATION = "location";

        public static final String VALUE_CALL = "call";
        public static final String VALUE_CONSTRUCTOR = "constructor";
        public static final String VALUE_CONST = "const";
        public static final String VALUE_THROWN_EXCEPTION = "thrownException";
        public static final String VALUE_CHAIN = "chain";
        public static final String VALUE_REFPATH = "ref";
        public static final String VALUE_ARRAYINDEX = "array";
        public static final String VALUE_THREADSTACK = "threadStack";
        public static final String VALUE_DUMP = "dump";
        public static final String VALUE_RETURNVALUE = "returnValue";
        public static final String VALUE_THREADNAME = "threadName";
        public static final String VALUE_MODIFICATIONCURRENT = "modificationCurrent";
        public static final String VALUE_MODIFICATIONNEW = "modificationNew";
        public static final String VALUE_TIME = "time";
        public static final String VALUE_ARGPLACEHOLDER = "arg";
    }

    public static final String SCHEMA_FILE = "Configuration.xsd";

    @XmlElement(name = "entry")
    private List<ConfigEntry> entries = new ArrayList<>();

    public static Configuration load(Path configFile) throws Exception
    {
        return load(configFile, true);
    }

    public static Configuration load(Path configFile, boolean validate) throws Exception
    {
        Configuration configuration;
        if ( validate )
          validateAgainstSchema(configFile);

        configuration = JAXB.unmarshal(configFile.toFile(), Configuration.class);
        return configuration;
    }

    public static InputStream getSchemaAsStream() throws IOException
    {
        class F{};
        Class thisClass = F.class.getEnclosingClass();
        URL schemaURL = thisClass.getResource(SCHEMA_FILE);
        InputStream schemaStream = schemaURL.openStream();
        return schemaStream;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("Configuration{");
        sb.append("entries=").append(entries);
        sb.append('}');
        return sb.toString();
    }

    private static void validateAgainstSchema(Path configFile) throws Exception
    {
        InputStream schemaStream = getSchemaAsStream();
        SchemaFactory factory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(schemaStream));
        Validator validator = schema.newValidator();
        try {
            validator.validate(new StreamSource(new FileReader(configFile.toFile())));
        } catch (Exception ex) {
            System.out.println("XML validation failed: " + ex.getMessage());
            throw new Exception(ex.getMessage());
        }
    }

    public List<ConfigEntry> getEntries()
    {
        return entries;
    }
}
