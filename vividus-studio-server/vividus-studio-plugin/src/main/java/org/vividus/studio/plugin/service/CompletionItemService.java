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

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Suppliers;
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
import org.vividus.studio.plugin.document.TextDocumentProvider;
import org.vividus.studio.plugin.match.TokenMatcher;
import org.vividus.studio.plugin.match.TokenMatcher.MatchOutcome;
import org.vividus.studio.plugin.model.Parameter;
import org.vividus.studio.plugin.model.StepDefinition;
import org.vividus.studio.plugin.model.StepType;

@Singleton
public class CompletionItemService implements ICompletionItemService
{
    private static final List<String> STEP_BREAKERS = List.of(
        "!--",
        "Scenario:",
        "Meta:",
        "GivenStories:",
        "Examples:"
    );
    private static final String HASH = "hash";
    private static final String TRIGGER = "trigger";
    private static final String SNIPPER_FORMAT = "${%d:%s}";

    private final List<StepDefinition> stepDefinitions = new ArrayList<>();
    private final Supplier<Map<StepType, List<StepDefinition>>> groupedStepDefinitions = Suppliers.memoize(
        () -> stepDefinitions.stream()
                             .collect(Collectors.groupingBy(StepDefinition::getStepType, Collectors.toList())));

    private final TextDocumentProvider textDocumentProvider;

    @Inject
    public CompletionItemService(TextDocumentProvider textDocumentProvider)
    {
        this.textDocumentProvider = textDocumentProvider;
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
        String name = parameter.getName();
        String nameNoAnchor = name.substring(1);
        return String.format(SNIPPER_FORMAT, parameter.getIndex(), nameNoAnchor);
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
    public void setStepDefinitions(Collection<StepDefinition> stepDefinitions)
    {
        this.stepDefinitions.addAll(stepDefinitions);
    }

    @SuppressWarnings("MagicNumber")
    @Override
    public List<CompletionItem> findAllAtPosition(String documentIdentifier, Position position)
    {
        Optional<Entry<StepType, String>> stepWrapper = findStep(
                textDocumentProvider.getTextDocument(documentIdentifier), position);
        if (stepWrapper.isEmpty())
        {
            return List.of();
        }
        Entry<StepType, String> step = stepWrapper.get();

        return groupedStepDefinitions.get().get(step.getKey())
                                           .stream()
                                           .map(def -> entry(def, TokenMatcher.match(step.getValue(),
                                                   def.getMatchTokens())))
                                           .filter(e -> e.getValue().isMatch())
                                           .map(match(position))
                                           .collect(Collectors.toList());
    }

    private static Function<Entry<StepDefinition, MatchOutcome>, CompletionItem> match(Position position)
    {
        return e ->
        {
            StepDefinition def = e.getKey();
            CompletionItem item = asCompletionItem(def);

            MatchOutcome outcome = e.getValue();
            int matchTokenIndex = outcome.getTokenIndex();
            String subToken = outcome.getSubToken();
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

    private static Optional<Entry<StepType, String>> findStep(List<String> document, Position position)
    {
        int lineIndex = position.getLine();
        String line = document.get(lineIndex);
        if (isNotStep(line))
        {
            return Optional.empty();
        }

        String token = line.substring(0, position.getCharacter());
        Optional<StepType> type = StepType.detectSafely(token);
        if (type.isPresent())
        {
            return Optional.of(entry(type.get(), token));
        }

        return findStepHeadIndex(document).map(e ->
        {
            String multilineToken = document.subList(e.getValue(), lineIndex).stream()
                    .collect(Collectors.joining(System.lineSeparator(), "", token));
            return entry(e.getKey(), multilineToken);
        });
    }

    private static boolean isNotStep(String line)
    {
        return STEP_BREAKERS.stream().anyMatch(line::startsWith);
    }

    private static Optional<Entry<StepType, Integer>> findStepHeadIndex(List<String> lines)
    {
        for (int index = lines.size() - 1; index >= 0; index--)
        {
            String line = lines.get(index);
            Optional<StepType> type = StepType.detectSafely(line);
            if (type.isPresent())
            {
                return Optional.of(entry(type.get(), index));
            }

            if (isNotStep(line))
            {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
