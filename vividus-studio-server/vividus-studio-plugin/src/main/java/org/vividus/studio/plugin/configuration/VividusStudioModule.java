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

import java.util.function.Function;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.vividus.studio.plugin.document.TextDocumentEditor;
import org.vividus.studio.plugin.document.TextDocumentEventListener;
import org.vividus.studio.plugin.finder.IStepDefinitionFinder;
import org.vividus.studio.plugin.finder.StepDefinitionFinder;
import org.vividus.studio.plugin.loader.IJavaProjectLoader;
import org.vividus.studio.plugin.loader.LocalJavaProjectLoader;
import org.vividus.studio.plugin.server.SocketListener;
import org.vividus.studio.plugin.server.VividusStudioLanguageServer;
import org.vividus.studio.plugin.service.CompletionItemService;
import org.vividus.studio.plugin.service.ICompletionItemService;
import org.vividus.studio.plugin.service.VividusStudioTextDocumentService;
import org.vividus.studio.plugin.service.VividusStudioWorkspaceService;

public class VividusStudioModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(TextDocumentService.class).to(VividusStudioTextDocumentService.class);
        bind(ICompletionItemService.class).to(CompletionItemService.class);
        bind(SocketListener.class).to(VividusStudioLanguageServer.class);
        bind(WorkspaceService.class).to(VividusStudioWorkspaceService.class);
        bind(IJavaProjectLoader.class).to(LocalJavaProjectLoader.class);
        bind(IWorkspace.class).toProvider(ResourcesPlugin::getWorkspace);
        bind(IStepDefinitionFinder.class).to(StepDefinitionFinder.class);
        bind(TextDocumentEventListener.class).to(TextDocumentEditor.class);
        bind(Key.get(new TypeLiteral<Function<IProject, IJavaProject>>() { }))
            .toProvider(() -> JavaCore::create);
    }
}
