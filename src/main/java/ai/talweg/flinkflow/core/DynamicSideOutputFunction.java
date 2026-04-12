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
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.Method;

/**
 * A Flink ProcessFunction that allows for dynamic Java code execution.
 * It uses the Janino compiler to compile a provided logic snippet into a
 * temporary Java class at runtime.
 * It also supports emitting data to a side output.
 */
public class DynamicSideOutputFunction extends ProcessFunction<String, String> {

    /**
     * The Java code snippet to be executed.
     */
    private final String codeBody;

    /**
     * The OutputTag for the side output.
     */
    private final OutputTag<String> outputTag;

    /**
     * The instance of the dynamically generated class.
     */
    private transient Object dynamicSideOutputInstance;

    /**
     * The method of the dynamic class that performs the operation.
     */
    private transient Method processMethod;

    /**
     * Constructs a DynamicSideOutputFunction with the specified code snippet and
     * output tag.
     * 
     * @param codeBody       the code snippet to be embedded in the process method
     * @param sideOutputName the name of the side output
     */
    public DynamicSideOutputFunction(String codeBody, String sideOutputName) {
        this.codeBody = codeBody;
        this.outputTag = new OutputTag<String>(sideOutputName) {
        };
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
        String className = "DynamicSideOutput_" + System.nanoTime();

        // This class template implements a simple method which we'll call reflectively.
        // It provides input, out (for main stream), and side (for side output)
        String classCode = "import org.apache.flink.util.Collector;\n" +
                "import org.apache.flink.metrics.MetricGroup;\n" +
                "public class " + className + " {\n" +
                "    public void execute(String input, Collector<String> out, Context ctx, MetricGroup metrics) throws Exception {\n"
                +
                "        " + codeBody + "\n" +
                "    }\n" +
                "    \n" +
                "    public interface Context {\n" +
                "        void output(String value);\n" +
                "    }\n" +
                "}";

        SimpleCompiler compiler = new SimpleCompiler();
        compiler.cook(classCode);
        Class<?> clazz = compiler.getClassLoader().loadClass(className);
        dynamicSideOutputInstance = clazz.getDeclaredConstructor().newInstance();

        Class<?> contextInterface = clazz.getDeclaredClasses()[0];

        processMethod = clazz.getMethod("execute", String.class, Collector.class, contextInterface,
                org.apache.flink.metrics.MetricGroup.class);
    }

    /**
     * Invokes the dynamically compiled code.
     * 
     * @param value the input element
     * @param ctx   the context supporting side outputs
     * @param out   the collector for emitting results
     * @throws Exception if code execution fails or the process is not initialized
     */
    @Override
    public void processElement(String value, Context ctx, Collector<String> out) throws Exception {
        if (dynamicSideOutputInstance == null) {
            throw new RuntimeException("Dynamic side-output not initialized");
        }

        // We use an anonymous class or a proxy to implement the inner Context interface
        // since Janino compiled it. A simple proxy works beautifully.
        Object contextProxy = java.lang.reflect.Proxy.newProxyInstance(
                dynamicSideOutputInstance.getClass().getClassLoader(),
                new Class[] { processMethod.getParameterTypes()[2] },
                (proxy, method, args) -> {
                    if (method.getName().equals("output")) {
                        ctx.output(outputTag, (String) args[0]);
                        return null;
                    }
                    return null;
                });

        processMethod.invoke(dynamicSideOutputInstance, value, out, contextProxy, getRuntimeContext().getMetricGroup());
    }
}
