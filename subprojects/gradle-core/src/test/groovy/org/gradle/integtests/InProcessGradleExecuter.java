/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.integtests;

import junit.framework.AssertionFailedError;
import org.gradle.*;
import org.gradle.api.GradleException;
import org.gradle.api.GradleScriptException;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.execution.TaskExecutionListener;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.StandardOutputListener;
import org.gradle.execution.BuiltInTasksBuildExecuter;
import static org.gradle.util.Matchers.*;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// todo: implement more of the unsupported methods
public class InProcessGradleExecuter extends AbstractGradleExecuter {
    private final StartParameter parameter;
    private final List<String> tasks = new ArrayList<String>();
    private final List<Task> planned = new ArrayList<Task>();
    private OutputListenerImpl outputListener = new OutputListenerImpl();
    private OutputListenerImpl errorListener = new OutputListenerImpl();

    public InProcessGradleExecuter(StartParameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public GradleExecuter inDirectory(File directory) {
        parameter.setCurrentDir(directory);
        return this;
    }

    @Override
    public InProcessGradleExecuter withSearchUpwards() {
        parameter.setSearchUpwards(true);
        return this;
    }

    @Override
    public InProcessGradleExecuter withTasks(String... names) {
        parameter.setTaskNames(Arrays.asList(names));
        return this;
    }

    @Override
    public GradleExecuter withTasks(List<String> names) {
        parameter.setTaskNames(names);
        return this;
    }

    @Override
    public InProcessGradleExecuter withTaskList() {
        parameter.setBuildExecuter(new BuiltInTasksBuildExecuter(BuiltInTasksBuildExecuter.Options.TASKS));
        return this;
    }

    @Override
    public InProcessGradleExecuter withDependencyList() {
        parameter.setBuildExecuter(new BuiltInTasksBuildExecuter(BuiltInTasksBuildExecuter.Options.DEPENDENCIES));
        return this;
    }

    @Override
    public InProcessGradleExecuter usingSettingsFile(File settingsFile) {
        parameter.setSettingsFile(settingsFile);
        return this;
    }

    @Override
    public GradleExecuter usingInitScript(File initScript) {
        parameter.addInitScript(initScript);
        return this;
    }

    @Override
    public GradleExecuter usingBuildScript(String script) {
        parameter.useEmbeddedBuildFile(script);
        return this;
    }

    @Override
    public GradleExecuter withQuietLogging() {
        parameter.setLogLevel(LogLevel.QUIET);
        return this;
    }

    @Override
    public GradleExecuter withArguments(String... args) {
        new DefaultCommandLine2StartParameterConverter().convert(args, parameter);
        return this;
    }

    public ExecutionResult run() {
        GradleLauncher gradleLauncher = GradleLauncher.newInstance(parameter);
        gradleLauncher.addListener(new BuildListenerImpl());
        gradleLauncher.addStandardOutputListener(outputListener);
        gradleLauncher.addStandardErrorListener(errorListener);
        BuildResult result = gradleLauncher.run();
        result.rethrowFailure();
        return new InProcessExecutionResult(tasks, outputListener.toString(), errorListener.toString());
    }

    public ExecutionFailure runWithFailure() {
        try {
            run();
            throw new AssertionFailedError("expected build to fail.");
        } catch (GradleException e) {
            return new InProcessExecutionFailure(tasks, outputListener.writer.toString(),
                    errorListener.writer.toString(), e);
        }
    }

    private class BuildListenerImpl extends BuildAdapter {
        private TaskListenerImpl listener = new TaskListenerImpl();

        public void taskGraphPopulated(TaskExecutionGraph graph) {
            planned.clear();
            planned.addAll(graph.getAllTasks());
            graph.addTaskExecutionListener(listener);
        }
    }

    private class OutputListenerImpl implements StandardOutputListener {
        private StringWriter writer = new StringWriter();

        @Override public String toString() {
            return writer.toString();
        }

        public void onOutput(CharSequence output) {
            writer.append(output);
        }
    }

    private class TaskListenerImpl implements TaskExecutionListener {
        private Task current;

        public void beforeExecute(Task task) {
            assertThat(current, nullValue());
            assertTrue(planned.contains(task));
            current = task;
        }

        public void afterExecute(Task task, Throwable failure) {
            assertThat(task, sameInstance(current));
            current = null;
            tasks.add(task.getPath());
        }
    }

    public static class InProcessExecutionResult implements ExecutionResult {
        private final List<String> plannedTasks;
        private final String output;
        private final String error;

        public InProcessExecutionResult(List<String> plannedTasks, String output, String error) {
            this.plannedTasks = plannedTasks;
            this.output = output;
            this.error = error;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        public void assertTasksExecuted(String... taskPaths) {
            List<String> expected = Arrays.asList(taskPaths);
            assertThat(plannedTasks, equalTo(expected));
        }
    }

    private static class InProcessExecutionFailure extends InProcessExecutionResult implements ExecutionFailure {
        private final GradleException failure;

        public InProcessExecutionFailure(List<String> tasks, String output, String error, GradleException failure) {
            super(tasks, output, error);
            if (failure instanceof GradleScriptException) {
                this.failure = ((GradleScriptException) failure).getReportableException();
            } else {
                this.failure = failure;
            }
        }

        public void assertHasLineNumber(int lineNumber) {
            assertThat(failure.getMessage(), containsString(String.format(" line: %d", lineNumber)));
        }

        public void assertHasFileName(String filename) {
            assertThat(failure.getMessage(), startsWith(String.format("%s", filename)));
        }

        public void assertHasCause(String description) {
            assertThatCause(equalTo(description));
        }

        public void assertThatCause(final Matcher<String> matcher) {
            if (failure instanceof GradleScriptException) {
                GradleScriptException exception = (GradleScriptException) failure;
                assertThat(exception.getReportableCauses(), hasItem(hasMessage(matcher)));
            } else {
                assertThat(failure.getCause().getMessage(), matcher);
            }
        }

        public void assertHasDescription(String context) {
            assertThatDescription(startsWith(context));
        }

        public void assertThatDescription(Matcher<String> matcher) {
            assertThat(failure.getMessage(), containsLine(matcher));
        }
    }
}
