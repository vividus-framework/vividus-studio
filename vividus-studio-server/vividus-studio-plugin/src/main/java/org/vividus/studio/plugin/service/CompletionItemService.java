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

package org.vividus.studio.plugin.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Suppliers;
import com.google.gson.JsonObject;
import com.google.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.vividus.studio.plugin.model.Parameter;
import org.vividus.studio.plugin.model.StepDefinition;

@Singleton
public class CompletionItemService implements ICompletionItemService
{
    private static final String HASH = "hash";
    private static final String TRIGGER = "trigger";
    private static final String SNIPPER_FORMAT = "${%d:%s}";

    private final List<StepDefinition> stepDefinitions = new ArrayList<>();
    private final Supplier<Map<String, Map<Integer, CompletionItem>>> completionItem = Suppliers.memoize(
        () -> stepDefinitions.stream()
                             .collect(Collectors.groupingBy(s -> s.getStepType().getId(),
                                    Collectors.toMap(StepDefinition::hashCode, this::asCompletionItem))));

    private CompletionItem asCompletionItem(StepDefinition stepDefinition)
    {
        CompletionItem item = new CompletionItem(stepDefinition.getStepAsString());
        item.setKind(CompletionItemKind.Method);

        setSnippet(item, stepDefinition);
        setInfo(item, stepDefinition);
        setData(item, stepDefinition);

        return item;
    }

    private void setSnippet(CompletionItem item, StepDefinition stepDefinition)
    {
        item.setInsertTextFormat(InsertTextFormat.Snippet);

        List<Parameter> parameters = stepDefinition.getParameters();
        int size = parameters.size();
        String [] search = new String[size];
        String [] replacement = new String[size];

        IntStream.range(0, size).forEach(i ->
        {
            Parameter parameter = parameters.get(i);
            String name = parameter.getName();
            String nameNoAnchor = name.substring(1);
            search[i] = name;
            replacement[i] = String.format(SNIPPER_FORMAT, parameter.getIndex(), nameNoAnchor);
        });

        String snipperText = StringUtils.replaceEach(stepDefinition.getStepAsString(), search, replacement);
        item.setInsertText(snipperText);
    }

    private void setInfo(CompletionItem item, StepDefinition stepDefinition)
    {
        item.setDetail(stepDefinition.getModule());
        item.setDocumentation(stepDefinition.getDocumentation());
        item.setDeprecated(stepDefinition.isDeprecated());
    }

    private void setData(CompletionItem item, StepDefinition stepDefinition)
    {
        JsonObject state = new JsonObject();
        state.addProperty(TRIGGER, stepDefinition.getStepType().getId());
        state.addProperty(HASH, stepDefinition.hashCode());
        item.setData(state);
    }

    @Override
    public CompletionItem findOne(CompletionItem unresolved)
    {
        JsonObject data = (JsonObject) unresolved.getData();
        String key = data.get(TRIGGER).getAsString();
        int hash = data.get(HASH).getAsInt();
        return completionItem.get().get(key).get(hash);
    }

    @Override
    public List<CompletionItem> findAll(String trigger)
    {
        return completionItem.get().get(trigger).values().stream().collect(Collectors.toList());
    }

    @Override
    public void setStepDefinitions(Collection<StepDefinition> stepDefinitions)
    {
        this.stepDefinitions.addAll(stepDefinitions);
    }
}
