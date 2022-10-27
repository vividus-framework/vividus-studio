import { ExtensionContext, Uri } from 'vscode';
import { AddressInfo, createServer } from 'net';
import { launch, Application } from './lib/equinox';
import { LanguageClient, StreamInfo } from "vscode-languageclient/node";
import { LanguageClientOptions, CompletionClientCapabilities, CompletionItemKind } from 'vscode-languageclient';

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

    const clientOptions: LanguageClientOptions = {
        documentSelector: [
            {
                language: 'vividus-dsl',
                scheme: 'file'
            }
        ],
        initializationOptions: [
            completionClientCapabilites
        ],
        progressOnInitialization: true
    };

    const client = new LanguageClient("Client", () => createServerOptions(context), clientOptions);
    context.subscriptions.push(client.start());
}

function createServerOptions(context: ExtensionContext): Promise<StreamInfo> {
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
            launch(address, application);
        });
        return server;
    });
}

export function deactivate() {
}
