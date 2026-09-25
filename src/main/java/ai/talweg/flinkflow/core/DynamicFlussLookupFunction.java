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

import ai.talweg.flinkflow.core.fluss.FlussManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.apache.fluss.client.Connection;
import org.apache.fluss.client.table.Table;
import org.apache.fluss.metadata.TablePath;
import org.apache.fluss.client.lookup.LookupResult;
import org.apache.fluss.client.lookup.Lookuper;
import org.apache.fluss.row.BinaryString;
import org.apache.fluss.row.GenericRow;
import org.apache.fluss.row.InternalRow;
import org.apache.fluss.types.DataType;
import org.apache.fluss.types.RowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance, non-blocking asynchronous stream enrichment function
 * querying Apache Fluss TabletServers via the native Key-Value point lookup API.
 */
public class DynamicFlussLookupFunction extends RichAsyncFunction<String, String> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(DynamicFlussLookupFunction.class);

    private final Map<String, String> properties;
    private transient Connection connection;
    private transient Table table;
    private transient Lookuper lookuper;
    private transient ObjectMapper mapper;
    private transient RowType rowType;
    private transient List<String> fieldNames;
    private transient Map<Object, CacheEntry> lruCache;

    private String lookupKeyField;
    private String outputField;
    private long cacheTtlMs;
    private int maxCacheSize;

    public DynamicFlussLookupFunction(Map<String, String> properties) {
        this.properties = properties != null ? properties : Collections.emptyMap();
    }

    private static class CacheEntry {
        final JsonNode data;
        final long timestamp;

        CacheEntry(JsonNode data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }

        boolean isExpired(long ttlMs) {
            return ttlMs > 0 && (System.currentTimeMillis() - timestamp) > ttlMs;
        }
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        this.mapper = new ObjectMapper();
        
        String tableProp = properties.getOrDefault("table", properties.get("table.path"));
        if (tableProp == null || tableProp.trim().isEmpty()) {
            throw new IllegalArgumentException("Fluss lookup step requires 'table' or 'table.path' property.");
        }
        
        this.lookupKeyField = properties.getOrDefault("key", properties.get("lookupKey"));
        if (this.lookupKeyField == null || this.lookupKeyField.trim().isEmpty()) {
            throw new IllegalArgumentException("Fluss lookup step requires 'key' or 'lookupKey' property.");
        }

        this.outputField = properties.getOrDefault("outputField", "enriched");

        // Cache configuration
        long ttlSec = Long.parseLong(properties.getOrDefault("cacheTtlSec", properties.getOrDefault("cache.ttl", "0")));
        this.cacheTtlMs = ttlSec * 1000L;
        this.maxCacheSize = Integer.parseInt(properties.getOrDefault("cacheSize", properties.getOrDefault("cache.max-rows", "10000")));

        if (this.maxCacheSize > 0) {
            this.lruCache = Collections.synchronizedMap(new LinkedHashMap<Object, CacheEntry>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Object, CacheEntry> eldest) {
                    return size() > maxCacheSize;
                }
            });
        }

        TablePath tablePath = FlussManager.resolveTablePath(tableProp);
        this.connection = FlussManager.getConnection(properties);
        this.table = connection.getTable(tablePath);
        this.lookuper = table.newLookup().createLookuper();

        this.rowType = table.getTableInfo().getRowType();
        this.fieldNames = rowType.getFieldNames();
    }

    @Override
    public void asyncInvoke(String inputJson, ResultFuture<String> resultFuture) {
        try {
            JsonNode rootNode = mapper.readTree(inputJson);
            if (!rootNode.isObject()) {
                resultFuture.complete(Collections.singleton(inputJson));
                return;
            }
            ObjectNode objectNode = (ObjectNode) rootNode;

            JsonNode keyNode = objectNode.get(lookupKeyField);
            if (keyNode == null || keyNode.isNull()) {
                resultFuture.complete(Collections.singleton(inputJson));
                return;
            }

            Object keyValue = extractKeyValue(keyNode);

            // 1. Check local LRU Cache
            if (lruCache != null) {
                CacheEntry cached = lruCache.get(keyValue);
                if (cached != null && !cached.isExpired(cacheTtlMs)) {
                    objectNode.set(outputField, cached.data);
                    resultFuture.complete(Collections.singleton(objectNode.toString()));
                    return;
                }
            }

            // 2. Perform point query on Fluss TabletServer
            GenericRow keyRow = createKeyRow(keyValue);
            CompletableFuture<LookupResult> lookupFuture = lookuper.lookup(keyRow);

            lookupFuture.whenComplete((lookupResult, throwable) -> {
                if (throwable != null) {
                    LOG.warn("Fluss point lookup failed for key: {}. Passing through original record.", keyValue, throwable);
                    resultFuture.complete(Collections.singleton(inputJson));
                    return;
                }

                if (lookupResult == null || lookupResult.getRowList() == null || lookupResult.getRowList().isEmpty()) {
                    // No match found in Fluss
                    resultFuture.complete(Collections.singleton(inputJson));
                    return;
                }

                InternalRow row = lookupResult.getSingletonRow();
                JsonNode enrichedNode = rowToJson(row);

                if (lruCache != null) {
                    lruCache.put(keyValue, new CacheEntry(enrichedNode, System.currentTimeMillis()));
                }

                objectNode.set(outputField, enrichedNode);
                resultFuture.complete(Collections.singleton(objectNode.toString()));
            });

        } catch (Exception e) {
            LOG.error("Error during Fluss async lookup processing", e);
            resultFuture.complete(Collections.singleton(inputJson));
        }
    }

    private Object extractKeyValue(JsonNode keyNode) {
        if (keyNode.isIntegralNumber()) {
            return keyNode.asLong();
        } else if (keyNode.isDouble() || keyNode.isFloat()) {
            return keyNode.asDouble();
        } else if (keyNode.isBoolean()) {
            return keyNode.asBoolean();
        } else {
            return keyNode.asText();
        }
    }

    private GenericRow createKeyRow(Object keyValue) {
        if (keyValue instanceof Long) {
            return GenericRow.of((Long) keyValue);
        } else if (keyValue instanceof Integer) {
            return GenericRow.of((Integer) keyValue);
        } else if (keyValue instanceof Double) {
            return GenericRow.of((Double) keyValue);
        } else {
            return GenericRow.of(BinaryString.fromString(keyValue.toString()));
        }
    }

    private JsonNode rowToJson(InternalRow row) {
        ObjectNode node = mapper.createObjectNode();
        if (row == null || fieldNames == null) {
            return node;
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
        return node;
    }

    @Override
    public void close() throws Exception {
        if (table != null) {
            table.close();
        }
    }
}
