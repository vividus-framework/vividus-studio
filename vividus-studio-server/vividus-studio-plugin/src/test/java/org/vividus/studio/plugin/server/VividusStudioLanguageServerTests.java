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

package org.vividus.studio.plugin.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.WorkDoneProgressEnd;
import org.eclipse.lsp4j.WorkDoneProgressNotification;
import org.eclipse.lsp4j.WorkDoneProgressReport;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.configuration.JVMConfigurator;
import org.vividus.studio.plugin.finder.IStepDefinitionFinder;
import org.vividus.studio.plugin.loader.IJavaProjectLoader;
import org.vividus.studio.plugin.loader.IJavaProjectLoader.Event;
import org.vividus.studio.plugin.model.StepDefinition;
import org.vividus.studio.plugin.service.ICompletionItemService;

@ExtendWith(MockitoExtension.class)
class VividusStudioLanguageServerTests
{
    @Mock private ICompletionItemService completionItemService;
    @Mock private IStepDefinitionFinder stepDefinitionFinder;
    @Mock private IJavaProjectLoader projectLoader;
    @Mock private LanguageClient languageClient;
    @Mock private JVMConfigurator jvmConfigurator;
    @InjectMocks private VividusStudioLanguageServer languageServer;

    @BeforeEach
    void beforeEach() throws IllegalArgumentException, IllegalAccessException
    {
        Field languageClientField = ReflectionUtils.findFields(VividusStudioLanguageServer.class,
            f -> "languageClient".equals(f.getName()), HierarchyTraversalMode.TOP_DOWN).get(0);
        ReflectionUtils.makeAccessible(languageClientField);
        languageClientField.set(languageServer, languageClient);
    }

    @Test
    void testInitialize() throws InterruptedException, ExecutionException, CoreException
    {
        InitializeParams params = mock(InitializeParams.class);
        String rootUri = "root uri";
        IJavaProject javaProject = mock(IJavaProject.class);
        StepDefinition stepDefinition = mock(StepDefinition.class);
        Either<String, Integer> token = Either.forLeft("token");

        when(params.getWorkDoneToken()).thenReturn(token);
        when(params.getRootUri()).thenReturn(rootUri);
        String info = "info";
        when(projectLoader.load(eq(rootUri), argThat(events -> {
            Consumer<String> infoConsumer = events.get(Event.INFO);
            infoConsumer.accept(info);
            return true;
        }))).thenReturn(Optional.of(javaProject));
        when(stepDefinitionFinder.find(javaProject)).thenReturn(List.of(stepDefinition));

        InOrder languageClientOrder = inOrder(languageClient);

        InitializeResult result = languageServer.initialize(params).get();

        List<String> triggers = result.getCapabilities().getCompletionProvider().getTriggerCharacters();
        assertEquals(List.of("G", "W", "T"), triggers);
        verify(completionItemService).setStepDefinitions(List.of(stepDefinition));
        verify(jvmConfigurator).configureDefaultJvm();

        languageClientOrder.verify(languageClient).notifyProgress(argThat(progress ->
        {
            WorkDoneProgressNotification notification = progress.getValue().getLeft();
            if (!(notification instanceof WorkDoneProgressBegin))
            {
                return false;
            }
            WorkDoneProgressBegin begin = (WorkDoneProgressBegin) notification;
            return token.equals(progress.getToken()) && "Initialization".equals(begin.getTitle())
                    && "Initialize project".equals(begin.getMessage()) && !begin.getCancellable();
        }));

        languageClientOrder.verify(languageClient).notifyProgress(argThat(progress ->
        {
            WorkDoneProgressNotification notification = progress.getValue().getLeft();
            if (!(notification instanceof WorkDoneProgressReport))
            {
                return false;
            }
            WorkDoneProgressReport report = (WorkDoneProgressReport) notification;
            return token.equals(progress.getToken()) && info.equals(report.getMessage()) && !report.getCancellable();
        }));

        languageClientOrder.verify(languageClient).notifyProgress(argThat(progress ->
        {
            WorkDoneProgressNotification notification = progress.getValue().getLeft();
            if (!(notification instanceof WorkDoneProgressEnd))
            {
                return false;
            }
            WorkDoneProgressEnd end = (WorkDoneProgressEnd) notification;
            return token.equals(progress.getToken()) && "Completed".equals(end.getMessage());
        }));
    }
}
