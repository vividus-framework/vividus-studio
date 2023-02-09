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

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.lsp4j.Position;
import org.vividus.studio.plugin.document.TextDocumentProvider;
import org.vividus.studio.plugin.match.TokenMatcher;
import org.vividus.studio.plugin.match.TokenMatcher.MatchOutcome;
import org.vividus.studio.plugin.model.ResolvedStepDefinition;
import org.vividus.studio.plugin.model.Step;
import org.vividus.studio.plugin.model.StepDefinition;
import org.vividus.studio.plugin.model.StepType;

@Singleton
public class StepDefinitionResolver implements IStepDefinitionsAware
{
    private static final List<String> STEP_BREAKERS = List.of(
        "!--",
        "Scenario:",
        "Meta:",
        "GivenStories:",
        "Examples:"
    );

    private final List<StepDefinition> stepDefinitions = new ArrayList<>();
    private final Supplier<Map<StepType, List<StepDefinition>>> groupedStepDefinitions = Suppliers.memoize(
        () -> stepDefinitions.stream()
                             .collect(Collectors.groupingBy(StepDefinition::getStepType, Collectors.toList())));

    private final TextDocumentProvider textDocumentProvider;

    @Inject
    public StepDefinitionResolver(TextDocumentProvider textDocumentProvider)
    {
        this.textDocumentProvider = textDocumentProvider;
    }

    public Stream<ResolvedStepDefinition> resolveAtPosition(String documentIdentifier, Position position)
    {
        return findStep(textDocumentProvider.getTextDocument(documentIdentifier), position)
                .stream()
                .flatMap(step -> resolve(step, groupedStepDefinitions.get().get(step.getType()), false));
    }

    /**
     * The method resolves each step line in the document to step definition that the step matches, if step matches
     * more than one definition, the one more specific definition will be returned.
     *
     * @param documentIdentifier The identifier of document containing steps to resolve
     * @return The stream of resolved step definitions
     */
    public Stream<ResolvedStepDefinition> resolve(String documentIdentifier)
    {
        List<String> document = textDocumentProvider.getTextDocument(documentIdentifier);
        int searchIndex = document.size() - 1;

        List<Step> steps = new ArrayList<>(document.size());
        do
        {
            String currentLine = document.get(searchIndex);
            Optional<Step> outcome = findStep(document, new Position(searchIndex, currentLine.length()));
            if (outcome.isPresent())
            {
                Step step = outcome.get();
                steps.add(step);
                searchIndex = step.getLineIndex() - 1;
            }
            else
            {
                searchIndex--;
            }
        }
        while (searchIndex > 0);

        Collections.reverse(steps);

        return steps.stream().flatMap(step -> resolve(step, groupedStepDefinitions.get().get(step.getType()), true));
    }

    @Override
    public void setStepDefinitions(Collection<StepDefinition> stepDefinitions)
    {
        this.stepDefinitions.addAll(stepDefinitions);
    }

    private Stream<ResolvedStepDefinition> resolve(Step step, List<StepDefinition> definitions,
            boolean limitResultsToOne)
    {
        List<Entry<StepDefinition, MatchOutcome>> matchedDefinitions = new ArrayList<>();
        for (StepDefinition definition : definitions)
        {
            List<String> matchTokens = definition.getMatchTokens();
            MatchOutcome outcome = TokenMatcher.match(step.getValue(), matchTokens);

            if (outcome.isMatch())
            {
                if (limitResultsToOne && outcome.getTokenIndex() == matchTokens.size() - 1)
                {
                    return Stream.of(createResolved(step, outcome, definition));
                }

                matchedDefinitions.add(entry(definition, outcome));
            }
        }

        Collections.sort(matchedDefinitions,
                Comparator.comparing(e -> e.getValue().getTokenIndex(), Comparator.reverseOrder()));

        if (limitResultsToOne && !matchedDefinitions.isEmpty())
        {
            Entry<StepDefinition, MatchOutcome> entry = matchedDefinitions.get(0);
            return Stream.of(createResolved(step, entry.getValue(), entry.getKey()));
        }

        return matchedDefinitions.stream().map(e -> createResolved(step, e.getValue(), e.getKey()));
    }

    private static ResolvedStepDefinition createResolved(Step step, MatchOutcome outcome, StepDefinition definition)
    {
        return new ResolvedStepDefinition(step, outcome.getTokenIndex(), outcome.getSubToken(),
                outcome.getArgIndices(), definition);
    }

    private static Optional<Step> findStep(List<String> document, Position position)
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
            return Optional.of(new Step(lineIndex, type.get(), token));
        }

        return findStepHeadIndex(lineIndex, document).map(e ->
        {
            String multilineToken = document.subList(e.getValue(), lineIndex).stream()
                    .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator() + token));
            return new Step(e.getValue(), e.getKey(), multilineToken);
        });
    }

    private static boolean isNotStep(String line)
    {
        return STEP_BREAKERS.stream().anyMatch(line::startsWith);
    }

    private static Optional<Entry<StepType, Integer>> findStepHeadIndex(int currentIndex, List<String> lines)
    {
        for (int index = lines.size() - 1; index >= 0; index--)
        {
            String line = lines.get(index);
            Optional<StepType> type = StepType.detectSafely(line);
            if (type.isPresent() && currentIndex > index)
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
