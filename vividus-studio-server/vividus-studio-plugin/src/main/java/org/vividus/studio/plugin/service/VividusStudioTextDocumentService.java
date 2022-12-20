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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionTriggerKind;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.vividus.studio.plugin.document.TextDocumentEventListener;

@Singleton
public class VividusStudioTextDocumentService implements TextDocumentService
{
    private final ICompletionItemService completionItemService;
    private final TextDocumentEventListener textDocumentEventListener;
    private final SemanticTokensService semanticTokensService;

    @Inject
    public VividusStudioTextDocumentService(ICompletionItemService completionItemService,
            TextDocumentEventListener textDocumentEventListener, SemanticTokensService semanticTokensService)
    {
        this.completionItemService = completionItemService;
        this.textDocumentEventListener = textDocumentEventListener;
        this.semanticTokensService = semanticTokensService;
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            List<CompletionItem> completionItems = List.of();
            if (completionParams.getContext().getTriggerKind() == CompletionTriggerKind.Invoked)
            {
                String identifier = completionParams.getTextDocument().getUri();
                completionItems = completionItemService.findAllAtPosition(identifier, completionParams.getPosition());
            }
            return Either.forLeft(completionItems);
        });
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved)
    {
        return CompletableFuture.completedFuture(unresolved);
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            String documentIdentifier = params.getTextDocument().getUri();
            List<Integer> semanticTokens = semanticTokensService.getSemanticTokens(documentIdentifier);
            return new SemanticTokens(semanticTokens);
        });
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params)
    {
        textDocumentEventListener.onOpen(params);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params)
    {
        textDocumentEventListener.onChange(params);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params)
    {
        textDocumentEventListener.onClose(params);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params)
    {
    }
}
