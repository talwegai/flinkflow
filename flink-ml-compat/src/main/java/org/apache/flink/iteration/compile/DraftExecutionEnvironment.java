package org.apache.flink.iteration.compile;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.dag.Transformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.execution.DefaultExecutorServiceLoader;
import org.apache.flink.iteration.compile.translator.*;
import org.apache.flink.iteration.operator.OperatorWrapper;
import org.apache.flink.iteration.utils.ReflectionUtils;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.streaming.api.transformations.*;
import org.apache.flink.util.Preconditions;

import java.util.*;

public class DraftExecutionEnvironment extends StreamExecutionEnvironment {
    private static final Map<Class<? extends Transformation>, DraftTransformationTranslator> translators = new HashMap<>();

    static {
        translators.put(BroadcastStateTransformation.class, new BroadcastStateTransformationTranslator());
        translators.put(KeyedBroadcastStateTransformation.class, new KeyedBroadcastStateTransformationTranslator());
        translators.put(KeyedMultipleInputTransformation.class, new KeyedBroadcastStateTransformationTranslator());
        translators.put(MultipleInputTransformation.class, new MultipleInputTransformationTranslator());
        translators.put(OneInputTransformation.class, new OneInputTransformationTranslator());
        translators.put(PartitionTransformation.class, new PartitionTransformationTranslator());
        translators.put(ReduceTransformation.class, new ReduceTransformationTranslator());
        translators.put(SideOutputTransformation.class, new SideOutputTransformationTranslator());
        translators.put(TwoInputTransformation.class, new TwoInputTransformationTranslator());
        translators.put(UnionTransformation.class, new UnionTransformationTranslator());
    }

    private final StreamExecutionEnvironment actualEnv;
    private final Set<Integer> explicitlyAddedTransformations = new HashSet<>();
    private final Map<Integer, OperatorWrapper<?, ?>> draftWrappers = new HashMap<>();
    private final Map<Integer, Transformation<?>> draftToActualTransformations = new HashMap<>();
    private OperatorWrapper<?, ?> currentWrapper;

    public DraftExecutionEnvironment(StreamExecutionEnvironment actualEnv, OperatorWrapper<?, ?> currentWrapper) {
        super(
            new DefaultExecutorServiceLoader(),
            (Configuration) ReflectionUtils.getFieldValue(actualEnv, StreamExecutionEnvironment.class, "configuration"),
            (ClassLoader) ReflectionUtils.getFieldValue(actualEnv, StreamExecutionEnvironment.class, "userClassloader")
        );
        this.actualEnv = actualEnv;
        setParallelism(actualEnv.getParallelism());
        if (actualEnv.getMaxParallelism() > 0) {
            setMaxParallelism(actualEnv.getMaxParallelism());
        }
        setBufferTimeout(actualEnv.getBufferTimeout());
        this.currentWrapper = currentWrapper;
    }

    public OperatorWrapper<?, ?> setCurrentWrapper(OperatorWrapper<?, ?> currentWrapper) {
        OperatorWrapper<?, ?> old = this.currentWrapper;
        this.currentWrapper = currentWrapper;
        return old;
    }

    @Override
    public void addOperator(Transformation<?> transformation) {
        recordWrapper(transformation);
        super.addOperator(transformation);
        explicitlyAddedTransformations.add(transformation.getId());
    }

    private void recordWrapper(Transformation<?> transformation) {
        if (draftWrappers.containsKey(transformation.getId()) || draftToActualTransformations.containsKey(transformation.getId())) {
            return;
        }
        draftWrappers.put(transformation.getId(), currentWrapper);
        for (Transformation<?> input : transformation.getInputs()) {
            recordWrapper(input);
        }
    }

    public void addOperatorIfNotExists(Transformation<?> transformation) {
        if (!explicitlyAddedTransformations.contains(transformation.getId())) {
            addOperator(transformation);
        }
    }

    // Binary compatibility override to handle old SourceFunction parameter descriptors in Flink ML bytecode
    public <OUT> DataStreamSource<OUT> addSource(
            org.apache.flink.streaming.api.functions.source.SourceFunction<OUT> function) {
        return super.addSource((org.apache.flink.streaming.api.functions.source.legacy.SourceFunction<OUT>) function);
    }

    public <T> DataStream<T> addDraftSource(DataStream<?> input, TypeInformation<T> typeInfo) {
        DataStreamSource<T> source = addSource(new EmptySource<T>());
        source.setParallelism(input.getParallelism());
        SingleOutputStreamOperator<T> sourceOp = source.returns(typeInfo);
        addOperator(sourceOp.getTransformation());
        draftToActualTransformations.put(sourceOp.getId(), input.getTransformation());
        return sourceOp;
    }

    public void copyToActualEnvironment() {
        for (Transformation<?> transformation : this.transformations) {
            transform(transformation);
        }
    }

    public <T> DataStream<T> getActualStream(int id) {
        return new DataStream<>(actualEnv, getActualTransformation(id));
    }

    private <TF extends Transformation<?>> void transform(TF transformation) {
        if (draftToActualTransformations.containsKey(transformation.getId())) {
            return;
        }
        for (Transformation<?> input : transformation.getInputs()) {
            transform(input);
        }
        OperatorWrapper<?, ?> wrapper = draftWrappers.get(transformation.getId());
        Objects.requireNonNull(wrapper);
        
        DraftTransformationTranslator translator = translators.get(transformation.getClass());
        Preconditions.checkState(translator != null, "Unsupported transformation: " + transformation);
        
        Transformation<?> actual = translator.translate(
            transformation, 
            wrapper, 
            new TranslatorContext(this)
        );
        actualEnv.addOperator(actual);
        draftToActualTransformations.put(transformation.getId(), actual);
    }

    @SuppressWarnings("unchecked")
    private <T> Transformation<T> getActualTransformation(int id) {
        return (Transformation<T>) Objects.requireNonNull(draftToActualTransformations.get(id));
    }

    @Override
    public JobExecutionResult execute(StreamGraph streamGraph) throws Exception {
        throw new UnsupportedOperationException("Unable to execute with a draft execution environment.");
    }

    private static class EmptySource<T> extends org.apache.flink.streaming.api.functions.source.RichSourceFunction<T> {
        private static final long serialVersionUID = 1L;

        @Override
        public void run(org.apache.flink.streaming.api.functions.source.legacy.SourceFunction.SourceContext<T> ctx) throws Exception {}

        @Override
        public void cancel() {}
    }

    private static class TranslatorContext implements DraftTransformationTranslator.Context {
        private final DraftExecutionEnvironment env;

        private TranslatorContext(DraftExecutionEnvironment env) {
            this.env = env;
        }

        @Override
        public Transformation<?> getActualTransformation(int id) {
            return env.getActualTransformation(id);
        }

        @Override
        public ExecutionConfig getExecutionConfig() {
            return env.getConfig();
        }
    }
}
