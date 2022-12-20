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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.document.TextDocumentProvider;
import org.vividus.studio.plugin.model.Parameter;
import org.vividus.studio.plugin.model.StepDefinition;

@ExtendWith(MockitoExtension.class)
class SemanticTokensServiceTests
{
    private static final String DOCUMENT_ID = "document-id";

    private static final String DOCS = "documentation";
    private static final String MODULE = "module";

    @Mock TextDocumentProvider textDocumentProvider;
    private SemanticTokensService tokenService;

    @Test
    void shouldReturnSemanticTokens()
    {
        StepDefinition givenStepDefinition = new StepDefinition(MODULE, "Given random value", DOCS, List.of(),
                List.of("Given random value"));
        StepDefinition whenStepDefinition = new StepDefinition(MODULE, "When I convert $value into custom type", DOCS,
                List.of(new Parameter(1, "$value", 15)), List.of("When I convert ", " into custom type"));
        StepDefinition thenStepDefinition = new StepDefinition(MODULE,
                "Then $value is equal to $expected after conversion", DOCS,
                List.of(new Parameter(1, "$value", 5), new Parameter(2, "$expected", 24)),
                List.of("Then ", " is equal to ", " after conversion"));
        StepDefinitionResolver resolver = new StepDefinitionResolver(textDocumentProvider);
        resolver.setStepDefinitions(List.of(givenStepDefinition, whenStepDefinition, thenStepDefinition));
        tokenService = new SemanticTokensService(resolver);

        when(textDocumentProvider.getTextDocument(DOCUMENT_ID)).thenReturn(List.of(
            "Scenario: Conversion",
            "Given a task to calculate number conversion",
            "When I convert PI into custom type",
            "Then PI is equal to 3.14159265359 after conversion",
            givenStepDefinition.getStepAsString(),
            "Then ${random-value} is equal to ??? after conversion",
            "When I convert XII into",
            "Then XII is equ",
            "Then the end is reached"
        ));

        List<Integer> expectedTokens = List.of(
            2, 15, 2, 0, 0,
            1, 5, 2, 0, 0,
            0, 15, 13, 0, 0,
            2, 5, 15, 0, 0,
            0, 28, 3, 0, 0,
            1, 15, 3, 0, 0,
            1, 5, 3, 0, 0
        );

        assertEquals(expectedTokens, tokenService.getSemanticTokens(DOCUMENT_ID));
    }
}
