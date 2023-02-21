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
import java.util.Objects;

public class StepDefinition
{
    private final String module;
    private final String stepAsString;
    private final String documentation;
    private final List<Parameter> parameters;
    private final List<String> matchTokens;
    private final StepType stepType;

    private boolean deprecated;
    private boolean composite;
    private boolean dynamic;

    public StepDefinition(String module, String stepAsString, String documentation, List<Parameter> parameters,
            List<String> matchTokens)
    {
        this.module = module;
        this.stepAsString = stepAsString;
        this.documentation = documentation;
        this.parameters = parameters;
        this.matchTokens = matchTokens;
        this.stepType = StepType.detect(stepAsString);
    }

    public String getModule()
    {
        return module;
    }

    public String getStepAsString()
    {
        return stepAsString;
    }

    public String getDocumentation()
    {
        return documentation;
    }

    public List<Parameter> getParameters()
    {
        return parameters;
    }

    public List<String> getMatchTokens()
    {
        return matchTokens;
    }

    public StepType getStepType()
    {
        return stepType;
    }

    public boolean isDeprecated()
    {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated)
    {
        this.deprecated = deprecated;
    }

    public boolean isComposite()
    {
        return composite;
    }

    public void setComposite(boolean composite)
    {
        this.composite = composite;
    }

    public boolean isDynamic()
    {
        return dynamic;
    }

    public void setDynamic(boolean dynamic)
    {
        this.dynamic = dynamic;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(documentation, module, stepAsString, composite, dynamic);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof StepDefinition))
        {
            return false;
        }
        StepDefinition other = (StepDefinition) obj;
        return Objects.equals(documentation, other.documentation)
                && Objects.equals(module, other.module)
                && Objects.equals(stepAsString, other.stepAsString)
                && composite == other.composite
                && dynamic == other.dynamic;
    }
}
