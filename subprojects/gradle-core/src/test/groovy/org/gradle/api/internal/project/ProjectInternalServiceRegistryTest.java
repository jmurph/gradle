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
package org.gradle.api.internal.project;

import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandlerFactory;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.artifacts.ConfigurationContainerFactory;
import org.gradle.api.internal.artifacts.configurations.DependencyMetaDataProvider;
import org.gradle.api.internal.artifacts.dsl.DefaultArtifactHandler;
import org.gradle.api.internal.artifacts.dsl.PublishArtifactFactory;
import org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactory;
import org.gradle.api.internal.initialization.DefaultScriptHandler;
import org.gradle.api.internal.initialization.ScriptClassLoaderProvider;
import org.gradle.api.internal.plugins.DefaultConvention;
import org.gradle.api.internal.tasks.DefaultTaskContainer;
import org.gradle.api.plugins.Convention;
import org.gradle.api.tasks.TaskContainer;
import static org.hamcrest.Matchers.*;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class ProjectInternalServiceRegistryTest {
    private final JUnit4Mockery context = new JUnit4Mockery();
    private final ProjectInternal project = context.mock(ProjectInternal.class);
    private final ConfigurationContainer configurationContainer = context.mock(ConfigurationContainer.class);
    private final GradleInternal gradle = context.mock(GradleInternal.class);
    private final ConfigurationContainerFactory configurationContainerFactory = context.mock(
            ConfigurationContainerFactory.class);
    private final RepositoryHandlerFactory repositoryHandlerFactory = context.mock(RepositoryHandlerFactory.class);
    private final ITaskFactory taskFactory = context.mock(ITaskFactory.class);
    private final PublishArtifactFactory publishArtifactFactory = context.mock(PublishArtifactFactory.class);
    private final DependencyFactory dependencyFactory = context.mock(DependencyFactory.class);
    private final ServiceRegistry parent = context.mock(ServiceRegistry.class);
    private final ProjectInternalServiceRegistry registry = new ProjectInternalServiceRegistry(parent, project);

    @Before
    public void setUp() {
        context.checking(new Expectations() {{
            allowing(project).getGradle();
            will(returnValue(gradle));
            allowing(parent).get(ITaskFactory.class);
            will(returnValue(taskFactory));
            allowing(parent).get(RepositoryHandlerFactory.class);
            will(returnValue(repositoryHandlerFactory));
            allowing(parent).get(ConfigurationContainerFactory.class);
            will(returnValue(configurationContainerFactory));
            allowing(parent).get(PublishArtifactFactory.class);
            will(returnValue(publishArtifactFactory));
            allowing(parent).get(DependencyFactory.class);
            will(returnValue(dependencyFactory));
        }});
    }

    @Test
    public void createsARegistryForATask() {
        ServiceRegistryFactory taskRegistry = registry.createFor(context.mock(TaskInternal.class));
        assertThat(taskRegistry, instanceOf(TaskInternalServiceRegistry.class));
    }

    @Test
    public void projectProvidesAConvention() {
        assertThat(registry.get(Convention.class), instanceOf(DefaultConvention.class));
        assertThat(registry.get(Convention.class), sameInstance(registry.get(Convention.class)));
    }

    @Test
    public void projectProvidesATaskContainer() {
        assertThat(registry.get(TaskContainer.class), instanceOf(DefaultTaskContainer.class));
        assertThat(registry.get(TaskContainer.class), sameInstance(registry.get(TaskContainer.class)));
    }

    @Test
    public void projectProvidesARepositoryHandler() {
        final RepositoryHandler repositoryHandler = context.mock(RepositoryHandler.class);

        context.checking(new Expectations() {{
            one(repositoryHandlerFactory).createRepositoryHandler(with(any(Convention.class)));
            will(returnValue(repositoryHandler));
        }});

        assertThat(registry.get(RepositoryHandler.class), sameInstance(repositoryHandler));
        assertThat(registry.get(RepositoryHandler.class), sameInstance(registry.get(RepositoryHandler.class)));
    }

    @Test
    public void projectProvidesAnArtifactHandler() {
        expectConfigurationHandlerCreated();

        assertThat(registry.get(ArtifactHandler.class), instanceOf(DefaultArtifactHandler.class));
        assertThat(registry.get(ArtifactHandler.class), sameInstance(registry.get(ArtifactHandler.class)));
    }

    @Test
    public void projectProvidesADependencyHandler() {
        expectConfigurationHandlerCreated();

        assertThat(registry.get(DependencyHandler.class), instanceOf(DefaultDependencyHandler.class));
        assertThat(registry.get(DependencyHandler.class), sameInstance(registry.get(DependencyHandler.class)));
    }

    @Test
    public void projectProvidesAnAntBuilderFactory() {
        assertThat(registry.get(AntBuilderFactory.class), instanceOf(DefaultAntBuilderFactory.class));
        assertThat(registry.get(AntBuilderFactory.class), sameInstance(registry.get(AntBuilderFactory.class)));
    }

    @Test
    public void projectProvidesAScriptHandlerAndScriptClassLoaderProvider() {
        expectConfigurationHandlerCreated();
        context.checking(new Expectations() {{
            allowing(project).getParent();
            will(returnValue(null));

            allowing(gradle).getBuildScriptClassLoader();
            will(returnValue(null));

            ignoring(configurationContainer);
        }});

        assertThat(registry.get(ScriptHandler.class), instanceOf(DefaultScriptHandler.class));
        assertThat(registry.get(ScriptHandler.class), sameInstance(registry.get(ScriptHandler.class)));
        assertThat(registry.get(ScriptClassLoaderProvider.class), sameInstance((Object) registry.get(
                ScriptHandler.class)));
    }

    private void expectConfigurationHandlerCreated() {
        context.checking(new Expectations() {{
            RepositoryHandler repositoryHandler = context.mock(RepositoryHandler.class);

            one(repositoryHandlerFactory).createRepositoryHandler(with(notNullValue(Convention.class)));
            will(returnValue(repositoryHandler));

            one(configurationContainerFactory).createConfigurationContainer(with(sameInstance(repositoryHandler)), with(
                    notNullValue(DependencyMetaDataProvider.class)));
            will(returnValue(configurationContainer));
        }});
    }
}
