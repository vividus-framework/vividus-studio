import { ExtensionContext, OutputChannel, Uri, window } from 'vscode';
import { AddressInfo, createServer } from 'net';
import { launch, Application } from './lib/equinox';
import { findJavaExecutable } from './lib/utils';
import { LanguageClient, StreamInfo } from "vscode-languageclient/node";
import { LanguageClientOptions, CompletionClientCapabilities, CompletionItemKind, Disposable } from 'vscode-languageclient';
import { IJavaRuntime } from 'jdk-utils';
import { resolve } from 'path';
import { registerInsertStepCommand, registerRefreshProjectCommand } from './lib/codeActions';

let client: LanguageClient;

export function activate(context: ExtensionContext) {

    const completionClientCapabilites: CompletionClientCapabilities = {
        completionItem: {
            snippetSupport: true
        },
        completionItemKind: {
            valueSet: [
                CompletionItemKind.Method
            ]
        },
        contextSupport: true
    };

    const name: string = "VIVIDUS Studio";
    const channel: OutputChannel = window.createOutputChannel(name);
    channel.show();

    const clientOptions: LanguageClientOptions = {
        documentSelector: [
            {
                language: 'vividus-story',
                scheme: 'file'
            },
            {
                language: 'vividus-composite-step',
                scheme: 'file'
            }
        ],
        initializationOptions: [
            completionClientCapabilites
        ],
        progressOnInitialization: true,
        outputChannel: channel,
    };

    client = new LanguageClient("Client", () => createServerOptions(context), clientOptions);

    const disposables: Disposable[] = context.subscriptions;

    disposables.push(registerInsertStepCommand(client));
    disposables.push(...registerRefreshProjectCommand(client) as Disposable[]);

    client.start();
}

async function createServerOptions(context: ExtensionContext): Promise<StreamInfo> {

    const javaRuntime: IJavaRuntime = await findJavaExecutable();
    window.showInformationMessage(`Using Java ${javaRuntime.version?.java_version}`);

    return new Promise((res, rej) => {
        const server = createServer(connection => res({ writer: connection, reader: connection }));
        server.on('error', rej);

        server.listen(() => {
            const address: AddressInfo = server.address() as AddressInfo;
            const application: Application = {
                application: 'org.vividus.studio.plugin.application',
                product: 'org.vividus.studio.plugin.product',
                applicationDir: context.extensionPath,
                storageDir: context.storageUri as Uri
            };
            launch(resolve(javaRuntime.homedir, 'bin', 'java'), address, application);
        });
        return server;
    });
}

export function deactivate(): Thenable<void> | undefined {
    if (client) {
        return client.dispose();
    }
    return undefined;
}
