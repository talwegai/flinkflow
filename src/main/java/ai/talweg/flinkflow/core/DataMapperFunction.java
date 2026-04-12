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

import net.sf.saxon.s9api.*;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.functions.OpenContext;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A Flink RichMapFunction that performs data transformation using XSLT 3.0.
 * This function uses the Saxon-HE library for XSLT processing.
 */
public class DataMapperFunction extends RichMapFunction<String, String> {
    /**
     * The path to the XSLT stylesheet file.
     */
    private final String xsltPath;

    /**
     * The Saxon processor used for XSLT operations.
     */
    private transient Processor processor;

    /**
     * The compiled XSLT executable.
     */
    private transient XsltExecutable executable;

    /**
     * Constructs a DataMapperFunction with the specified XSLT path.
     * 
     * @param xsltPath the path to the XSLT stylesheet
     */
    public DataMapperFunction(String xsltPath) {
        this.xsltPath = xsltPath;
    }

    /**
     * Initializes the Saxon processor and compiles the XSLT stylesheet.
     * 
     * @param parameters the Flink configuration parameters
     * @throws Exception if XSLT compilation fails
     */
    @Override
    public void open(OpenContext parameters) throws Exception {
        processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        executable = compiler.compile(new StreamSource(xsltPath));
    }

    /**
     * Transforms the input string using the compiled XSLT stylesheet.
     * 
     * @param input the input string to be transformed
     * @return the transformed string
     * @throws Exception if transformation fails
     */
    @Override
    public String map(String input) throws Exception {
        Xslt30Transformer transformer = executable.load30();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer serializer = processor.newSerializer(out);
        serializer.setOutputProperty(Serializer.Property.METHOD, "text");
        serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");

        // Pass the raw string input as the context item to the XSLT templates
        transformer.applyTemplates(new XdmAtomicValue(input), serializer);

        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
