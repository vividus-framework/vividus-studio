import { commands, window, WorkspaceEdit, Uri, workspace, Position, Disposable, QuickPickItem } from "vscode"
import { LanguageClient, RequestType0 } from "vscode-languageclient/node"

export function registerInsertStepCommand(client: LanguageClient): Disposable {
    return commands.registerCommand('vividus.action.insertStep', async (args) => {
        const getStepsType: RequestType0<string[], void> = new RequestType0<string[], void>('vividus/getSteps')
        const steps: string[] = await client.sendRequest(getStepsType)

        const pickItems: QuickPickItem[] = steps.map(item => {
            const pickItem: QuickPickItem = {
                label: item
            }
            return pickItem
        });

        await window.showQuickPick(pickItems, {
            canPickMany: false,
            placeHolder: 'Type step name...',
            title: 'Insert step'
        }).then((item) => {
            const edit: WorkspaceEdit = new WorkspaceEdit()
            edit.insert(Uri.parse(args.uri), args.position, item?.label as string)
            workspace.applyEdit(edit)
        })
    } )
}

export interface InsertStepParameters {
    uri: string,
    position: Position,
}
