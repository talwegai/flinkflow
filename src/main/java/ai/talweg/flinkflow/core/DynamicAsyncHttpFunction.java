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
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * A Flink AsyncFunction that allows for dynamic Java code execution
 * to perform asynchronous HTTP lookups.
 */
public class DynamicAsyncHttpFunction extends RichAsyncFunction<String, String> {

    private final String urlCode;
    private final String responseCode;
    private final String authCode;
    private final String language;

    private transient HttpClient httpClient;
    
    // Java (Janino) Handlers
    private transient Object dynamicHandlerInstance;
    private transient Method urlMethod;
    private transient Method responseMethod;
    private transient Method authMethod;

    // Python (GraalVM) Handlers
    private transient PythonEvaluator urlEvaluator;
    private transient PythonEvaluator responseEvaluator;
    private transient PythonEvaluator authEvaluator;

    public DynamicAsyncHttpFunction(String urlCode, String responseCode, String authCode, String language) {
        this.urlCode = urlCode;
        this.responseCode = responseCode;
        this.authCode = authCode;
        this.language = (language != null) ? language.toLowerCase() : "java";
    }

    @Override
    public void open(OpenContext parameters) throws Exception {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        if ("python".equals(language)) {
            urlEvaluator = new PythonEvaluator(urlCode, "def process(input)");
            urlEvaluator.open();

            responseEvaluator = new PythonEvaluator(responseCode, "def process(input, response)");
            responseEvaluator.open();

            if (authCode != null && !authCode.isEmpty()) {
                authEvaluator = new PythonEvaluator(authCode, "def process(input)");
                authEvaluator.open();
            }
        } else {
            String className = "DynamicHttpHandler_" + System.nanoTime();
            String classCode = "public class " + className + " {\n" +
                    "    public String getUrl(String input) throws Exception {\n" +
                    "        " + urlCode + "\n" +
                    "    }\n" +
                    "    public String handleResponse(String input, String response) throws Exception {\n" +
                    "        " + responseCode + "\n" +
                    "    }\n" +
                    "    public String getAuth(String input) throws Exception {\n" +
                    "        " + (authCode != null ? authCode : "return null;") + "\n" +
                    "    }\n" +
                    "}";

            SimpleCompiler compiler = new SimpleCompiler();
            compiler.cook(classCode);
            Class<?> clazz = compiler.getClassLoader().loadClass(className);
            dynamicHandlerInstance = clazz.getDeclaredConstructor().newInstance();
            urlMethod = clazz.getMethod("getUrl", String.class);
            responseMethod = clazz.getMethod("handleResponse", String.class, String.class);
            authMethod = clazz.getMethod("getAuth", String.class);
        }
    }

    @Override
    public void asyncInvoke(String input, ResultFuture<String> resultFuture) throws Exception {
        String url;
        String authHeader;

        if ("python".equals(language)) {
            url = urlEvaluator.execute(input).asString();
            authHeader = (authEvaluator != null) ? authEvaluator.execute(input).asString() : null;
        } else {
            url = (String) urlMethod.invoke(dynamicHandlerInstance, input);
            authHeader = (String) authMethod.invoke(dynamicHandlerInstance, input);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

        if (authHeader != null && !authHeader.isEmpty()) {
            requestBuilder.header("Authorization", authHeader);
        }

        HttpRequest request = requestBuilder.build();

        CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(request,
                HttpResponse.BodyHandlers.ofString());

        responseFuture.thenAccept(response -> {
            try {
                String result;
                if ("python".equals(language)) {
                    result = responseEvaluator.execute(input, response.body()).asString();
                } else {
                    result = (String) responseMethod.invoke(dynamicHandlerInstance, input, response.body());
                }

                if (result != null) {
                    resultFuture.complete(Collections.singleton(result));
                } else {
                    resultFuture.complete(Collections.emptyList());
                }
            } catch (Exception e) {
                resultFuture.completeExceptionally(e);
            }
        }).exceptionally(ex -> {
            resultFuture.completeExceptionally(ex);
            return null;
        });
    }

    @Override
    public void close() throws Exception {
        if (urlEvaluator != null) urlEvaluator.close();
        if (responseEvaluator != null) responseEvaluator.close();
        if (authEvaluator != null) authEvaluator.close();
    }

    @Override
    public void timeout(String input, ResultFuture<String> resultFuture) {
        resultFuture.completeExceptionally(new RuntimeException("HTTP Request timed out for input: " + input));
    }
}
