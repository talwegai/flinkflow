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

    private transient HttpClient httpClient;
    private transient Object dynamicHandlerInstance;
    private transient Method urlMethod;
    private transient Method responseMethod;
    private transient Method authMethod;

    public DynamicAsyncHttpFunction(String urlCode, String responseCode, String authCode) {
        this.urlCode = urlCode;
        this.responseCode = responseCode;
        this.authCode = authCode;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

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

    @Override
    public void asyncInvoke(String input, ResultFuture<String> resultFuture) throws Exception {
        String url = (String) urlMethod.invoke(dynamicHandlerInstance, input);
        String authHeader = (String) authMethod.invoke(dynamicHandlerInstance, input);

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
                String result = (String) responseMethod.invoke(dynamicHandlerInstance, input, response.body());
                resultFuture.complete(Collections.singleton(result));
            } catch (Exception e) {
                resultFuture.completeExceptionally(e);
            }
        }).exceptionally(ex -> {
            resultFuture.completeExceptionally(ex);
            return null;
        });
    }

    @Override
    public void timeout(String input, ResultFuture<String> resultFuture) {
        resultFuture.completeExceptionally(new RuntimeException("HTTP Request timed out for input: " + input));
    }
}
