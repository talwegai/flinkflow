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

import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A Flink JdbcStatementBuilder that executes dynamically compiled Janino code
 * mapped over incoming String values in a DataStream.
 */
public class DynamicJdbcStatementBuilder implements JdbcStatementBuilder<String> {

    private final String codeBody;
    private transient Object dynamicInstance;
    private transient Method statementMethod;

    public DynamicJdbcStatementBuilder(String codeBody) {
        this.codeBody = codeBody;
    }

    private void init() throws Exception {
        if (dynamicInstance == null) {
            String className = "DynamicJdbcStatementBuilder_" + System.nanoTime();
            String classCode = "import java.sql.PreparedStatement;\n" +
                    "public class " + className + " {\n" +
                    "    public void accept(PreparedStatement stmt, String input) throws Exception {\n" +
                    "        " + codeBody + "\n" +
                    "    }\n" +
                    "}";

            SimpleCompiler compiler = new SimpleCompiler();
            compiler.cook(classCode);
            Class<?> clazz = compiler.getClassLoader().loadClass(className);
            dynamicInstance = clazz.getDeclaredConstructor().newInstance();
            statementMethod = clazz.getMethod("accept", PreparedStatement.class, String.class);
        }
    }

    @Override
    public void accept(PreparedStatement preparedStatement, String input) throws SQLException {
        try {
            init();
            statementMethod.invoke(dynamicInstance, preparedStatement, input);
        } catch (Exception e) {
            throw new SQLException("Failed to dynamically build JDBC statement for input: " + input, e);
        }
    }
}
