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
package org.gradle.external.junit;

import org.apache.commons.lang.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.AbstractTestTask;
import org.gradle.api.tasks.testing.ForkMode;
import org.gradle.api.tasks.testing.JunitForkOptions;
import org.gradle.api.tasks.testing.junit.AntJUnitExecute;
import org.gradle.api.tasks.testing.junit.AntJUnitReport;
import org.gradle.api.tasks.testing.junit.JUnitOptions;
import org.gradle.api.testing.fabric.AbstractTestFrameworkInstance;
import org.gradle.util.exec.ExecHandleBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Tom Eyckmans
 */
public class JUnitTestFrameworkInstance extends AbstractTestFrameworkInstance<JUnitTestFramework> {
    private AntJUnitExecute antJUnitExecute = null;
    private AntJUnitReport antJUnitReport = null;
    private JUnitOptions options = null;
    private JUnitDetector detector = null;

    protected JUnitTestFrameworkInstance(AbstractTestTask testTask, JUnitTestFramework testFramework) {
        super(testTask, testFramework);
    }

    public void initialize(Project project, AbstractTestTask testTask) {
        antJUnitExecute = new AntJUnitExecute();
        antJUnitReport = new AntJUnitReport();
        options = new JUnitOptions(testFramework);

        final JunitForkOptions forkOptions = options.getForkOptions();

        options.setFork(true);
        forkOptions.setForkMode(ForkMode.ONCE);
        forkOptions.setDir(project.getProjectDir());

        detector = new JUnitDetector(testTask.getTestClassesDir(), testTask.getClasspath());
    }

    public void execute(Project project, AbstractTestTask testTask, Collection<String> includes,
                        Collection<String> excludes) {
        antJUnitExecute.execute(testTask.getTestClassesDir(), new ArrayList<File>(testTask.getClasspath().getFiles()),
                testTask.getTestResultsDir(), includes, excludes, options, project.getAnt(), testTask.getTestListenerBroadcaster());
    }

    public void report(Project project, AbstractTestTask testTask) {
        antJUnitReport.execute(testTask.getTestResultsDir(), testTask.getTestReportDir(), project.getAnt());
    }

    public JUnitOptions getOptions() {
        return options;
    }

    void setOptions(JUnitOptions options) {
        this.options = options;
    }

    AntJUnitExecute getAntJUnitExecute() {
        return antJUnitExecute;
    }

    void setAntJUnitExecute(AntJUnitExecute antJUnitExecute) {
        this.antJUnitExecute = antJUnitExecute;
    }

    AntJUnitReport getAntJUnitReport() {
        return antJUnitReport;
    }

    void setAntJUnitReport(AntJUnitReport antJUnitReport) {
        this.antJUnitReport = antJUnitReport;
    }

    public JUnitDetector getDetector() {
        return detector;
    }

    public void applyForkArguments(ExecHandleBuilder forkHandleBuilder) {
        final JunitForkOptions forkOptions = options.getForkOptions();

        if (StringUtils.isNotEmpty(forkOptions.getJvm())) {
            forkHandleBuilder.execCommand(forkOptions.getJvm());
        } else {
            useDefaultJvm(forkHandleBuilder);
        }

        if (forkOptions.getDir() != null) {
            forkHandleBuilder.execDirectory(forkOptions.getDir());
        } else {
            useDefaultDirectory(forkHandleBuilder);
        }
    }

    public void applyForkJvmArguments(ExecHandleBuilder forkHandleBuilder) {
        final JunitForkOptions forkOptions = options.getForkOptions();

        if (StringUtils.isNotEmpty(forkOptions.getMaxMemory())) {
            forkHandleBuilder.arguments("-Xmx=" + forkOptions.getMaxMemory());
        }

        final List<String> jvmArgs = forkOptions.getJvmArgs();
        if (jvmArgs != null && !jvmArgs.isEmpty()) {
            forkHandleBuilder.arguments(jvmArgs);
        }

        if (forkOptions.isNewEnvironment()) {
            final Map<String, String> environment = forkOptions.getEnvironment();
            if (environment != null && !environment.isEmpty()) {
                forkHandleBuilder.environment(environment);
            }
        } else {
            forkHandleBuilder.environment(System.getenv());
        }

        // TODO clone
        // TODO bootstrapClasspath - not sure which bootstrap classpath option to use:
        // TODO one of: -Xbootclasspath or -Xbootclasspath/a or -Xbootclasspath/p
        // TODO -Xbootclasspath/a seems the correct one - to discuss or improve and make it
        // TODO possible to specify which one to use. -> will break ant task compatibility in options.
    }
}
