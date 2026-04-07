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

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;

/**
 * Factory class for creating Flink processing functions (mappers, filters,
 * key selectors, etc.).
 *
 * <p>Each factory method accepts an explicit {@code language} parameter
 * ("java" or "python"). Java snippets are compiled at runtime by Janino;
 * Python snippets are executed by the embedded GraalVM Python engine.
 */
public class ProcessorFactory {

    // -----------------------------------------------------------------------
    // Map / Process
    // -----------------------------------------------------------------------

    /**
     * Creates a MapFunction backed by the appropriate runtime for {@code language}.
     *
     * @param codeBody the transformation snippet
     * @param language "java" (Janino) or "python" (GraalVM)
     * @return a Flink MapFunction
     */
    public static MapFunction<String, String> createMapper(String codeBody, String language) {
        if ("python".equalsIgnoreCase(language)) {
            return new DynamicPythonMapFunction(codeBody);
        }
        return new DynamicCodeFunction(codeBody);
    }

    /**
     * Creates a MapFunction using the default Java/Janino runtime.
     *
     * @param codeBody the transformation snippet
     * @return a Flink MapFunction
     */
    public static MapFunction<String, String> createMapper(String codeBody) {
        return createMapper(codeBody, "java");
    }

    // -----------------------------------------------------------------------
    // Filter
    // -----------------------------------------------------------------------

    /**
     * Creates a FilterFunction backed by the appropriate runtime for {@code language}.
     *
     * @param codeBody the filtering snippet (must return boolean)
     * @param language "java" (Janino) or "python" (GraalVM)
     * @return a Flink FilterFunction
     */
    public static FilterFunction<String> createFilter(String codeBody, String language) {
        if ("python".equalsIgnoreCase(language)) {
            return new DynamicPythonFilterFunction(codeBody);
        }
        return new DynamicFilterFunction(codeBody);
    }

    /**
     * Creates a FilterFunction using the default Java/Janino runtime.
     *
     * @param codeBody the filtering snippet
     * @return a Flink FilterFunction
     */
    public static FilterFunction<String> createFilter(String codeBody) {
        return createFilter(codeBody, "java");
    }

    // -----------------------------------------------------------------------
    // FlatMap
    // -----------------------------------------------------------------------

    /**
     * Creates a FlatMapFunction backed by the appropriate runtime for {@code language}.
     *
     * @param codeBody the flat-map snippet (Python must return a list)
     * @param language "java" (Janino) or "python" (GraalVM)
     * @return a Flink FlatMapFunction
     */
    public static FlatMapFunction<String, String> createFlatMap(String codeBody, String language) {
        if ("python".equalsIgnoreCase(language)) {
            return new DynamicPythonFlatMapFunction(codeBody);
        }
        return new DynamicFlatMapFunction(codeBody);
    }

    /**
     * Creates a FlatMapFunction using the default Java/Janino runtime.
     *
     * @param codeBody the flat-map snippet
     * @return a Flink FlatMapFunction
     */
    public static FlatMapFunction<String, String> createFlatMap(String codeBody) {
        return createFlatMap(codeBody, "java");
    }

    // -----------------------------------------------------------------------
    // Reduce (Janino-only; Python not yet supported for stateful reduction)
    // -----------------------------------------------------------------------

    /**
     * Creates a new ReduceFunction based on dynamic code.
     *
     * @param codeBody the logic for the reduction operation
     * @param language the language of the code (java, python)
     * @return a new ReduceFunction instance
     */
    public static ReduceFunction<String> createReducer(String codeBody, String language) {
        if ("python".equalsIgnoreCase(language)) {
            return new DynamicPythonReduceFunction(codeBody);
        }
        return new DynamicReduceFunction(codeBody);
    }

    public static ReduceFunction<String> createReducer(String codeBody) {
        return createReducer(codeBody, "java");
    }

    /**
     * Creates a plain (non-rich) ReduceFunction for windowed reductions.
     *
     * @param codeBody the logic for the reduction operation
     * @param language the language of the code (java, python)
     * @return a new ReduceFunction instance
     */
    public static ReduceFunction<String> createWindowReducer(String codeBody, String language) {
        if ("python".equalsIgnoreCase(language)) {
            return new DynamicPythonWindowReduceFunction(codeBody);
        }
        return new DynamicWindowReduceFunction(codeBody);
    }

    public static ReduceFunction<String> createWindowReducer(String codeBody) {
        return createWindowReducer(codeBody, "java");
    }

    // -----------------------------------------------------------------------
    // Side Output (Janino-only)
    // -----------------------------------------------------------------------

    /**
     * Creates a new ProcessFunction based on dynamic code that emits to a side
     * output.
     *
     * @param codeBody       the logic for the side output operation
     * @param sideOutputName the tag name of the side output
     * @param language        the language of the code (java, python)
     * @return a new ProcessFunction instance
     */
    public static ProcessFunction<String, String> createSideOutput(String codeBody, String sideOutputName, String language) {
        if ("python".equalsIgnoreCase(language)) {
            return new DynamicPythonSideOutputFunction(codeBody, sideOutputName);
        }
        return new DynamicSideOutputFunction(codeBody, sideOutputName);
    }

    public static ProcessFunction<String, String> createSideOutput(String codeBody, String sideOutputName) {
        return createSideOutput(codeBody, sideOutputName, "java");
    }

    // -----------------------------------------------------------------------
    // Join (Janino-only)
    // -----------------------------------------------------------------------

    /**
     * Creates a new ProcessJoinFunction based on dynamic code.
     *
     * @param codeBody the logic for the join operation
     * @param language the language of the code (java, python)
     * @return a new ProcessJoinFunction instance
     */
    public static ProcessJoinFunction<String, String, String> createJoiner(String codeBody, String language) {
        if ("python".equalsIgnoreCase(language)) {
            return new DynamicPythonJoinFunction(codeBody);
        }
        return new DynamicJoinFunction(codeBody);
    }

    public static ProcessJoinFunction<String, String, String> createJoiner(String codeBody) {
        return createJoiner(codeBody, "java");
    }

    // -----------------------------------------------------------------------
    // KeySelector (Janino-only)
    // -----------------------------------------------------------------------

    /**
     * Creates a new KeySelector based on dynamic code.
     *
     * @param codeBody the logic for key extraction
     * @param language the language of the code (java, python)
     * @return a new KeySelector instance
     */
    public static KeySelector<String, String> createKeySelector(String codeBody, String language) {
        if ("python".equalsIgnoreCase(language)) {
            return new DynamicPythonKeySelector(codeBody);
        }
        return new DynamicKeySelector(codeBody);
    }

    public static KeySelector<String, String> createKeySelector(String codeBody) {
        return createKeySelector(codeBody, "java");
    }

    // -----------------------------------------------------------------------
    // DataMapper (XSLT)
    // -----------------------------------------------------------------------

    /**
     * Creates a new MapFunction based on an XSLT transformation.
     *
     * @param xsltPath the path to the XSLT stylesheet
     * @return a new DataMapperFunction instance
     */
    public static MapFunction<String, String> createDataMapper(String xsltPath) {
        return new DataMapperFunction(xsltPath);
    }

    // -----------------------------------------------------------------------
    // Async HTTP Lookup
    // -----------------------------------------------------------------------

    /**
     * Creates a new AsyncFunction for dynamic HTTP lookups.
     *
     * @param urlCode      the logic to generate the URL
     * @param responseCode the logic to handle the HTTP response
     * @param authCode     the logic to handle the auth header
     * @return a new DynamicAsyncHttpFunction instance
     */
    public static DynamicAsyncHttpFunction createAsyncHttpLookup(String urlCode, String responseCode, String authCode) {
        return new DynamicAsyncHttpFunction(urlCode, responseCode, authCode);
    }
}
