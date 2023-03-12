import * as assert from 'assert';
import * as sinon from 'sinon';

import { commands, Position, QuickPickItem, window, workspace } from 'vscode'
import { LanguageClient } from 'vscode-languageclient/node';
import { InsertStepParameters, registerInsertStepCommand } from '../../lib/codeActions'

suite('Code Actions', () => {

    test('Insert Step', async () => {
        const position: Position = new Position(10, 0)
        const parameters: InsertStepParameters = {
            uri: 'document-uri',
            position: position,
        }
        const itemAsString: string = 'item'
        const pickItem: QuickPickItem = {
            label: itemAsString
        }
        const item: Thenable<QuickPickItem> = Promise.resolve(pickItem);
        const showQuickPickStub: sinon.SinonStub = sinon.stub(window, 'showQuickPick').returns(item)
        const workspaceStub: sinon.SinonStub = sinon.stub(workspace, 'applyEdit')

        const languageClient: LanguageClient = sinon.createStubInstance(LanguageClient, {
            sendRequest: Promise.resolve([itemAsString])
        })

        registerInsertStepCommand(languageClient)

        await commands.executeCommand('vividus.action.insertStep', parameters)

        assert.equal(true, showQuickPickStub.calledWith([pickItem], {
            canPickMany: false,
            placeHolder: 'Type step name...',
            title: 'Insert step'
        }))
        assert.equal(true, workspaceStub.calledOnce)
    })
})
