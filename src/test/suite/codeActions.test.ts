import * as assert from 'assert';
import * as sinon from 'sinon';

import { commands, FileSystemWatcher, Position, QuickPickItem, StatusBarAlignment, StatusBarItem, window, workspace } from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';
import { InsertStepParameters, registerInsertStepCommand, registerRefreshProjectCommand } from '../../lib/codeActions';

suite('Code Actions', () => {

    test('Insert Step', async () => {
        const position: Position = new Position(10, 0);
        const parameters: InsertStepParameters = {
            uri: 'document-uri',
            position: position,
        };
        const itemAsString: string = 'item';
        const pickItem: QuickPickItem = {
            label: itemAsString
        };
        const item: Thenable<QuickPickItem> = Promise.resolve(pickItem);
        const showQuickPickStub: sinon.SinonStub = sinon.stub(window, 'showQuickPick').returns(item);
        const workspaceStub: sinon.SinonStub = sinon.stub(workspace, 'applyEdit');

        const languageClient: LanguageClient = sinon.createStubInstance(LanguageClient, {
            sendRequest: Promise.resolve([itemAsString])
        });

        registerInsertStepCommand(languageClient);

        await commands.executeCommand('vividus.action.insertStep', parameters);

        assert.equal(true, showQuickPickStub.calledWith([pickItem], {
            canPickMany: false,
            placeHolder: 'Type step name...',
            title: 'Insert step'
        }));
        assert.equal(true, workspaceStub.calledOnce);
    });

    test('Refresh Project', async () => {
        const showStub: sinon.SinonStub = sinon.stub();
        const hideStub: sinon.SinonStub = sinon.stub();

        const refreshBar = <StatusBarItem>{};
        refreshBar.show = showStub;
        refreshBar.hide = hideStub;

        const createStatusBarStub: sinon.SinonStub = sinon.stub(window, 'createStatusBarItem').returns(refreshBar);

        const gradleWatcherStub: FileSystemWatcher = <FileSystemWatcher>{
            onDidChange: (_uri) => {}
        };
        // refresh bar show is not covered by this unit test
        const createFileSystemWatcherStub: sinon.SinonStub = sinon.stub(workspace, 'createFileSystemWatcher').returns(gradleWatcherStub);
        
        const sendRequestStub: sinon.SinonStub = sinon.stub();

        const languageClient: LanguageClient = sinon.createStubInstance(LanguageClient, {
            sendRequest: Promise.resolve([sendRequestStub])
        });

        registerRefreshProjectCommand(languageClient);

        await commands.executeCommand('vividus.action.refreshProject');

        assert.equal('vividus.action.refreshProject', refreshBar.command);
        assert.equal('$(refresh) Re-build VIVIDUS project', refreshBar.text);
        assert.equal(true, createStatusBarStub.calledOnceWith(StatusBarAlignment.Right, 100));
        assert.equal(true, hideStub.calledOnce);
        assert.equal(true, createFileSystemWatcherStub.calledOnceWith('**/*.gradle', true, false, true));
    });
});
