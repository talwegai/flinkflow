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
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.flink.api.common.functions.MapFunction;

import java.io.IOException;

/**
 * Maps a JSON String to an Avro GenericRecord using a provided Schema.
 * Used for writing Avro data to Kafka from a JSON-based Flinkflow stream.
 */
public class JsonToGenericRecordMapper implements MapFunction<String, GenericRecord> {
    private final String schemaString;
    private transient Schema schema;
    private transient GenericDatumReader<GenericRecord> reader;

    public JsonToGenericRecordMapper(String schemaString) {
        this.schemaString = schemaString;
    }

    @Override
    public GenericRecord map(String value) throws Exception {
        if (schema == null) {
            schema = new Schema.Parser().parse(schemaString);
            reader = new GenericDatumReader<>(schema);
        }

        try {
            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, value);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert JSON to Avro GenericRecord: " + e.getMessage(), e);
        }
    }
}
