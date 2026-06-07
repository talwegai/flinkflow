package org.apache.flink.streaming.api.functions.source;

import org.apache.flink.annotation.Public;

@Public
public interface ParallelSourceFunction<T> extends org.apache.flink.streaming.api.functions.source.legacy.ParallelSourceFunction<T>, SourceFunction<T> {
}
