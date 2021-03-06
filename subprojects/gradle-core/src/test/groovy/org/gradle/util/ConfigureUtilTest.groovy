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
package org.gradle.util

import org.junit.Test
import static org.junit.Assert.*
import static org.hamcrest.Matchers.*

class ConfigureUtilTest {
    @Test
    public void canConfigureObjectUsingClosure() {
        List obj = []
        def cl = {
            add('a');
            assertThat(size(), equalTo(1));
            assertThat(obj, equalTo(['a']))
        }
        ConfigureUtil.configure(cl, obj)
        assertThat(obj, equalTo(['a']))
    }

    @Test
    public void passesConfiguredObjectToClosure() {
        List obj = []
        def cl = {
            assertThat(it, sameInstance(obj))
        }
        def cl2 = { List list ->
            assertThat(list, sameInstance(obj))
        }
        def cl3 = { ->
        }
        ConfigureUtil.configure(cl, obj)
        ConfigureUtil.configure(cl2, obj)
        ConfigureUtil.configure(cl3, obj)
    }
}
