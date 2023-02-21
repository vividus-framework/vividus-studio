/*-
 * *
 * *
 * Copyright (C) 2020 - 2023 the original author or authors.
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

package org.vividus.studio.plugin.composite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.configuration.VividusStudioEnvronment;
import org.vividus.studio.plugin.document.TextDocumentProvider;
import org.vividus.studio.plugin.factory.StepDefinitionFactory;
import org.vividus.studio.plugin.model.StepDefinition;
import org.vividus.studio.plugin.service.IStepDefinitionsAware;

@ExtendWith(MockitoExtension.class)
class CompositeStepsSaveListenerTests
{
    @Mock private TextDocumentProvider textDocumentProvider;
    @Spy private StepDefinitionFactory stepDefinitionFactory;
    @Mock private VividusStudioEnvronment vividusStudioEnvronment;
    @Mock private IStepDefinitionsAware stepDefinitionsAware;
    @Captor private ArgumentCaptor<List<StepDefinition>> definitionsCaptor;
    @InjectMocks private CompositeStepsSaveListener listener;

    @Test
    void shouldRefreshStepDefinitionsOnCompositeStepsFileSaveEvent() throws IOException
    {
        String uri = getClass().getResource("/project/src/main/resources/composite/composite.steps").getFile();

        DidSaveTextDocumentParams params = new DidSaveTextDocumentParams(new TextDocumentIdentifier(uri));
        List<String> lines = Files.readAllLines(Path.of(uri), StandardCharsets.UTF_8);
        when(textDocumentProvider.getTextDocument(uri)).thenReturn(lines);
        IProject project = mock();
        IPath location = mock();
        when(project.getLocation()).thenReturn(location);
        when(location.toString()).thenReturn(getClass().getResource("/project").getFile());
        when(vividusStudioEnvronment.getProject()).thenReturn(project);

        listener.onSave(params);

        verify(stepDefinitionsAware).refresh(definitionsCaptor.capture());

        List<StepDefinition> stepDefinitions = definitionsCaptor.getValue();
        assertThat(stepDefinitions, hasSize(1));
        StepDefinition definition = stepDefinitions.get(0);
        assertTrue(definition.isDynamic());
        assertTrue(definition.isComposite());
        assertEquals("Then I param $param1 and $param2 and $param3", definition.getStepAsString());
        assertEquals("composite/composite.steps", definition.getModule());
    }
}
