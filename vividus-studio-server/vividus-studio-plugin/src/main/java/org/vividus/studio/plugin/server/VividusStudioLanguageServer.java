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

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.studio.plugin.VividusStudioActivator;

public class VividusStudioLanguageServer implements LanguageServer, SocketListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VividusStudioActivator.class);

    private final TextDocumentService textDocumentService;
    private final WorkspaceService workspaceService;

    private LanguageClient languageClient;
    private boolean exit;

    @Inject
    public VividusStudioLanguageServer(TextDocumentService textDocumentService, WorkspaceService workspaceService)
    {
        this.textDocumentService = textDocumentService;
        this.workspaceService = workspaceService;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            InitializeResult initResult = new InitializeResult();
            ServerCapabilities capabilities = new ServerCapabilities();
            capabilities.setTextDocumentSync(TextDocumentSyncKind.Incremental);
            initResult.setCapabilities(capabilities);
            return initResult;
        });
    }

    @Override
    public CompletableFuture<Object> shutdown()
    {
        return CompletableFuture.supplyAsync(() ->
        {
            LOGGER.info("Shutting down...");
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
                TimeUnit.SECONDS.sleep(3);
            }
        }
        finally
        {
            exit = true;
        }
    }

    private void showInfo(String message)
    {
        MessageParams messageParams = new MessageParams(MessageType.Info, message);
        this.languageClient.showMessage(messageParams);
    }
}
