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
        } else if ("camel".equalsIgnoreCase(language) || "camel-simple".equalsIgnoreCase(language)) {
            return new DynamicCamelMapFunction(codeBody, "simple");
        } else if ("jsonpath".equalsIgnoreCase(language) || "camel-jsonpath".equalsIgnoreCase(language)) {
            return new DynamicCamelMapFunction(codeBody, "jsonpath");
        } else if ("groovy".equalsIgnoreCase(language) || "camel-groovy".equalsIgnoreCase(language)) {
            return new DynamicCamelMapFunction(codeBody, "groovy");
        } else if ("camel-yaml".equalsIgnoreCase(language)) {
            return new DynamicCamelYamlMapFunction(codeBody);
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
        } else if ("camel".equalsIgnoreCase(language) || "camel-simple".equalsIgnoreCase(language)) {
            return new DynamicCamelFilterFunction(codeBody, "simple");
        } else if ("jsonpath".equalsIgnoreCase(language) || "camel-jsonpath".equalsIgnoreCase(language)) {
            return new DynamicCamelFilterFunction(codeBody, "jsonpath");
        } else if ("groovy".equalsIgnoreCase(language) || "camel-groovy".equalsIgnoreCase(language)) {
            return new DynamicCamelFilterFunction(codeBody, "groovy");
        } else if ("camel-yaml".equalsIgnoreCase(language)) {
            return new DynamicCamelYamlFilterFunction(codeBody);
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
        } else if ("camel".equalsIgnoreCase(language) || "camel-simple".equalsIgnoreCase(language)) {
            return new DynamicCamelFlatMapFunction(codeBody, "simple");
        } else if ("jsonpath".equalsIgnoreCase(language) || "camel-jsonpath".equalsIgnoreCase(language)) {
            return new DynamicCamelFlatMapFunction(codeBody, "jsonpath");
        } else if ("groovy".equalsIgnoreCase(language) || "camel-groovy".equalsIgnoreCase(language)) {
            return new DynamicCamelFlatMapFunction(codeBody, "groovy");
        } else if ("camel-yaml".equalsIgnoreCase(language)) {
            return new DynamicCamelYamlFlatMapFunction(codeBody);
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
        } else if ("camel".equalsIgnoreCase(language) || "camel-simple".equalsIgnoreCase(language)) {
            return new DynamicCamelReduceFunction(codeBody, "simple");
        } else if ("jsonpath".equalsIgnoreCase(language) || "camel-jsonpath".equalsIgnoreCase(language)) {
            return new DynamicCamelReduceFunction(codeBody, "jsonpath");
        } else if ("groovy".equalsIgnoreCase(language) || "camel-groovy".equalsIgnoreCase(language)) {
            return new DynamicCamelReduceFunction(codeBody, "groovy");
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
        } else if ("camel".equalsIgnoreCase(language) || "camel-simple".equalsIgnoreCase(language)) {
            return new DynamicCamelReduceFunction(codeBody, "simple");
        } else if ("jsonpath".equalsIgnoreCase(language) || "camel-jsonpath".equalsIgnoreCase(language)) {
            return new DynamicCamelReduceFunction(codeBody, "jsonpath");
        } else if ("groovy".equalsIgnoreCase(language) || "camel-groovy".equalsIgnoreCase(language)) {
            return new DynamicCamelReduceFunction(codeBody, "groovy");
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
        } else if ("camel".equalsIgnoreCase(language) || "camel-simple".equalsIgnoreCase(language)) {
            return new DynamicCamelJoinFunction(codeBody, "simple");
        } else if ("jsonpath".equalsIgnoreCase(language) || "camel-jsonpath".equalsIgnoreCase(language)) {
            return new DynamicCamelJoinFunction(codeBody, "jsonpath");
        } else if ("groovy".equalsIgnoreCase(language) || "camel-groovy".equalsIgnoreCase(language)) {
            return new DynamicCamelJoinFunction(codeBody, "groovy");
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
        } else if ("camel".equalsIgnoreCase(language) || "camel-simple".equalsIgnoreCase(language)) {
            return new DynamicCamelKeySelector(codeBody, "simple");
        } else if ("jsonpath".equalsIgnoreCase(language) || "camel-jsonpath".equalsIgnoreCase(language)) {
            return new DynamicCamelKeySelector(codeBody, "jsonpath");
        } else if ("groovy".equalsIgnoreCase(language) || "camel-groovy".equalsIgnoreCase(language)) {
            return new DynamicCamelKeySelector(codeBody, "groovy");
        } else if ("camel-yaml".equalsIgnoreCase(language)) {
            return new DynamicCamelYamlKeySelector(codeBody);
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
     * @param language     the language of the code (java, python)
     * @return a new DynamicAsyncHttpFunction instance
     */
    public static ai.talweg.flinkflow.core.DynamicAsyncHttpFunction createAsyncHttpLookup(String urlCode, String responseCode, String authCode, String language) {
        return new ai.talweg.flinkflow.core.DynamicAsyncHttpFunction(urlCode, responseCode, authCode, language);
    }

    public static ai.talweg.flinkflow.core.DynamicAsyncHttpFunction createAsyncHttpLookup(String urlCode, String responseCode, String authCode) {
        return createAsyncHttpLookup(urlCode, responseCode, authCode, "java");
    }

    // -----------------------------------------------------------------------
    // Agentic Bridge (Flink Agents)
    // -----------------------------------------------------------------------

    /**
     * Creates a new ProcessFunction that acts as an AI Agent.
     *
     * @param agentName    The name of the agent
     * @param model         The LLM model to use
     * @param systemPrompt  The base instructions for the agent
     * @param useMemory    Whether to persist agent history in Flink state
     * @param properties    Additional configuration properties (e.g., tool mappings)
     * @param flowletCatalog The registry of available flowlets for tool resolution
     * @return a new DynamicAgentFunction instance
     */
    public static org.apache.flink.streaming.api.functions.ProcessFunction<String, String> createAgent(String agentName, String model, String systemPrompt, 
                                                            boolean useMemory, java.util.Map<String, String> properties,
                                                            java.util.Map<String, ?> flowletCatalog) {
        return new DynamicAgentFunction(agentName, model, systemPrompt, useMemory, properties, flowletCatalog);
    }
}
