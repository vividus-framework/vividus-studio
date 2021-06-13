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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.buildship.core.GradleBuild;
import org.eclipse.buildship.core.GradleCore;
import org.eclipse.buildship.core.GradleWorkspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.ProjectConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.loader.IJavaProjectLoader.Event;

@ExtendWith(MockitoExtension.class)
class LocalJavaProjectLoaderTests
{
    private static final String PROJECT_NAME = "projectName";
    private static final String PROJECT = ".project";

    @Mock private IWorkspace workspace;
    @Mock private Function<IProject, IJavaProject> projectTransformer;
    @InjectMocks private LocalJavaProjectLoader projectLoader;

    @Test
    void testLoadProject() throws Exception
    {
        IProjectDescription projectDescription = mock(IProjectDescription.class);
        IProject project = mockProject(projectDescription);
        IJavaProject javaProject = mock(IJavaProject.class);

        when(project.exists()).thenReturn(false);
        when(project.hasNature(JavaCore.NATURE_ID)).thenReturn(true);
        when(projectTransformer.apply(project)).thenReturn(javaProject);

        Map<Event, Consumer<String>> events = mockEvents();

        Optional<IJavaProject> loaded = doLoad(getProjectFolder(), events);

        assertTrue(loaded.isPresent());
        IJavaProject output = loaded.get();
        assertEquals(javaProject, output);

        verify(events.get(Event.LOADED)).accept(PROJECT_NAME);
        verify(project).create(projectDescription, IResource.HIDDEN, null);
        verify(project).open(null);
        verifyNoMoreInteractions(events.get(Event.CORRUPTED), workspace, projectTransformer, project);
    }

    @Test
    void testLoadProjectCorrupted() throws Exception
    {
        IProjectDescription projectDescription = mock(IProjectDescription.class);
        IProject project = mockProject(projectDescription);

        when(project.exists()).thenReturn(true);
        when(project.hasNature(JavaCore.NATURE_ID)).thenReturn(false);

        Map<Event, Consumer<String>> events = mockEvents();
        Optional<IJavaProject> loaded = doLoad(getProjectFolder(), events);

        assertTrue(loaded.isEmpty());

        verify(events.get(Event.CORRUPTED)).accept(getProjectFile());
        verify(project).open(null);
        verifyNoMoreInteractions(events.get(Event.LOADED), workspace, projectTransformer, project);
    }

    private Optional<IJavaProject> doLoad(String uri, Map<Event, Consumer<String>> handlers) throws Exception
    {
        try (MockedStatic<GradleCore> gradleCore = mockStatic(GradleCore.class))
        {
            GradleWorkspace gradleWorkspace = mock(GradleWorkspace.class);
            GradleBuild gradleBuild = mock(GradleBuild.class);
            ProjectConnection projectConnection = mock(ProjectConnection.class);
            BuildLauncher buildLauncher = mock(BuildLauncher.class);

            gradleCore.when(GradleCore::getWorkspace).thenReturn(gradleWorkspace);
            when(gradleWorkspace.createBuild(any())).thenReturn(gradleBuild);
            doAnswer(a ->
            {
                Function<ProjectConnection, Object> connectionFunction = a.getArgument(0);
                return connectionFunction.apply(projectConnection);
            }).when(gradleBuild).withConnection(any(), any());
            when(projectConnection.newBuild()).thenReturn(buildLauncher);
            when(buildLauncher.forTasks("eclipse")).thenReturn(buildLauncher);

            Optional<IJavaProject> loaded = projectLoader.load(uri, handlers);

            verify(buildLauncher).run();

            return loaded;
        }
    }

    private IProject mockProject(IProjectDescription projectDescription) throws CoreException
    {
        IProject project = mock(IProject.class);
        IWorkspaceRoot workspaceRoot = mock(IWorkspaceRoot.class);

        when(workspace.loadProjectDescription(new Path(getProjectFile()))).thenReturn(projectDescription);
        when(workspace.getRoot()).thenReturn(workspaceRoot);
        when(projectDescription.getName()).thenReturn(PROJECT_NAME);
        when(workspaceRoot.getProject(PROJECT_NAME)).thenReturn(project);

        return project;
    }

    @SuppressWarnings("unchecked")
    private Map<Event, Consumer<String>> mockEvents()
    {
        return Map.of(
            Event.LOADED, mock(Consumer.class),
            Event.CORRUPTED, mock(Consumer.class)
        );
    }

    private String getProjectFolder()
    {
        return new File(getProjectFile()).getParentFile().getAbsolutePath();
    }

    private String getProjectFile()
    {
        return getClass().getResource(PROJECT).getPath();
    }
}
