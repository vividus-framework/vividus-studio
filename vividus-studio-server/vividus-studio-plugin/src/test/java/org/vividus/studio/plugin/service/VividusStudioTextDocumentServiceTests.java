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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionContext;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionTriggerKind;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VividusStudioTextDocumentServiceTests
{
    @Mock private ICompletionItemService completionItemService;
    @InjectMocks private VividusStudioTextDocumentService textDocumentService;

    @Test
    void testCompletion() throws InterruptedException, ExecutionException
    {
        CompletionParams params = mock(CompletionParams.class);
        CompletionContext context = mock(CompletionContext.class);
        CompletionItem item = mock(CompletionItem.class);
        String triggerCharacter = "W";

        when(params.getContext()).thenReturn(context);
        when(context.getTriggerKind()).thenReturn(CompletionTriggerKind.TriggerCharacter);
        when(context.getTriggerCharacter()).thenReturn(triggerCharacter);
        when(completionItemService.findAll(triggerCharacter)).thenReturn(List.of(item));

        List<CompletionItem> items = textDocumentService.completion(params).get().getLeft();

        assertEquals(List.of(item), items);
        verifyNoMoreInteractions(completionItemService, context, params, item);
    }

    @Test
    void testResolveCompletionItem() throws InterruptedException, ExecutionException
    {
        CompletionItem unresolvedItem = mock(CompletionItem.class);
        CompletionItem resolvedItem = mock(CompletionItem.class);
        when(completionItemService.findOne(unresolvedItem)).thenReturn(resolvedItem);
        CompletionItem outputItem = textDocumentService.resolveCompletionItem(unresolvedItem).get();
        assertEquals(resolvedItem, outputItem);
        verifyNoMoreInteractions(completionItemService, resolvedItem, unresolvedItem);
    }

    @Test
    void testDidOpen()
    {
        DidOpenTextDocumentParams docParams = mock(DidOpenTextDocumentParams.class);
        textDocumentService.didOpen(docParams);
        verifyNoInteractions(docParams, completionItemService);
    }

    @Test
    void testDidChange()
    {
        DidChangeTextDocumentParams docParams = mock(DidChangeTextDocumentParams.class);
        textDocumentService.didChange(docParams);
        verifyNoInteractions(docParams, completionItemService);
    }

    @Test
    void testDidClose()
    {
        DidCloseTextDocumentParams docParams = mock(DidCloseTextDocumentParams.class);
        textDocumentService.didClose(docParams);
        verifyNoInteractions(docParams, completionItemService);
    }

    @Test
    void testDidSave()
    {
        DidSaveTextDocumentParams docParams = mock(DidSaveTextDocumentParams.class);
        textDocumentService.didSave(docParams);
        verifyNoInteractions(docParams, completionItemService);
    }
}
