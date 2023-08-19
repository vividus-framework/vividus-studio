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

import static java.util.Optional.ofNullable;

import java.io.File;
import java.util.Map;
import java.util.Optional;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.ProgressListener;
import org.vividus.studio.plugin.exception.VividusStudioException;
import org.vividus.studio.plugin.util.ResourceUtils;
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
            File projectFolder = ResourceUtils.asFile(uri);

            build(projectFolder, ofNullable(handlers.get(Event.INFO)));

            String absProjectPath = projectFolder.toPath().resolve(".project").toString();
            Path projectPath = new Path(absProjectPath);
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

    @SuppressWarnings("restriction")
    public void reload(IProject project, Consumer<String> messageConsumer) throws Exception
    {
        JavaModelManager.PerProjectInfo perProjectInfo = JavaModelManager.getJavaModelManager()
                .getPerProjectInfo(project, true);
        perProjectInfo.setRawClasspath(null, null, null);
        perProjectInfo.resetResolvedClasspath();
        perProjectInfo.forgetExternalTimestampsAndIndexes();

        build(project.getLocation().toFile(), Optional.of(messageConsumer));
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    private static void build(File projectFolder, Optional<Consumer<String>> messageConsumer) throws Exception
    {
        BuildConfiguration config = BuildConfiguration.forRootProjectDirectory(projectFolder)
                                                      .build();

        GradleBuild build = GradleCore.getWorkspace().createBuild(config);

        build.withConnection(connection ->
        {
            BuildLauncher launcher = connection.newBuild().forTasks("eclipse");
            messageConsumer.ifPresent(msgConsumer ->
            {
                ProgressListener listener = event -> msgConsumer.accept(event.getDescription());
                launcher.addProgressListener(listener);
            });
            launcher.run();
            return null;
        }, new NullProgressMonitor());
    }
}
