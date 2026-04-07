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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.io.IOAccess;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A helper class that abstracts GraalVM Python context management and
 * code execution for use in Flink dynamic functions.
 */
public class PythonEvaluator implements Serializable {
    private final String scriptDefinition;
    private transient Context context;
    private transient Value processFunc;

    public PythonEvaluator(String codeBody, String functionSignature) {
        String indentedCode = Arrays.stream(codeBody.split("\n"))
                .map(line -> "    " + line)
                .collect(Collectors.joining("\n"));
        // functionSignature should be something like "def process(input, out, ctx)"
        this.scriptDefinition = functionSignature + ":\n" + indentedCode;
    }

    public void open() {
        context = Context.newBuilder("python")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(c -> false)
                .allowIO(IOAccess.NONE)
                .allowNativeAccess(false)
                .allowCreateThread(false)
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        context.eval("python", scriptDefinition);
        // By convention, we assume the signature declares a function named 'process'
        processFunc = context.getBindings("python").getMember("process");
    }

    public Value execute(Object... args) {
        return processFunc.execute(args);
    }

    public void close() {
        if (context != null) {
            context.close();
        }
    }
}
