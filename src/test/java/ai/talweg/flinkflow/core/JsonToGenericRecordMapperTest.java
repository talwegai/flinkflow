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

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonToGenericRecordMapperTest {

    @Test
    public void testJsonToGenericRecordConversion() throws Exception {
        String schemaString = "{\n" +
                "  \"type\": \"record\",\n" +
                "  \"name\": \"User\",\n" +
                "  \"fields\": [\n" +
                "    {\"name\": \"id\", \"type\": \"int\"},\n" +
                "    {\"name\": \"name\", \"type\": \"string\"},\n" +
                "    {\"name\": \"active\", \"type\": \"boolean\"}\n" +
                "  ]\n" +
                "}";

        JsonToGenericRecordMapper mapper = new JsonToGenericRecordMapper(schemaString);

        String jsonInput = "{\"id\": 123, \"name\": \"John Doe\", \"active\": true}";
        GenericRecord record = mapper.map(jsonInput);

        assertEquals(123, record.get("id"));
        assertEquals("John Doe", record.get("name").toString());
        assertEquals(true, record.get("active"));
    }

    @Test
    public void testAvroToStringMappingInFlinkflowApp() throws Exception {
        // This simulates the logic used in FlinkflowApp to convert Avro Source results to JSON Strings
        String schemaString = "{\n" +
                "  \"type\": \"record\",\n" +
                "  \"name\": \"Test\",\n" +
                "  \"fields\": [{\"name\": \"f1\", \"type\": \"string\"}]\n" +
                "}";
        Schema schema = new Schema.Parser().parse(schemaString);
        GenericRecord record = new GenericData.Record(schema);
        record.put("f1", "value1");

        // The logic in FlinkflowApp: .map(record -> record.toString())
        String jsonResult = record.toString();
        
        // GenericRecord.toString() produces JSON-like output
        assertTrue(jsonResult.contains("\"f1\": \"value1\""));
    }
}
