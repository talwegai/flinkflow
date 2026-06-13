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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class to centralize schema extraction and type resolution for Flink Table/SQL and ML steps.
 */
public class SchemaHelper {

    /**
     * Extract schema properties (starting with "schema.") from properties.
     */
    public static LinkedHashMap<String, String> extractSchema(Map<String, String> properties) {
        LinkedHashMap<String, String> schemaMap = new LinkedHashMap<>();
        if (properties != null) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("schema.")) {
                    schemaMap.put(key.substring(7), entry.getValue());
                }
            }
        }
        return schemaMap;
    }

    /**
     * Extract schema properties for a specific table (starting with "schema.<tableName>.") from properties.
     */
    public static LinkedHashMap<String, String> extractSchema(Map<String, String> properties, String tableName) {
        if (tableName == null) {
            return extractSchema(properties);
        }
        LinkedHashMap<String, String> schemaMap = new LinkedHashMap<>();
        String prefix = "schema." + tableName + ".";
        if (properties != null) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(prefix)) {
                    schemaMap.put(key.substring(prefix.length()), entry.getValue());
                }
            }
        }
        return schemaMap;
    }

    /**
     * Resolve a string type representation to Flink TypeInformation.
     */
    public static org.apache.flink.api.common.typeinfo.TypeInformation<?> resolveType(String typeStr) {
        String norm = typeStr.toLowerCase().trim();
        switch (norm) {
            case "string":
                return org.apache.flink.api.common.typeinfo.Types.STRING;
            case "int":
            case "integer":
                return org.apache.flink.api.common.typeinfo.Types.INT;
            case "long":
                return org.apache.flink.api.common.typeinfo.Types.LONG;
            case "double":
            case "float":
                return org.apache.flink.api.common.typeinfo.Types.DOUBLE;
            case "boolean":
                return org.apache.flink.api.common.typeinfo.Types.BOOLEAN;
            case "vector":
                return org.apache.flink.ml.linalg.typeinfo.VectorTypeInfo.INSTANCE;
            case "timestamp":
                return org.apache.flink.api.common.typeinfo.Types.LOCAL_DATE_TIME;
            case "date":
                return org.apache.flink.api.common.typeinfo.Types.LOCAL_DATE;
            case "decimal":
                return org.apache.flink.api.common.typeinfo.Types.BIG_DEC;
            default:
                if (norm.startsWith("array<") && norm.endsWith(">")) {
                    String elemType = norm.substring(6, norm.length() - 1).trim();
                    return org.apache.flink.api.common.typeinfo.Types.LIST(resolveType(elemType));
                }
                if (norm.startsWith("map<") && norm.endsWith(">")) {
                    String inner = norm.substring(4, norm.length() - 1).trim();
                    String[] parts = inner.split(",", 2);
                    if (parts.length == 2) {
                        return org.apache.flink.api.common.typeinfo.Types.MAP(resolveType(parts[0].trim()), resolveType(parts[1].trim()));
                    }
                }
                throw new IllegalArgumentException("Unsupported schema type: " + typeStr);
        }
    }

    /**
     * Build the Row TypeInformation for the given schema map.
     */
    public static org.apache.flink.api.common.typeinfo.TypeInformation<org.apache.flink.types.Row> buildRowTypeInfo(Map<String, String> schemaMap) {
        String[] fieldNames = schemaMap.keySet().toArray(new String[0]);
        org.apache.flink.api.common.typeinfo.TypeInformation<?>[] fieldTypes =
            new org.apache.flink.api.common.typeinfo.TypeInformation<?>[fieldNames.length];
        for (int idx = 0; idx < fieldNames.length; idx++) {
            fieldTypes[idx] = resolveType(schemaMap.get(fieldNames[idx]));
        }
        return org.apache.flink.api.common.typeinfo.Types.ROW_NAMED(fieldNames, fieldTypes);
    }
}
