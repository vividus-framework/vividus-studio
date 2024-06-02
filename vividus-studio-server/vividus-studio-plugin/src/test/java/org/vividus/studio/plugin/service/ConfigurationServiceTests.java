/*-
 * *
 * *
 * Copyright (C) 2020 - 2024 the original author or authors.
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutionException;

import com.google.gson.JsonPrimitive;

import org.eclipse.lsp4j.ConfigurationItem;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConfigurationServiceTests
{
    @Captor private ArgumentCaptor<ConfigurationParams> configurationParamsCaptor;
    @Mock private LanguageClient languageClient;
    @InjectMocks private ConfigurationService service;

    @Test
    void shouldGetConfigurationItem() throws InterruptedException, ExecutionException
    {
        String runnerClass = "org.vividus.runner.StoriesRunner";
        CompletableFuture future = CompletableFuture.completedFuture(List.of(new JsonPrimitive(runnerClass)));
        when(languageClient.configuration(configurationParamsCaptor.capture())).thenReturn(future);

        assertEquals(runnerClass, service.getConfigurationItem("stories-runner"));

        ConfigurationItem item = new ConfigurationItem();
        item.setSection("vividus-studio.stories-runner");
        assertEquals(List.of(item), configurationParamsCaptor.getValue().getItems());
    }
}
