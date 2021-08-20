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

package org.vividus.studio.plugin.service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.vividus.studio.plugin.command.ICommand;

@Singleton
public class VividusStudioWorkspaceService implements WorkspaceService
{
    private final Set<ICommand> commands;

    @Inject
    public VividusStudioWorkspaceService(Set<ICommand> commands)
    {
        this.commands = commands;
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params)
    {
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params)
    {
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params)
    {
        String commandName = params.getCommand();
        return commands.stream().filter(cmd -> cmd.getName().equals(commandName))
                                .findFirst()
                                .get()
                                .execute();
    }
}
