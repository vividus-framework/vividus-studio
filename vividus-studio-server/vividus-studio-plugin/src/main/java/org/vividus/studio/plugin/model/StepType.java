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

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public enum StepType
{
    GIVEN("Given"),
    WHEN("When"),
    THEN("Then");

    private final String type;
    private final String id;

    StepType(String type)
    {
        this.type = type;
        this.id = String.valueOf(type.charAt(0));
    }

    public String getType()
    {
        return this.type;
    }

    public String getId()
    {
        return this.id;
    }

    public static StepType detect(String stepAsString)
    {
        return detectSafely(stepAsString).orElseThrow(
            () -> new IllegalArgumentException(String.format("Unable to detect type for step '%s'", stepAsString)));
    }

    public static Optional<StepType> detectSafely(String stepAsString)
    {
        String prefix = StringUtils.substringBefore(stepAsString, " ");
        return Stream.of(StepType.values())
                     .filter(st -> st.getType().equals(prefix))
                     .findFirst();
    }
}
