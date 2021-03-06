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
package org.gradle.integtests

import org.junit.runner.RunWith
import org.junit.Test

@RunWith(DistributionIntegrationTestRunner.class)
class SamplesCustomBuildLanguageIntegrationTest {
    // Injected by test runner
    private GradleDistribution dist
    private GradleExecuter executer

    @Test
    public void testBuildProductDistributions() {
        TestFile rootDir = dist.samplesDir.file('customBuildLanguage')
        executer.inDirectory(rootDir).withTasks('clean', 'dist').run()

        TestFile expandDir = dist.testFile('expand-basic')
        rootDir.file('basicEdition/build/distributions/some-company-basic-edition-1.0.zip').unzipTo(expandDir)
        expandDir.assertHasDescendants(
                'readme.txt',
                'end-user-license-agreement.txt',
                'bin/start.sh',
                'lib/some-company-identity-management-1.0.jar',
                'lib/some-company-billing-1.0.jar',
                'lib/commons-lang-2.4.jar'
        )

        expandDir = dist.testFile('expand-enterprise')
        rootDir.file('enterpriseEdition/build/distributions/some-company-enterprise-edition-1.0.zip').unzipTo(expandDir)
        expandDir.assertHasDescendants(
                'readme.txt',
                'end-user-license-agreement.txt',
                'bin/start.sh',
                'lib/some-company-identity-management-1.0.jar',
                'lib/some-company-billing-1.0.jar',
                'lib/some-company-reporting-1.0.jar',
                'lib/commons-lang-2.4.jar',
                'lib/commons-io-1.2.jar'
        )
    }
}
