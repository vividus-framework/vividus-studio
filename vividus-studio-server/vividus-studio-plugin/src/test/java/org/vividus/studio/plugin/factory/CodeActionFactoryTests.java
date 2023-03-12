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

package org.vividus.studio.plugin.factory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeActionTriggerKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.factory.CodeActionFactory.InsertStepParameters;

@ExtendWith(MockitoExtension.class)
class CodeActionFactoryTests
{
    @InjectMocks private CodeActionFactory codeActionFactory;

    @Test
    void shouldCreateCodeActions()
    {
        CodeActionParams params = createBaseParams(CodeActionTriggerKind.Invoked);
        Position position = new Position(7, 0);
        params.setRange(new Range(position, position));
        String identifier = "identifier";
        params.setTextDocument(new TextDocumentIdentifier(identifier));

        List<Either<Command, CodeAction>> codeActions = codeActionFactory.createCodeActions(params);

        assertThat(codeActions, hasSize(1));

        CodeAction insertStepAction = codeActions.get(0).getRight();
        assertEquals(CodeActionKind.Source, insertStepAction.getKind());
        assertEquals("Insert Step...", insertStepAction.getTitle());
        Command command = insertStepAction.getCommand();
        assertEquals("vividus.action.insertStep", command.getCommand());
        List<Object> args = command.getArguments();
        assertThat(args, hasSize(1));
        InsertStepParameters insertParams = (InsertStepParameters) args.get(0);
        assertEquals(identifier, insertParams.getUri());
        assertEquals(position, insertParams.getPosition());
    }

    @Test
    void shouldReturnEmptyCodeActionsOnAutomaticTrigger()
    {
        assertThat(codeActionFactory.createCodeActions(createBaseParams(CodeActionTriggerKind.Automatic)), empty());
    }

    @Test
    void shouldReturnEmptyCodeActionsOnUnsupportedPositions()
    {
        CodeActionParams params = createBaseParams(CodeActionTriggerKind.Invoked);
        params.setRange(new Range(new Position(1, 2), new Position(3, 4)));

        assertThat(codeActionFactory.createCodeActions(params), empty());
    }

    private CodeActionParams createBaseParams(CodeActionTriggerKind kind)
    {
        CodeActionParams params = new CodeActionParams();
        CodeActionContext context = new CodeActionContext();
        context.setTriggerKind(kind);
        params.setContext(context);
        return params;
    }
}
