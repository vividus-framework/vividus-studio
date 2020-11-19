import { AddressInfo } from 'net';
import { sync } from 'glob';
import { spawn } from 'child_process';

export function launch(address: AddressInfo, application: Application) {

    const serverArgs: string[] = [];

    serverArgs.push(`-Dhost=${address.address}`);
    serverArgs.push(`-Dport=${address.port}`);

    serverArgs.push(`-Declipse.application=${application.application}`);
    serverArgs.push(`-Declipse.product=${application.product}`);

    const jar: string = search('**/repository/plugins/org.eclipse.equinox.launcher_*.jar', application.applicationDir);
    serverArgs.push('-jar');
    serverArgs.push(`${application.applicationDir}/${jar}`);

    let platform;
    switch (process.platform) {
        case 'darwin':
            platform = 'macos';
            break;
        default:
            throw new Error(`Unsupported platform ${platform}`);
    }
    const bundle: string = search(`**/repository/configuration-${platform}`, application.applicationDir);

    serverArgs.push('-configuration');
    serverArgs.push(`${application.applicationDir}/${bundle}`);

    const serverProcess = spawn('java', serverArgs, { detached: true });
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

    constructor(application: string, product: string, applicationDir: string) {
        this.application = application;
        this.product = product;
        this.applicationDir = applicationDir;
    }
}

function search(pattern: string, workingDirectory: string): string {
    const launchers: Array<string> = sync(pattern, { cwd: workingDirectory });
    return launchers[0];
}
