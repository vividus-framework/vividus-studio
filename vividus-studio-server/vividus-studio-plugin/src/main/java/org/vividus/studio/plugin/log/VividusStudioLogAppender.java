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

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.vividus.studio.plugin.service.ClientNotificationService;

@Plugin(category = Core.CATEGORY_NAME,
        name = "VividusStudioLogAppender",
        elementType = Appender.ELEMENT_TYPE,
        printObject = true)
public class VividusStudioLogAppender extends AbstractAppender
{
    private static VividusStudioLogAppender instance;
    private ClientNotificationService clientNotificationService;

    protected VividusStudioLogAppender(String name, Filter filter, Layout<? extends Serializable> layout)
    {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    @PluginFactory
    public static VividusStudioLogAppender createAppender(@PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter, @PluginElement("Layout") Layout<? extends Serializable> layout)
    {
        instance = new VividusStudioLogAppender(name, filter, layout);
        return instance;
    }

    @Override
    public void append(LogEvent event)
    {
        if (clientNotificationService != null)
        {
            String logEntry = new String(getLayout().toByteArray(event), StandardCharsets.UTF_8);
            clientNotificationService.logMessage(logEntry.strip());
        }
    }

    public static VividusStudioLogAppender getInstance()
    {
        return instance;
    }

    public void setClientNotificationService(ClientNotificationService clientNotificationService)
    {
        this.clientNotificationService = clientNotificationService;
    }
}
