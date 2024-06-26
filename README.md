VIVIDUS extension for Visual Studio Code
==========================

[![Visual Studio Marketplace](https://img.shields.io/visual-studio-marketplace/v/vividus.vividus-studio?label=VS%20Marketplace&logo=visual-studio-code)](https://marketplace.visualstudio.com/items?itemName=vividus.vividus-studio)
[![Installs](https://img.shields.io/visual-studio-marketplace/i/vividus.vividus-studio?logo=visual-studio-code)](https://marketplace.visualstudio.com/items?itemName=vividus.vividus-studio)
[![Rating](https://img.shields.io/visual-studio-marketplace/r/vividus.vividus-studio?logo=visual-studio-code)](https://marketplace.visualstudio.com/items?itemName=vividus.vividus-studio)
[![VIVIDUS Studio CI](https://github.com/vividus-framework/vividus-studio/actions/workflows/build.yml/badge.svg)](https://github.com/vividus-framework/vividus-studio/actions/workflows/build.yml)
[![Codecov](https://codecov.io/gh/vividus-framework/vividus-studio/branch/main/graph/badge.svg)](https://codecov.io/gh/vividus-framework/vividus-studio)
[![License](https://img.shields.io/github/license/vividus-framework/vividus-studio?logo=eclipse)](https://github.com/vividus-framework/vividus-studio/blob/main/LICENSE)

Quick Start
============

1. Download and install JDK 21.
2. Install the [VIVIDUS Studio Extension](https://marketplace.visualstudio.com/items?itemName=vividus.vividus-studio).
3. The extension is activated when you open a project containing story or steps files.

VS Code Settings
==========================

* `vividus-studio.java-home`: The directory path containing JDK to run the extension, if not specified the JDK is located automatically.
* `vividus-studio.stories-runner`: Stories runner class (the fully qualified name).

Source Code Actions
==========================

### Action 'Insert Step'

![Insert Step Example](./images/insert-step-action.gif)

Configure Shortcuts
==========================

The keyboard shortcut for `Source Action...` can be configured by the following path `Code -> Preferences -> Keyboard Shortcuts -> Source Action...`
