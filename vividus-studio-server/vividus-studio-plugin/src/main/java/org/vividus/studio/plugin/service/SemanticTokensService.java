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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang3.mutable.MutableInt;

@Singleton
public class SemanticTokensService
{
    private final StepDefinitionResolver stepDefinitionResolver;

    @Inject
    public SemanticTokensService(StepDefinitionResolver stepDefinitionResolver)
    {
        this.stepDefinitionResolver = stepDefinitionResolver;
    }

    public List<Integer> getSemanticTokens(String documentIdentifier)
    {
        List<Integer> semanticTokens = new ArrayList<>();

        MutableInt lineShift = new MutableInt(0);

        stepDefinitionResolver.resolve(documentIdentifier).filter(def -> !def.getArgIndices().isEmpty()).forEach(def ->
        {
            int shiftedLine = def.getLineIndex() - lineShift.intValue();
            lineShift.setValue(def.getLineIndex());

            List<Integer> argIndices = def.getArgIndices();
            semanticTokens.add(shiftedLine);

            Integer previousFrom = 0;
            for (int pos = 0; pos < argIndices.size() - 1; pos += 2)
            {
                if (pos != 0)
                {
                    semanticTokens.add(0);
                }

                Integer from = argIndices.get(pos);
                Integer length = argIndices.get(pos + 1) - from;

                semanticTokens.addAll(List.of(
                    from - previousFrom,
                    length,
                    0,
                    0
                ));

                previousFrom = from;
            }
        });

        return semanticTokens;
    }
}
