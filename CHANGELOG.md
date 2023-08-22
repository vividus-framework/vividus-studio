# Change Log

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
