/*-
 * *
 * *
 * Copyright (C) 2020 - 2023 the original author or authors.
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

public class Step
{
    private final int lineIndex;
    private final StepType type;
    private final String value;

    public Step(int lineIndex, StepType type, String value)
    {
        this.lineIndex = lineIndex;
        this.type = type;
        this.value = value.strip();
    }

    public int getLineIndex()
    {
        return lineIndex;
    }

    public StepType getType()
    {
        return type;
    }

    public String getValue()
    {
        return value;
    }
}
