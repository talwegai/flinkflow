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

package org.apache.flink.ml.linalg.typeinfo;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.serialization.SerializerConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.ml.linalg.DenseVector;

public class DenseVectorTypeInfo extends TypeInformation<DenseVector> {
    private static final long serialVersionUID = 1L;
    public static final DenseVectorTypeInfo INSTANCE = new DenseVectorTypeInfo();

    @Override
    public boolean isBasicType() {
        return false;
    }

    @Override
    public boolean isTupleType() {
        return false;
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    public int getTotalFields() {
        return 1;
    }

    @Override
    public Class<DenseVector> getTypeClass() {
        return DenseVector.class;
    }

    @Override
    public boolean isKeyType() {
        return false;
    }

    public TypeSerializer<DenseVector> createSerializer(ExecutionConfig config) {
        return new DenseVectorSerializer();
    }

    @Override
    public TypeSerializer<DenseVector> createSerializer(SerializerConfig config) {
        return new DenseVectorSerializer();
    }

    @Override
    public String toString() {
        return "DenseVectorTypeInfo";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DenseVectorTypeInfo;
    }

    @Override
    public int hashCode() {
        return DenseVectorTypeInfo.class.hashCode();
    }

    public boolean canEqual(Object obj) {
        return obj instanceof DenseVectorTypeInfo;
    }
}
