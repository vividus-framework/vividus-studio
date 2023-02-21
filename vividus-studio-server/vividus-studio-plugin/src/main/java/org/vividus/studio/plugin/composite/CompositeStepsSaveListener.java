/*-
 * *
 * *
 * Copyright (C) 2020 - 2023 the original author or authors.
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

package org.vividus.studio.plugin.composite;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.vividus.studio.plugin.configuration.VividusStudioEnvronment;
import org.vividus.studio.plugin.document.TextDocumentEventListener;
import org.vividus.studio.plugin.document.TextDocumentProvider;
import org.vividus.studio.plugin.factory.StepDefinitionFactory;
import org.vividus.studio.plugin.model.StepDefinition;
import org.vividus.studio.plugin.service.IStepDefinitionsAware;
import org.vividus.studio.plugin.util.ResourceUtils;

@Singleton
public class CompositeStepsSaveListener implements TextDocumentEventListener
{
    private final TextDocumentProvider textDocumentProvider;
    private final StepDefinitionFactory stepDefinitionFactory;
    private final VividusStudioEnvronment vividusStudioEnvronment;
    private final IStepDefinitionsAware stepDefinitionsAware;

    @Inject
    public CompositeStepsSaveListener(TextDocumentProvider textDocumentProvider,
            StepDefinitionFactory stepDefinitionFactory, VividusStudioEnvronment vividusStudioEnvronment,
            IStepDefinitionsAware stepDefinitionsAware)
    {
        this.textDocumentProvider = textDocumentProvider;
        this.stepDefinitionFactory = stepDefinitionFactory;
        this.vividusStudioEnvronment = vividusStudioEnvronment;
        this.stepDefinitionsAware = stepDefinitionsAware;
    }

    @Override
    public void onSave(DidSaveTextDocumentParams params)
    {
        String documentUri = params.getTextDocument().getUri();
        if (ResourceUtils.isCompositeFile(documentUri))
        {
            String document = textDocumentProvider.getTextDocument(documentUri).stream()
                    .collect(Collectors.joining(System.lineSeparator()));
            Path documentPath = ResourceUtils.asFile(documentUri).toPath();
            Path resourcesPath = ResourceUtils.resolveResourcesPath(vividusStudioEnvronment.getProject());
            String location = resourcesPath.relativize(documentPath).toString();

            List<StepDefinition> composites = CompositeStepParser.parse(document)
                               .map(cs -> stepDefinitionFactory.createStepDefinition(location, cs.getName(),
                                       cs.getBody(), true, true))
                               .collect(Collectors.toList());

            stepDefinitionsAware.refresh(composites);
        }
    }
}
