import * as assert from 'assert';
import * as vscode from 'vscode';

suite('Vividus Studio', () => {
    test('Extension should be present', () => {
        assert.ok(vscode.extensions.getExtension('vividus-framework.vividus-studio'));
    });
});
