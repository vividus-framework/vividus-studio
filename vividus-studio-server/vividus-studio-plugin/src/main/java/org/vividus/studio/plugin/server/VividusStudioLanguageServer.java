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

import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.WorkDoneProgressEnd;
import org.eclipse.lsp4j.WorkDoneProgressReport;
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
import org.vividus.studio.plugin.exception.VividusStudioException;
import org.vividus.studio.plugin.finder.IStepDefinitionFinder;
import org.vividus.studio.plugin.loader.IJavaProjectLoader;
import org.vividus.studio.plugin.loader.IJavaProjectLoader.Event;
import org.vividus.studio.plugin.model.StepType;
import org.vividus.studio.plugin.service.ICompletionItemService;
import org.vividus.studio.plugin.util.RuntimeWrapper;

@Singleton
public class VividusStudioLanguageServer implements LanguageServer, SocketListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VividusStudioActivator.class);

    private static final int TIME_TO_WAIT_EXIT = 3;

    private final TextDocumentService textDocumentService;
    private final ICompletionItemService completionItemService;
    private final WorkspaceService workspaceService;
    private final IJavaProjectLoader projectLoader;
    private final IWorkspace workspace;
    private final IStepDefinitionFinder stepDefinitionFinder;

    private LanguageClient languageClient;
    private boolean exit;

    @Inject
    public VividusStudioLanguageServer(
            TextDocumentService textDocumentService,
            ICompletionItemService completionItemService,
            WorkspaceService workspaceService,
            IJavaProjectLoader projectLoader,
            IWorkspace workspace,
            IStepDefinitionFinder stepDefinitionFinder)
    {
        this.textDocumentService = textDocumentService;
        this.completionItemService = completionItemService;
        this.workspaceService = workspaceService;
        this.projectLoader = projectLoader;
        this.workspace = workspace;
        this.stepDefinitionFinder = stepDefinitionFinder;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            Either<String, Integer> token = params.getWorkDoneToken();

            startProgress(token, "Initialization", "Initialize project");

            InitializeResult initResult = new InitializeResult();
            ServerCapabilities capabilities = new ServerCapabilities();
            capabilities.setTextDocumentSync(TextDocumentSyncKind.Incremental);
            List<String> triggers = Stream.of(StepType.values())
                                          .map(StepType::getId)
                                          .collect(Collectors.toList());
            capabilities.setCompletionProvider(new CompletionOptions(true, triggers));

            initResult.setCapabilities(capabilities);

            Optional<IJavaProject> javaProject = projectLoader.load(params.getRootUri(), Map.of(
                    Event.LOADED, n -> showInfo(format("Project with the name '%s' is loaded", n)),
                    Event.CORRUPTED, p -> showError(format("Project file by path '%s' is corrupted", p)),
                    Event.INFO, msg -> progress(token, msg)));

            javaProject.map(stepDefinitionFinder::find).ifPresent(completionItemService::setStepDefinitions);

            endProgress(token, "Completed");

            return initResult;
        });
    }

    private void startProgress(Either<String, Integer> token, String title, String message)
    {
        WorkDoneProgressBegin progress = new WorkDoneProgressBegin();
        progress.setTitle(title);
        progress.setMessage(message);
        progress.setCancellable(false);
        this.languageClient.notifyProgress(new ProgressParams(token, Either.forLeft(progress)));
    }

    private void progress(Either<String, Integer> token, String message)
    {
        WorkDoneProgressReport progress = new WorkDoneProgressReport();
        progress.setCancellable(false);
        progress.setMessage(message);
        this.languageClient.notifyProgress(new ProgressParams(token, Either.forLeft(progress)));
    }

    private void endProgress(Either<String, Integer> token, String message)
    {
        WorkDoneProgressEnd progress = new WorkDoneProgressEnd();
        progress.setMessage(message);
        this.languageClient.notifyProgress(new ProgressParams(token, Either.forLeft(progress)));
    }

    @Override
    public CompletableFuture<Object> shutdown()
    {
        return CompletableFuture.supplyAsync(() ->
        {
            LOGGER.info("Shutting down...");
            RuntimeWrapper.wrapMono(() -> this.workspace.save(true, null), VividusStudioException::new);
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

    @Override
    public void listen(InetAddress address, int port) throws Exception
    {
        try (Socket socket = new Socket(address, port))
        {
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(this, socket.getInputStream(),
                    socket.getOutputStream());
            this.languageClient = launcher.getRemoteProxy();
            launcher.startListening();
            LOGGER.info("Socket is listening on {}:{}", socket.getInetAddress(), socket.getPort());
            showInfo("Welcome to the Vividus Studio");
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

    private void showInfo(String message)
    {
        showMessage(message, MessageType.Info);
    }

    private void showError(String message)
    {
        showMessage(message, MessageType.Error);
    }

    private void showMessage(String message, MessageType messageType)
    {
        MessageParams messageParams = new MessageParams(messageType, message);
        this.languageClient.showMessage(messageParams);
    }
}
