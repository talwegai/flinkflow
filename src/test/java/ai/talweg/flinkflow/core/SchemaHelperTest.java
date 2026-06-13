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

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.types.Row;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class SchemaHelperTest {

    @Test
    public void testExtractSchemaSingleTable() {
        Map<String, String> properties = new HashMap<>();
        properties.put("schema.userId", "string");
        properties.put("schema.age", "int");
        properties.put("other.prop", "val");

        LinkedHashMap<String, String> schema = SchemaHelper.extractSchema(properties);
        assertEquals(2, schema.size());
        assertEquals("string", schema.get("userId"));
        assertEquals("int", schema.get("age"));
    }

    @Test
    public void testExtractSchemaMultiTable() {
        Map<String, String> properties = new HashMap<>();
        properties.put("schema.orders.orderId", "string");
        properties.put("schema.orders.amount", "double");
        properties.put("schema.customers.name", "string");

        LinkedHashMap<String, String> ordersSchema = SchemaHelper.extractSchema(properties, "orders");
        assertEquals(2, ordersSchema.size());
        assertEquals("string", ordersSchema.get("orderId"));
        assertEquals("double", ordersSchema.get("amount"));

        LinkedHashMap<String, String> customersSchema = SchemaHelper.extractSchema(properties, "customers");
        assertEquals(1, customersSchema.size());
        assertEquals("string", customersSchema.get("name"));
    }

    @Test
    public void testResolveTypeSimple() {
        assertEquals(Types.STRING, SchemaHelper.resolveType("string"));
        assertEquals(Types.INT, SchemaHelper.resolveType("int"));
        assertEquals(Types.INT, SchemaHelper.resolveType("integer"));
        assertEquals(Types.LONG, SchemaHelper.resolveType("long"));
        assertEquals(Types.DOUBLE, SchemaHelper.resolveType("double"));
        assertEquals(Types.DOUBLE, SchemaHelper.resolveType("float"));
        assertEquals(Types.BOOLEAN, SchemaHelper.resolveType("boolean"));
        assertEquals(Types.LOCAL_DATE_TIME, SchemaHelper.resolveType("timestamp"));
        assertEquals(Types.LOCAL_DATE, SchemaHelper.resolveType("date"));
        assertEquals(Types.BIG_DEC, SchemaHelper.resolveType("decimal"));
    }

    @Test
    public void testResolveTypeArray() {
        TypeInformation<?> expected = Types.LIST(Types.STRING);
        assertEquals(expected, SchemaHelper.resolveType("array<string>"));

        TypeInformation<?> expectedNested = Types.LIST(Types.LIST(Types.INT));
        assertEquals(expectedNested, SchemaHelper.resolveType("array<array<int>>"));
    }

    @Test
    public void testResolveTypeMap() {
        TypeInformation<?> expected = Types.MAP(Types.STRING, Types.INT);
        assertEquals(expected, SchemaHelper.resolveType("map<string, int>"));
    }

    @Test
    public void testBuildRowTypeInfo() {
        Map<String, String> schema = new LinkedHashMap<>();
        schema.put("name", "string");
        schema.put("age", "int");

        TypeInformation<Row> rowType = SchemaHelper.buildRowTypeInfo(schema);
        assertNotNull(rowType);
        assertTrue(rowType.isTupleType());
        assertEquals(Row.class, rowType.getTypeClass());
    }

    @Test
    public void testResolveTypeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> SchemaHelper.resolveType("invalid_type"));
        assertThrows(IllegalArgumentException.class, () -> SchemaHelper.resolveType("map<string>")); // missing comma
    }

    @Test
    public void testResolveTypeVector() {
        TypeInformation<?> expected = org.apache.flink.ml.linalg.typeinfo.VectorTypeInfo.INSTANCE;
        assertEquals(expected, SchemaHelper.resolveType("vector"));
    }

    @Test
    public void testExtractSchemaNullTable() {
        Map<String, String> properties = new HashMap<>();
        properties.put("schema.userId", "string");
        LinkedHashMap<String, String> schema = SchemaHelper.extractSchema(properties, null);
        assertEquals(1, schema.size());
        assertEquals("string", schema.get("userId"));
    }
}
