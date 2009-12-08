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
package org.gradle.external.testng;

import org.gradle.api.JavaVersion;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.tasks.testing.AbstractTestFrameworkInstanceTest;
import org.gradle.api.tasks.testing.AbstractTestTask;
import org.gradle.api.tasks.testing.AntTest;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.testng.AntTestNGExecute;
import org.gradle.api.tasks.testing.testng.TestNGOptions;
import org.gradle.listener.ListenerBroadcast;
import org.jmock.Expectations;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;

import java.io.File;

/**
 * @author Tom Eyckmans
 */
public class TestNGTestFrameworkInstanceTest extends AbstractTestFrameworkInstanceTest {

    private TestNGTestFramework testNgTestFrameworkMock;
    private TestNGTestFrameworkInstance testNGTestFrameworkInstance;

    private AntTestNGExecute antTestNGExecuteMock;
    private TestNGOptions testngOptionsMock;
    private ListenerBroadcast<TestListener> listenerBroadcastMock;
    private AbstractTestTask testTask;


    @Before
    public void setUp() throws Exception {
        super.setUp();

        testNgTestFrameworkMock = context.mock(TestNGTestFramework.class);
        antTestNGExecuteMock = context.mock(AntTestNGExecute.class);
        testngOptionsMock = context.mock(TestNGOptions.class);
        listenerBroadcastMock = context.mock(ListenerBroadcast.class);
        testTask = context.mock(AntTest.class, "TestNGTestFrameworkInstanceTest");

        testNGTestFrameworkInstance = new TestNGTestFrameworkInstance(testTask, testNgTestFrameworkMock);
    }

    @org.junit.Test
    public void testInitialize() {
        setMocks();

        final JavaVersion sourceCompatibility = JavaVersion.VERSION_1_5;
        context.checking(new Expectations() {{
            one(projectMock).getProjectDir(); will(returnValue(new File("projectDir")));
            one(projectMock).property("sourceCompatibility"); will(returnValue(sourceCompatibility));
            one(testngOptionsMock).setAnnotationsOnSourceCompatibility(sourceCompatibility);
            one(testMock).getTestClassesDir();will(returnValue(testClassesDir));
            one(testMock).getClasspath();will(returnValue(classpathMock));
            one(classpathMock).getAsFileTree();will(returnValue(classpathAsFileTreeMock));
            one(classpathAsFileTreeMock).visit(with(aNonNull(FileVisitor.class)));
        }});

        testNGTestFrameworkInstance.initialize(projectMock, testMock);

        assertNotNull(testNGTestFrameworkInstance.getOptions());
        assertNotNull(testNGTestFrameworkInstance.getAntTestNGExecute());
    }

    @org.junit.Test
    public void testExecuteWithJDKAnnoations() {
        setMocks();

        context.checking(new Expectations() {{
            one(testMock).getTestListenerBroadcaster(); will(returnValue(listenerBroadcastMock));
            one(testMock).getTestSrcDirs();  will(returnValue(testSrcDirs));
            one(testngOptionsMock).setTestResources(testSrcDirs);
            one(testMock).getTestClassesDir(); will(returnValue(testClassesDir));
            one(testMock).getClasspath(); will(returnValue(classpathMock));
            one(testMock).getTestResultsDir(); will(returnValue(testResultsDir));
            one(testMock).getTestReportDir(); will(returnValue(testReportDir));
            one(projectMock).getAnt(); will(returnValue(antBuilderMock));
            one(antTestNGExecuteMock).execute(testClassesDir, classpathMock, testResultsDir, testReportDir, null, null,
                                              testngOptionsMock, antBuilderMock, listenerBroadcastMock);
        }});

        testNGTestFrameworkInstance.execute(projectMock, testMock, null, null);
    }

    private void setMocks() {
        testNGTestFrameworkInstance.setAntTestNGExecute(antTestNGExecuteMock);
        testNGTestFrameworkInstance.setOptions(testngOptionsMock);
    }
}
