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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.CompletionContext;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionTriggerKind;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.document.TextDocumentEventListener;

@ExtendWith(MockitoExtension.class)
class VividusStudioTextDocumentServiceTests
{
    private static final String TEXT_DOCUMENT_IDENTIFIER = "identifier";

    @Mock private ICompletionItemService completionItemService;
    @Mock private TextDocumentEventListener textDocumentEventListener;
    @Mock private SemanticTokensService semanticTokensService;
    @InjectMocks private VividusStudioTextDocumentService textDocumentService;

    @BeforeEach
    void init()
    {
        this.textDocumentService = new VividusStudioTextDocumentService(completionItemService,
                Set.of(textDocumentEventListener), semanticTokensService);
    }

    @Test
    void testCompletionInvoked() throws InterruptedException, ExecutionException
    {
        CompletionParams params = mock(CompletionParams.class);
        CompletionContext context = mock(CompletionContext.class);
        CompletionItem item = mockCompletionItem("text");
        TextDocumentIdentifier identifier = mock(TextDocumentIdentifier.class);
        Position position = mock(Position.class);

        when(params.getContext()).thenReturn(context);
        when(context.getTriggerKind()).thenReturn(CompletionTriggerKind.Invoked);
        when(params.getTextDocument()).thenReturn(identifier);
        when(identifier.getUri()).thenReturn(TEXT_DOCUMENT_IDENTIFIER);
        when(params.getPosition()).thenReturn(position);
        when(completionItemService.findAllAtPosition(TEXT_DOCUMENT_IDENTIFIER, position)).thenReturn(List.of(item));

        List<CompletionItem> items = textDocumentService.completion(params).get().getLeft();

        assertEquals(List.of(item), items);
        verifyNoMoreInteractions(completionItemService, context, params, item);
    }

    @Test
    void testResolveCompletionItem() throws InterruptedException, ExecutionException
    {
        CompletionItem item = mock(CompletionItem.class);
        CompletionItem outputItem = textDocumentService.resolveCompletionItem(item).get();
        assertEquals(item, outputItem);
        verifyNoInteractions(completionItemService, item);
    }

    @Test
    void testDidOpen()
    {
        DidOpenTextDocumentParams docParams = mock(DidOpenTextDocumentParams.class);
        textDocumentService.didOpen(docParams);
        verify(textDocumentEventListener).onOpen(docParams);
        verifyNoMoreInteractions(textDocumentEventListener);
        verifyNoInteractions(docParams, completionItemService);
    }

    @Test
    void testDidChange()
    {
        DidChangeTextDocumentParams docParams = mockDidChange(StringUtils.EMPTY, 0);
        textDocumentService.didChange(docParams);
        verify(textDocumentEventListener).onChange(docParams);
        verify(docParams).getContentChanges();
        verifyNoMoreInteractions(docParams, textDocumentEventListener);
        verifyNoInteractions(completionItemService);
    }

    @Test
    void testDidClose()
    {
        DidCloseTextDocumentParams docParams = mock(DidCloseTextDocumentParams.class);
        textDocumentService.didClose(docParams);
        verify(textDocumentEventListener).onClose(docParams);
        verifyNoMoreInteractions(textDocumentEventListener);
        verifyNoInteractions(docParams, completionItemService);
    }

    @Test
    void testDidSave()
    {
        DidSaveTextDocumentParams docParams = mock(DidSaveTextDocumentParams.class);
        textDocumentService.didSave(docParams);
        verifyNoInteractions(docParams, completionItemService);
    }

    @ParameterizedTest
    @CsvSource({
        "'', 0",
        "new text, 1"
    })
    void shouldFilterCompletionItemsBasedOnInvocationType(String newText, int expectedSize)
            throws InterruptedException, ExecutionException
    {
        CompletionParams params = mock(CompletionParams.class);
        CompletionContext context = mock(CompletionContext.class);
        CompletionItem item = mockCompletionItem(newText);
        TextDocumentIdentifier identifier = mock(TextDocumentIdentifier.class);
        Position position = spy(Position.class);
        when(position.getCharacter()).thenReturn(11);

        when(params.getContext()).thenReturn(context);
        when(context.getTriggerKind()).thenReturn(CompletionTriggerKind.Invoked);
        when(params.getTextDocument()).thenReturn(identifier);
        when(identifier.getUri()).thenReturn(TEXT_DOCUMENT_IDENTIFIER);
        when(params.getPosition()).thenReturn(position);
        when(completionItemService.findAllAtPosition(TEXT_DOCUMENT_IDENTIFIER, position)).thenReturn(List.of(item));

        DidChangeTextDocumentParams docParams = mockDidChange("O", 10);
        textDocumentService.didChange(docParams);

        List<CompletionItem> items = textDocumentService.completion(params).get().getLeft();

        assertThat(items, hasSize(expectedSize));
    }

    private static DidChangeTextDocumentParams mockDidChange(String text, int character)
    {
        DidChangeTextDocumentParams docParams = mock(DidChangeTextDocumentParams.class);
        TextDocumentContentChangeEvent changeEvent = mock(TextDocumentContentChangeEvent.class);
        when(docParams.getContentChanges()).thenReturn(List.of(changeEvent));
        when(changeEvent.getText()).thenReturn(text);
        Range range = mock(Range.class);
        when(changeEvent.getRange()).thenReturn(range);
        Position position = mock(Position.class);
        when(range.getStart()).thenReturn(position);
        when(position.getCharacter()).thenReturn(character);
        return docParams;
    }

    private static CompletionItem mockCompletionItem(String text)
    {
        CompletionItem item = mock(CompletionItem.class);
        TextEdit textEdit = mock(TextEdit.class);
        when(item.getTextEdit()).thenReturn(Either.forLeft(textEdit));
        when(textEdit.getNewText()).thenReturn(text);
        return item;
    }
}
