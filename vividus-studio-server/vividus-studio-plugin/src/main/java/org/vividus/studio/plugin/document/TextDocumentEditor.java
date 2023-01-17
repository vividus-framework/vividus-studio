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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TextDocumentEditor implements TextDocumentEventListener, TextDocumentProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TextDocumentEditor.class);
    private final Map<String, List<String>> textDocuments = new HashMap<>();

    @Override
    public void onOpen(DidOpenTextDocumentParams openEvent)
    {
        TextDocumentItem textDocumentItem = openEvent.getTextDocument();
        List<String> documentLines = split(textDocumentItem.getText());
        /**
         * Add empty line since if we have several empty lines and the end of event text the last one gets trimmed.
         * Added empty line doesn't affect cases where we do not have ending empty lines.
         */
        documentLines.add(StringUtils.EMPTY);
        textDocuments.put(textDocumentItem.getUri(), documentLines);
    }

    @Override
    public void onChange(DidChangeTextDocumentParams changeEvent)
    {
        String uri = changeEvent.getTextDocument().getUri();
        List<String> documentLines = textDocuments.get(uri);
        changeEvent.getContentChanges().forEach(e ->
        {
            boolean outcome = handleEvent(e, documentLines);
            if (!outcome)
            {
                LOGGER.atError().addArgument(System::lineSeparator)
                                .addArgument(changeEvent)
                                .log("Unable to handle change event:{}{}");
            }
        });
    }

    @Override
    public void onClose(DidCloseTextDocumentParams closeEvent)
    {
        String uri = closeEvent.getTextDocument().getUri();
        textDocuments.remove(uri);
    }

    @Override
    public List<String> getTextDocument(String identifier)
    {
        List<String> text = textDocuments.get(identifier);
        return text == null ? List.of() : new ArrayList<>(text);
    }

    private boolean handleEvent(TextDocumentContentChangeEvent event, List<String> container)
    {
        String eventText = event.getText();

        Position startPos = event.getRange().getStart();
        Position endPos = event.getRange().getEnd();

        String startLine = container.get(startPos.getLine());

        // handle new line insertion
        if (System.lineSeparator().equals(eventText))
        {
            int lineIndex = startPos.getLine();
            String currentLine = startLine.substring(0, startPos.getCharacter());
            String nextLine = startLine.substring(startPos.getCharacter());
            container.set(lineIndex, currentLine);
            container.add(lineIndex + 1, nextLine);
            return true;
        }

        List<String> tokens = split(eventText);

        // handle insertion of text with several lines
        if (tokens.size() > 1)
        {
            String endLine = container.get(endPos.getLine());

            String startToken = startLine.substring(0, startPos.getCharacter());
            String endToken = endLine.substring(endPos.getCharacter());

            List<String> bulk = new ArrayList<>();
            bulk.add(startToken + tokens.remove(0));
            bulk.addAll(tokens.subList(0, tokens.size() - 1));
            bulk.add(tokens.get(tokens.size() - 1) + endToken);

            insertLines(bulk, container, startPos, endPos);

            return true;
        }

        // handle insertion/removal of characters at the same line
        if (startPos.getLine() == endPos.getLine())
        {
            container.set(startPos.getLine(), positionalConcat(startLine, startPos, endPos, eventText));
            return true;
        }

        // handle removal across multiple lines
        if (startPos.getLine() < endPos.getLine())
        {
            String endLine = container.get(endPos.getLine());

            insertLines(List.of(positionalConcat(startLine, startPos, endLine, endPos, eventText)), container, startPos,
                    endPos);
            return true;
        }

        return false;
    }

    private static void insertLines(List<String> lines, List<String> container, Position startPos, Position endPos)
    {
        int lineDiff = endPos.getLine() - startPos.getLine() + 1;
        int deleteAt = startPos.getLine();

        IntStream.range(0, lineDiff).forEach(i -> container.remove(deleteAt));

        container.addAll(startPos.getLine(), lines);
    }

    private static String positionalConcat(String startLine, Position startPos, String endLine, Position endPos,
            String concatToken)
    {
        return startLine.substring(0, startPos.getCharacter()) + concatToken + endLine.substring(endPos.getCharacter());
    }

    private static String positionalConcat(String line, Position startPos, Position endPos, String concatToken)
    {
        return positionalConcat(line, startPos, line, endPos, concatToken);
    }

    private static List<String> split(String text)
    {
        return Stream.of(text.split("\\R", -1)).collect(Collectors.toList());
    }
}
