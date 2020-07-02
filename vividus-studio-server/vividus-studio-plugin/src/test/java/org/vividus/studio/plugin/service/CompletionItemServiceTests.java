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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonObject;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionItemTag;
import org.eclipse.lsp4j.InsertTextFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private static final String HASH = "hash";
    private static final String TRIGGER = "trigger";
    private static final String SEP = ",";
    private static final String EMPTY = "";

    @InjectMocks private CompletionItemService completionItemService;

    @BeforeEach
    void init()
    {
        StepDefinition givenStepDefinition = new StepDefinition(MODULE, GIVEN_STEP, DOCS, List.of());
        StepDefinition whenStepDefinition = new StepDefinition(MODULE, WHEN_STEP, DOCS, List.of(
                new Parameter(1, "$value", 15)
        ));
        StepDefinition thenStepDefinition = new StepDefinition(MODULE, THEN_STEP, DOCS, List.of(
                new Parameter(1, "$value", 5),
                new Parameter(2, "$expected", 24)
        ));
        thenStepDefinition.setDeprecated(true);
        completionItemService.setStepDefinitions(List.of(givenStepDefinition, whenStepDefinition, thenStepDefinition));
    }

    static Stream<Arguments> findAllDataset()
    {
        return Stream.of(
            arguments(GIVEN_TRIGGER, GIVEN_STEP, GIVEN_STEP_SNIPPET, GIVEN_STEP_HASH, null),
            arguments(WHEN_TRIGGER, WHEN_STEP, WHEN_STEP_SNIPPET, WHEN_STEP_HASH, null),
            arguments(THEN_TRIGGER, THEN_STEP, THEN_STEP_SNIPPET, THEN_STEP_HASH, List.of(CompletionItemTag.Deprecated))
        );
    }

    @MethodSource("findAllDataset")
    @ParameterizedTest
    void testFindAll(String trigger, String label, String snippet, int hash, List<CompletionItemTag> tags)
    {
        List<CompletionItem> completions = completionItemService.findAll(trigger);
        assertThat(completions, hasSize(1));
        CompletionItem item = completions.get(0);
        JsonObject data = (JsonObject) item.getData();
        assertAll(
            () -> assertEquals(CompletionItemKind.Method, item.getKind()),
            () -> assertEquals(label, item.getLabel()),
            () -> assertEquals(InsertTextFormat.Snippet, item.getInsertTextFormat()),
            () -> assertEquals(snippet, item.getInsertText()),
            () -> assertEquals(MODULE, item.getDetail()),
            () -> assertEquals(DOCS, item.getDocumentation().getLeft()),
            () -> assertEquals(trigger, data.get(TRIGGER).getAsString()),
            () -> assertEquals(hash, data.get(HASH).getAsInt()),
            () -> assertEquals(tags, item.getTags())
        );
    }

    @CsvSource({
        GIVEN_TRIGGER + SEP + GIVEN_STEP + SEP + GIVEN_STEP_SNIPPET + SEP + GIVEN_STEP_HASH,
        WHEN_TRIGGER + SEP + WHEN_STEP + SEP + WHEN_STEP_SNIPPET + SEP + WHEN_STEP_HASH,
        THEN_TRIGGER + SEP + THEN_STEP + SEP + THEN_STEP_SNIPPET + SEP + THEN_STEP_HASH
    })
    @ParameterizedTest
    void testFindOne(String trigger, String label, String snippet, int hash)
    {
        JsonObject data = new JsonObject();
        data.addProperty(TRIGGER, trigger);
        data.addProperty(HASH, hash);
        CompletionItem unresolved = new CompletionItem();
        unresolved.setData(data);
        CompletionItem resolved = completionItemService.findOne(unresolved);
        JsonObject resolvedData = (JsonObject) resolved.getData();
        assertAll(
            () -> assertEquals(label, resolved.getLabel()),
            () -> assertEquals(trigger, resolvedData.get(TRIGGER).getAsString()),
            () -> assertEquals(hash, resolvedData.get(HASH).getAsInt())
        );
    }
}
