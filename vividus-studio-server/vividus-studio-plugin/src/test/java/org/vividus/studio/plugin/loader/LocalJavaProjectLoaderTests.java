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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableRunnable;
import org.eclipse.buildship.core.GradleBuild;
import org.eclipse.buildship.core.GradleCore;
import org.eclipse.buildship.core.GradleWorkspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaModelManager.PerProjectInfo;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.util.ResourceUtils;

@ExtendWith(MockitoExtension.class)
class LocalJavaProjectLoaderTests
{
    private static final String PROJECT_NAME = "projectName";
    private static final String PROJECT = ".project";
    private static final String INFO = "info message";

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

        Consumer<String> onInfo = mock();
        Consumer<String> onLoad = mock();
        Consumer<String> onError = mock();

        mockGradlelBuild(() ->
        {
            Optional<IJavaProject> loaded = projectLoader.load(getProjectFolder(), onInfo, onLoad, onError);
            assertTrue(loaded.isPresent());
            IJavaProject output = loaded.get();
            assertEquals(javaProject, output);
        }, StringUtils.EMPTY);

        verify(onInfo).accept(INFO);
        verify(onLoad).accept(String.format("Project with the name '%s' is loaded", PROJECT_NAME));
        verify(project).create(projectDescription, IResource.HIDDEN, null);
        verify(project).open(null);
        verifyNoMoreInteractions(onError, workspace, projectTransformer, project);
    }

    @Test
    void testLoadProjectCorrupted() throws Exception
    {
        IProjectDescription projectDescription = mock(IProjectDescription.class);
        IProject project = mockProject(projectDescription);

        when(project.exists()).thenReturn(true);
        when(project.hasNature(JavaCore.NATURE_ID)).thenReturn(false);

        Consumer<String> onInfo = mock();
        Consumer<String> onLoad = mock();
        Consumer<String> onError = mock();

        mockGradlelBuild(() ->
        {
            Optional<IJavaProject> loaded = projectLoader.load(getProjectFolder(), onInfo, onLoad, onError);
            assertTrue(loaded.isEmpty());
        }, StringUtils.EMPTY);

        verify(onInfo).accept(INFO);
        verify(onError).accept(String.format("Project file by path '%s' is corrupted", getProjectFile()));
        verify(project).open(null);
        verifyNoMoreInteractions(onLoad, workspace, projectTransformer, project);
    }

    @SuppressWarnings("restriction")
    @Test
    void shouldReloadProject() throws Exception
    {
        IProject project = mock();
        IPath path = mock();
        when(project.getLocation()).thenReturn(path);
        when(path.toFile()).thenReturn(ResourceUtils.asFile(getProjectFolder()));
        JavaModelManager javaModelManager = mock();

        try (MockedStatic<JavaModelManager> javaModelManagerMocked = mockStatic(JavaModelManager.class))
        {
            javaModelManagerMocked.when(() -> JavaModelManager.getJavaModelManager()).thenReturn(javaModelManager);
            PerProjectInfo info = mock();
            when(javaModelManager.getPerProjectInfo(project, true)).thenReturn(info);

            Consumer<String> onInfo = mock();
            Consumer<String> onError = mock();

            String unresolvedDependency = "Could not resolve: org.vividus:vividus-plugin-web-app:7.7.7";
            mockGradlelBuild(() -> projectLoader.reload(project, onInfo, onError), unresolvedDependency);

            verify(onInfo).accept(INFO);
            verify(info).setRawClasspath(null, null, null);
            verify(info).resetResolvedClasspath();
            verify(info).forgetExternalTimestampsAndIndexes();
            verify(project).refreshLocal(IResource.DEPTH_INFINITE, null);
            verify(onError).accept(unresolvedDependency);
        }
    }

    private void mockGradlelBuild(FailableRunnable<Exception> run, String outputs) throws Exception
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
            when(buildLauncher.setStandardOutput(argThat(os ->
            {
                ByteArrayOutputStream baos = (ByteArrayOutputStream) os;
                baos.writeBytes(outputs.getBytes(StandardCharsets.UTF_8));
                return true;
            }))).thenReturn(buildLauncher);
            doAnswer(a -> {
                ProgressListener listener = a.getArgument(0);
                ProgressEvent event = () -> INFO;
                listener.statusChanged(event);
                return null;
            }).when(buildLauncher).addProgressListener(any(ProgressListener.class));

            run.run();

            verify(buildLauncher).run();
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

    private String getProjectFolder()
    {
        return new File(getProjectFile()).getParentFile().getAbsolutePath();
    }

    private String getProjectFile()
    {
        return getClass().getResource(PROJECT).getPath();
    }
}
