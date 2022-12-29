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

package org.vividus.studio.plugin.log;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.service.ClientNotificationService;

@ExtendWith(MockitoExtension.class)
class VividusStudioLogAppenderTests
{
    private static final String VALUE = "value";

    @Mock private ClientNotificationService clientNotificationService;
    @Mock private Filter filter;
    @Mock private Layout<? extends Serializable> layout;
    @Mock private LogEvent event;

    private VividusStudioLogAppender appender;

    @Test
    void shouldAppendMessage()
    {
        appender = VividusStudioLogAppender.createAppender(VALUE, filter, layout);
        appender.setClientNotificationService(clientNotificationService);
        when(layout.toByteArray(event)).thenReturn(VALUE.getBytes(StandardCharsets.UTF_8));

        appender.append(event);

        verify(clientNotificationService).logMessage(VALUE);
    }

    @Test
    void shouldNotAppendMessageIfNotificationMessageIsNotSet()
    {
        appender = VividusStudioLogAppender.createAppender(VALUE, filter, layout);

        appender.append(event);

        verifyNoInteractions(clientNotificationService, event);
    }
}
