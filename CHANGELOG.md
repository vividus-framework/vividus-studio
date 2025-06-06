# Change Log

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.7] - 2025-05-27
* Bump VS Code engine from 1.98.0 to 1.99.1
* Bump version of Gradle used to build projects from 8.14 to 8.14.1

## [0.2.6] - 2025-05-19
* Bump VS Code engine from 1.97.0 to 1.98.0
* Bump version of Gradle used to build projects from 8.13 to 8.14

## [0.2.5] - 2025-03-11
* Bump VS Code engine from 1.96.0 to 1.97.0
* Bump version of Gradle used to build projects from 8.12.1 to 8.13

## [0.2.4] - 2025-02-10
* Bump VS Code engine from 1.95.0 to 1.96.0
* Bump version of Gradle used to build projects from 8.12 to 8.12.1

## [0.2.3] - 2025-01-19
* Bump VS Code engine from 1.94.0 to 1.95.0
* Bump version of Gradle used to build projects from 8.10.2 to 8.12

## [0.2.2] - 2024-11-11
* Bump VS Code engine from 1.93.0 to 1.94.0

## [0.2.1] - 2024-09-24
* Bump minimum required Java for server from 17 to 21

## [0.2.0] - 2024-09-24
* Bump minimum required Java from 17 to 21
* Bump version of Gradle used to build projects from 8.9 to 8.10.2
* Bump VS Code engine from 1.92.0 to 1.93.0

## [0.1.19] - 2024-09-12
* Bump version of Gradle used to build projects from 8.8 to 8.9
* Bump VS Code engine from 1.91.0 to 1.92.0

## [0.1.18] - 2024-08-06
* Bump VS Code engine from 1.90.0 to 1.91.0

## [0.1.17] - 2024-06-24
* Bump VS Code engine from 1.89.0 to 1.90.0

## [0.1.16] - 2024-06-03
* Add ability to configure stories runner class: `vividus-studio.stories-runner`

## [0.1.15] - 2024-06-01
* Bump version of Gradle used to build projects from 8.7 to 8.8
* Bump VS Code engine from 1.88.0 to 1.89.0

## [0.1.14] - 2024-05-13
* Bump version of Gradle used to build projects from 8.6 to 8.7
* Bump VS Code engine from 1.87.0 to 1.88.0

## [0.1.13] - 2024-03-18

* Use predefined `comment` token for VIVIDUS comments
* Bump VS Code engine from 1.86.0 to 1.87.0

## [0.1.12] - 2024-02-27

* Fix message of unsupported platform error
* Add Linux support
* Bump VS Code engine from 1.85.0 to 1.86.0
* Bump version of Gradle used to build projects to 8.6

## [0.1.11] - 2024-02-22

* Fix highlighting of GivenStories filtered by scenario meta parameters
* Add highlighting for 'Descrption' story element

## [0.1.10] - 2024-01-02

* Add auto-completion support for step parameters with limited number of choices
* Enable comments toggling in story and steps files

    The keyboard shortcut for a comment toggling can be configured by the following path `Code -> Preferences -> Keyboard Shortcuts -> Toggle Line Comment`

## [0.1.9] - 2023-08-22

* Add ability to refresh project after changes in dependencies
* Show notifications on errors encountered during project build process
* Bump version of Gradle used to build projects to 8.3

## [0.1.8] - 2023-04-12

* Fix invalid boundary resolving of composite steps in composite files

## [0.1.7] - 2023-04-05

* (Fixes https://github.com/vividus-framework/vividus-studio/issues/406) Fix invalid multiline step highlighting when it's followed by a comment

## [0.1.6] - 2023-03-13

* (Fixes https://github.com/vividus-framework/vividus-studio/issues/380) Add 'Insert Step' source code action

## [0.1.5] - 2023-02-23

### Fixed

* (Fixes https://github.com/vividus-framework/vividus-studio/issues/367) Fix meta parsing to not consider  meta sign in step as part of meta tags block
* Fix semantics error occurred when user selects VIVIDUS language mode for new text file that was not saved on the file system

## [0.1.4] - 2023-02-21

### Added

* Add auto-completion for locally defined composite steps
* Highlight composite step files content

## [0.1.3] - 2023-02-13

### Fixed

* Do not invoke completion items list on typing inside unbounded argument

## [0.1.2] - 2023-02-10

### Fixed

* Highlight multiline step arguments

## [0.1.1] - 2023-02-06

### Fixed

* Fix internal accessibility error (#32603) when running `VIVIDUS: Run Stories` command

## [0.1.0] - 2023-02-03

### Added

* Add auto-completion for regular and composite steps
* Add syntax highlighting for scenarios, scenarios examples, steps, steps arguments, comments, meta info, lifecycle, story examples and given stories
* Add command to run stories from IDE
* Add VIVIDUS icon for story and commposite step files
* Add auto-assembling of test project
* Add automatic location of JDK 17 in the system
