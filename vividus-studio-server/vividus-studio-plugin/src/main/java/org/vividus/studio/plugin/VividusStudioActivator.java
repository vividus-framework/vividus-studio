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

package org.vividus.studio.plugin;

import java.net.InetAddress;

import com.google.inject.Guice;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.studio.plugin.configuration.VividusStudioModule;
import org.vividus.studio.plugin.server.SocketListener;

public class VividusStudioActivator implements BundleActivator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VividusStudioActivator.class);

    @Override
    public void start(BundleContext context) throws Exception
    {
        logBundleStage("Start", context);

        String host = getPropertySafely("host", context);
        String port = getPropertySafely("port", context);

        Guice.createInjector(new VividusStudioModule())
            .getInstance(SocketListener.class)
            .listen(InetAddress.getByName(host), Integer.parseInt(port));
    }

    private static String getPropertySafely(String property, BundleContext context)
    {
        String propertyValue = context.getProperty(property);
        Validate.isTrue(StringUtils.isNotBlank(propertyValue), "Property '%s' must not be empty", property);
        return propertyValue;
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        logBundleStage("Stop", context);
    }

    private static void logBundleStage(String stage, BundleContext context)
    {
        Bundle bundle = context.getBundle();
        LOGGER.info("{} OSGI bundle with name '{}' and version '{}'", stage, bundle.getSymbolicName(),
                bundle.getVersion());
    }

    public void run(Object arguments) throws Exception
    {
    }
}
