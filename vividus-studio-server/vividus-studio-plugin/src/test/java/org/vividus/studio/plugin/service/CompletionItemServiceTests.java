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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemTag;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.document.TextDocumentProvider;
import org.vividus.studio.plugin.model.Parameter;
import org.vividus.studio.plugin.model.StepDefinition;

@ExtendWith(MockitoExtension.class)
class CompletionItemServiceTests
{
    private static final int GIVEN_STEP_HASH = -462107804;
    private static final String GIVEN_TRIGGER = "G";
    private static final String GIVEN_STEP = "Given random value";
    private static final String GIVEN_STEP_SNIPPET = "Given random value";

    private static final int WHEN_STEP_HASH = 382930399;
    private static final String WHEN_TRIGGER = "W";
    private static final String WHEN_STEP = "When I convert $value into custom type";
    private static final String WHEN_STEP_SNIPPET = "When I convert ${1:value} into custom type";

    private static final int THEN_STEP_HASH = -1351541186;
    private static final String THEN_TRIGGER = "T";
    private static final String THEN_STEP = "Then $value is equal to $expected after conversion";
    private static final String THEN_STEP_SNIPPET = "Then ${1:value} is equal to ${2:expected} after conversion";

    private static final String DOCS = "documentation";
    private static final String MODULE = "module";

    private static final String DOCUMENT_ID = "document-id";

    @Mock TextDocumentProvider textDocumentProvider;
    private CompletionItemService completionItemService;

    @BeforeEach
    void init()
    {
        StepDefinition givenStepDefinition = new StepDefinition(MODULE, GIVEN_STEP, DOCS, List.of(), List.of(GIVEN_STEP));
        StepDefinition whenStepDefinition = new StepDefinition(MODULE, WHEN_STEP, DOCS, List.of(
                new Parameter(1, "$value", 15)
        ), List.of("When I convert ", " into custom type"));
        StepDefinition thenStepDefinition = new StepDefinition(MODULE, THEN_STEP, DOCS, List.of(
                new Parameter(1, "$value", 5),
                new Parameter(2, "$expected", 24)
        ), List.of("Then ", " is equal to ", " after conversion"));
        thenStepDefinition.setDeprecated(true);
        StepDefinitionResolver resolver = new StepDefinitionResolver(textDocumentProvider);
        resolver.setStepDefinitions(List.of(givenStepDefinition, whenStepDefinition, thenStepDefinition));
        completionItemService = new CompletionItemService(resolver);
    }

    static Stream<Arguments> findAllDataset()
    {
        return Stream.of(
            arguments(GIVEN_TRIGGER, GIVEN_STEP, GIVEN_STEP_SNIPPET, GIVEN_STEP_HASH, null),
            arguments(WHEN_TRIGGER, WHEN_STEP, WHEN_STEP_SNIPPET, WHEN_STEP_HASH, null),
            arguments(THEN_TRIGGER, THEN_STEP, THEN_STEP_SNIPPET, THEN_STEP_HASH, List.of(CompletionItemTag.Deprecated))
        );
    }

    static Stream<Arguments> findAllAtPositionDataset()
    {
        return Stream.of(
            arguments(List.of("Given rand"), "om value", 0, 10),
            arguments(List.of("Then McDonald's is equal t"), "o ${2:expected} after conversion", 0, 26),
            arguments(List.of("Then ", "line1", "line2", " is equa"), "l to ${2:expected} after conversion", 3, 8),
            arguments(List.of("Then McDonald's is equal to Fat ass after conversion"), "", 0, 52)
        );
    }

    @MethodSource("findAllAtPositionDataset")
    @ParameterizedTest
    void testFindAllAtPosition(List<String> lines, String textEdit, int line, int charPos)
    {
        Position position = new Position(line, charPos);

        when(textDocumentProvider.getTextDocument(DOCUMENT_ID)).thenReturn(lines);

        List<CompletionItem> items = completionItemService.findAllAtPosition(DOCUMENT_ID, position);
        assertThat(items, hasSize(1));
        CompletionItem item = items.get(0);

        assertEquals(textEdit, item.getTextEdit().getLeft().getNewText());
    }

    static Stream<Arguments> noMatchDataSet()
    {
        return Stream.of(
            arguments(List.of("Nutty Putty Cave"), 0, 15),
            arguments(List.of("Nutty", "Putty", "Cave"), 2, 3),
            arguments(List.of("When I procrastinate"), 0, 20),
            arguments(List.of("Scenario: Salute to the Sun"), 0, 35),
            arguments(List.of("Scenario: Salute", "to", "the", "Sun"), 3, 2),
            arguments(List.of(
                "Scenario: Have no idea how to name it :(",
                "Given friday",
                "When I try to work",
                "O",
                "Then work is not going"
                ), 3, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("noMatchDataSet")
    void testFindAllAtPositionNoMatch(List<String> lines, int line, int charPos)
    {
        Position position = new Position(line, charPos);

        when(textDocumentProvider.getTextDocument(DOCUMENT_ID)).thenReturn(lines);

        assertThat(completionItemService.findAllAtPosition(DOCUMENT_ID, position), hasSize(0));
    }
}
