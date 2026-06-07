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
 * Maps a Flink Row object to a JSON String.
 * Uses the output schema field names to convert index-based Row positions to JSON properties.
 * double[] fields (ML feature vectors) are serialized naturally as JSON arrays by Jackson.
 */
public class RowToJsonMapper implements MapFunction<Row, String> {
    private static final long serialVersionUID = 1L;
    private final String[] fieldNames;
    private transient ObjectMapper mapper;

    public RowToJsonMapper(String[] fieldNames) {
        this.fieldNames = fieldNames;
    }

    @Override
    public String map(Row row) throws Exception {
        try {
            if (mapper == null) {
                mapper = new ObjectMapper();
            }

            Map<String, Object> map = new LinkedHashMap<>();
            int arity = Math.min(row.getArity(), fieldNames.length);

            for (int i = 0; i < arity; i++) {
                String name = fieldNames[i];
                // tEnv.toDataStream() returns NAME_BASED rows (fieldByName != null, fieldByPosition == null).
                // Calling getField(int) on a NAME_BASED row throws IllegalArgumentException.
                // Try getField(String) first; fall back to getField(int) for POSITION_BASED rows.
                Object val;
                try {
                    val = row.getField(name);
                } catch (IllegalArgumentException e) {
                    // row is POSITION_BASED or name not found — fall back to positional access
                    val = row.getField(i);
                }
                if (val instanceof org.apache.flink.ml.linalg.Vector) {
                    map.put(name, ((org.apache.flink.ml.linalg.Vector) val).toArray());
                } else {
                    map.put(name, val);
                }
            }

            return mapper.writeValueAsString(map);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }
}
