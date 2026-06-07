package org.apache.flink.streaming.api.functions.source;

import org.apache.flink.annotation.Public;

@Public
public interface SourceFunction<T> extends org.apache.flink.streaming.api.functions.source.legacy.SourceFunction<T> {
    
    @Public
    public interface SourceContext<T> extends org.apache.flink.streaming.api.functions.source.legacy.SourceFunction.SourceContext<T> {
    }
}
