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

package org.vividus.studio.plugin.document;

import static java.lang.System.lineSeparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TextDocumentEditorTests
{
    private static final String ABOUT = "VIVIDUS is a continuously growing collection of testing actions and"
            + System.lineSeparator() + "utilities combined into a one-box application to enable human-readable"
            + System.lineSeparator() +  "tests.";
    private static final String ID = "document-id";

    private static DidOpenTextDocumentParams OPEN_EVENT;

    @BeforeAll
    static void loadBaseDocument() throws IOException
    {
        String document = loadDocument("base.txt");
        OPEN_EVENT = new DidOpenTextDocumentParams(new TextDocumentItem(ID, StringUtils.EMPTY, 0, document));
    }

    static Stream<Arguments> changeDocumentEvents()
    {
        return Stream.of(
            arguments(event(range(position(4, 10), position(4, 10)), 0, "Simple example of "),
                    "singlelineinsert/data.txt"),
            arguments(event(range(position(6, 83), position(6, 92)), 9, "[A-Za-z0-9]{10}"),
                    "singlelinereplace/data.txt"),
            arguments(event(range(position(1, 10), position(1, 25)), 15, ""), "singlelinedelete/data.txt"),
            arguments(event(range(position(6, 96), position(6, 96)), 0, lineSeparator()), "addnewline/data.txt"),
            arguments(event(range(position(7, 34), position(11, 46)), 310, ""), "multilinedelete/data.txt"),
            arguments(event(range(position(4, 10), position(8, 53)), 209, ABOUT), "multilinereplace/data.txt"),
            arguments(event(range(position(5, 0), position(5, 0)), 0, ABOUT + System.lineSeparator()),
                    "multilineinsert/data.txt")
        );
    }

    @MethodSource("changeDocumentEvents")
    @ParameterizedTest
    void testUpdateTextDocument(TextDocumentContentChangeEvent event, String document) throws IOException
    {
        TextDocumentEditor textDocumentEditor = new TextDocumentEditor();
        textDocumentEditor.onOpen(OPEN_EVENT);

        textDocumentEditor
                .onChange(new DidChangeTextDocumentParams(new VersionedTextDocumentIdentifier(ID, 0), List.of(event)));

        assertEquals(loadDocument(document) + lineSeparator(),
                textDocumentEditor.getTextDocument(ID).stream().collect(Collectors.joining(lineSeparator())));

        DidCloseTextDocumentParams closeEvent = new DidCloseTextDocumentParams(new TextDocumentIdentifier(ID));
        textDocumentEditor.onClose(closeEvent);
        assertThat(textDocumentEditor.getTextDocument(ID), is(empty()));
    }

    private static Position position(int line, int character)
    {
        return new Position(line, character);
    }

    private static Range range(Position start, Position end)
    {
        return new Range(start, end);
    }

    private static TextDocumentContentChangeEvent event(Range range, Integer rangeLength, String text)
    {
        return new TextDocumentContentChangeEvent(range, rangeLength, text);
    }

    private static String loadDocument(String document) throws IOException
    {
        URL documentUrl = TextDocumentEditorTests.class.getClassLoader()
                .getResource("org/vividus/studio/plugin/document/" + document);
        return IOUtils.toString(documentUrl, StandardCharsets.UTF_8);
    }
}
