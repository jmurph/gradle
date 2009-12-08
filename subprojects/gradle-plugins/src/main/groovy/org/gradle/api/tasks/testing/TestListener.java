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
package org.gradle.api.tasks.testing;

import java.io.Serializable;

// todo: consider multithreading/multiprocess issues
// Teamcity has the concept of a "wave" of messages
// where each thread/process uses a unique wave id
public interface TestListener {
    public interface Suite extends Serializable {
        public String getName();
    }

    public interface Test extends Serializable {
        public String getName();
    }

    public enum ResultType { SUCCESS, FAILURE, ERROR, SKIPPED }
    
    public interface Result extends Serializable {
        public ResultType getResultType();
        public Throwable getError(); // throws exception if type != ERROR
        public Throwable getFailure(); // throws exception if type != FAILURE
    }

    void suiteStarting(Suite suite);
    void suiteFinished(Suite suite);
    void testStarting(Test test);
    void testFinished(Test test, Result result);
}

