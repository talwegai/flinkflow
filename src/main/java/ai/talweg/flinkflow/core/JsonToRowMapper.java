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
import org.apache.flink.types.Row;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Map;

/**
 * Maps a JSON String to a Flink Row object based on a user-defined schema.
 * Supports standard primitives and ML feature vectors (represented as double[]).
 */
public class JsonToRowMapper implements MapFunction<String, Row> {
    private static final long serialVersionUID = 1L;
    private final Map<String, String> schema;
    private transient ObjectMapper mapper;

    public JsonToRowMapper(Map<String, String> schema) {
        this.schema = schema;
    }

    @Override
    public Row map(String value) throws Exception {
        if (mapper == null) {
            mapper = new ObjectMapper();
        }

        JsonNode node;
        try {
            node = mapper.readTree(value);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON input in JsonToRowMapper: " + e.getMessage(), e);
        }

        Row row = new Row(schema.size());
        int idx = 0;
        for (Map.Entry<String, String> entry : schema.entrySet()) {
            String fieldName = entry.getKey();
            String fieldType = entry.getValue().toLowerCase();
            JsonNode fieldNode = node.get(fieldName);

            if (fieldNode == null || fieldNode.isNull()) {
                row.setField(idx, null);
                idx++;
                continue;
            }

            switch (fieldType) {
                case "string":
                    row.setField(idx, fieldNode.asText());
                    break;
                case "int":
                case "integer":
                    row.setField(idx, fieldNode.asInt());
                    break;
                case "long":
                    row.setField(idx, fieldNode.asLong());
                    break;
                case "double":
                case "float":
                    row.setField(idx, fieldNode.asDouble());
                    break;
                case "boolean":
                    row.setField(idx, fieldNode.asBoolean());
                    break;
                case "vector":
                    if (fieldNode.isArray()) {
                        double[] arr = new double[fieldNode.size()];
                        for (int i = 0; i < fieldNode.size(); i++) {
                            arr[i] = fieldNode.get(i).asDouble();
                        }
                        row.setField(idx, new org.apache.flink.ml.linalg.DenseVector(arr));
                    } else {
                        String[] parts = fieldNode.asText().split(",");
                        double[] arr = new double[parts.length];
                        for (int i = 0; i < parts.length; i++) {
                            arr[i] = Double.parseDouble(parts[i].trim());
                        }
                        row.setField(idx, new org.apache.flink.ml.linalg.DenseVector(arr));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported schema type: " + fieldType);
            }
            idx++;
        }

        return row;
    }
}
