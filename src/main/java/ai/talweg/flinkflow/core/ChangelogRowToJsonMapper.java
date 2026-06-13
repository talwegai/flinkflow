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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps retract/changelog Row streams to JSON strings, appending the RowKind as '_op'.
 */
public class ChangelogRowToJsonMapper implements MapFunction<Row, String> {
    private static final long serialVersionUID = 1L;
    private final String[] fieldNames;
    private transient ObjectMapper mapper;

    public ChangelogRowToJsonMapper(String[] fieldNames) {
        this.fieldNames = fieldNames;
    }

    @Override
    public String map(Row row) throws Exception {
        if (mapper == null) {
            mapper = new ObjectMapper();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("_op", row.getKind().shortString()); // "+I", "-D", "+U", "-U"
        
        int arity = Math.min(row.getArity(), fieldNames.length);
        for (int i = 0; i < arity; i++) {
            String name = fieldNames[i];
            Object val;
            try {
                val = row.getField(name);
            } catch (IllegalArgumentException e) {
                val = row.getField(i);
            }
            map.put(name, formatValue(val));
        }
        return mapper.writeValueAsString(map);
    }

    private Object formatValue(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof org.apache.flink.ml.linalg.Vector) {
            return ((org.apache.flink.ml.linalg.Vector) val).toArray();
        }
        if (val instanceof java.time.LocalDateTime || val instanceof java.time.LocalDate) {
            return val.toString();
        }
        if (val instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) val;
            java.util.List<Object> formattedList = new java.util.ArrayList<>(list.size());
            for (Object item : list) {
                formattedList.add(formatValue(item));
            }
            return formattedList;
        }
        if (val instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) val;
            java.util.Map<Object, Object> formattedMap = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                formattedMap.put(formatValue(entry.getKey()), formatValue(entry.getValue()));
            }
            return formattedMap;
        }
        return val;
    }
}
