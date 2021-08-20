/*-
 * *
 * *
 * Copyright (C) 2020 - 2021 the original author or authors.
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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Singleton;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.WorkDoneProgressCreateParams;
import org.eclipse.lsp4j.WorkDoneProgressEnd;
import org.eclipse.lsp4j.WorkDoneProgressNotification;
import org.eclipse.lsp4j.WorkDoneProgressReport;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;

@Singleton
public class ClientNotificationService
{
    private LanguageClient languageClient;

    public void startProgress(Either<String, Integer> token, String title, String message)
    {
        WorkDoneProgressBegin progress = new WorkDoneProgressBegin();
        progress.setTitle(title);
        progress.setMessage(message);
        progress.setCancellable(false);
        notify(token, progress);
    }

    public void progress(Either<String, Integer> token, String message)
    {
        WorkDoneProgressReport progress = new WorkDoneProgressReport();
        progress.setCancellable(false);
        progress.setMessage(message);
        notify(token, progress);
    }

    public void endProgress(Either<String, Integer> token, String message)
    {
        WorkDoneProgressEnd progress = new WorkDoneProgressEnd();
        progress.setMessage(message);
        notify(token, progress);
    }

    public void showInfo(String message)
    {
        showMessage(message, MessageType.Info);
    }

    public void showError(String message)
    {
        showMessage(message, MessageType.Error);
    }

    public CompletableFuture<Either<String, Integer>> createProgress()
    {
        Either<String, Integer> token = Either.forLeft(UUID.randomUUID().toString());
        WorkDoneProgressCreateParams params = new WorkDoneProgressCreateParams(token);
        return this.languageClient.createProgress(params).thenApplyAsync(empty -> token);
    }

    public void logMessage(String message)
    {
        MessageParams messageParams = new MessageParams(MessageType.Log, message);
        this.languageClient.logMessage(messageParams);
    }

    private void showMessage(String message, MessageType messageType)
    {
        MessageParams messageParams = new MessageParams(messageType, message);
        this.languageClient.showMessage(messageParams);
    }

    private void notify(Either<String, Integer> token, WorkDoneProgressNotification notification)
    {
        this.languageClient.notifyProgress(new ProgressParams(token, Either.forLeft(notification)));
    }

    public void setLanguageClient(LanguageClient languageClient)
    {
        this.languageClient = languageClient;
    }
}
