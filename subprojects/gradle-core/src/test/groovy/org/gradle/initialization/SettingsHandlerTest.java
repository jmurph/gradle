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
package org.gradle.initialization;

import org.gradle.StartParameter;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.api.internal.project.IProjectRegistry;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Hans Dockter
 */
public class SettingsHandlerTest {
    private JUnit4Mockery context = new JUnit4Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    private GradleInternal gradle = context.mock(GradleInternal.class);
    private IGradlePropertiesLoader gradlePropertiesLoader = context.mock(IGradlePropertiesLoader.class);
    private SettingsInternal settings = context.mock(SettingsInternal.class);
    private SettingsLocation settingsLocation = new SettingsLocation(new File("someDir"), null);
    private StartParameter startParameter = new StartParameter();
    private URLClassLoader urlClassLoader = new URLClassLoader(new URL[0]);
    private ISettingsFinder settingsFinder = context.mock(ISettingsFinder.class);
    private SettingsProcessor settingsProcessor = context.mock(SettingsProcessor.class);
    private BuildSourceBuilder buildSourceBuilder = context.mock(BuildSourceBuilder.class);
    private SettingsHandler settingsHandler = new SettingsHandler(settingsFinder, settingsProcessor, buildSourceBuilder);

    @org.junit.Test
    public void findAndLoadSettingsWithExistingSettings() {
        prepareForExistingSettings();
        context.checking(new Expectations() {{
            allowing(buildSourceBuilder).buildAndCreateClassLoader(with(aBuildSrcStartParameter(
                    new File(settingsLocation.getSettingsDir(), BaseSettings.DEFAULT_BUILD_SRC_DIR))));
            will(returnValue(urlClassLoader));    
        }});
        assertThat(settingsHandler.findAndLoadSettings(gradle, gradlePropertiesLoader), sameInstance(settings));
    }

    private void prepareForExistingSettings() {
        final ProjectSpec projectSpec = context.mock(ProjectSpec.class);
        final IProjectRegistry projectRegistry = context.mock(IProjectRegistry.class);
        startParameter.setDefaultProjectSelector(projectSpec);

        context.checking(new Expectations() {{
            allowing(settings).getProjectRegistry();
            will(returnValue(projectRegistry));

            allowing(settings).createClassLoader();
            will(returnValue(urlClassLoader));

            one(gradle).setBuildScriptClassLoader(urlClassLoader);

            allowing(projectSpec).containsProject(projectRegistry);
            will(returnValue(true));

            allowing(gradle).getStartParameter();
            will(returnValue(startParameter));

            allowing(settingsFinder).find(startParameter);
            will(returnValue(settingsLocation));

            allowing(settingsProcessor).process(settingsLocation, urlClassLoader, startParameter, gradlePropertiesLoader);
            will(returnValue(settings));
        }});
    }

    @Factory
    public static Matcher<StartParameter> aBuildSrcStartParameter(File currentDir) {
        return new BuildSrcParameterMatcher(currentDir);
    }

    public static class BuildSrcParameterMatcher extends TypeSafeMatcher<StartParameter> {
        private File currentDir;

        public BuildSrcParameterMatcher(File currentDir) {
            this.currentDir = currentDir;
        }

        public boolean matchesSafely(StartParameter startParameter) {
            return startParameter.getCurrentDir().getAbsoluteFile().equals(currentDir.getAbsoluteFile());
        }

        public void describeTo(Description description) {
            description.appendText("a startparameter with ").appendValue(currentDir);
        }
    }
}
