import { IJavaRuntime, getRuntime, findRuntimes, IJavaVersion } from "jdk-utils";
import { workspace } from "vscode";

export async function findJavaExecutable(): Promise<IJavaRuntime> {
    const javaHomeProperty: string = 'vividus-studio.java-home';
    const userJavaHome: string = workspace.getConfiguration().get(javaHomeProperty) as string;

    if (userJavaHome != null) {
        const runtime: IJavaRuntime = await getRuntime(userJavaHome, { withVersion: true }) as IJavaRuntime;

        if (!runtime) {
            throw new Error(`Unable to find JDK at location specified by ${javaHomeProperty} user property: ${userJavaHome}`);
        }

        if (runtime.version?.major as number < 17) {
            throw new Error(`The ${javaHomeProperty} user property points to JDK ${runtime.version?.java_version} installation,`
                + ` but JDK 17 or higher is required`)
        }

        return runtime;
    }

    const runtime: IJavaRuntime = (await findRuntimes({ withVersion: true })).find(runtime => {
        const version: IJavaVersion = runtime.version as IJavaVersion;
        return version.major >= 17
    }) as IJavaRuntime;

    if (!runtime) {
        throw new Error('Unable to find JDK 17 or higher installation');
    }

    return runtime;
}
