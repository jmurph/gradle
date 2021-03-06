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
package org.gradle.api.testing.execution.control.messages.server;

import static org.junit.Assert.*;
import org.junit.Before;
import org.gradle.api.testing.execution.control.refork.ReforkReasonConfigs;

/**
 * @author Tom Eyckmans
 */
public class InitializeActionMessageTest extends AbstractTestServerControlMessageTest<InitializeActionMessage> {

    private final String testFrameworkId = "testng";
    private ReforkReasonConfigs reforkReasonConfigs;

    @Before
    public void setUp() throws Exception
    {
        reforkReasonConfigs = new ReforkReasonConfigs();
    }

    protected InitializeActionMessage createMessageObject(int pipelineId) {
        final InitializeActionMessage messageObject = new InitializeActionMessage(pipelineId);

        messageObject.setTestFrameworkId(testFrameworkId);
        messageObject.setReforkItemConfigs(reforkReasonConfigs);

        return messageObject;
    }

    protected void assertTestServerControlMessage(InitializeActionMessage originalMessage, InitializeActionMessage deserializedMessage) {
        assertEquals(originalMessage.getTestFrameworkId(), deserializedMessage.getTestFrameworkId());
        assertEquals(originalMessage.getReforkItemConfigs().getKeys().isEmpty(), deserializedMessage.getReforkItemConfigs().getKeys().isEmpty());
    }
}
