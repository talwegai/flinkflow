# `flink-ml-compat` — Flink ML Binary-Compatibility Shims

## Why does this module exist?

`flink-ml-core` / `flink-ml-lib` / `flink-ml-iteration` **2.2.0** were compiled against a
pre-Flink-2.x version of the Flink runtime. When loaded on the current Flink 2.2.1 runtime,
the JVM fails at class-loading time with `NoClassDefFoundError` or `NoSuchFieldError` because
Flink 2.x removed or changed several internal classes that Flink ML's compiled bytecode still
references.

This module re-introduces those classes at their **original package paths** as minimal stubs or
faithful re-implementations. The JVM classloader finds them here before looking in the Flink
runtime JARs, satisfying Flink ML's binary expectations without modifying either Flink or Flink ML.

> **Your application code never calls these classes directly.** They exist purely so the
> classloader doesn't crash when Flink ML is loaded.

---

## What is shimmed and why

| Class | Reason |
|---|---|
| `o.a.f.streaming.api.functions.source.SourceFunction` | Flink ML sources extend this; removed in Flink 2.x in favour of the new `Source<T,S,E>` SPI. |
| `o.a.f.streaming.api.functions.source.ParallelSourceFunction` | Same. |
| `o.a.f.streaming.api.functions.source.RichSourceFunction` | Same. |
| `o.a.f.streaming.api.functions.source.RichParallelSourceFunction` | Same. |
| `o.a.f.streaming.api.graph.StreamConfig` | Flink ML reads fields (`chainingStrategy`, etc.) removed from `StreamConfig` in Flink 2.x. |
| `o.a.f.streaming.api.operators.StreamingRuntimeContext` | Flink ML calls removed 1.x methods (`getIndexOfThisSubtask()`, etc.) on this class. |
| `o.a.f.streaming.api.operators.StreamOperatorStateContext` | State initialisation interface changed. |
| `o.a.f.streaming.api.operators.StreamTaskStateInitializer` | State init hook removed. |
| `o.a.f.runtime.metrics.groups.TaskMetricGroup` | Flink ML calls the 2-arg `getOrAddOperator(OperatorID, String)` overload removed in Flink 2.2.1. |
| `o.a.f.ml.linalg.typeinfo.DenseVectorTypeInfo` | Flink ML's type system references this; class signature changed. |
| `o.a.f.ml.linalg.typeinfo.SparseVectorTypeInfo` | Same. |
| `o.a.f.ml.linalg.typeinfo.VectorTypeInfo` | Same. |
| `o.a.f.ml.linalg.typeinfo.VectorWithNormTypeInfo` | Same. |
| `o.a.f.api.common.typeinfo.TypeInformation` | Abstract base type info API changed in Flink 2.x. |
| `o.a.f.iteration.compile.DraftExecutionEnvironment` | Flink ML's iteration engine references this removed class. |

---

## When to remove this module

When the Apache Flink ML project publishes a release that declares
`flink-streaming-java ≥ 2.0` in its own POM and no longer references the legacy
`SourceFunction` / `SourceContext` API, this entire module can be deleted and its
dependency removed from the root `pom.xml`.

Track upstream progress at:
- https://github.com/apache/flink-ml/issues (search "Flink 2.x compatibility")

---

## Module structure

```
flink-ml-compat/
├── pom.xml                        ← child POM, parent = ai.talweg:flinkflow
└── src/main/java/org/apache/flink/
    ├── api/common/typeinfo/       ← TypeInformation shim
    ├── iteration/compile/         ← DraftExecutionEnvironment shim
    ├── ml/linalg/typeinfo/        ← Vector type info shims (4 files)
    ├── runtime/metrics/groups/    ← TaskMetricGroup shim
    └── streaming/api/
        ├── functions/source/      ← SourceFunction family shims (4 files)
        ├── graph/                 ← StreamConfig shim
        └── operators/             ← StreamingRuntimeContext + state shims (3 files)
```
