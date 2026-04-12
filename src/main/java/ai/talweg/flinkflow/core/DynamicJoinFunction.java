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

import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction;
import org.apache.flink.util.Collector;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.Method;

/**
 * A Flink ProcessJoinFunction that allows for dynamic Java code execution
 * during a join.
 * It uses the Janino compiler to compile a provided join logic snippet into a
 * temporary Java class at runtime.
 */
public class DynamicJoinFunction extends ProcessJoinFunction<String, String, String> {

    /**
     * The Java code snippet to be executed within the generated class's join
     * method.
     */
    private final String codeBody;

    /**
     * The instance of the dynamically generated class.
     */
    private transient Object dynamicJoinerInstance;

    /**
     * The method of the dynamic class that performs the join operation.
     */
    private transient Method joinMethod;

    /**
     * Constructs a DynamicJoinFunction with the specified code snippet.
     * 
     * @param codeBody the code snippet to be embedded in the join method
     */
    public DynamicJoinFunction(String codeBody) {
        this.codeBody = codeBody;
    }

    /**
     * Generates and compiles a temporary class containing the provided join code
     * snippet.
     * Initializes the instance and method required for execution.
     * 
     * @param parameters the Flink configuration parameters
     * @throws Exception if code compilation or instance creation fails
     */
    @Override
    public void open(OpenContext parameters) throws Exception {
        String className = "DynamicJoiner_" + System.nanoTime();

        String classCode = "import org.apache.flink.metrics.MetricGroup;\n" +
                "public class " + className + " {\n" +
                "    public String join(String left, String right, MetricGroup metrics) throws Exception {\n" +
                "        " + codeBody + "\n" +
                "    }\n" +
                "}";

        SimpleCompiler compiler = new SimpleCompiler();
        compiler.cook(classCode);
        Class<?> clazz = compiler.getClassLoader().loadClass(className);
        dynamicJoinerInstance = clazz.getDeclaredConstructor().newInstance();
        joinMethod = clazz.getMethod("join", String.class, String.class, org.apache.flink.metrics.MetricGroup.class);
    }

    /**
     * Invokes the dynamically compiled join logic on the input elements from both
     * streams.
     * 
     * @param left    the element from the left stream
     * @param right   the element from the right stream
     * @param context the context of the join operation
     * @param out     the collector for emitting the result
     * @throws Exception if code execution fails or the joiner is not initialized
     */
    @Override
    public void processElement(String left, String right, Context context, Collector<String> out) throws Exception {
        if (dynamicJoinerInstance == null) {
            throw new RuntimeException("Dynamic joiner not initialized");
        }
        String result = (String) joinMethod.invoke(dynamicJoinerInstance, left, right,
                getRuntimeContext().getMetricGroup());
        if (result != null) {
            out.collect(result);
        }
    }
}
