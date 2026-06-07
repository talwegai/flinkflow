package org.apache.flink.streaming.api.functions.source;

import org.apache.flink.annotation.Public;

@Public
@SuppressWarnings("deprecation")
public abstract class RichParallelSourceFunction<T> extends RichSourceFunction<T> implements ParallelSourceFunction<T> {
    private static final long serialVersionUID = 1L;
}
