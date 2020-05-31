/*-
 * *
 * *
 * Copyright (C) 2020 the original author or authors.
 * *
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
 * *
 */

package org.vividus.studio.plugin.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StepTypeTests
{
    @CsvSource({
        "Given step, GIVEN",
        "When step, WHEN",
        "Then step, THEN"
    })
    @ParameterizedTest
    void testDetect(String stepNaming, StepType expectedType)
    {
        assertEquals(expectedType, StepType.detect(stepNaming));
    }

    @Test
    void testDetectUnsupportedType()
    {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> StepType.detect("New step"));
        assertEquals("Unable to detect type for step 'New step'", exception.getMessage());
    }
}
