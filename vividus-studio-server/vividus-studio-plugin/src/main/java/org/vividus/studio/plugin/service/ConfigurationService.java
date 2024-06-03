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

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.gson.JsonPrimitive;
import com.google.inject.Singleton;

import org.eclipse.lsp4j.ConfigurationItem;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.services.LanguageClient;

@Singleton
public class ConfigurationService
{
    private LanguageClient languageClient;

    @SuppressWarnings("unchecked")
    public String getConfigurationItem(String section) throws InterruptedException, ExecutionException
    {
        ConfigurationItem configurationItem = new ConfigurationItem();
        configurationItem.setSection("vividus-studio." + section);

        ConfigurationParams configurationParams = new ConfigurationParams(List.of(configurationItem));
        return ((JsonPrimitive) languageClient.configuration(configurationParams).get().get(0)).getAsString();
    }

    public void setLanguageClient(LanguageClient languageClient)
    {
        this.languageClient = languageClient;
    }
}
