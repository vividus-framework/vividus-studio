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

import static java.lang.String.format;
import static java.lang.String.join;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionItemTag;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.vividus.studio.plugin.model.Parameter;
import org.vividus.studio.plugin.model.ResolvedStepDefinition;
import org.vividus.studio.plugin.model.StepDefinition;

@Singleton
public class CompletionItemService implements ICompletionItemService
{
    private static final String HASH = "hash";
    private static final String TRIGGER = "trigger";
    private static final String PLACEHOLDER_FORMAT = "${%d:%s}";
    private static final String CHOICE_FORMAT = "${%d|%s|}";

    private final StepDefinitionResolver stepDefinitionResolver;

    @Inject
    public CompletionItemService(StepDefinitionResolver stepDefinitionResolver)
    {
        this.stepDefinitionResolver = stepDefinitionResolver;
    }

    private static CompletionItem asCompletionItem(StepDefinition stepDefinition)
    {
        CompletionItem item = new CompletionItem(stepDefinition.getStepAsString());
        item.setKind(CompletionItemKind.Method);

        setSnippet(item, stepDefinition);
        setInfo(item, stepDefinition);
        setData(item, stepDefinition);

        return item;
    }

    private static void setSnippet(CompletionItem item, StepDefinition stepDefinition)
    {
        item.setInsertTextFormat(InsertTextFormat.Snippet);

        List<Parameter> parameters = stepDefinition.getParameters();
        int size = parameters.size();
        String [] search = new String[size];
        String [] replacement = new String[size];

        IntStream.range(0, size).forEach(i ->
        {
            Parameter parameter = parameters.get(i);
            search[i] = parameter.getName();
            replacement[i] = createSnippet(parameter);
        });

        String snipperText = StringUtils.replaceEach(stepDefinition.getStepAsString(), search, replacement);
        item.setInsertText(snipperText);
    }

    private static String createSnippet(Parameter parameter)
    {
        List<String> values = parameter.getValues();
        return values.isEmpty()
                ? format(PLACEHOLDER_FORMAT, parameter.getIndex(), parameter.getName().substring(1))
                : format(CHOICE_FORMAT, parameter.getIndex(), join(",", values));
    }

    private static void setInfo(CompletionItem item, StepDefinition stepDefinition)
    {
        item.setDetail(stepDefinition.getModule());
        item.setDocumentation(stepDefinition.getDocumentation());
        if (stepDefinition.isDeprecated())
        {
            item.setTags(List.of(CompletionItemTag.Deprecated));
        }
    }

    private static void setData(CompletionItem item, StepDefinition stepDefinition)
    {
        JsonObject state = new JsonObject();
        state.addProperty(TRIGGER, stepDefinition.getStepType().getId());
        state.addProperty(HASH, stepDefinition.hashCode());
        item.setData(state);
    }

    @Override
    public List<CompletionItem> findAllAtPosition(String documentIdentifier, Position position)
    {
        return stepDefinitionResolver.resolveAtPosition(documentIdentifier, position)
                .map(match(position))
                .collect(Collectors.toList());
    }

    private Function<ResolvedStepDefinition, CompletionItem> match(Position position)
    {
        return def ->
        {
            CompletionItem item = asCompletionItem(def);

            int matchTokenIndex = def.getTokenIndex();
            String subToken = def.getSubToken();
            int charPosition = position.getCharacter();

            String insertText = "";
            if (!subToken.isEmpty())
            {
                if (matchTokenIndex == 0)
                {
                    insertText = item.getInsertText().substring(charPosition);
                }
                else
                {
                    Parameter parameter = def.getParameters()
                            .get(matchTokenIndex - 1);
                    insertText = StringUtils.substringAfter(item.getInsertText(),
                            createSnippet(parameter) + subToken);
                }
            }

            Position snippetPosition = new Position(position.getLine(), charPosition);
            TextEdit textEdit = new TextEdit(new Range(snippetPosition,
                    snippetPosition), insertText);
            item.setTextEdit(Either.forLeft(textEdit));

            return item;
        };
    }
}
