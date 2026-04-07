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


package ai.talweg.flinkflow.config;

import java.util.List;

/**
 * Configuration class representing the overall Flink job.
 * This class holds the job metadata and the list of processing steps.
 */
public class JobConfig {
    /**
     * The name of the Flink job.
     */
    private String name;

    /**
     * The parallelism level for the job. Defaults to 1.
     */
    private int parallelism = 1;

    /**
     * The list of processing steps that define the job's pipeline.
     */
    private List<StepConfig> steps;

    /**
     * Gets the name of the job.
     * 
     * @return the job name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the job.
     * 
     * @param name the job name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the parallelism level of the job.
     * 
     * @return the parallelism level
     */
    public int getParallelism() {
        return parallelism;
    }

    /**
     * Sets the parallelism level of the job.
     * 
     * @param parallelism the parallelism level to set
     */
    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }

    /**
     * Gets the list of steps configured for this job.
     * 
     * @return the list of step configurations
     */
    public List<StepConfig> getSteps() {
        return steps;
    }

    /**
     * Sets the list of steps for this job.
     * 
     * @param steps the list of step configurations to set
     */
    public void setSteps(List<StepConfig> steps) {
        this.steps = steps;
    }
}
