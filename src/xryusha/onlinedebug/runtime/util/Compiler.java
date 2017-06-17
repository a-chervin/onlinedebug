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

package xryusha.onlinedebug.runtime.util;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.tools.*;


public enum Compiler
{
    INSTANCE;

    private final JavaCompiler compiler;
    private final InMemoryManager memoryMgr;

    Compiler()
    {
        compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null);
        memoryMgr = new InMemoryManager(manager);
    }

    public byte[] compile(String name, String code, String vmVersion) throws IOException
    {
        JavaFileObject file = new StringSource(name, code);
        List<JavaFileObject> list = Arrays.asList(file);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<String> options = new ArrayList<>();
        options.add("-g");
        if ( vmVersion != null ) {
            options.addAll(Arrays.asList("-source", vmVersion, "-target", vmVersion));
        }

        JavaCompiler.CompilationTask task = compiler.getTask(null, memoryMgr, diagnostics, options, null, list);
        boolean success = task.call();
        if ( !success ) {
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                System.out.println(diagnostic);
            }
            throw new IOException("Compilation failed");
        }
        ByteArrayOutputStream compiled = memoryMgr.getCompiled().get(name);
        memoryMgr.getCompiled().remove(name);
        return compiled.toByteArray();
    } // compile

    private static URI fromName(String name, String extension)
    {
        String uri = "string:///" + name.replace('.','/') + extension;
        return URI.create(uri);
    }

    private class StringSource extends SimpleJavaFileObject
    {
        String code;

        StringSource(String name, String code)
        {
            super(fromName(name, Kind.SOURCE.extension),Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors)
        {
            return code;
        }
    } // StringSource

    private class CompiledClass extends SimpleJavaFileObject
    {
        private ByteArrayOutputStream baos;

        CompiledClass(String name, ByteArrayOutputStream baos)
        {
            super(fromName(name, Kind.SOURCE.extension), Kind.CLASS);
            this.baos = baos;
        }

        public OutputStream openOutputStream()
        {
            return baos;
        }
    } // class CompiledClass

    private class InMemoryManager extends ForwardingJavaFileManager
    {
        private Map<String,ByteArrayOutputStream> compiled = new ConcurrentHashMap<>();

        public InMemoryManager(JavaFileManager fileManager)
        {
            super(fileManager);
        }

        public Map<String, ByteArrayOutputStream> getCompiled()
        {
            return compiled;
        }

        public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location,
                                                   String className,
                                                   JavaFileObject.Kind kind,
                                                   FileObject sibling) throws IOException
        {
            if (kind == JavaFileObject.Kind.CLASS) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                compiled.put(className, baos);
                return new CompiledClass(className, baos);
            } else {
                return super.getJavaFileForOutput(location, className, kind, sibling);
            }
        } // getJavaFileForOutput
    } // InMemoryManager
}
