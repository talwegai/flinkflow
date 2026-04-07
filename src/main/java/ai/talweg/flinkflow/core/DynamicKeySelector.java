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

import org.apache.flink.api.java.functions.KeySelector;
import org.codehaus.janino.SimpleCompiler;
import java.lang.reflect.Method;

/**
 * A Flink KeySelector that allows for dynamic Java code execution to extract a
 * key from an element.
 * It uses the Janino compiler to compile a provided logic snippet into a
 * temporary Java class at runtime.
 */
public class DynamicKeySelector implements KeySelector<String, String> {

    /**
     * The Java code snippet to be executed within the generated class's select
     * method.
     */
    private final String codeBody;

    /**
     * The instance of the dynamically generated class.
     */
    private transient Object dynamicSelectorInstance;

    /**
     * The method of the dynamic class that performs the key extraction.
     */
    private transient Method selectMethod;

    /**
     * Constructs a DynamicKeySelector with the specified code snippet.
     * 
     * @param codeBody the code snippet to be embedded in the select method
     */
    public DynamicKeySelector(String codeBody) {
        this.codeBody = codeBody;
    }

    /**
     * Initializes the dynamically generated class if not already done.
     * Generates and compiles a temporary class containing the provided code
     * snippet.
     * 
     * @throws Exception if code compilation or instance creation fails
     */
    private void init() throws Exception {
        if (dynamicSelectorInstance == null) {
            String className = "DynamicKeySelector_" + System.nanoTime();
            String classCode = "public class " + className + " {\n" +
                    "    public String select(String input) throws Exception {\n" +
                    "        " + codeBody + "\n" +
                    "    }\n" +
                    "}";

            SimpleCompiler compiler = new SimpleCompiler();
            compiler.cook(classCode);
            Class<?> clazz = compiler.getClassLoader().loadClass(className);
            dynamicSelectorInstance = clazz.getDeclaredConstructor().newInstance();
            selectMethod = clazz.getMethod("select", String.class);
        }
    }

    /**
     * Invokes the dynamically compiled key extraction logic on the input stream
     * element.
     * 
     * @param value the input element
     * @return the extracted key
     * @throws Exception if code execution fails
     */
    @Override
    public String getKey(String value) throws Exception {
        init();
        return (String) selectMethod.invoke(dynamicSelectorInstance, value);
    }
}
