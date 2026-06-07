package org.apache.flink.runtime.metrics.groups;

import org.apache.flink.metrics.CharacterFilter;
import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.metrics.MetricRegistry;
import org.apache.flink.runtime.metrics.dump.QueryScopeInfo;
import org.apache.flink.runtime.metrics.scope.ScopeFormat;
import org.apache.flink.runtime.metrics.util.MetricUtils;
import org.apache.flink.util.AbstractID;
import org.apache.flink.util.Preconditions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Complete drop-in replacement for {@code org.apache.flink.runtime.metrics.groups.TaskMetricGroup}
 * that restores binary compatibility with Flink ML 2.2.0.
 *
 * <p>Flink ML's {@code AbstractBroadcastWrapperOperator} was compiled against a version of Flink
 * where this class exposed a 2-argument overload:
 *
 * <pre>getOrAddOperator(OperatorID, String)</pre>
 *
 * <p>In Flink 2.2.0 that overload was removed in favour of a 3-argument form that additionally
 * accepts a {@code Map<String,String>} of custom metric variables. This local classpath override
 * re-introduces the 2-argument overload, delegating to the 3-argument version with an empty map.
 *
 * <p>All other methods are faithful re-implementations of the Flink 2.2.0 behaviour, derived from
 * the compiled bytecode of {@code flink-runtime-2.2.0.jar}.
 */
public class TaskMetricGroup
        extends ComponentMetricGroup<TaskManagerJobMetricGroup> {

    private final Map<String, InternalOperatorMetricGroup> operators = new HashMap<>();
    private final TaskIOMetricGroup ioMetrics;
    private final ExecutionAttemptID executionId;
    protected final JobVertexID vertexId;
    private final String taskName;
    protected final int subtaskIndex;
    private final int attemptNumber;

    // Package-private constructor — mirrors the Flink 2.2.0 class.
    TaskMetricGroup(
            MetricRegistry registry,
            TaskManagerJobMetricGroup parent,
            ExecutionAttemptID executionAttemptID,
            String taskName) {
        super(
                registry,
                registry.getScopeFormats()
                        .getTaskFormat()
                        .formatScope(
                                Preconditions.checkNotNull(parent),
                                Preconditions.checkNotNull(executionAttemptID)
                                        .getJobVertexId(),
                                executionAttemptID,
                                taskName,
                                executionAttemptID.getSubtaskIndex(),
                                executionAttemptID.getAttemptNumber()),
                parent);
        this.executionId = Preconditions.checkNotNull(executionAttemptID);
        this.vertexId = executionAttemptID.getJobVertexId();
        this.taskName = Preconditions.checkNotNull(taskName);
        this.subtaskIndex = executionAttemptID.getSubtaskIndex();
        this.attemptNumber = executionAttemptID.getAttemptNumber();
        this.ioMetrics = new TaskIOMetricGroup(this);
    }

    // -----------------------------------------------------------------------
    //  Accessors
    // -----------------------------------------------------------------------

    public final TaskManagerJobMetricGroup parent() {
        return (TaskManagerJobMetricGroup) parent;
    }

    public ExecutionAttemptID executionId() {
        return executionId;
    }

    public AbstractID vertexId() {
        return vertexId;
    }

    public String taskName() {
        return taskName;
    }

    public int subtaskIndex() {
        return subtaskIndex;
    }

    public int attemptNumber() {
        return attemptNumber;
    }

    public TaskIOMetricGroup getIOMetricGroup() {
        return ioMetrics;
    }

    // -----------------------------------------------------------------------
    //  Operator metric groups
    // -----------------------------------------------------------------------

    /**
     * 1-argument overload: derives an {@link OperatorID} from the task's vertex ID.
     * Delegates to the 3-argument form.
     */
    public InternalOperatorMetricGroup getOrAddOperator(String name) {
        return getOrAddOperator(OperatorID.fromJobVertexID(vertexId), name, Collections.emptyMap());
    }

    /**
     * 3-argument form: present in Flink 2.2.0 {@code TaskMetricGroup}.
     */
    public InternalOperatorMetricGroup getOrAddOperator(
            OperatorID operatorID, String name, Map<String, String> variables) {
        String truncated = MetricUtils.truncateOperatorName(name);
        String key = String.valueOf(operatorID) + truncated;
        synchronized (this) {
            return operators.computeIfAbsent(
                    key,
                    k -> new InternalOperatorMetricGroup(registry, this, operatorID, truncated, variables));
        }
    }

    /**
     * 2-argument compatibility shim.
     * Flink ML 2.2.0 bytecode calls {@code getOrAddOperator(OperatorID, String)}.
     * This re-introduces that overload and delegates to the 3-argument form with an empty map.
     */
    public InternalOperatorMetricGroup getOrAddOperator(OperatorID operatorID, String name) {
        return getOrAddOperator(operatorID, name, Collections.emptyMap());
    }

    // -----------------------------------------------------------------------
    //  Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void close() {
        super.close();
        parent().removeTaskMetricGroup(executionId);
    }

    // -----------------------------------------------------------------------
    //  AbstractMetricGroup / ComponentMetricGroup contract
    // -----------------------------------------------------------------------

    @Override
    protected void putVariables(Map<String, String> variables) {
        variables.put(ScopeFormat.SCOPE_TASK_VERTEX_ID, vertexId.toString());
        variables.put(ScopeFormat.SCOPE_TASK_NAME, taskName);
        variables.put(ScopeFormat.SCOPE_TASK_ATTEMPT_ID, executionId.toString());
        variables.put(ScopeFormat.SCOPE_TASK_ATTEMPT_NUM, String.valueOf(attemptNumber));
        variables.put(ScopeFormat.SCOPE_TASK_SUBTASK_INDEX, String.valueOf(subtaskIndex));
    }

    @Override
    protected Iterable<? extends ComponentMetricGroup<?>> subComponents() {
        return operators.values();
    }

    @Override
    protected String getGroupName(CharacterFilter filter) {
        return "task";
    }

    @Override
    protected QueryScopeInfo.TaskQueryScopeInfo createQueryServiceMetricInfo(
            CharacterFilter filter) {
        return new QueryScopeInfo.TaskQueryScopeInfo(
                parent().jobId.toString(),
                vertexId.toString(),
                subtaskIndex,
                attemptNumber);
    }
}
