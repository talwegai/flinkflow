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
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * A Flink SinkFunction that triggers an external HTTP/Webhook service.
 * Supports configurable methods (POST/PUT) and dynamic authorization.
 */
public class DynamicHttpSinkFunction extends RichSinkFunction<String> {

    private final String urlCode;
    private final String method;
    private final String authCode;

    private transient HttpClient httpClient;
    private transient Object dynamicHandlerInstance;
    private transient Method urlMethod;
    private transient Method authMethod;

    public DynamicHttpSinkFunction(String urlCode, String method, String authCode) {
        this.urlCode = urlCode != null ? urlCode : "return \"http://localhost\";";
        this.method = method != null ? method : "POST";
        this.authCode = authCode;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String className = "DynamicHttpSinkHandler_" + System.nanoTime();
        String classCode = "public class " + className + " {\n" +
                "    public String getUrl(String input) throws Exception {\n" +
                "        " + urlCode + "\n" +
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
        authMethod = clazz.getMethod("getAuth", String.class);
    }

    @Override
    public void invoke(String value, Context context) throws Exception {
        String url = (String) urlMethod.invoke(dynamicHandlerInstance, value);
        String authHeader = (String) authMethod.invoke(dynamicHandlerInstance, value);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url));

        if ("POST".equalsIgnoreCase(method)) {
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(value));
        } else if ("PUT".equalsIgnoreCase(method)) {
            requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(value));
        } else {
            throw new IllegalArgumentException("Unsupported HTTP Method for Sink: " + method);
        }

        // Add default Content-Type which implies we usually POST typical JSON or text
        // payloads
        requestBuilder.header("Content-Type", "application/json");

        if (authHeader != null && !authHeader.isEmpty()) {
            requestBuilder.header("Authorization", authHeader);
        }

        HttpRequest request = requestBuilder.build();

        // Blocking call (synchronous mapping for standard RichSinkFunction)
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException(
                    "HTTP Sink Failed with Status " + response.statusCode() + ": " + response.body());
        }
    }
}
