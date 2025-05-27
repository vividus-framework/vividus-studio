import * as assert from 'assert';
import * as sinon from 'sinon';

import { findJavaExecutable } from '../../lib/utils';
import { workspace, WorkspaceConfiguration } from 'vscode';
import { IJavaRuntime } from 'jdk-utils';
import { SinonStub } from 'sinon';
import { beforeEach } from 'mocha';
import * as jdkUtils from 'jdk-utils';

suite('Utils', () => {
    beforeEach(() => {
        sinon.restore();
    });

    const java17: IJavaRuntime = {
        homedir: 'home_dir',
        version: {
            java_version: '17.0.2',
            major: 17
        }
    };

    const java21: IJavaRuntime = {
        homedir: 'home_dir',
        version: {
            java_version: '21.0.4',
            major: 21
        }
    };

    test('Should return Java 21 installation pointed by vividus-studio.java-home property', async () => {
        const getRuntimeStub: SinonStub = sinon.stub(jdkUtils, 'getRuntime').returns(Promise.resolve(java21));

        sinon.stub(workspace, 'getConfiguration').returns(<WorkspaceConfiguration>{
            get: (_section: string) => { return 'home_dir'; }
        });

        assert.equal(java21, await findJavaExecutable());
        assert.equal(true, getRuntimeStub.calledWith('home_dir', { withVersion: true }));
    });

    test('Should return Java 21 system installation', async () => {
        const getRuntimeStub: SinonStub = sinon.stub(jdkUtils, 'findRuntimes').returns(Promise.resolve([java21]));

        sinon.stub(workspace, 'getConfiguration').returns(<WorkspaceConfiguration>{
            get: (_section: string) => { return null; }
        });

        assert.equal(java21, await findJavaExecutable());
        assert.equal(true, getRuntimeStub.calledWith({ withVersion: true }));
    });

    test('Should fail if vividus-studio.java-home property points to not Java dir', async () => {
        const getRuntimeStub: SinonStub = sinon.stub(jdkUtils, 'getRuntime').returns(Promise.resolve(undefined));

        sinon.stub(workspace, 'getConfiguration').returns(<WorkspaceConfiguration>{
            get: (_section: string) => { return 'home_dir'; }
        });

        await assert.rejects(findJavaExecutable(),
            { message: 'Unable to find Java at location specified by vividus-studio.java-home user property: home_dir' });
        assert.equal(true, getRuntimeStub.calledOnce);
        assert.equal(true, getRuntimeStub.calledWith('home_dir', { withVersion: true }));
    });

    test('Should fail if vividus-studio.java-home property points to old Java versions', async () => {
        const getRuntimeStub: SinonStub = sinon.stub(jdkUtils, 'getRuntime').returns(Promise.resolve(java17));

        sinon.stub(workspace, 'getConfiguration').returns(<WorkspaceConfiguration>{
            get: (_section: string) => { return 'home_dir'; }
        });

        await assert.rejects(findJavaExecutable(),
            { message: 'The vividus-studio.java-home user property points to Java 17.0.2 installation, but Java 21 or higher is required' });
        assert.equal(true, getRuntimeStub.calledOnce);
        assert.equal(true, getRuntimeStub.calledWith('home_dir', { withVersion: true }));
    });

    test('Should fail if there is no Java 21 installed in the system', async () => {
        const getRuntimeStub: SinonStub = sinon.stub(jdkUtils, 'findRuntimes').returns(Promise.resolve([java17]));

        sinon.stub(workspace, 'getConfiguration').returns(<WorkspaceConfiguration>{
            get: (_section: string) => { return null; }
        });

        await assert.rejects(findJavaExecutable(),
            { message: 'Unable to find Java 21 or higher installation' });
        assert.equal(true, getRuntimeStub.calledOnce);
        assert.equal(true, getRuntimeStub.calledWith({ withVersion: true }));
    });
});
