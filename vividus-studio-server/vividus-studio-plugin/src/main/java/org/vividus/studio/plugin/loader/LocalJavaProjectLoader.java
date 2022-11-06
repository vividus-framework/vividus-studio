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

package org.vividus.studio.plugin.loader;

import static java.net.URI.create;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.eclipse.buildship.core.GradleDistribution.forRemoteDistribution;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.buildship.core.BuildConfiguration;
import org.eclipse.buildship.core.GradleBuild;
import org.eclipse.buildship.core.GradleCore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.ProgressListener;
import org.vividus.studio.plugin.exception.VividusStudioException;
import org.vividus.studio.plugin.util.RuntimeWrapper;

@Singleton
public class LocalJavaProjectLoader implements IJavaProjectLoader
{
    private final IWorkspace workspace;
    private final Function<IProject, IJavaProject> projectTransformer;

    @Inject
    public LocalJavaProjectLoader(IWorkspace workspace, Function<IProject, IJavaProject> projectTransformer)
    {
        this.workspace = workspace;
        this.projectTransformer = projectTransformer;
    }

    @Override
    public Optional<IJavaProject> load(String uri, Map<Event, Consumer<String>> handlers)
    {
        return RuntimeWrapper.wrapMono(() ->
        {
            File projectFolder = Paths.get(new URI(uri).getPath()).toFile();

            build(projectFolder, ofNullable(handlers.get(Event.INFO)), ofNullable(handlers.get(Event.ERROR)));

            String absProjectPath = projectFolder.toPath().resolve(".project").toString();
            org.eclipse.core.runtime.Path projectPath = new org.eclipse.core.runtime.Path(absProjectPath);
            IProjectDescription description = workspace.loadProjectDescription(projectPath);
            IProject project = workspace.getRoot().getProject(description.getName());

            if (!project.exists())
            {
                project.create(description, IResource.HIDDEN, null);
            }
            project.open(null);

            if (!project.hasNature(JavaCore.NATURE_ID))
            {
                ofNullable(handlers.get(Event.CORRUPTED)).ifPresent(c -> c.accept(absProjectPath));
                return Optional.empty();
            }

            ofNullable(handlers.get(Event.LOADED)).ifPresent(c -> c.accept(description.getName()));
            return Optional.of(projectTransformer.apply(project));
        }, VividusStudioException::new);
    }

    private static void build(File projectFolder, Optional<Consumer<String>> buildMessageConsumer,
            Optional<Consumer<String>> errorMessageConsumer) throws Exception
    {
        Path propertiesFile = projectFolder.toPath().resolve("gradle.properties");
        String buildSystemVersion = findProperty(propertiesFile, "buildSystemVersion", true, errorMessageConsumer);

        String buildSystemHome = System.getenv("VIVIDUS_BUILD_SYSTEM_HOME");
        if (buildSystemHome == null)
        {
            String buildSystemRootDir = findProperty(propertiesFile, "buildSystemRootDir", false, errorMessageConsumer);
            buildSystemHome = projectFolder.toPath().resolve(buildSystemRootDir != null ? buildSystemRootDir : EMPTY)
                    .toString();
        }

        Path buildSystem = Paths.get(buildSystemHome, buildSystemVersion);
        if (!Files.exists(buildSystem))
        {
            String message = "Neither environment variable \"VIVIDUS_BUILD_SYSTEM_HOME\" is set nor"
                    + " embedded build system is synced. Please check the build system guide at "
                    + "https://github.com/vividus-framework/vividus-build-system or clone this repo "
                    + "recursively: git clone --recursive <git-repository-url>";
            errorMessageConsumer.ifPresent(c -> c.accept(message));
            throw new BuildException(message);
        }

        Path wrapper = buildSystem.resolve("gradle").resolve("wrapper").resolve("gradle-wrapper.properties");
        String distribution = findProperty(wrapper, "distributionUrl", true, errorMessageConsumer);

        BuildConfiguration config = BuildConfiguration.forRootProjectDirectory(projectFolder)
                                                      .overrideWorkspaceConfiguration(true)
                                                      .gradleDistribution(forRemoteDistribution(create(distribution)))
                                                      .build();

        GradleBuild build = GradleCore.getWorkspace().createBuild(config);

        build.withConnection(connection ->
        {
            BuildLauncher launcher = connection.newBuild().forTasks("eclipse");
            buildMessageConsumer.ifPresent(msgConsumer ->
            {
                ProgressListener listener = event -> msgConsumer.accept(event.getDescription());
                launcher.addProgressListener(listener);
            });
            launcher.run();
            return null;
        }, new NullProgressMonitor());
    }

    private static String findProperty(Path propertiesPath, String propertyKey, boolean failOnMissingProperty,
            Optional<Consumer<String>> errorMessageConsumer) throws IOException
    {
        if (!Files.exists(propertiesPath))
        {
            String message = String.format("Unable to find %s file in the project folder",
                    propertiesPath.getFileName());
            errorMessageConsumer.ifPresent(c -> c.accept(message));
            throw new BuildException(message);
        }

        try (InputStream is = Files.newInputStream(propertiesPath))
        {
            Properties properties = new Properties();
            properties.load(is);
            String propertyValue = properties.getProperty(propertyKey);
            if (propertyValue == null && failOnMissingProperty)
            {
                String message = String.format("The %s property is missing in the project properties",
                        propertyKey);
                errorMessageConsumer.ifPresent(c -> c.accept(message));
                throw new BuildException(message);
            }
            return propertyValue;
        }
    }

    private static final class BuildException extends RuntimeException
    {
        private static final long serialVersionUID = -671515108781706516L;

        BuildException(String message)
        {
            super(message);
        }
    }
}
