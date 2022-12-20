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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
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
                .map(step -> groupedStepDefinitions.get().get(step.getType())
                        .stream()
                        .map(tryToResolve(step))
                        .flatMap(Optional::stream))
                .orElseGet(Stream::empty);
    }

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

        return steps.stream()
                    .map(step -> groupedStepDefinitions.get().get(step.getType())
                        .stream()
                        .map(tryToResolve(step))
                        .filter(Optional::isPresent)
                        .findFirst())
                    .flatMap(Optional::stream)
                    .flatMap(Optional::stream);
    }

    @Override
    public void setStepDefinitions(Collection<StepDefinition> stepDefinitions)
    {
        this.stepDefinitions.addAll(stepDefinitions);
    }

    private Function<StepDefinition, Optional<ResolvedStepDefinition>> tryToResolve(Step step)
    {
        return def ->
        {
            MatchOutcome outcome = TokenMatcher.match(step.getValue().strip(), def.getMatchTokens());
            if (!outcome.isMatch())
            {
                return Optional.empty();
            }
            ResolvedStepDefinition resolved = new ResolvedStepDefinition(step.getLineIndex(), outcome.getTokenIndex(),
                    outcome.getSubToken(), outcome.getArgIndices(), def);
            return Optional.of(resolved);
        };
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
                    .collect(Collectors.joining(System.lineSeparator(), "", token));
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

    private static final class Step
    {
        private final int lineIndex;
        private final StepType type;
        private final String value;

        private Step(int lineIndex, StepType type, String value)
        {
            this.lineIndex = lineIndex;
            this.type = type;
            this.value = value;
        }

        private int getLineIndex()
        {
            return lineIndex;
        }

        private StepType getType()
        {
            return type;
        }

        private String getValue()
        {
            return value;
        }
    }
}
