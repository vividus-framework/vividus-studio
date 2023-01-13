import { ExtensionContext, OutputChannel, Uri, window } from 'vscode';
import { AddressInfo, createServer } from 'net';
import { launch, Application } from './lib/equinox';
import { LanguageClient, StreamInfo } from "vscode-languageclient/node";
import { LanguageClientOptions, CompletionClientCapabilities, CompletionItemKind } from 'vscode-languageclient';

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
                language: 'vividus-dsl',
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
    client.start()
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

export function deactivate(): Thenable<void> | undefined {
    if (client) {
        return client.dispose()
    }
    return undefined;
}
