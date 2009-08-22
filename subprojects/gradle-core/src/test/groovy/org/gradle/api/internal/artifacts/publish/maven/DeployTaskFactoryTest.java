/*
 * Copyright 2007-2008 the original author or authors.
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
package org.gradle.api.internal.artifacts.publish.maven;

import static junit.framework.Assert.assertTrue;
import org.gradle.api.internal.artifacts.publish.maven.deploy.CustomDeployTask;
import org.gradle.api.internal.artifacts.publish.maven.deploy.DefaultDeployTaskFactory;
import org.junit.Test;

/**
 * @author Hans Dockter
 */
public class DeployTaskFactoryTest {
    @Test
    public void create() {
        assertTrue(new DefaultDeployTaskFactory().createDeployTask() instanceof CustomDeployTask);
    }
}