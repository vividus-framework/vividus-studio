# Development guide

## Tools
* Node.js 18 or higher
* JDK 17 or higher
* Eclipse IDE 2023-06 (4.28.0) or higher
* Visual Studio Code 1.79.1 or higher

## General
1. Run `git clone https://github.com/vividus-framework/vividus-studio.git`
1. Run `npm install -g yo generator-code`
1. Install `Eclipse PDE (Plug-in Development Environment)` plugin for Eclipse IDE through `Eclipse Marketplace`

## Import project to Eclipse
### Import plugin
1. Open `Eclipse IDE`
1. Go to `File` tab in main menu and select `Import...`
1. Select `Plug-ins and fragments` under `Plug-in development` section
1. Set import directory to `<path-to-repo>/vividus-studio/vividus-studio-server` under `Import From` section
1. Select `Select from all plug-ins and fragments found at the specified location` under `Plug-ins and Fragments to Import` section
1. Select `Projects with source folders` under `Import As` section
1. Click `Add all ->` and then `Finish` buttons
1. Add the follwing entries into `<path-to-repo>/vividus-studio/vividus-studio-server/vividus-studio-plugin/.classpath` XML file under `<classpath>` section and then refresh the project
    ```xml
    <classpathentry exported="true" kind="lib" path="external/slf4j-api.jar"/>
    <classpathentry exported="true" kind="lib" path="external/log4j-api.jar"/>
    <classpathentry exported="true" kind="lib" path="external/log4j-core.jar"/>
    <classpathentry exported="true" kind="lib" path="external/log4j-slf4j18-impl.jar"/>
    <classpathentry exported="true" kind="lib" path="external/junit-jupiter-api.jar"/>
    <classpathentry exported="true" kind="lib" path="external/junit-jupiter-params.jar"/>
    <classpathentry exported="true" kind="lib" path="external/mockito-core.jar"/>
    <classpathentry exported="true" kind="lib" path="external/mockito-junit-jupiter.jar"/>
    <classpathentry exported="true" kind="lib" path="external/hamcrest.jar"/>
    <classpathentry exported="true" kind="lib" path="external/opentest4j.jar"/>
    ```

### Import target platform
1. Open `Eclipse IDE`
1. Go to `File` tab in main menu and select `Import...`
1. Select `Existing Project into Workspace` under `General` section
1. Set root directory to `<path-to-repo>/vividus-studio/vividus-studio-server/vividus-studio-target-definition` under `Select root directory` section
1. Click `Finish`
1. Nevigate to `Eclipse -> Preferences... -> Plug-in Development -> Target Platform`
1. Select `Vividus Studio Target Definition`
1. Click `Apply and Close`

## Build server
1. Go to `<path-to-repo>/vividus-studio/vividus-studio-server`
1. Run `./mwnw clean install`

## Use extension
1. Run `git clone --recursive https://github.com/vividus-framework/vividus-starter.git`
1. Run `cd vividus-starter && unset BUILD_SYSTEM_ROOT && ./gradlew runStories`
1. Nevigate to `<path-to-repo>/vividus-studio`
1. Run `code .`
1. Press `F5` to start extension
1. After project window is open click `File -> Open...`
1. In the opened window open `vividus-starter` project cloned previously
