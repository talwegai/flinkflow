/*
 * Copyright 2026 Talweg Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.talweg.flinkflow.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AgentToolBridgeTest {

    @Test
    public void testDynamicToolExecution() {
        AgentToolBridge.DynamicTool tool = new AgentToolBridge.DynamicTool(
            "test-tool", 
            "A test tool description", 
            "flowlet"
        );
        
        String result = tool.execute("hello world");
        
        assertNotNull(result);
        assertTrue(result.contains("test-tool"));
        assertTrue(result.contains("hello world"));
        assertTrue(result.contains("Mock data response"));
    }
}
