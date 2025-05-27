import { commands, window, WorkspaceEdit, Uri, workspace, Position, Disposable, QuickPickItem, FileSystemWatcher, StatusBarAlignment, StatusBarItem } from "vscode";
import { LanguageClient, RequestType0 } from "vscode-languageclient/node";

export function registerInsertStepCommand(client: LanguageClient): Disposable {
    return commands.registerCommand('vividus.action.insertStep', async (args) => {
        const getStepsType: RequestType0<string[], void> = new RequestType0<string[], void>('vividus/getSteps');
        const steps: string[] = await client.sendRequest(getStepsType);

        const pickItems: QuickPickItem[] = steps.map(item => {
            const pickItem: QuickPickItem = {
                label: item
            };
            return pickItem;
        });

        await window.showQuickPick(pickItems, {
            canPickMany: false,
            placeHolder: 'Type step name...',
            title: 'Insert step'
        }).then((item) => {
            const edit: WorkspaceEdit = new WorkspaceEdit();
            edit.insert(Uri.parse(args.uri), args.position, item?.label as string);
            workspace.applyEdit(edit);
        });
    } );
}

export function registerRefreshProjectCommand(client: LanguageClient): Disposable | Disposable[] {

    const refreshProject: StatusBarItem = window.createStatusBarItem(StatusBarAlignment.Right, 100);

    const commandKey: string = 'vividus.action.refreshProject';
    refreshProject.command = commandKey;
    refreshProject.text = '$(refresh) Re-build VIVIDUS project';
    
    const gradleWatcher: FileSystemWatcher = workspace.createFileSystemWatcher('**/*.gradle', true, false, true);
    gradleWatcher.onDidChange((_uri) => refreshProject.show());

    const command: Disposable = commands.registerCommand(commandKey, async (_args) => {
        refreshProject.hide();
        const refreshProjectType: RequestType0<void, void> = new RequestType0<void, void>('vividus/refreshProject');
        await client.sendRequest(refreshProjectType);
    });

    return [gradleWatcher, command];
}

export interface InsertStepParameters {
    uri: string,
    position: Position,
}
