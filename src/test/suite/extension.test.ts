import * as assert from 'assert';
import * as vscode from 'vscode';

suite('VIVIDUS Studio', () => {
    test('Extension should be present', () => {
        assert.ok(vscode.extensions.getExtension('vividus.vividus-studio'));
    });
});
