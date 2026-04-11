FROM flink:1.18-java17

WORKDIR /opt/flink

# Copy application JAR
COPY target/flinkflow-0.9.0-BETA.jar /opt/flink/usrlib/flinkflow.jar

# Enable Prometheus metrics reporter
RUN cp /opt/flink/plugins/metrics-prometheus/flink-metrics-prometheus-*.jar /opt/flink/lib/

# Copy all examples for runtime selection
COPY examples /opt/flink/examples/

# Default pipeline configuration (defaults to hello-world)
COPY examples/standalone/java/hello-world.yaml /opt/flink/conf/pipeline.yaml

# Flexible entrypoint 
ENTRYPOINT ["/docker-entrypoint.sh"]

# Default command remains standalone-job for the static deployment model
CMD ["standalone-job", "-Djobmanager.rpc.address=flink-jobmanager", "--job-classname", "ai.talweg.flinkflow.FlinkflowApp", "--job-args", "/opt/flink/conf/pipeline.yaml"]
