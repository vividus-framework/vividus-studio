/*-
 * *
 * *
 * Copyright (C) 2020 - 2022 the original author or authors.
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

package org.vividus.studio.plugin.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang3.mutable.MutableInt;
import org.vividus.studio.plugin.model.Step;
import org.vividus.studio.plugin.util.Splitter;

@Singleton
public class SemanticTokensService
{
    private static final int NEW_LINE = 1;

    private final StepDefinitionResolver stepDefinitionResolver;

    @Inject
    public SemanticTokensService(StepDefinitionResolver stepDefinitionResolver)
    {
        this.stepDefinitionResolver = stepDefinitionResolver;
    }

    public List<Integer> getSemanticTokens(String documentIdentifier)
    {
        SemanticTokens semanticTokens = new SemanticTokens();

        MutableInt lineShift = new MutableInt(0);

        stepDefinitionResolver.resolve(documentIdentifier).filter(def -> !def.getArgIndices().isEmpty()).forEach(def ->
        {
            Step step = def.getStep();

            int shiftedLine = step.getLineIndex() - lineShift.intValue();
            lineShift.setValue(step.getLineIndex());

            List<Integer> argIndices = def.getArgIndices();
            semanticTokens.shiftLine(shiftedLine);

            Integer previousFrom = 0;
            for (int pos = 0; pos < argIndices.size() - 1; pos += 2)
            {
                if (pos != 0)
                {
                    semanticTokens.shiftLine(0);
                }

                Integer from = argIndices.get(pos);
                int to = argIndices.get(pos + 1);
                Integer length = to - from;

                String argument = step.getValue().substring(from, to);
                List<Integer> argumentLengths = Splitter.split(argument)
                        .stream()
                        .map(String::length)
                        .collect(Collectors.toList());

                if (argumentLengths.size() == 1)
                {
                    semanticTokens.addToken(from - previousFrom, length);
                    previousFrom = from;
                }
                else
                {
                    for (int index = 0; index < argumentLengths.size(); index++)
                    {
                        int argumentLength = argumentLengths.get(index);
                        if (index == 0)
                        {
                            semanticTokens.addToken(from - previousFrom, argumentLength);
                        }
                        else
                        {
                            semanticTokens.shiftLine(1);
                            semanticTokens.addToken(0, argumentLength);
                        }
                    }

                    int limit = argumentLengths.size() - 1;
                    int shift = argumentLengths.stream()
                                               .limit(limit)
                                               .mapToInt(Integer::intValue)
                                               .map(size -> size + NEW_LINE)
                                               .sum();

                    lineShift.add(limit);
                    previousFrom = from + shift;
                }
            }
        });

        return semanticTokens.getSemanticTokens();
    }

    private static final class SemanticTokens
    {
        private final List<Integer> semanticTokens;

        private SemanticTokens()
        {
            semanticTokens = new ArrayList<>();
        }

        private void shiftLine(Integer nextLine)
        {
            semanticTokens.add(nextLine);
        }

        private void addToken(Integer from, Integer length)
        {
            semanticTokens.add(from);
            semanticTokens.add(length);
            semanticTokens.add(0);
            semanticTokens.add(0);
        }

        private List<Integer> getSemanticTokens()
        {
            return semanticTokens;
        }
    }
}
