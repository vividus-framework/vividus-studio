import { AddressInfo } from 'net';
import { sync } from 'glob';
import { spawn } from 'child_process';
import { Uri } from 'vscode';
import { resolve } from 'path';
import { existsSync, mkdirSync } from 'fs';

export function launch(exec: string, address: AddressInfo, application: Application) {

    const serverArgs: string[] = [];

    serverArgs.push(`-Dhost=${address.address}`);
    serverArgs.push(`-Dport=${address.port}`);

    serverArgs.push(`-Declipse.application=${application.application}`);
    serverArgs.push(`-Declipse.product=${application.product}`);

    const jar: string = search('**/repository/plugins/org.eclipse.equinox.launcher_*.jar', application.applicationDir);
    serverArgs.push('-jar');
    serverArgs.push(`${application.applicationDir}/${jar}`);

    let platform;
    const processPlatform = process.platform;
    switch (processPlatform) {
        case 'darwin':
            platform = 'macos';
            break;
        case 'win32':
            platform = 'windows';
            break;
        default:
            throw new Error(`Unsupported platform ${processPlatform}`);
    }
    const bundle: string = search(`**/repository/configuration-${platform}`, application.applicationDir);

    serverArgs.push('-configuration');
    serverArgs.push(`${application.applicationDir}/${bundle}`);

    const workspace = resolve(application.storageDir.fsPath, 'workspace');
    if(!existsSync(workspace)) {
        mkdirSync(workspace, {
            recursive: true
        });
    }
    serverArgs.push('-data');
    serverArgs.push(workspace)

    const serverProcess = spawn(exec, serverArgs, { detached: true });
    serverProcess.stdout.on('data', function(data) {
        console.log(data.toString());
    });
    serverProcess.stderr.on('data', function(data) {
        console.log(data.toString());
    });
}

export class Application {
    readonly application: string;
    readonly product: string;
    readonly applicationDir: string;
    readonly storageDir: Uri

    constructor(application: string, product: string, applicationDir: string, storageDir: Uri) {
        this.application = application;
        this.product = product;
        this.applicationDir = applicationDir;
        this.storageDir = storageDir;
    }
}

function search(pattern: string, workingDirectory: string): string {
    const launchers: Array<string> = sync(pattern, { cwd: workingDirectory });
    return launchers[0];
}
