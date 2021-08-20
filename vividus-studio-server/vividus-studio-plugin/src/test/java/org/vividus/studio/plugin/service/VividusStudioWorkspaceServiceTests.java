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

package org.vividus.studio.plugin.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.junit.jupiter.api.Test;
import org.vividus.studio.plugin.command.ICommand;

class VividusStudioWorkspaceServiceTests
{
    @Test
    void shouldExecuteCommand()
    {
        ICommand command = mock(ICommand.class);
        String commandKey = "command-key";
        when(command.getName()).thenReturn(commandKey);

        ExecuteCommandParams params = new ExecuteCommandParams(commandKey, List.of());

        VividusStudioWorkspaceService service = new VividusStudioWorkspaceService(Set.of(command));
        service.executeCommand(params);

        verify(command).execute();
    }
}
