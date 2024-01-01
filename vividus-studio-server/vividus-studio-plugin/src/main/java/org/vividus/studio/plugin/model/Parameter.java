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

import java.util.List;

public class Parameter
{
    private final int index;
    private final String name;
    private final int startAt;
    private final List<String> values;

    public Parameter(int index, String name, int startAt, List<String> values)
    {
        this.index = index;
        this.name = name;
        this.startAt = startAt;
        this.values = values;
    }

    public int getIndex()
    {
        return index;
    }

    public String getName()
    {
        return name;
    }

    public int getStartAt()
    {
        return startAt;
    }

    public List<String> getValues()
    {
        return values;
    }
}
