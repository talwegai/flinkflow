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

package ai.talweg.flinkflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ai.talweg.flinkflow.config.JobConfig;
import ai.talweg.flinkflow.config.StepConfig;
import ai.talweg.flinkflow.core.ProcessorFactory;
import ai.talweg.flinkflow.flowlet.FlowletRegistry;
import ai.talweg.flinkflow.flowlet.FlowletResolver;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import java.time.Duration;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import java.util.concurrent.TimeUnit;
import ai.talweg.flinkflow.config.k8s.PipelineKubernetesLoader;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroSerializationSchema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.Schema;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.connector.kafka.sink.KafkaSinkBuilder;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import ai.talweg.flinkflow.config.SecretResolver;

import java.io.File;
import java.time.Duration;
import java.util.Map;

/**
 * Main application class for Flinkflow.
 * This class reads a YAML configuration file, constructs a Flink streaming
 * pipeline
 * based on the defined steps, and executes the job.
 */
public class FlinkflowApp {

    /**
     * Entry point for the Flinkflow application.
     * 
     * @param args Command line arguments. Expects at least one argument: the path
     *             to the YAML config file.
     * @throws Exception If there is an error reading the config or executing the
     *                   Flink job.
     */
    public static void main(String[] args) throws Exception {
        int status = execute(args);
        if (status != 0) {
            System.exit(status);
        }
    }

    /**
     * Executes the Flinkflow application logic.
     * 
     * @param args Command line arguments.
     * @return Status code (0 for success, non-zero for error).
     * @throws Exception If there is an error during execution.
     */
    public static int execute(String[] args) throws Exception {
        // Parse optional runner flags
        String k8sNamespace = null;
        boolean enableK8sFlowlets = false;
        String k8sPipelineName = null;
        String localConfigPath = null;
        boolean dryRun = false;
        String flowletDirPath = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                switch (args[i]) {
                    case "--dry-run":
                        dryRun = true;
                        break;
                    case "--enable-k8s-flowlets":
                        enableK8sFlowlets = true;
                        break;
                    case "--k8s-namespace":
                        if (i + 1 < args.length)
                            k8sNamespace = args[++i];
                        break;
                    case "--k8s-pipeline":
                        if (i + 1 < args.length)
                            k8sPipelineName = args[++i];
                        break;
                    case "--flowlet-dir":
                        if (i + 1 < args.length)
                            flowletDirPath = args[++i];
                        break;
                    default:
                        break;
                }
            } else if (localConfigPath == null) {
                localConfigPath = args[i];
            }
        }

        if (localConfigPath == null && k8sPipelineName == null) {
            System.err.println("Usage: flinkflow [path-to-config.yaml] [options]");
            System.err.println("Options:");
            System.err.println(
                    "  --k8s-pipeline <name>      Load entire pipeline specification from a Kubernetes Pipeline CR");
            System.err.println(
                    "  --enable-k8s-flowlets      Discover Flowlet CRs from the Kubernetes cluster (auto-enabled if --k8s-pipeline is used)");
            System.err.println("  --k8s-namespace <ns>       Kubernetes namespace to query (default: in-cluster ns)");
            System.err.println("  --flowlet-dir <dir>        Load local Flowlet definitions from a directory");
            System.err.println("  --dry-run                  Resolve config and print expanded YAML without actually executing");
            return 1;
        }

        JobConfig jobConfig;
        if (k8sPipelineName != null) {
            jobConfig = new PipelineKubernetesLoader().load(k8sPipelineName, k8sNamespace);
            // If we load from K8s Pipeline, we automatically enable K8s Flowlet discovery
            // too
            enableK8sFlowlets = true;
        } else {
            File configFile = new File(localConfigPath);
            if (!configFile.exists()) {
                System.err.println("Error: Config file not found: " + localConfigPath);
                return 1;
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            jobConfig = mapper.readValue(configFile, JobConfig.class);
        }

        // Build the Flowlet registry with local directory and discovery from Kubernetes CRs
        FlowletRegistry flowletRegistry = new FlowletRegistry(flowletDirPath, k8sNamespace, enableK8sFlowlets);
        FlowletResolver flowletResolver = new FlowletResolver(flowletRegistry);

        // Expand any 'type: flowlet' steps into their concrete constituent steps
        java.util.List<StepConfig> resolvedSteps = new java.util.ArrayList<>();
        for (StepConfig step : jobConfig.getSteps()) {
            if ("flowlet".equalsIgnoreCase(step.getType())) {
                resolvedSteps.addAll(flowletResolver.resolve(step));
            } else {
                resolvedSteps.add(step);
            }
        }

        // Resolve Kubernetes secrets starting with 'secret:' in all steps
        try (SecretResolver secretResolver = new SecretResolver()) {
            for (StepConfig step : resolvedSteps) {
                secretResolver.resolveStepSecrets(step, k8sNamespace);
            }
        }

        // Validate the structure of the resolved job pipeline graph
        ai.talweg.flinkflow.validation.GraphValidator.validate(resolvedSteps);

        if (dryRun) {
            jobConfig.setSteps(resolvedSteps);
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            System.out.println("--- DRY RUN MODE: Expanded Job Configuration ---");
            System.out.println(yamlMapper.writeValueAsString(jobConfig));
            System.out.println("------------------------------------------------");
            System.out.println("Dry-run validation completed successfully.");
            return 0;
        }

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(jobConfig.getParallelism());

        DataStream<String> stream = null;

        for (StepConfig step : resolvedSteps) {
            switch (step.getType().toLowerCase()) {
                case "source":
                case "datagen":
                case "datagen-source":
                    stream = createSource(env, step);
                    break;
                case "process":
                case "transform":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    stream = stream.map(ProcessorFactory.createMapper(step.getCode(), step.getLanguage()));
                    break;
                case "filter":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    stream = stream.filter(ProcessorFactory.createFilter(step.getCode(), step.getLanguage()));
                    break;
                case "flatmap":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    stream = stream.flatMap(ProcessorFactory.createFlatMap(step.getCode(), step.getLanguage()));
                    break;
                case "keyby":
                case "groupby":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    stream = stream.keyBy(ProcessorFactory.createKeySelector(step.getCode(), step.getLanguage()));
                    break;
                case "reduce":
                case "aggregate":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    if (!(stream instanceof org.apache.flink.streaming.api.datastream.KeyedStream)) {
                        throw new RuntimeException("Reduce/Aggregate step must be preceded by a keyby/groupby step.");
                    }
                    stream = ((org.apache.flink.streaming.api.datastream.KeyedStream<String, ?>) stream)
                            .reduce(ProcessorFactory.createReducer(step.getCode(), step.getLanguage()));
                    break;
                case "window":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    if (!(stream instanceof org.apache.flink.streaming.api.datastream.KeyedStream)) {
                        throw new RuntimeException("Window step must be preceded by a keyby/groupby step.");
                    }
                    org.apache.flink.streaming.api.datastream.KeyedStream<String, ?> keyedStream = (org.apache.flink.streaming.api.datastream.KeyedStream<String, ?>) stream;

                    String windowType = step.getProperties().getOrDefault("windowType", "tumbling").toLowerCase();
                    long windowSize = Long.parseLong(step.getProperties().getOrDefault("size", "10"));

                    if ("tumbling".equals(windowType)) {
                        stream = keyedStream
                                .window(org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows
                                        .of(Duration.ofSeconds(windowSize)))
                                .reduce(ProcessorFactory.createWindowReducer(step.getCode(), step.getLanguage()));
                    } else if ("sliding".equals(windowType)) {
                        long slide = Long.parseLong(step.getProperties().getOrDefault("slide", "5"));
                        stream = keyedStream
                                .window(org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows
                                        .of(Duration.ofSeconds(windowSize),
                                                Duration.ofSeconds(slide)))
                                .reduce(ProcessorFactory.createWindowReducer(step.getCode(), step.getLanguage()));
                    } else if ("session".equals(windowType)) {
                        long gap = Long.parseLong(step.getProperties().getOrDefault("gap", "5"));
                        stream = keyedStream
                                .window(org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows
                                        .withGap(Duration.ofSeconds(gap)))
                                .reduce(ProcessorFactory.createWindowReducer(step.getCode(), step.getLanguage()));
                    } else {
                        throw new RuntimeException("Unknown window type: " + windowType);
                    }
                    break;
                case "sideoutput":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    String sideOutputName = step.getProperties().getOrDefault("outputName", "side-output");

                    org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator<String> processStream = stream
                            .process(ProcessorFactory.createSideOutput(step.getCode(), sideOutputName, step.getLanguage()));

                    org.apache.flink.util.OutputTag<String> outputTag = new org.apache.flink.util.OutputTag<String>(
                            sideOutputName) {
                    };
                    processStream.getSideOutput(outputTag).print();
                    stream = processStream;
                    break;
                case "datamapper":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    String xsltPath = step.getProperties().get("xsltPath");
                    stream = stream.map(ProcessorFactory.createDataMapper(xsltPath));
                    break;
                case "join":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    stream = applyJoin(env, stream, step);
                    break;
                case "http-lookup":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    stream = applyHttpLookup(stream, step);
                    break;
                case "agent":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    String agentModel = step.getProperties().getOrDefault("model", "gpt-4o");
                    String systemPrompt = step.getProperties().getOrDefault("systemPrompt", "You are a helpful assistant.");
                    boolean useMemory = Boolean.parseBoolean(step.getProperties().getOrDefault("memory", "true"));
                    
                    if (useMemory && stream instanceof org.apache.flink.streaming.api.datastream.KeyedStream) {
                        // Flink 2.x: Enable async state for the keyed stream before applying the process function
                        // This allows State V2 async operations to work correctly.
                        stream = ((org.apache.flink.streaming.api.datastream.KeyedStream<String, ?>) stream).enableAsyncState();
                    }
                    
                    stream = stream.process(ProcessorFactory.createAgent(step.getName(), agentModel, systemPrompt, useMemory, step.getProperties(), flowletRegistry.getCatalog()));
                    break;
                case "sink":
                    if (stream == null)
                        throw new RuntimeException("Stream not initialized. Source step missing.");
                    createSink(stream, step);
                    break;
                default:
                    throw new RuntimeException("Unknown step type: " + step.getType());
            }
        }

        env.execute(jobConfig.getName());
        return 0;
    }

    /**
     * Creates a DataStream source based on the step configuration.
     * Supports "kafka-source", "file-source", and "static-source".
     * 
     * @param env  The Flink execution environment.
     * @param step The configuration for the source step.
     * @return A DataStream representing the source.
     */
    private static DataStream<String> createSource(StreamExecutionEnvironment env, StepConfig step) {
        Map<String, String> props = step.getProperties();
        String sourceName = step.getConnector() != null ? step.getConnector() : step.getName();

        if ("kafka-source".equalsIgnoreCase(sourceName)) {
            var builder = KafkaSource.<String>builder()
                    .setBootstrapServers(props.get("bootstrap.servers"))
                    .setTopics(props.get("topic"))
                    .setGroupId(props.get("group.id"))
                    .setStartingOffsets(OffsetsInitializer.earliest())
                    .setValueOnlyDeserializer(new SimpleStringSchema());

            // Pass additional Kafka properties (e.g., for auth)
            props.forEach((k, v) -> {
                if (!k.equals("bootstrap.servers") && !k.equals("topic") && !k.equals("group.id")) {
                    builder.setProperty(k, v);
                }
            });

            KafkaSource<String> source = builder.build();

            return env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");
        } else if ("file-source".equalsIgnoreCase(sourceName) || "s3-source".equalsIgnoreCase(sourceName)) {
            String path = props.get("path");
            org.apache.flink.connector.file.src.FileSource.FileSourceBuilder<String> builder = FileSource
                    .forRecordStreamFormat(
                            new TextLineInputFormat(), new Path(path));

            if (props.containsKey("monitorInterval")) {
                long interval = Long.parseLong(props.get("monitorInterval"));
                builder.monitorContinuously(Duration.ofSeconds(interval));
            }

            FileSource<String> source = builder.build();
            return env.fromSource(source, WatermarkStrategy.noWatermarks(), "File/S3 Source");
        } else if ("static-source".equalsIgnoreCase(sourceName)) {
            // Useful for testing without Kafka
            String content = props.getOrDefault("content", "Hello Flinkflow");
            return env.fromElements(content.split("\\|"));
        } else if ("datagen-source".equalsIgnoreCase(sourceName) || "datagen".equalsIgnoreCase(sourceName)) {
            long rowsPerSecond = Long.parseLong(props.getOrDefault("rowsPerSecond", "1"));
            long totalRows = Long.parseLong(props.getOrDefault("totalRows", "-1")); // -1 for unbounded

            String payload = props.getOrDefault("payload", "test-data");

            DataGeneratorSource<String> dataGenSource = new DataGeneratorSource<>(
                    (GeneratorFunction<Long, String>) index -> payload + "-" + index,
                    totalRows == -1 ? Long.MAX_VALUE : totalRows,
                    RateLimiterStrategy.perSecond(rowsPerSecond),
                    org.apache.flink.api.common.typeinfo.Types.STRING);

            return env.fromSource(dataGenSource, WatermarkStrategy.noWatermarks(), "DataGen Source");
        } else if ("kafka-avro-source".equalsIgnoreCase(sourceName) || ("kafka-source".equalsIgnoreCase(sourceName) && "avro".equalsIgnoreCase(props.get("format")))) {
            String registryUrl = props.get("schema.registry.url");
            String topic = props.get("topic");
            
            if (registryUrl == null) {
                throw new RuntimeException("avro format requires 'schema.registry.url' property.");
            }

            // In a future update, we will add automatic schema fetching by subject.
            String schemaString = props.get("schema.literal");
            String subject = props.getOrDefault("schema.subject", topic + "-value");
            
            Schema readerSchema;
            if (schemaString != null) {
                readerSchema = new Schema.Parser().parse(schemaString);
            } else {
                readerSchema = fetchLatestSchema(registryUrl, subject);
            }

            KafkaSourceBuilder<GenericRecord> builder = KafkaSource.<GenericRecord>builder()
                    .setBootstrapServers(props.get("bootstrap.servers"))
                    .setTopics(topic)
                    .setGroupId(props.get("group.id"))
                    .setStartingOffsets(OffsetsInitializer.earliest());
            
            if (readerSchema != null) {
                builder.setValueOnlyDeserializer(ConfluentRegistryAvroDeserializationSchema.forGeneric(readerSchema, registryUrl));
            } else {
                // Fallback or handle missing schema - Flink's Confluent deserializer often needs the reader schema
                // Try a dummy schema or fetch it - for now we'll throw an error if missing
                throw new RuntimeException("avro format currently requires 'schema.literal' for the reader schema.");
            }

            props.forEach((k, v) -> {
                if (!k.equals("bootstrap.servers") && !k.equals("topic") && !k.equals("group.id") && !k.equals("schema.registry.url") && !k.equals("format")) {
                    builder.setProperty(k, v);
                }
            });

            KafkaSource<GenericRecord> source = builder.build();
            DataStream<GenericRecord> avroStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Avro Source");
            
            // Map GenericRecord to JSON String to maintain our String wire format
            return avroStream.map(record -> record.toString());
        } else {
            throw new RuntimeException("Unsupported source: " + sourceName);
        }
    }

    /**
     * Adds a sink to the DataStream based on the step configuration.
     * Supports "console", "kafka-sink", and "file-sink".
     * 
     * @param stream The DataStream to sink.
     * @param step   The configuration for the sink step.
     */
    private static void createSink(DataStream<String> stream, StepConfig step) {

        String sinkName = step.getConnector() != null ? step.getConnector() : step.getName();

        if ("console".equalsIgnoreCase(sinkName) || "console-sink".equalsIgnoreCase(sinkName)) {
            stream.print();
        } else if ("kafka-sink".equalsIgnoreCase(sinkName) && !"avro".equalsIgnoreCase(step.getProperties().get("format"))) {
            Map<String, String> props = step.getProperties();
            var builder = KafkaSink.<String>builder()
                    .setBootstrapServers(props.get("bootstrap.servers"))
                    .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                            .setTopic(props.get("topic"))
                            .setValueSerializationSchema(new SimpleStringSchema())
                            .build());

            // Pass additional Kafka properties (e.g., for auth)
            props.forEach((k, v) -> {
                if (!k.equals("bootstrap.servers") && !k.equals("topic")) {
                    builder.setProperty(k, v);
                }
            });

            KafkaSink<String> sink = builder.build();

            stream.sinkTo(sink);
        } else if ("kafka-avro-sink".equalsIgnoreCase(sinkName) || ("kafka-sink".equalsIgnoreCase(sinkName) && "avro".equalsIgnoreCase(step.getProperties().get("format")))) {
            Map<String, String> props = step.getProperties();
            String registryUrl = props.get("schema.registry.url");
            String topic = props.get("topic");
            String subject = props.getOrDefault("schema.subject", topic + "-value");
            
            if (registryUrl == null) {
                throw new RuntimeException("avro format requires 'schema.registry.url' property.");
            }

            String schemaString = props.get("schema.literal");
            Schema writerSchema;
            String finalSchemaString = schemaString;
            if (schemaString != null) {
                writerSchema = new Schema.Parser().parse(schemaString);
            } else {
                writerSchema = fetchLatestSchema(registryUrl, subject);
                finalSchemaString = writerSchema.toString();
            }

            KafkaSinkBuilder<GenericRecord> builder = KafkaSink.<GenericRecord>builder()
                    .setBootstrapServers(props.get("bootstrap.servers"))
                    .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                            .setTopic(topic)
                            .setValueSerializationSchema(ConfluentRegistryAvroSerializationSchema.forGeneric(subject, writerSchema, registryUrl))
                            .build());

            props.forEach((k, v) -> {
                if (!k.equals("bootstrap.servers") && !k.equals("topic") && !k.equals("schema.registry.url") && !k.equals("schema.subject") && !k.equals("format") && !k.equals("schema.literal")) {
                    builder.setProperty(k, v);
                }
            });

            KafkaSink<GenericRecord> sink = builder.build();
            
            // Map the String stream to GenericRecord and perform the sink
            stream.map(new ai.talweg.flinkflow.core.JsonToGenericRecordMapper(finalSchemaString))
                   .sinkTo(sink);
        } else if ("file-sink".equalsIgnoreCase(sinkName) || "s3-sink".equalsIgnoreCase(sinkName)) {
            Map<String, String> props = step.getProperties();
            String path = props.get("path");

            long rolloverInterval = Long.parseLong(props.getOrDefault("rolloverInterval", "60"));
            long inactivityInterval = Long.parseLong(props.getOrDefault("inactivityInterval", "30"));
            String maxPartSize = props.getOrDefault("maxPartSize", "128mb");

            FileSink<String> sink = FileSink
                    .forRowFormat(new Path(path), new SimpleStringEncoder<String>("UTF-8"))
                    .withRollingPolicy(
                            DefaultRollingPolicy.builder()
                                    .withRolloverInterval(Duration.ofSeconds(rolloverInterval))
                                    .withInactivityInterval(Duration.ofSeconds(inactivityInterval))
                                    .withMaxPartSize(MemorySize.parse(maxPartSize))
                                    .build())
                    .build();
            stream.sinkTo(sink);
        } else if ("http-sink".equalsIgnoreCase(sinkName) || "webhook-sink".equalsIgnoreCase(sinkName)) {
            Map<String, String> props = step.getProperties();

            String endpointUrl = props.get("url");
            String urlCode = props.get("urlCode");
            if (urlCode == null && endpointUrl != null) {
                // simple static URL
                urlCode = "return \"" + endpointUrl + "\";";
            } else if (urlCode == null && endpointUrl == null) {
                throw new RuntimeException("http-sink requires 'url' or 'urlCode' property.");
            }

            String method = props.getOrDefault("method", "POST");
            String authCode = props.get("authCode");

            stream.process(new ai.talweg.flinkflow.core.DynamicHttpSinkFunction(urlCode, method, authCode)).name(step.getName());
        } else if ("jdbc-sink".equalsIgnoreCase(sinkName)) {
            Map<String, String> props = step.getProperties();
            String driverUrl = props.get("url"); // e.g. jdbc:postgresql://localhost:5432/mydb
            String username = props.get("username");
            String password = props.get("password");
            String sql = props.get("sql");
            String codeBody = step.getCode();

            if (driverUrl == null || sql == null || codeBody == null) {
                throw new RuntimeException("jdbc-sink requires 'url', 'sql', and 'code' (Java builder) body.");
            }

            org.apache.flink.connector.jdbc.JdbcExecutionOptions.Builder execBuilder = org.apache.flink.connector.jdbc.JdbcExecutionOptions
                    .builder();
            if (props.containsKey("batchSize"))
                execBuilder.withBatchSize(Integer.parseInt(props.get("batchSize")));
            if (props.containsKey("batchIntervalMs"))
                execBuilder.withBatchIntervalMs(Long.parseLong(props.get("batchIntervalMs")));
            if (props.containsKey("maxRetries"))
                execBuilder.withMaxRetries(Integer.parseInt(props.get("maxRetries")));

            org.apache.flink.connector.jdbc.JdbcConnectionOptions.JdbcConnectionOptionsBuilder connectionOptionsBuilder = new org.apache.flink.connector.jdbc.JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                    .withUrl(driverUrl)
                    .withDriverName(props.getOrDefault("driverName", "org.postgresql.Driver"));

            if (username != null)
                connectionOptionsBuilder.withUsername(username);
            if (password != null)
                connectionOptionsBuilder.withPassword(password);

            org.apache.flink.api.connector.sink2.Sink<String> jdbcSink = org.apache.flink.connector.jdbc.core.datastream.sink.JdbcSink
                    .<String>builder()
                    .withQueryStatement(sql, new ai.talweg.flinkflow.core.DynamicJdbcStatementBuilder(codeBody))
                    .withExecutionOptions(execBuilder.build())
                    .buildAtLeastOnce(connectionOptionsBuilder.build());

            stream.sinkTo(jdbcSink).name(step.getName());
        } else {
            throw new RuntimeException("Unsupported sink: " + sinkName);
        }
    }

    /**
     * Applies an interval join between the current stream and a secondary source
     * defined in the step.
     * 
     * @param env        The Flink execution environment.
     * @param leftStream The current (left) DataStream.
     * @param step       The configuration for the join step, including secondary
     *                   source details and join logic.
     * @return The resulting DataStream after the join operation.
     */
    private static DataStream<String> applyJoin(StreamExecutionEnvironment env, DataStream<String> leftStream,
            StepConfig step) {
        Map<String, String> props = step.getProperties();

        // Create secondary source
        DataStream<String> rightStream = createSource(env, step);

        // Interval Join requires watermarks. We'll add a default one for now.
        // In a real app, this should be configurable.
        leftStream = leftStream.assignTimestampsAndWatermarks(
                WatermarkStrategy.<String>forMonotonousTimestamps()
                        .withTimestampAssigner((event, timestamp) -> System.currentTimeMillis()));
        rightStream = rightStream.assignTimestampsAndWatermarks(
                WatermarkStrategy.<String>forMonotonousTimestamps()
                        .withTimestampAssigner((event, timestamp) -> System.currentTimeMillis()));

        String leftKeyExpr = props.getOrDefault("leftKey", "return input;");
        String rightKeyExpr = props.getOrDefault("rightKey", "return input;");
        long lowerBound = Long.parseLong(props.getOrDefault("lowerBound", "-5"));
        long upperBound = Long.parseLong(props.getOrDefault("upperBound", "5"));

        return leftStream.keyBy(ProcessorFactory.createKeySelector(leftKeyExpr, step.getLanguage()))
                .intervalJoin(rightStream.keyBy(ProcessorFactory.createKeySelector(rightKeyExpr, step.getLanguage())))
                .between(Duration.ofSeconds(lowerBound), Duration.ofSeconds(upperBound))
                .process(ProcessorFactory.createJoiner(step.getCode(), step.getLanguage()));
    }

    /**
     * Applies an asynchronous HTTP lookup to the DataStream.
     * 
     * @param stream The current DataStream.
     * @param step   The configuration for the http-lookup step.
     * @return The resulting DataStream after enrichment.
     */
    private static DataStream<String> applyHttpLookup(DataStream<String> stream, StepConfig step) {
        Map<String, String> props = step.getProperties();
        String urlCode = props.getOrDefault("urlCode", "return \"http://localhost\";");
        String authCode = props.get("authCode");
        String responseCode = step.getCode();
        long timeout = Long.parseLong(props.getOrDefault("timeout", "10000"));
        int capacity = Integer.parseInt(props.getOrDefault("capacity", "100"));

        return AsyncDataStream.unorderedWait(
                stream,
                ProcessorFactory.createAsyncHttpLookup(urlCode, responseCode, authCode, step.getLanguage()),
                timeout,
                TimeUnit.MILLISECONDS,
                capacity);
    }

    /**
     * Fetches the latest schema from a Confluent Schema Registry.
     * 
     * @param registryUrl The URL of the Schema Registry.
     * @param subject     The subject name.
     * @return The latest Schema for that subject.
     * @throws RuntimeException if the schema cannot be fetched.
     */
    private static Schema fetchLatestSchema(String registryUrl, String subject) {
        try (SchemaRegistryClient client = new CachedSchemaRegistryClient(registryUrl, 100)) {
            SchemaMetadata metadata = client.getLatestSchemaMetadata(subject);
            return new Schema.Parser().parse(metadata.getSchema());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch latest schema for subject '" + subject + "' from registry: " + e.getMessage(), e);
        }
    }
}
