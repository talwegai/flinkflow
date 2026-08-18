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

package ai.talweg.flinkflow.core.fluss;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.legacy.RichSinkFunction;
import org.apache.fluss.client.Connection;
import org.apache.fluss.client.table.Table;
import org.apache.fluss.metadata.TablePath;
import org.apache.fluss.client.table.writer.AppendWriter;
import org.apache.fluss.client.table.writer.UpsertWriter;
import org.apache.fluss.row.BinaryString;
import org.apache.fluss.row.GenericRow;
import org.apache.fluss.types.DataType;
import org.apache.fluss.types.RowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Dynamic sink for Apache Fluss supporting Append Logs, Primary Key Upserts,
 * and Partial Updates with automatic schema discovery from the Fluss Catalog.
 */
public class DynamicFlussSinkFunction extends RichSinkFunction<String> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(DynamicFlussSinkFunction.class);

    private final Map<String, String> properties;
    private transient Connection connection;
    private transient Table table;
    private transient AppendWriter appendWriter;
    private transient UpsertWriter upsertWriter;
    private transient ObjectMapper mapper;
    private transient RowType rowType;
    private transient List<String> fieldNames;
    private transient boolean isUpsertMode;
    private transient boolean isPartialUpdate;

    public DynamicFlussSinkFunction(Map<String, String> properties) {
        this.properties = properties != null ? properties : Collections.emptyMap();
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        this.mapper = new ObjectMapper();

        String tableProp = properties.getOrDefault("table", properties.get("table.path"));
        if (tableProp == null || tableProp.trim().isEmpty()) {
            throw new IllegalArgumentException("Fluss sink requires 'table' or 'table.path' property.");
        }

        TablePath tablePath = FlussManager.resolveTablePath(tableProp);
        this.connection = FlussManager.getConnection(properties);
        this.table = connection.getTable(tablePath);

        this.rowType = table.getTableInfo().getRowType();
        this.fieldNames = rowType.getFieldNames();

        String mergeEngine = properties.getOrDefault("merge-engine", properties.getOrDefault("mergeEngine", ""));
        this.isPartialUpdate = "partial-update".equalsIgnoreCase(mergeEngine) 
            || Boolean.parseBoolean(properties.getOrDefault("partial", properties.getOrDefault("partial-update", "false")));

        boolean tableHasPk = table.getTableInfo().hasPrimaryKey();
        this.isUpsertMode = tableHasPk || isPartialUpdate || properties.containsKey("primary-key") || properties.containsKey("key");

        if (isUpsertMode) {
            this.upsertWriter = table.newUpsert().createWriter();
        } else {
            this.appendWriter = table.newAppend().createWriter();
        }
        LOG.info("Initialized Fluss sink for table '{}' (UpsertMode: {}, PartialUpdate: {})", 
                tablePath, isUpsertMode, isPartialUpdate);
    }

    @Override
    public void invoke(String value, Context context) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        JsonNode rootNode = mapper.readTree(value);
        if (!rootNode.isObject()) {
            return;
        }

        GenericRow row = buildRowFromJson(rootNode);

        if (isUpsertMode && upsertWriter != null) {
            upsertWriter.upsert(row);
        } else if (appendWriter != null) {
            appendWriter.append(row);
        }
    }

    private GenericRow buildRowFromJson(JsonNode node) {
        int fieldCount = fieldNames.size();
        GenericRow row = new GenericRow(fieldCount);
        List<DataType> fieldTypes = rowType.getChildren();

        for (int i = 0; i < fieldCount; i++) {
            String fieldName = fieldNames.get(i);
            JsonNode fieldNode = node.get(fieldName);

            if (fieldNode == null || fieldNode.isNull()) {
                row.setField(i, null);
                continue;
            }

            DataType type = fieldTypes.get(i);
            String typeRoot = type.getTypeRoot().name();

            switch (typeRoot) {
                case "BOOLEAN":
                    row.setField(i, fieldNode.asBoolean());
                    break;
                case "TINYINT":
                    row.setField(i, (byte) fieldNode.asInt());
                    break;
                case "SMALLINT":
                    row.setField(i, (short) fieldNode.asInt());
                    break;
                case "INTEGER":
                    row.setField(i, fieldNode.asInt());
                    break;
                case "BIGINT":
                    row.setField(i, fieldNode.asLong());
                    break;
                case "FLOAT":
                    row.setField(i, (float) fieldNode.asDouble());
                    break;
                case "DOUBLE":
                    row.setField(i, fieldNode.asDouble());
                    break;
                case "CHAR":
                case "VARCHAR":
                case "STRING":
                    row.setField(i, BinaryString.fromString(fieldNode.asText()));
                    break;
                default:
                    row.setField(i, BinaryString.fromString(fieldNode.asText()));
                    break;
            }
        }
        return row;
    }

    @Override
    public void close() throws Exception {
        if (upsertWriter != null) {
            upsertWriter.flush();
        }
        if (appendWriter != null) {
            appendWriter.flush();
        }
        if (table != null) {
            table.close();
        }
    }
}
