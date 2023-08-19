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

import static java.lang.String.format;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.vividus.studio.plugin.util.RuntimeWrapper.wrap;
import static org.vividus.studio.plugin.util.RuntimeWrapper.wrapMono;

import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.studio.plugin.VividusStudioActivator;
import org.vividus.studio.plugin.command.ICommand;
import org.vividus.studio.plugin.configuration.JVMConfigurator;
import org.vividus.studio.plugin.configuration.VividusStudioEnvronment;
import org.vividus.studio.plugin.exception.VividusStudioException;
import org.vividus.studio.plugin.loader.IJavaProjectLoader;
import org.vividus.studio.plugin.loader.IJavaProjectLoader.Event;
import org.vividus.studio.plugin.log.VividusStudioLogAppender;
import org.vividus.studio.plugin.service.ClientNotificationService;
import org.vividus.studio.plugin.service.StepDefinitionResolver;

@SuppressWarnings("paramNum")
@Singleton
public class VividusStudioLanguageServer implements LanguageServer, SocketListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VividusStudioActivator.class);

    private static final int TIME_TO_WAIT_EXIT = 3;

    private final TextDocumentService textDocumentService;
    private final StepDefinitionResolver stepDefinitionResolver;
    private final WorkspaceService workspaceService;
    private final IJavaProjectLoader projectLoader;
    private final IWorkspace workspace;
    private final JVMConfigurator jvmConfigurator;
    private final ClientNotificationService clientNotificationService;
    private final VividusStudioEnvronment vividusStudioConfiguration;
    private final Set<ICommand> commands;

    private boolean exit;

    @Inject
    public VividusStudioLanguageServer(
            TextDocumentService textDocumentService,
            StepDefinitionResolver stepDefinitionResolver,
            WorkspaceService workspaceService,
            IJavaProjectLoader projectLoader,
            IWorkspace workspace,
            JVMConfigurator jvmConfigurator,
            ClientNotificationService clientNotificationService,
            VividusStudioEnvronment vividusStudioConfiguration,
            Set<ICommand> commands)
    {
        this.textDocumentService = textDocumentService;
        this.stepDefinitionResolver = stepDefinitionResolver;
        this.workspaceService = workspaceService;
        this.projectLoader = projectLoader;
        this.workspace = workspace;
        this.jvmConfigurator = jvmConfigurator;
        this.clientNotificationService = clientNotificationService;
        this.vividusStudioConfiguration = vividusStudioConfiguration;
        this.commands = commands;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            Either<String, Integer> token = params.getWorkDoneToken();

            clientNotificationService.startProgress(token, "Initialization", "Initialize project");

            ServerCapabilities capabilities = new ServerCapabilities();
            capabilities.setTextDocumentSync(TextDocumentSyncKind.Incremental);
            capabilities.setCompletionProvider(new CompletionOptions(true, List.of()));

            ExecuteCommandOptions commandOptions = commands.stream()
                    .map(ICommand::getName)
                    .collect(collectingAndThen(toList(), ExecuteCommandOptions::new));
            capabilities.setExecuteCommandProvider(commandOptions);

            SemanticTokensLegend legend = new SemanticTokensLegend(List.of("vividus-step-argument"), List.of());
            SemanticTokensWithRegistrationOptions options = new SemanticTokensWithRegistrationOptions(legend, true);
            options.setDocumentSelector(List.of(
                new DocumentFilter("vividus-story", null, null),
                new DocumentFilter("vividus-composite-step", null, null)
            ));
            capabilities.setSemanticTokensProvider(options);

            CodeActionOptions codeActionOptions = new CodeActionOptions();
            codeActionOptions.setCodeActionKinds(List.of(CodeActionKind.Source));
            codeActionOptions.setResolveProvider(true);
            capabilities.setCodeActionProvider(codeActionOptions);

            Optional<IJavaProject> javaProject = projectLoader.load(params.getRootUri(), Map.of(
                    Event.LOADED, n -> clientNotificationService.showInfo(
                            format("Project with the name '%s' is loaded", n)),
                    Event.CORRUPTED, p -> clientNotificationService.showError(
                            format("Project file by path '%s' is corrupted", p)),
                    Event.INFO, msg -> clientNotificationService.progress(token, msg)));

            javaProject.ifPresent(jp ->
            {
                vividusStudioConfiguration.setJavaProject(jp);
                stepDefinitionResolver.refresh();
            });

            wrap(jvmConfigurator::configureDefaultJvm, VividusStudioException::new);

            clientNotificationService.endProgress(token, "Completed");

            return new InitializeResult(capabilities);
        });
    }

    @Override
    public CompletableFuture<Object> shutdown()
    {
        return CompletableFuture.supplyAsync(() ->
        {
            LOGGER.info("Shutting down...");
            wrapMono(() -> this.workspace.save(true, null), VividusStudioException::new);
            return new Object();
        });
    }

    @Override
    public void exit()
    {
        LOGGER.info("Exiting from the server process...");
        exit = true;
    }

    @Override
    public TextDocumentService getTextDocumentService()
    {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService()
    {
        return workspaceService;
    }

    @SuppressWarnings("AvoidEscapedUnicodeCharacters")
    @Override
    public void listen(InetAddress address, int port) throws Exception
    {
        try (Socket socket = new Socket(address, port))
        {
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(this, socket.getInputStream(),
                    socket.getOutputStream());
            clientNotificationService.setLanguageClient(launcher.getRemoteProxy());
            VividusStudioLogAppender.getInstance().setClientNotificationService(clientNotificationService);
            launcher.startListening();
            LOGGER.info("Socket is listening on {}:{}", socket.getInetAddress(), socket.getPort());
            clientNotificationService.showInfo("Welcome to the VIVIDUS Studio");
            while (!exit)
            {
                TimeUnit.SECONDS.sleep(TIME_TO_WAIT_EXIT);
            }
        }
        finally
        {
            exit = true;
        }
    }
}
