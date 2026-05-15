FROM flink:2.2-java17

LABEL org.opencontainers.image.source="https://github.com/talwegai/flinkflow"
LABEL org.opencontainers.image.description="Declarative, Kubernetes-native Stream Processing on Apache Flink"
LABEL org.opencontainers.image.licenses="Apache-2.0"

WORKDIR /opt/flink

# Pass the project version as a build argument
ARG JAR_VERSION
# Copy application JAR using the passed version
COPY target/flinkflow-${JAR_VERSION}.jar /opt/flink/usrlib/flinkflow.jar

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
