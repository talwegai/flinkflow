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

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.functions.OpenContext;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.Method;

/**
 * A Flink RichMapFunction that allows for dynamic Java code execution.
 * It uses the Janino compiler to compile a provided logic snippet into a
 * temporary Java class at runtime.
 */
public class DynamicCodeFunction extends RichMapFunction<String, String> {

    /**
     * The Java code snippet to be executed within the generated class's method.
     */
    private final String codeBody;

    /**
     * The instance of the dynamically generated class.
     */
    private transient Object dynamicMapperInstance;

    /**
     * The method of the dynamic class that performs the transformation.
     */
    private transient Method mapMethod;

    /**
     * Constructs a DynamicCodeFunction with the specified code snippet.
     * 
     * @param codeBody the code snippet to be embedded in the transformation method
     */
    public DynamicCodeFunction(String codeBody) {
        this.codeBody = codeBody;
    }

    /**
     * Generates and compiles a temporary class containing the provided code
     * snippet.
     * Initializes the instance and method required for execution.
     * 
     * @param parameters the Flink configuration parameters
     * @throws Exception if code compilation or instance creation fails
     */
    @Override
    public void open(OpenContext parameters) throws Exception {
        String className = "DynamicMapper_" + System.nanoTime();

        // This class template implements a simple transformation method which we'll
        // call reflectively
        String classCode = "import org.apache.flink.metrics.MetricGroup;\n" +
                "public class " + className + " {\n" +
                "    public String execute(String input, MetricGroup metrics) throws Exception {\n" +
                "        " + codeBody + "\n" +
                "    }\n" +
                "}";

        SimpleCompiler compiler = new SimpleCompiler();
        compiler.cook(classCode);
        Class<?> clazz = compiler.getClassLoader().loadClass(className);
        dynamicMapperInstance = clazz.getDeclaredConstructor().newInstance();
        mapMethod = clazz.getMethod("execute", String.class, org.apache.flink.metrics.MetricGroup.class);
    }

    /**
     * Invokes the dynamically compiled code transformation on the input stream
     * element.
     * 
     * @param value the input element
     * @return the result of the transformation
     * @throws Exception if code execution fails or the mapper is not initialized
     */
    @Override
    public String map(String value) throws Exception {
        if (dynamicMapperInstance == null) {
            throw new RuntimeException("Dynamic mapper not initialized");
        }
        return (String) mapMethod.invoke(dynamicMapperInstance, value, getRuntimeContext().getMetricGroup());
    }
}
