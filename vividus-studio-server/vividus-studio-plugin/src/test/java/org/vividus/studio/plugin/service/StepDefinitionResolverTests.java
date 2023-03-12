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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.document.TextDocumentProvider;
import org.vividus.studio.plugin.model.Parameter;
import org.vividus.studio.plugin.model.ResolvedStepDefinition;
import org.vividus.studio.plugin.model.StepDefinition;

@ExtendWith(MockitoExtension.class)
class StepDefinitionResolverTests
{
    private static final String GIVEN_STEP = "Given random value";
    private static final String DOCUMENT_ID = "document-id";
    private static final String DOCS = "documentation";
    private static final String MODULE = "module";

    @Mock private TextDocumentProvider textDocumentProvider;
    @InjectMocks private StepDefinitionResolver resolver;

    @BeforeEach
    void init()
    {
        StepDefinition givenStepDefinition = new StepDefinition(MODULE, GIVEN_STEP, DOCS, List.of(),
                List.of(GIVEN_STEP));
        StepDefinition whenStepDefinition = new StepDefinition(MODULE, "When I convert $value into custom type", DOCS,
                List.of(new Parameter(1, "$value", 15)), List.of("When I convert ", " into custom type"));
        StepDefinition thenStepDefinition = new StepDefinition(MODULE,
                "Then $value is equal to $expected after conversion", DOCS,
                List.of(new Parameter(1, "$value", 5), new Parameter(2, "$expected", 24)),
                List.of("Then ", " is equal to ", " after conversion"));

        StepDefinition dynamicStepDefinitionOne = new StepDefinition("composite/users.steps",
                "Given dynamic step definition one", DOCS, List.of(), List.of("Given dynamic step definition one"));
        dynamicStepDefinitionOne.setDynamic(true);

        resolver.refresh(List.of(givenStepDefinition, whenStepDefinition, thenStepDefinition, dynamicStepDefinitionOne));
    }

    static Stream<Arguments> resolveAtPositionDataset()
    {
        return Stream.of(
            arguments(
                List.of("Given rand"),
                new Position(0, 10),
                0, 0,
                GIVEN_STEP,
                List.of()
            ),
            arguments(
                List.of("Then McDonald's is equal t"),
                new Position(0, 26),
                0, 1,
                " is equal t",
                List.of(5, 15)
            ),
            arguments(
                List.of("Then ", "line1", "line2", " is equa"),
                new Position(3, 8),
                0, 1,
                " is equa",
                List.of(5, 18)
            ),
            arguments(
                List.of("Given some step", "Then McDonald's is equal to Fat ass after conversion"),
                new Position(1, 52),
                1, 2,
                "",
                List.of(5, 15, 28, 35)
            )
        );
    }

    @MethodSource("resolveAtPositionDataset")
    @ParameterizedTest
    void shouldResolveAtPosition(List<String> lines, Position position, int lineIndex, int tokenIndex, String subToken,
            List<Integer> argIndices)
    {
        when(textDocumentProvider.getTextDocument(DOCUMENT_ID)).thenReturn(lines);

        List<ResolvedStepDefinition> resolvedDefinitions = resolver.resolveAtPosition(DOCUMENT_ID, position)
                .collect(Collectors.toList());
        assertThat(resolvedDefinitions, hasSize(1));
        ResolvedStepDefinition resolved = resolvedDefinitions.get(0);
        assertEquals(subToken, resolved.getSubToken());
        assertStepDefinition(resolved, lineIndex, tokenIndex, argIndices);
    }

    @Test
    void shouldRefreshDynamicSteps()
    {
        when(textDocumentProvider.getTextDocument(DOCUMENT_ID)).thenReturn(List.of(
            "Scenario: Dynamic steps",
            "Given dynamic step definition one",
            "Given dynamic step definition two",
            "Given dynamic step definition three"
        ));

        List<ResolvedStepDefinition> resolvedDefinitions = resolver.resolve(DOCUMENT_ID).collect(Collectors.toList());
        assertThat(resolvedDefinitions, hasSize(1));
        assertEquals("Given dynamic step definition one", resolvedDefinitions.get(0).getStepAsString());

        StepDefinition dynamicStepDefinitionTwo = new StepDefinition("composite/orders.steps",
                "Given dynamic step definition two", DOCS, List.of(), List.of("Given dynamic step definition two"));
        dynamicStepDefinitionTwo.setDynamic(true);
        resolver.refresh(List.of(dynamicStepDefinitionTwo));

        resolvedDefinitions = resolver.resolve(DOCUMENT_ID).collect(Collectors.toList());
        assertThat(resolvedDefinitions, hasSize(2));
        assertEquals("Given dynamic step definition one", resolvedDefinitions.get(0).getStepAsString());
        assertEquals("Given dynamic step definition two", resolvedDefinitions.get(1).getStepAsString());

        StepDefinition dynamicStepDefinitionThree = new StepDefinition("composite/orders.steps",
                "Given dynamic step definition three", DOCS, List.of(), List.of("Given dynamic step definition three"));
        dynamicStepDefinitionThree.setDynamic(true);
        resolver.refresh(List.of(dynamicStepDefinitionThree));

        resolvedDefinitions = resolver.resolve(DOCUMENT_ID).collect(Collectors.toList());
        assertThat(resolvedDefinitions, hasSize(2));
        assertEquals("Given dynamic step definition one", resolvedDefinitions.get(0).getStepAsString());
        assertEquals("Given dynamic step definition three", resolvedDefinitions.get(1).getStepAsString());
    }

    @Test
    void shouldResolveForDocument()
    {
        when(textDocumentProvider.getTextDocument(DOCUMENT_ID)).thenReturn(List.of(
            "Scenario: Conversion",
            "Given a task to calculate number conversion",
            "When I convert PI into custom type",
            "Then PI is equal to 3.14159265359 after conversion",
            GIVEN_STEP,
            "Then ${random-value} is equal to ??? after conversion",
            "When I convert XII into",
            "Then XII is equ",
            "Then the end is reached"
        ));

        List<ResolvedStepDefinition> resolvedDefinitions = resolver.resolve(DOCUMENT_ID).collect(Collectors.toList());
        assertThat(resolvedDefinitions, hasSize(6));
        assertStepDefinition(resolvedDefinitions.get(0), 2, 1, List.of(15, 17));
        assertStepDefinition(resolvedDefinitions.get(1), 3, 2, List.of(5, 7, 20, 33));
        assertStepDefinition(resolvedDefinitions.get(2), 4, 0, List.of());
        assertStepDefinition(resolvedDefinitions.get(3), 5, 2, List.of(5, 20, 33, 36));
        assertStepDefinition(resolvedDefinitions.get(4), 6, 1, List.of(15, 18));
        assertStepDefinition(resolvedDefinitions.get(5), 7, 1, List.of(5, 8));
    }

    @Test
    void shouldNotResolveForEmptyDocument()
    {
        when(textDocumentProvider.getTextDocument(DOCUMENT_ID)).thenReturn(List.of());

        List<ResolvedStepDefinition> definitions = resolver.resolve(DOCUMENT_ID).collect(Collectors.toList());
        assertThat(definitions, is(empty()));
    }

    @Test
    void shouldReturnMoreSpecificResolvedDefinition()
    {
        StepDefinition whenStepDefinition1 = new StepDefinition(MODULE,
                "When I add '$product' into a bucket with the id '$id'", DOCS,
                List.of(new Parameter(1, "$product", 12), new Parameter(1, "$id", 49)),
                List.of("When I add '", "' into a bucket with the id '", "'"));
        StepDefinition whenStepDefinition2 = new StepDefinition(MODULE,
                "When I add '$product' into a bucket with the name '$name'", DOCS,
                List.of(new Parameter(1, "$product", 12), new Parameter(1, "$name", 51)),
                List.of("When I add '", "' into a bucket with the name '", "'"));
        resolver.refresh(List.of(whenStepDefinition1, whenStepDefinition2));

        when(textDocumentProvider.getTextDocument(DOCUMENT_ID))
                .thenReturn(List.of("When I add 'Milk' into a bucket with the name 'Grocery'"));

        List<ResolvedStepDefinition> resolvedDefinitions = resolver.resolve(DOCUMENT_ID).collect(Collectors.toList());
        assertThat(resolvedDefinitions, hasSize(1));
        assertEquals(whenStepDefinition2.getStepAsString(), resolvedDefinitions.get(0).getStepAsString());
    }

    @Test
    void shouldReturnStepDefinitions()
    {
        List<StepDefinition> stepDefinitions = resolver.getStepDefinitions().collect(Collectors.toList());
        assertThat(stepDefinitions, hasSize(4));
    }

    private static void assertStepDefinition(ResolvedStepDefinition resolved, int lineIndex, int tokenIndex,
            List<Integer> argIndices)
    {
        assertEquals(lineIndex, resolved.getStep().getLineIndex());
        assertEquals(tokenIndex, resolved.getTokenIndex());
        assertEquals(argIndices, resolved.getArgIndices());
    }
}
