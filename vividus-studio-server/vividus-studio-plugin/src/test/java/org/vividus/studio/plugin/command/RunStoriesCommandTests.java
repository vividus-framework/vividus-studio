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

package org.vividus.studio.plugin.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.configuration.VividusStudioEnvronment;
import org.vividus.studio.plugin.factory.LaunchConfigurationFactory;
import org.vividus.studio.plugin.service.ClientNotificationService;

@ExtendWith(MockitoExtension.class)
class RunStoriesCommandTests
{
    @Mock private ClientNotificationService clientNotificationService;
    @Mock private LaunchConfigurationFactory launchConfigurationFactory;
    @Mock private IProject project;
    private RunStoriesCommand command;

    @Test
    void shouldRunStories() throws InterruptedException, ExecutionException, CoreException
    {
        String projectName = "project-name";
        VividusStudioEnvronment configuration = new VividusStudioEnvronment();
        configuration.setProject(project);
        when(project.getName()).thenReturn(projectName);

        LaunchConfiguration launchConfiguration = mock(LaunchConfiguration.class);
        when(launchConfigurationFactory.create(projectName, "org.vividus.runner.StoriesRunner"))
                .thenReturn(launchConfiguration);

        ILaunch launch = mock(ILaunch.class);
        when(launchConfiguration.launch(ILaunchManager.RUN_MODE, null, true)).thenReturn(launch);

        Either<String, Integer> token = Either.forLeft("token");
        when(clientNotificationService.createProgress()).thenReturn(CompletableFuture.completedFuture(token));

        IProcess launchProcess = mock(IProcess.class);
        when(launch.getProcesses()).thenReturn(new IProcess[] { launchProcess });

        IStreamsProxy streamsProxy = mock(IStreamsProxy.class);
        when(launchProcess.getStreamsProxy()).thenReturn(streamsProxy);
        IStreamMonitor streamMonitor = mock(IStreamMonitor.class);
        when(streamsProxy.getOutputStreamMonitor()).thenReturn(streamMonitor);
        String message = "message";
        doAnswer(a -> {
            IStreamListener listener = a.getArgument(0, IStreamListener.class);
            listener.streamAppended("\033[31m" + message + "\033[m", null);
            return null;
        }).when(streamMonitor).addListener(any());

        when(launch.isTerminated()).thenReturn(false).thenReturn(true);

        command = new RunStoriesCommand(clientNotificationService, launchConfigurationFactory, configuration);
        assertEquals(0, command.execute().get());

        verify(project).refreshLocal(IResource.DEPTH_INFINITE, null);
        verify(clientNotificationService).startProgress(token, "Run Stories", "Running...");
        verify(clientNotificationService).endProgress(token, "Completed");
        verify(clientNotificationService).logMessage(message);
    }

    @Test
    void shouldReturnName()
    {
        command = new RunStoriesCommand(clientNotificationService, launchConfigurationFactory, null);
        assertEquals("vividus.runStories", command.getName());
    }
}
