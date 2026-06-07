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

import org.apache.flink.ml.api.Stage;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Dynamic factory for constructing and configuring Flink ML stages (Estimators and Transformers) using Java Reflection.
 * Supports short name aliases for built-in Flink ML algorithms.
 */
public class MLStageFactory {
    private static final Map<String, String> SHORT_NAMES = new HashMap<>();

    static {
        SHORT_NAMES.put("minmaxscaler", "org.apache.flink.ml.feature.minmaxscaler.MinMaxScaler");
        SHORT_NAMES.put("minmaxscalermodel", "org.apache.flink.ml.feature.minmaxscaler.MinMaxScalerModel");
        SHORT_NAMES.put("vectorassembler", "org.apache.flink.ml.feature.vectorassembler.VectorAssembler");
        SHORT_NAMES.put("kmeans", "org.apache.flink.ml.clustering.kmeans.KMeans");
        SHORT_NAMES.put("kmeansmodel", "org.apache.flink.ml.clustering.kmeans.KMeansModel");
        SHORT_NAMES.put("logisticregression", "org.apache.flink.ml.classification.logisticregression.LogisticRegression");
        SHORT_NAMES.put("logisticregressionmodel", "org.apache.flink.ml.classification.logisticregression.LogisticRegressionModel");
    }

    /**
     * Dynamically instantiates and configures a Flink ML Stage from step properties.
     * 
     * @param properties Step configuration properties.
     * @return An configured Flink ML Stage instance.
     */
    public static Stage<?> create(Map<String, String> properties) {
        String algo = properties.get("algorithm");
        if (algo == null || algo.trim().isEmpty()) {
            throw new IllegalArgumentException("ML step missing required property: 'algorithm'");
        }

        String className = SHORT_NAMES.get(algo.toLowerCase().trim());
        if (className == null) {
            className = algo; // fallback to user-specified full class name
        }

        try {
            Class<?> clazz = Class.forName(className);
            Stage<?> stage;

            String modelPath = properties.get("modelPath");
            if (modelPath != null && !modelPath.trim().isEmpty()) {
                try {
                    Method loadMethod = clazz.getMethod("load", String.class);
                    stage = (Stage<?>) loadMethod.invoke(null, modelPath);
                } catch (NoSuchMethodException e) {
                    // fallback to default constructor
                    stage = (Stage<?>) clazz.getDeclaredConstructor().newInstance();
                }
            } else {
                stage = (Stage<?>) clazz.getDeclaredConstructor().newInstance();
            }

            // Configure properties via reflection setters
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                if ("algorithm".equals(key) || "modelPath".equals(key) || key.startsWith("schema.")) {
                    continue;
                }

                String setterName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
                Method[] methods = clazz.getMethods();
                Method setter = null;

                for (Method m : methods) {
                    if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                        setter = m;
                        break;
                    }
                }

                if (setter != null) {
                    Class<?> paramType = setter.getParameterTypes()[0];
                    Object arg = convertValue(entry.getValue(), paramType);
                    setter.invoke(stage, arg);
                }
            }

            return stage;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate Flink ML stage '" + className + "': " + e.getMessage(), e);
        }
    }

    private static Object convertValue(String val, Class<?> type) {
        if (type == String.class) {
            return val;
        } else if (type == String[].class) {
            String[] parts = val.split(",");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }
            return parts;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(val.trim());
        } else if (type == Integer[].class) {
            String[] parts = val.split(",");
            Integer[] res = new Integer[parts.length];
            for (int i = 0; i < parts.length; i++) {
                res[i] = Integer.parseInt(parts[i].trim());
            }
            return res;
        } else if (type == int[].class) {
            String[] parts = val.split(",");
            int[] res = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                res[i] = Integer.parseInt(parts[i].trim());
            }
            return res;
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(val.trim());
        } else if (type == Double[].class) {
            String[] parts = val.split(",");
            Double[] res = new Double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                res[i] = Double.parseDouble(parts[i].trim());
            }
            return res;
        } else if (type == double[].class) {
            String[] parts = val.split(",");
            double[] res = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                res[i] = Double.parseDouble(parts[i].trim());
            }
            return res;
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(val.trim());
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(val.trim());
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(val.trim());
        } else if (type == org.apache.flink.ml.linalg.Vector.class) {
            String[] parts = val.split(",");
            double[] res = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                res[i] = Double.parseDouble(parts[i].trim());
            }
            return new org.apache.flink.ml.linalg.DenseVector(res);
        }
        throw new IllegalArgumentException("Unsupported setter parameter type: " + type.getName());
    }
}
