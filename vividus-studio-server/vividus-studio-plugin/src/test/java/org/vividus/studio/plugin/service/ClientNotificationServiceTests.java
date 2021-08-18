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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.WorkDoneProgressEnd;
import org.eclipse.lsp4j.WorkDoneProgressNotification;
import org.eclipse.lsp4j.WorkDoneProgressReport;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientNotificationServiceTests
{
    private static final Either<String, Integer> TOKEN = Either.forLeft("token");
    private static final String MESSAGE = "message";

    @Captor private ArgumentCaptor<ProgressParams> progressParamsCaptor;
    @Captor private ArgumentCaptor<MessageParams> messageParamsCaptor;
    @Mock private LanguageClient languageClient;
    @InjectMocks private ClientNotificationService service;

    @Test
    void shouldStartProgress()
    {
        String title = "title";
        service.startProgress(TOKEN, title, MESSAGE);

        verify(languageClient).notifyProgress(progressParamsCaptor.capture());
        ProgressParams progressParams = progressParamsCaptor.getValue();
        assertEquals(TOKEN, progressParams.getToken());
        WorkDoneProgressNotification notification = progressParams.getValue().getLeft();
        assertThat(notification, instanceOf(WorkDoneProgressBegin.class));
        WorkDoneProgressBegin begin = (WorkDoneProgressBegin) notification;
        assertEquals(title, begin.getTitle());
        assertEquals(MESSAGE, begin.getMessage());
        assertFalse(begin.getCancellable());
    }

    @Test
    void shouldReportProgress()
    {
        service.progress(TOKEN, MESSAGE);

        verify(languageClient).notifyProgress(progressParamsCaptor.capture());
        ProgressParams progressParams = progressParamsCaptor.getValue();
        assertEquals(TOKEN, progressParams.getToken());
        WorkDoneProgressNotification notification = progressParams.getValue().getLeft();
        assertThat(notification, instanceOf(WorkDoneProgressReport.class));
        WorkDoneProgressReport report = (WorkDoneProgressReport) notification;
        assertEquals(MESSAGE, report.getMessage());
        assertFalse(report.getCancellable());
    }

    @Test
    void shouldEndProgress()
    {
        service.endProgress(TOKEN, MESSAGE);

        verify(languageClient).notifyProgress(progressParamsCaptor.capture());
        ProgressParams progressParams = progressParamsCaptor.getValue();
        assertEquals(TOKEN, progressParams.getToken());
        WorkDoneProgressNotification notification = progressParams.getValue().getLeft();
        assertThat(notification, instanceOf(WorkDoneProgressEnd.class));
        WorkDoneProgressEnd end = (WorkDoneProgressEnd) notification;
        assertEquals(MESSAGE, end.getMessage());
    }

    @Test
    void shouldShowInfo()
    {
        service.showInfo(MESSAGE);

        verify(languageClient).showMessage(messageParamsCaptor.capture());
        MessageParams params = messageParamsCaptor.getValue();
        assertEquals(MessageType.Info, params.getType());
        assertEquals(MESSAGE, params.getMessage());
    }

    @Test
    void shouldShowError()
    {
        service.showError(MESSAGE);

        verify(languageClient).showMessage(messageParamsCaptor.capture());
        MessageParams params = messageParamsCaptor.getValue();
        assertEquals(MessageType.Error, params.getType());
        assertEquals(MESSAGE, params.getMessage());
    }
}
