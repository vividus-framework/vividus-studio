package org.vividus.studio.plugin.factory;

import java.util.List;

import com.google.inject.Singleton;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeActionTriggerKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

@Singleton
public class CodeActionFactory
{
    /**
     * Creates a list of code actions appropriate for given code action params
     *
     * @param params code action params
     * @return list of code actions
     */
    public List<Either<Command, CodeAction>> createCodeActions(CodeActionParams params)
    {
        CodeActionContext context = params.getContext();
        if (context.getTriggerKind() == CodeActionTriggerKind.Invoked
                && params.getRange().getStart().equals(params.getRange().getEnd()))
        {
            return List.of(
                    createCodeAction("Insert Step...", "vividus.action.insertStep", new InsertStepParameters(
                            params.getTextDocument().getUri(),
                            params.getRange().getStart()))
                    );
        }
        return List.of();
    }

    private Either<Command, CodeAction> createCodeAction(String title, String commandKey, Object args)
    {
        CodeAction codeAction = new CodeAction();
        codeAction.setKind(CodeActionKind.Source);
        codeAction.setTitle(title);
        Command command = new Command();
        command.setCommand(commandKey);
        command.setArguments(List.of(args));
        codeAction.setCommand(command);
        return Either.forRight(codeAction);
    }

    static final class InsertStepParameters
    {
        private final String uri;
        private final Position position;

        InsertStepParameters(String uri, Position position)
        {
            this.uri = uri;
            this.position = position;
        }

        String getUri()
        {
            return uri;
        }

        Position getPosition()
        {
            return position;
        }
    }
}
