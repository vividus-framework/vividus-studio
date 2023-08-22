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
import static org.vividus.studio.plugin.util.RuntimeWrapper.wrapMono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.lsp4j.Position;
import org.vividus.studio.plugin.configuration.VividusStudioEnvronment;
import org.vividus.studio.plugin.document.TextDocumentProvider;
import org.vividus.studio.plugin.exception.VividusStudioException;
import org.vividus.studio.plugin.finder.IStepDefinitionFinder;
import org.vividus.studio.plugin.match.TokenMatcher;
import org.vividus.studio.plugin.match.TokenMatcher.MatchOutcome;
import org.vividus.studio.plugin.model.ResolvedStepDefinition;
import org.vividus.studio.plugin.model.Step;
import org.vividus.studio.plugin.model.StepDefinition;
import org.vividus.studio.plugin.model.StepType;
import org.vividus.studio.plugin.util.ResourceUtils;

@Singleton
public class StepDefinitionResolver implements IStepDefinitionsAware, StepDefinitionsProvider
{
    private static final String COMMENT = "!--";
    private static final List<String> STORY_STEP_BREAKERS = List.of(
        COMMENT,
        "Scenario:",
        "Meta:",
        "GivenStories:",
        "Examples:"
    );
    private static final List<String> COMPOSITE_STEP_BREAKERS = List.of(
        COMMENT,
        "Composite:"
    );

    private final List<StepDefinition> staticStepDefinitions = new ArrayList<>();
    private final List<StepDefinition> dynamicStepDefinitions = new ArrayList<>();

    private Supplier<Map<StepType, List<StepDefinition>>> groupedStepDefinitions;

    private final TextDocumentProvider textDocumentProvider;
    private final IStepDefinitionFinder stepDefinitionFinder;
    private final VividusStudioEnvronment vividusStudioConfiguration;

    @Inject
    public StepDefinitionResolver(TextDocumentProvider textDocumentProvider, IStepDefinitionFinder stepDefinitionFinder,
            VividusStudioEnvronment vividusStudioConfiguration)
    {
        this.textDocumentProvider = textDocumentProvider;
        this.stepDefinitionFinder = stepDefinitionFinder;
        this.vividusStudioConfiguration = vividusStudioConfiguration;
    }

    public Stream<ResolvedStepDefinition> resolveAtPosition(String documentIdentifier, Position position)
    {
        return findStep(textDocumentProvider.getTextDocument(documentIdentifier),
                getNotStepPredicate(documentIdentifier), position)
                .getStep()
                .stream()
                .flatMap(step -> resolve(step, getByType(step.getType()), false));
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
        if (document.isEmpty())
        {
            return Stream.empty();
        }

        int searchIndex = document.size() - 1;

        Predicate<String> notStepPredicate = getNotStepPredicate(documentIdentifier);
        List<Step> steps = new ArrayList<>(document.size());
        do
        {
            String currentLine = document.get(searchIndex);
            FindResult result = findStep(document, notStepPredicate, new Position(searchIndex, currentLine.length()));
            searchIndex = result.getNextPosition();
            result.getStep().ifPresent(steps::add);
        }
        while (searchIndex > 0);

        Collections.reverse(steps);

        return steps.stream().flatMap(step -> resolve(step, getByType(step.getType()), true));
    }

    private List<StepDefinition> getByType(StepType stepType)
    {
        return Optional.ofNullable(groupedStepDefinitions.get().get(stepType)).orElse(List.of());
    }

    @Override
    public void refresh()
    {
        Collection<StepDefinition> stepDefinitions = wrapMono(
                () -> stepDefinitionFinder.find(vividusStudioConfiguration.getJavaProject()),
                VividusStudioException::new);

        this.staticStepDefinitions.clear();

        refresh(stepDefinitions);
    }

    @Override
    public void refresh(Collection<StepDefinition> stepDefinitions)
    {
        this.groupedStepDefinitions = Suppliers.memoize(() ->
        {
            List<StepDefinition> dynamicDefinitions = new ArrayList<>();
            stepDefinitions.forEach(sd ->
            {
                if (sd.isDynamic())
                {
                    dynamicDefinitions.add(sd);
                }
                else
                {
                    this.staticStepDefinitions.add(sd);
                }
            });

            if (!this.dynamicStepDefinitions.isEmpty())
            {
                Set<String> modules = dynamicDefinitions.stream()
                        .collect(Collectors.groupingBy(StepDefinition::getModule, Collectors.toList())).keySet();
                this.dynamicStepDefinitions.removeIf(sd -> modules.contains(sd.getModule()));
            }
            this.dynamicStepDefinitions.addAll(dynamicDefinitions);

            List<StepDefinition> allStepDefinitions = new ArrayList<>();
            allStepDefinitions.addAll(this.staticStepDefinitions);
            allStepDefinitions.addAll(this.dynamicStepDefinitions);

            return allStepDefinitions.stream()
                    .collect(Collectors.groupingBy(StepDefinition::getStepType, Collectors.toList()));
        });
    }

    @Override
    public Stream<StepDefinition> getStepDefinitions()
    {
        return groupedStepDefinitions.get().values().stream().flatMap(List::stream);
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

    private static FindResult findStep(List<String> document, Predicate<String> notStepPredicate, Position position)
    {
        int lineIndex = position.getLine();
        String line = document.get(lineIndex);
        if (notStepPredicate.test(line))
        {
            return FindResult.create(lineIndex, null);
        }

        String token = line.substring(0, position.getCharacter());
        Optional<StepType> type = StepType.detectSafely(token);
        if (type.isPresent())
        {
            Step step = new Step(lineIndex, type.get(), token);
            return FindResult.create(lineIndex, step);
        }

        return findStepHead(lineIndex, document, notStepPredicate, token);
    }

    private static Predicate<String> getNotStepPredicate(String documentId)
    {
        List<String> stepBreakers = ResourceUtils.isCompositeFile(documentId) ? COMPOSITE_STEP_BREAKERS
                : STORY_STEP_BREAKERS;
        return line -> stepBreakers.stream().anyMatch(line::startsWith);
    }

    private static FindResult findStepHead(int currentIndex, List<String> lines, Predicate<String> notStepPredicate,
            String ending)
    {
        for (int index = currentIndex - 1; index >= 0; index--)
        {
            String line = lines.get(index);
            Optional<StepType> type = StepType.detectSafely(line);
            if (type.isPresent())
            {
                String multilineToken = lines.subList(index, currentIndex).stream()
                        .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator() + ending));
                Step step = new Step(index, type.get(), multilineToken);
                return FindResult.create(index, step);
            }

            if (notStepPredicate.test(line))
            {
                return FindResult.create(currentIndex, null);
            }
        }

        return FindResult.create(currentIndex, null);
    }

    private static final class FindResult
    {
        private final int nextPosition;
        private final Optional<Step> step;

        private FindResult(int nextPosition, Optional<Step> step)
        {
            this.nextPosition = nextPosition;
            this.step = step;
        }

        private int getNextPosition()
        {
            return nextPosition;
        }

        private Optional<Step> getStep()
        {
            return step;
        }

        private static FindResult create(int currentIndex, Step step)
        {
            return new FindResult(currentIndex - 1, Optional.ofNullable(step));
        }
    }
}
