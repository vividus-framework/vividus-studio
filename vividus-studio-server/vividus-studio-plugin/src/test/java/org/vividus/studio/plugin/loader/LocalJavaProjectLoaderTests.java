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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.buildship.core.BuildConfiguration;
import org.eclipse.buildship.core.GradleBuild;
import org.eclipse.buildship.core.GradleCore;
import org.eclipse.buildship.core.GradleWorkspace;
import org.eclipse.buildship.core.RemoteGradleDistribution;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.exception.VividusStudioException;
import org.vividus.studio.plugin.loader.IJavaProjectLoader.Event;

@ExtendWith(MockitoExtension.class)
class LocalJavaProjectLoaderTests
{
    private static final String PROJECT_NAME = "projectName";
    private static final String REGULAR_PROJECT = "regular-project";

    @Mock private IWorkspace workspace;
    @Mock private Function<IProject, IJavaProject> projectTransformer;
    @InjectMocks private LocalJavaProjectLoader projectLoader;

    @Test
    void testLoadProject() throws Exception
    {
        IProjectDescription projectDescription = mock(IProjectDescription.class);
        IProject project = mockProject(REGULAR_PROJECT, projectDescription);
        IJavaProject javaProject = mock(IJavaProject.class);

        when(project.exists()).thenReturn(false);
        when(project.hasNature(JavaCore.NATURE_ID)).thenReturn(true);
        when(projectTransformer.apply(project)).thenReturn(javaProject);

        Map<Event, Consumer<String>> events = mockEvents();

        Optional<IJavaProject> loaded = doLoad(getProjectFolder(REGULAR_PROJECT), events);

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
        IProject project = mockProject(REGULAR_PROJECT, projectDescription);

        when(project.exists()).thenReturn(true);
        when(project.hasNature(JavaCore.NATURE_ID)).thenReturn(false);

        Map<Event, Consumer<String>> events = mockEvents();
        Optional<IJavaProject> loaded = doLoad(getProjectFolder(REGULAR_PROJECT), events);

        assertTrue(loaded.isEmpty());

        verify(events.get(Event.CORRUPTED)).accept(getProjectFile(REGULAR_PROJECT));
        verify(project).open(null);
        verifyNoMoreInteractions(events.get(Event.LOADED), workspace, projectTransformer, project);
    }

    @SuppressWarnings("unchecked")
    @CsvSource({
        "empty-project, Unable to find gradle.properties file in the project folder",
        "empty-version-project, The buildSystemVersion property is missing in the project properties",
        "empty-root-dir-project, Neither environment variable \"VIVIDUS_BUILD_SYSTEM_HOME\" is set nor embedded build"
        + " system is synced. Please check the build system guide at https://github.com/vividus-framework/"
        + "vividus-build-system or clone this repo recursively: git clone --recursive <git-repository-url>"
    })
    @ParameterizedTest
    void testLoadProjectWithInvalidConfiguration(String baseFolder, String error) throws Exception
    {
        Consumer<String> errorConsumer = mock(Consumer.class);
        Map<Event, Consumer<String>> handlers = Map.of(Event.ERROR, errorConsumer);

        String projectFolder = getProjectFolder(baseFolder);
        VividusStudioException thrown = assertThrows(VividusStudioException.class,
                () -> projectLoader.load(projectFolder, handlers));
        assertEquals(error, thrown.getCause().getMessage());
        verify(errorConsumer).accept(error);
    }

    private Optional<IJavaProject> doLoad(String uri, Map<Event, Consumer<String>> handlers) throws Exception
    {
        try (MockedStatic<GradleCore> gradleCore = mockStatic(GradleCore.class))
        {
            GradleWorkspace gradleWorkspace = mock(GradleWorkspace.class);
            GradleBuild gradleBuild = mock(GradleBuild.class);
            ProjectConnection projectConnection = mock(ProjectConnection.class);
            BuildLauncher buildLauncher = mock(BuildLauncher.class);

            ArgumentCaptor<BuildConfiguration> configurationCaptor = ArgumentCaptor.forClass(BuildConfiguration.class);
            gradleCore.when(GradleCore::getWorkspace).thenReturn(gradleWorkspace);
            when(gradleWorkspace.createBuild(configurationCaptor.capture())).thenReturn(gradleBuild);
            doAnswer(a ->
            {
                Function<ProjectConnection, Object> connectionFunction = a.getArgument(0);
                return connectionFunction.apply(projectConnection);
            }).when(gradleBuild).withConnection(any(), any());
            when(projectConnection.newBuild()).thenReturn(buildLauncher);
            when(buildLauncher.forTasks("eclipse")).thenReturn(buildLauncher);

            Optional<IJavaProject> loaded = projectLoader.load(uri, handlers);

            verify(buildLauncher).run();
            BuildConfiguration buildConfig = configurationCaptor.getValue();
            assertTrue(buildConfig.isOverrideWorkspaceConfiguration());
            RemoteGradleDistribution distribution = (RemoteGradleDistribution) buildConfig.getGradleDistribution();
            assertEquals("https://services.gradle.org/distributions/gradle-7.5.1-bin.zip",
                    distribution.getUrl().toString());

            return loaded;
        }
    }

    private IProject mockProject(String baseFolder, IProjectDescription projectDescription) throws CoreException
    {
        IProject project = mock(IProject.class);
        IWorkspaceRoot workspaceRoot = mock(IWorkspaceRoot.class);

        when(workspace.loadProjectDescription(new Path(getProjectFile(baseFolder))))
                .thenReturn(projectDescription);
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
            Event.CORRUPTED, mock(Consumer.class),
            Event.ERROR, mock(Consumer.class)
        );
    }

    private String getProjectFolder(String baseFolder)
    {
        return new File(getProjectFile(baseFolder)).getParentFile().getAbsolutePath();
    }

    private String getProjectFile(String baseFolder)
    {
        String resourcePath = Paths.get(baseFolder, ".project").toString();
        return getClass().getResource(resourcePath).getPath();
    }
}
