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

package org.vividus.studio.plugin.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Singleton;

import org.vividus.studio.plugin.model.Parameter;
import org.vividus.studio.plugin.model.StepDefinition;

@Singleton
public class StepDefinitionFactory
{
    private static final Pattern PARAMETER_NAME_PATTERN = Pattern.compile("\\$\\w+");

    public StepDefinition createStepDefinition(String module, String stepAsString, String documentation)
    {
        return createStepDefinition(module, stepAsString, documentation, false, false);
    }

    public StepDefinition createStepDefinition(String module, String stepAsString, String documentation,
            boolean composite, boolean dynamic)
    {
        List<Parameter> parameters = new ArrayList<>();
        Matcher parameterMatcher = PARAMETER_NAME_PATTERN.matcher(stepAsString);

        List<Integer> tokenIndices = new ArrayList<>();
        tokenIndices.add(0);

        int matchIndex = 1;
        while (parameterMatcher.find())
        {
            int start = parameterMatcher.start();
            String group = parameterMatcher.group();

            Parameter parameter = new Parameter(matchIndex++, group, start);
            parameters.add(parameter);

            tokenIndices.add(start);
            tokenIndices.add(start + group.length());
        }

        tokenIndices.add(stepAsString.length());

        List<String> matchTokens = new ArrayList<>(tokenIndices.size() / 2);
        for (int index = 0; index < tokenIndices.size(); index += 2)
        {
            String token = stepAsString.substring(tokenIndices.get(index), tokenIndices.get(index + 1));
            matchTokens.add(token);
        }

        StepDefinition definition = new StepDefinition(module, stepAsString, documentation, parameters, matchTokens);
        definition.setComposite(composite);
        definition.setDynamic(dynamic);
        return definition;
    }
}
