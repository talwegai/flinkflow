/*
 * Copyright 2026 Talweg Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package ai.talweg.flinkflow.core;

import org.apache.flink.api.common.functions.ReduceFunction;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.Method;

/**
 * A plain (non-rich) Flink {@link ReduceFunction} backed by a dynamically
 * compiled Janino snippet.
 *
 * <p>Flink's {@code WindowedStream.reduce(ReduceFunction)} does not accept
 * a {@link org.apache.flink.api.common.functions.RichFunction}, so this
 * lightweight variant is used exclusively inside window operations while
 * {@link DynamicReduceFunction} (which extends {@code RichReduceFunction})
 * is used for regular keyed-stream reduces.
 */
public class DynamicWindowReduceFunction implements ReduceFunction<String> {

    private final String codeBody;

    // Compiled lazily on first call (the function is not serialized, so
    // transient would be pointless here — but we init on first use).
    private transient Object instance;
    private transient Method method;

    public DynamicWindowReduceFunction(String codeBody) {
        this.codeBody = codeBody;
    }

    private void ensureInit() throws Exception {
        if (instance == null) {
            String className = "DynamicWindowReduce_" + System.nanoTime();
            String classCode =
                    "public class " + className + " {\n" +
                    "    public String execute(String value1, String value2) throws Exception {\n" +
                    "        " + codeBody + "\n" +
                    "    }\n" +
                    "}";

            SimpleCompiler compiler = new SimpleCompiler();
            compiler.cook(classCode);
            Class<?> clazz = compiler.getClassLoader().loadClass(className);
            instance = clazz.getDeclaredConstructor().newInstance();
            method = clazz.getMethod("execute", String.class, String.class);
        }
    }

    @Override
    public String reduce(String value1, String value2) throws Exception {
        ensureInit();
        return (String) method.invoke(instance, value1, value2);
    }
}
