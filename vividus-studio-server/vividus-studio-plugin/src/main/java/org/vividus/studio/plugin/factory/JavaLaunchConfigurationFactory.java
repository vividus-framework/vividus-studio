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

package org.vividus.studio.plugin.factory;

import static org.vividus.studio.plugin.util.RuntimeWrapper.wrapMono;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.debug.internal.core.LaunchConfigurationInfo;
import org.vividus.studio.plugin.exception.VividusStudioException;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Singleton
public class JavaLaunchConfigurationFactory implements LaunchConfigurationFactory
{
    private final LoadingCache<LaunchConfigurationKey, LaunchConfiguration> configurations;
    private final DocumentBuilder documentBuilder;

    public JavaLaunchConfigurationFactory(DocumentBuilder documentBuilder) throws ParserConfigurationException
    {
        this.configurations = CacheBuilder.newBuilder().build(new CacheLoader<>()
        {
            @Override
            public LaunchConfiguration load(LaunchConfigurationKey key)
            {
                return wrapMono(() ->
                {
                    Element configuration = readConfigurationXml(key.getProject(), key.getMain());
                    LaunchConfigurationInfo info = new JavaLaunchConfigurationInfo(configuration);
                    return new JavaLaunchConfiguration(info);
                }, VividusStudioException::new);
            }
        });
        this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    @Override
    public LaunchConfiguration create(String project, String main)
    {
        return configurations.getUnchecked(new LaunchConfigurationKey(project, main));
    }

    private Element readConfigurationXml(String project, String main)
            throws IOException, ParserConfigurationException, SAXException
    {
        try (InputStream inputStream = getClass().getResourceAsStream("/java-launch-config.xml"))
        {
            String xmlTemplate = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            String xml = String.format(xmlTemplate, project, main);
            InputSource source = new InputSource(new StringReader(xml));
            return documentBuilder.parse(source).getDocumentElement();
        }
    }

    private static final class JavaLaunchConfigurationInfo extends LaunchConfigurationInfo
    {
        private JavaLaunchConfigurationInfo(Element configuration) throws CoreException
        {
            initializeFromXML(configuration);
        }
    }

    private static final class JavaLaunchConfiguration extends LaunchConfiguration
    {
        private final LaunchConfigurationInfo launchConfigurationInfo;

        private JavaLaunchConfiguration(LaunchConfigurationInfo launchConfigurationInfo)
        {
            super("Java Launcher", null);
            this.launchConfigurationInfo = launchConfigurationInfo;
        }

        @Override
        public LaunchConfigurationInfo getInfo()
        {
            return this.launchConfigurationInfo;
        }
    }

    private static final class LaunchConfigurationKey
    {
        private final String project;
        private final String main;

        private LaunchConfigurationKey(String project, String main)
        {
            this.project = project;
            this.main = main;
        }

        private String getProject()
        {
            return project;
        }

        private String getMain()
        {
            return main;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(main, project);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!(obj instanceof LaunchConfigurationKey))
            {
                return false;
            }
            LaunchConfigurationKey other = (LaunchConfigurationKey) obj;
            return Objects.equals(main, other.main) && Objects.equals(project, other.project);
        }
    }
}
