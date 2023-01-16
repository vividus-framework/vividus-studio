import * as assert from 'assert';
import * as sinon from 'sinon';

import { findJavaExecutable } from '../../lib/utils'
import { workspace, WorkspaceConfiguration, extensions } from 'vscode';
import { IJavaRuntime } from 'jdk-utils';
import { SinonStub } from 'sinon';
import { beforeEach } from 'mocha';

suite('Utils', () => {
    beforeEach(() => {
        sinon.restore()
    })

    const jdk13: IJavaRuntime = {
        homedir: 'home_dir',
        version: {
            java_version: '13.0.2',
            major: 13
        }
    }

    const jdk17: IJavaRuntime = {
        homedir: 'home_dir',
        version: {
            java_version: '17.0.2',
            major: 17
        }
    }

    const jdkUtilsModule = require('jdk-utils')

    test('Should return JDK 17 installation pointed by vividus-studio.java-home property', async () => {
        const getRuntimeStub: SinonStub = sinon.stub(jdkUtilsModule, 'getRuntime').returns(jdk17)

        sinon.stub(workspace, 'getConfiguration').returns(<WorkspaceConfiguration>{
            get: (section: string) => { return 'home_dir' }
        });

        assert.equal(jdk17, await findJavaExecutable())
        assert.equal(true, getRuntimeStub.calledWith('home_dir', { withVersion: true }))
    })

    test('Should return JDK 17 system installation', async () => {
        const getRuntimeStub: SinonStub = sinon.stub(jdkUtilsModule, 'findRuntimes').returns([jdk17])

        sinon.stub(workspace, 'getConfiguration').returns(<WorkspaceConfiguration>{
            get: (section: string) => { return null }
        });

        assert.equal(jdk17, await findJavaExecutable())
        assert.equal(true, getRuntimeStub.calledWith({ withVersion: true }))
    })

    test('Should fail if vividus-studio.java-home property points to not java dir', async () => {
        const getRuntimeStub: SinonStub = sinon.stub(jdkUtilsModule, 'getRuntime').returns(undefined)

        sinon.stub(workspace, 'getConfiguration').returns(<WorkspaceConfiguration>{
            get: (section: string) => { return 'home_dir' }
        });

        await assert.rejects(findJavaExecutable(),
            { message: 'Unable to find JDK at location specified by vividus-studio.java-home user property: home_dir' })
        assert.equal(true, getRuntimeStub.calledOnce)
        assert.equal(true, getRuntimeStub.calledWith('home_dir', { withVersion: true }))
    })

    test('Should fail if vividus-studio.java-home property points to old jdk versions', async () => {
        const getRuntimeStub: SinonStub = sinon.stub(jdkUtilsModule, 'getRuntime').returns(jdk13)

        sinon.stub(workspace, 'getConfiguration').returns(<WorkspaceConfiguration>{
            get: (section: string) => { return 'home_dir' }
        });

        await assert.rejects(findJavaExecutable(),
            { message: 'The vividus-studio.java-home user property points to JDK 13.0.2 installation, but JDK 17 or higher is required' })
        assert.equal(true, getRuntimeStub.calledOnce)
        assert.equal(true, getRuntimeStub.calledWith('home_dir', { withVersion: true }))
    })

    test('Should fail if there is no JDK 17 installed in the system', async () => {
        const getRuntimeStub: SinonStub = sinon.stub(jdkUtilsModule, 'findRuntimes').returns([jdk13])

        sinon.stub(workspace, 'getConfiguration').returns(<WorkspaceConfiguration>{
            get: (section: string) => { return null }
        });

        await assert.rejects(findJavaExecutable(),
            { message: 'Unable to find JDK 17 or higher installation' })
        assert.equal(true, getRuntimeStub.calledOnce)
        assert.equal(true, getRuntimeStub.calledWith({ withVersion: true }))
    })
});
