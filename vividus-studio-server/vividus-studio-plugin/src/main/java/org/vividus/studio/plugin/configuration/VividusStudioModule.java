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

package org.vividus.studio.plugin.configuration;

import com.google.inject.AbstractModule;

import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.vividus.studio.plugin.server.SocketListener;
import org.vividus.studio.plugin.server.VividusStudioLanguageServer;
import org.vividus.studio.plugin.service.VividusStudioTextDocumentService;
import org.vividus.studio.plugin.service.VividusStudioWorkspaceService;

public class VividusStudioModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(TextDocumentService.class).to(VividusStudioTextDocumentService.class);
        bind(SocketListener.class).to(VividusStudioLanguageServer.class);
        bind(WorkspaceService.class).to(VividusStudioWorkspaceService.class);
    }
}
