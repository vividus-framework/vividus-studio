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

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.vividus.studio.plugin.util.RuntimeWrapper.wrap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.vividus.studio.plugin.configuration.VividusStudioConfiguration;
import org.vividus.studio.plugin.exception.VividusStudioException;
import org.vividus.studio.plugin.factory.LaunchConfigurationFactory;
import org.vividus.studio.plugin.service.ClientNotificationService;

@Singleton
public class RunStoriesCommand implements ICommand
{
    private final ClientNotificationService clientNotificationService;
    private final LaunchConfigurationFactory launchConfigurationFactory;
    private final VividusStudioConfiguration vividusStudioConfiguration;

    @Inject
    public RunStoriesCommand(ClientNotificationService clientNotificationService,
            LaunchConfigurationFactory launchConfigurationFactory,
            VividusStudioConfiguration vividusStudioConfiguration)
    {
        this.clientNotificationService = clientNotificationService;
        this.launchConfigurationFactory = launchConfigurationFactory;
        this.vividusStudioConfiguration = vividusStudioConfiguration;
    }

    @Override
    public CompletableFuture<Object> execute()
    {
        return supplyAsync(() -> clientNotificationService.createProgress().thenAccept(token -> wrap(() ->
        {
            clientNotificationService.startProgress(token, "Run Stories", "Running...");
            LaunchConfiguration runner = launchConfigurationFactory.create(vividusStudioConfiguration.getProjectName(),
                    "org.vividus.runner.StoriesRunner");
            ILaunch launch = runner.launch(ILaunchManager.RUN_MODE, null, true);
            IProcess launchProcess = launch.getProcesses()[0];

            IStreamMonitor streamMonitor = launchProcess.getStreamsProxy().getOutputStreamMonitor();
            streamMonitor.addListener((text, montor) ->
            {
                String messsage = removeEnd(text, "[m");
                messsage = removeStart(messsage, "\033");
                messsage = messsage.replaceFirst("^\\[..+?m", "").strip();

                clientNotificationService.logMessage(messsage);
            });

            while (!launch.isTerminated())
            {
                TimeUnit.SECONDS.sleep(1);
            }

            clientNotificationService.endProgress(token, "Completed");
        }, VividusStudioException::new)));
    }

    @Override
    public String getName()
    {
        return "vividus.runStories";
    }
}
