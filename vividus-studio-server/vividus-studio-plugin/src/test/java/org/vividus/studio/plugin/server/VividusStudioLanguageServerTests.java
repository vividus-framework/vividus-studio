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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.command.ICommand;
import org.vividus.studio.plugin.configuration.JVMConfigurator;
import org.vividus.studio.plugin.configuration.VividusStudioEnvronment;
import org.vividus.studio.plugin.loader.IJavaProjectLoader;
import org.vividus.studio.plugin.loader.IJavaProjectLoader.Event;
import org.vividus.studio.plugin.log.VividusStudioLogAppender;
import org.vividus.studio.plugin.service.ClientNotificationService;
import org.vividus.studio.plugin.service.StepDefinitionResolver;

@ExtendWith(MockitoExtension.class)
class VividusStudioLanguageServerTests
{
    private static final String COMMAND = "test-command";

    @Mock private StepDefinitionResolver stepDefinitionResolver;
    @Mock private IJavaProjectLoader projectLoader;
    @Mock private JVMConfigurator jvmConfigurator;
    @Mock private ClientNotificationService clientNotificationService;
    @Mock private WorkspaceService workspaceService;
    @Mock private VividusStudioEnvronment vividusStudioConfiguration;

    private VividusStudioLanguageServer languageServer;

    @BeforeEach
    void beforeEach() throws IllegalArgumentException, IllegalAccessException
    {
        ICommand command = mock(ICommand.class);
        lenient().when(command.getName()).thenReturn(COMMAND);

        languageServer = new VividusStudioLanguageServer(null, stepDefinitionResolver, workspaceService, projectLoader,
                null, jvmConfigurator, clientNotificationService, vividusStudioConfiguration, Set.of(command));
    }

    @Test
    void testInitialize() throws InterruptedException, ExecutionException, CoreException, IOException
    {
        InitializeParams params = mock(InitializeParams.class);
        String rootUri = "root uri";
        IJavaProject javaProject = mock(IJavaProject.class);
        Either<String, Integer> token = Either.forLeft("token");

        when(params.getWorkDoneToken()).thenReturn(token);
        when(params.getRootUri()).thenReturn(rootUri);
        String info = "info";
        when(projectLoader.load(eq(rootUri), argThat(events -> {
            Consumer<String> infoConsumer = events.get(Event.INFO);
            infoConsumer.accept(info);
            return true;
        }))).thenReturn(Optional.of(javaProject));

        InOrder notificationServiceOrder = inOrder(clientNotificationService);

        InitializeResult result = languageServer.initialize(params).get();

        ServerCapabilities serverCapabilities = result.getCapabilities();
        List<String> triggers = serverCapabilities.getCompletionProvider().getTriggerCharacters();
        assertEquals(List.of(), triggers);
        verify(jvmConfigurator).configureDefaultJvm();
        verify(vividusStudioConfiguration).setJavaProject(javaProject);
        verify(stepDefinitionResolver).refresh();
        assertEquals(List.of(COMMAND), serverCapabilities.getExecuteCommandProvider().getCommands());

        notificationServiceOrder.verify(clientNotificationService).startProgress(token, "Initialization",
                "Initialize project");
        notificationServiceOrder.verify(clientNotificationService).progress(token, info);
        notificationServiceOrder.verify(clientNotificationService).endProgress(token, "Completed");

        CodeActionOptions codeActionOptions = serverCapabilities.getCodeActionProvider().getRight();
        assertTrue(codeActionOptions.getResolveProvider());
        assertEquals(List.of(CodeActionKind.Source), codeActionOptions.getCodeActionKinds());
    }

    @Test
    void shouldListen() throws Exception
    {
        InputStream is = mock(InputStream.class);
        OutputStream os = mock(OutputStream.class);

        try(MockedConstruction<Socket> socketConstruction = mockConstruction(Socket.class, (mock, ctx) -> {
            when(mock.getInputStream()).thenReturn(is);
            when(mock.getOutputStream()).thenReturn(os);
        });
            MockedStatic<LSPLauncher> lspLauncher = mockStatic(LSPLauncher.class);
            MockedStatic<VividusStudioLogAppender> logAppender = mockStatic(VividusStudioLogAppender.class))
        {
            Launcher<LanguageClient> launcher = mock(Launcher.class);
            lspLauncher.when(() -> LSPLauncher.createServerLauncher(languageServer, is,
                    os)).thenReturn(launcher);
            VividusStudioLogAppender appender = mock(VividusStudioLogAppender.class);
            logAppender.when(() -> VividusStudioLogAppender.getInstance()).thenReturn(appender);
            LanguageClient client = mock(LanguageClient.class);
            when(launcher.getRemoteProxy()).thenReturn(client);

            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> languageServer.exit());

            languageServer.listen(null, 0);

            verify(clientNotificationService).setLanguageClient(client);
            verify(launcher).startListening();
            verify(clientNotificationService).showInfo("Welcome to the VIVIDUS Studio");
            verify(appender).setClientNotificationService(clientNotificationService);
        }
    }
}
