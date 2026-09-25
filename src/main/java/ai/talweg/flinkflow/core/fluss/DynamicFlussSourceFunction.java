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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.source.legacy.RichParallelSourceFunction;
import org.apache.fluss.client.Connection;
import org.apache.fluss.client.table.Table;
import org.apache.fluss.metadata.TablePath;
import org.apache.fluss.client.table.scanner.ScanRecord;
import org.apache.fluss.client.table.scanner.log.LogScanner;
import org.apache.fluss.client.table.scanner.log.ScanRecords;
import org.apache.fluss.row.InternalRow;
import org.apache.fluss.types.DataType;
import org.apache.fluss.types.RowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Dynamic streaming source for Apache Fluss tables, reading real-time logs / changelogs
 * and emitting JSON strings downstream.
 */
public class DynamicFlussSourceFunction extends RichParallelSourceFunction<String> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(DynamicFlussSourceFunction.class);

    private final Map<String, String> properties;
    private transient volatile boolean isRunning = true;
    private transient Connection connection;
    private transient Table table;
    private transient LogScanner logScanner;
    private transient ObjectMapper mapper;
    private transient RowType rowType;
    private transient List<String> fieldNames;

    public DynamicFlussSourceFunction(Map<String, String> properties) {
        this.properties = properties != null ? properties : Collections.emptyMap();
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        this.mapper = new ObjectMapper();

        String tableProp = properties.getOrDefault("table", properties.get("table.path"));
        if (tableProp == null || tableProp.trim().isEmpty()) {
            throw new IllegalArgumentException("Fluss source requires 'table' or 'table.path' property.");
        }

        TablePath tablePath = FlussManager.resolveTablePath(tableProp);
        this.connection = FlussManager.getConnection(properties);
        this.table = connection.getTable(tablePath);

        this.rowType = table.getTableInfo().getRowType();
        this.fieldNames = rowType.getFieldNames();

        this.logScanner = table.newScan().createLogScanner();
        
        // Subscribe to all buckets for this table partition
        int totalBuckets = table.getTableInfo().getNumBuckets();
        int subtaskIndex = getRuntimeContext().getTaskInfo().getIndexOfThisSubtask();
        int numSubtasks = getRuntimeContext().getTaskInfo().getNumberOfParallelSubtasks();

        for (int b = 0; b < totalBuckets; b++) {
            if (b % numSubtasks == subtaskIndex) {
                logScanner.subscribe(b, 0L);
            }
        }
        LOG.info("Fluss source started on subtask {}/{} for table '{}'", subtaskIndex, numSubtasks, tablePath);
    }

    @Override
    public void run(SourceContext<String> ctx) throws Exception {
        while (isRunning) {
            ScanRecords records = logScanner.poll(Duration.ofMillis(200));
            if (records != null && !records.isEmpty()) {
                Iterator<ScanRecord> iter = records.iterator();
                while (iter.hasNext() && isRunning) {
                    ScanRecord record = iter.next();
                    InternalRow row = record.getRow();
                    String json = rowToJson(row);
                    synchronized (ctx.getCheckpointLock()) {
                        ctx.collect(json);
                    }
                }
            }
        }
    }

    private String rowToJson(InternalRow row) {
        ObjectNode node = mapper.createObjectNode();
        if (row == null || fieldNames == null) {
            return node.toString();
        }

        List<DataType> fieldTypes = rowType.getChildren();
        for (int i = 0; i < fieldNames.size() && i < row.getFieldCount(); i++) {
            String name = fieldNames.get(i);
            if (row.isNullAt(i)) {
                node.putNull(name);
                continue;
            }

            DataType type = fieldTypes.get(i);
            String typeRoot = type.getTypeRoot().name();

            switch (typeRoot) {
                case "BOOLEAN":
                    node.put(name, row.getBoolean(i));
                    break;
                case "TINYINT":
                    node.put(name, row.getByte(i));
                    break;
                case "SMALLINT":
                    node.put(name, row.getShort(i));
                    break;
                case "INTEGER":
                    node.put(name, row.getInt(i));
                    break;
                case "BIGINT":
                    node.put(name, row.getLong(i));
                    break;
                case "FLOAT":
                    node.put(name, row.getFloat(i));
                    break;
                case "DOUBLE":
                    node.put(name, row.getDouble(i));
                    break;
                case "CHAR":
                case "VARCHAR":
                case "STRING":
                    node.put(name, row.getString(i).toString());
                    break;
                default:
                    node.put(name, row.getString(i).toString());
                    break;
            }
        }
        return node.toString();
    }

    @Override
    public void cancel() {
        this.isRunning = false;
        if (logScanner != null) {
            try {
                logScanner.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void close() throws Exception {
        cancel();
        if (table != null) {
            table.close();
        }
    }
}
