package org.apache.flink.streaming.api.functions.source;

import org.apache.flink.annotation.Public;
import org.apache.flink.api.common.functions.AbstractRichFunction;

@Public
@SuppressWarnings("deprecation")
public abstract class RichSourceFunction<T> extends AbstractRichFunction implements SourceFunction<T> {
    private static final long serialVersionUID = 1L;
}
