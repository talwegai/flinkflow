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

import org.apache.fluss.client.Connection;
import org.apache.fluss.client.ConnectionFactory;
import org.apache.fluss.config.Configuration;
import org.apache.fluss.metadata.TablePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe connection manager and configuration utility for Apache Fluss.
 * Dynamically converts step properties into native Fluss configurations,
 * providing zero-boilerplate cluster discovery and connection reuse.
 */
public class FlussManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(FlussManager.class);

    private static final String DEFAULT_DATABASE = "default";
    private static final ConcurrentHashMap<String, Connection> CONNECTION_POOL = new ConcurrentHashMap<>();

    /**
     * Resolves the Fluss coordinator/bootstrap address using standard hierarchy:
     * 1. Step properties ("bootstrap.servers" or "coordinator.server")
     * 2. Environment variables ("FLUSS_COORDINATOR_SERVER", "FLUSS_BOOTSTRAP_SERVERS")
     * 3. Fallback default ("localhost:9123")
     */
    public static String resolveBootstrapServers(Map<String, String> properties) {
        if (properties != null) {
            if (properties.containsKey("bootstrap.servers") && properties.get("bootstrap.servers") != null && !properties.get("bootstrap.servers").trim().isEmpty()) {
                return properties.get("bootstrap.servers").trim();
            }
            if (properties.containsKey("coordinator.server") && properties.get("coordinator.server") != null && !properties.get("coordinator.server").trim().isEmpty()) {
                return properties.get("coordinator.server").trim();
            }
        }
        String envCoord = System.getenv("FLUSS_COORDINATOR_SERVER");
        if (envCoord != null && !envCoord.trim().isEmpty()) {
            return envCoord.trim();
        }
        String envBootstrap = System.getenv("FLUSS_BOOTSTRAP_SERVERS");
        if (envBootstrap != null && !envBootstrap.trim().isEmpty()) {
            return envBootstrap.trim();
        }
        return "localhost:9123";
    }

    /**
     * Dynamically builds native {@link Configuration} by converting all YAML step properties.
     */
    public static Configuration buildConfiguration(Map<String, String> properties) {
        Configuration conf = new Configuration();
        String bootstrap = resolveBootstrapServers(properties);
        conf.setString("bootstrap.servers", bootstrap);

        if (properties != null) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    // Pass all dynamic Fluss properties directly to native configuration
                    conf.setString(entry.getKey(), entry.getValue());
                }
            }
        }
        return conf;
    }

    /**
     * Gets or creates a cached, reusable {@link Connection} for the given properties.
     */
    public static Connection getConnection(Map<String, String> properties) {
        String bootstrap = resolveBootstrapServers(properties);
        return CONNECTION_POOL.computeIfAbsent(bootstrap, k -> {
            LOG.info("Establishing connection to Apache Fluss cluster at: {}", k);
            Configuration conf = buildConfiguration(properties);
            return ConnectionFactory.createConnection(conf);
        });
    }

    /**
     * Resolves a table name string (e.g., "my_table" or "my_db.my_table") into a {@link TablePath}.
     */
    public static TablePath resolveTablePath(String tableOrPath) {
        if (tableOrPath == null || tableOrPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Fluss table name or path cannot be null or empty.");
        }
        String trimmed = tableOrPath.trim();
        if (trimmed.contains(".")) {
            String[] parts = trimmed.split("\\.", 2);
            return TablePath.of(parts[0], parts[1]);
        }
        return TablePath.of(DEFAULT_DATABASE, trimmed);
    }
}
