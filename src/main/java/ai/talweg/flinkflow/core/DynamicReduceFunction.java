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
import org.apache.flink.api.common.functions.RichReduceFunction;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.Method;

/**
 * A Flink ReduceFunction that allows for dynamic Java code execution.
 * It uses the Janino compiler to compile a provided logic snippet into a
 * temporary Java class at runtime.
 */
public class DynamicReduceFunction extends RichReduceFunction<String> {

    /**
     * The Java code snippet to be executed within the generated class's method.
     */
    private final String codeBody;

    /**
     * The instance of the dynamically generated class.
     */
    private transient Object dynamicReduceInstance;

    /**
     * The method of the dynamic class that performs the reduction.
     */
    private transient Method reduceMethod;

    /**
     * Constructs a DynamicReduceFunction with the specified code snippet.
     * 
     * @param codeBody the code snippet to be embedded in the reduction method
     */
    public DynamicReduceFunction(String codeBody) {
        this.codeBody = codeBody;
    }

    /**
     * Generates and compiles a temporary class containing the provided code
     * snippet.
     * Initializes the instance and method required for execution.
     * 
     * @throws Exception if code compilation or instance creation fails
     */
    @Override
    public void open(OpenContext parameters) throws Exception {
        if (dynamicReduceInstance == null) {
            String className = "DynamicReduce_" + System.nanoTime();

            // This class template implements a simple reduction method which we'll
            // call reflectively
            String classCode = "import org.apache.flink.metrics.MetricGroup;\n" +
                    "public class " + className + " {\n" +
                    "    public String execute(String value1, String value2, MetricGroup metrics) throws Exception {\n"
                    +
                    "        " + codeBody + "\n" +
                    "    }\n" +
                    "}";

            SimpleCompiler compiler = new SimpleCompiler();
            compiler.cook(classCode);
            Class<?> clazz = compiler.getClassLoader().loadClass(className);
            dynamicReduceInstance = clazz.getDeclaredConstructor().newInstance();
            reduceMethod = clazz.getMethod("execute", String.class, String.class,
                    org.apache.flink.metrics.MetricGroup.class);
        }
    }

    /**
     * Invokes the dynamically compiled code reduction on the two input values.
     * 
     * @param value1 the first input element
     * @param value2 the second input element
     * @return the result of the reduction
     * @throws Exception if code execution fails or the reducer is not initialized
     */
    @Override
    public String reduce(String value1, String value2) throws Exception {
        return (String) reduceMethod.invoke(dynamicReduceInstance, value1, value2,
                getRuntimeContext().getMetricGroup());
    }
}
