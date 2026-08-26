# Rider Symbol Graph

This is a small IntelliJ Platform plugin for Rider. Place the caret on a function or variable (or select its name) and press **Ctrl+Alt+G**. The dialog shows the declaration on the left and all indexed references on the right. Double-click any rectangle to open the corresponding file at the exact source offset. C# files use a project text-index fallback because Rider's C# model is provided by the ReSharper backend rather than IntelliJ PSI.

The ZIP containing this directory is a **source project**, not an installable plugin. Do not install it from **Settings | Plugins | Install Plugin from Disk**. Open the directory as a Gradle project, then run `buildPlugin`; install the generated ZIP from the external build directory.

## Run in Rider

1. Open `work/rider-symbol-graph` in Rider.
2. Select a JDK 17 (Rider's bundled runtime is suitable if it is JDK 17+).
3. Let Rider import the Gradle project and download the IntelliJ Platform SDK.
4. Run the generated `runIde` configuration. The sandbox IDE starts with the plugin installed.
5. In the sandbox, open a project, place the caret on a symbol, and use **Ctrl+Alt+G**.

To keep Rider's Gradle dependency cache off C:, set **Settings | Build, Execution, Deployment | Build Tools | Gradle | Gradle user home** to `D:\GradleUserHome\rider-symbol-graph` before importing or building.

For a distributable plugin, run `build-off-c.ps1` from PowerShell. It sets `JAVA_HOME=D:\software\JDK21`, `GRADLE_USER_HOME=D:\GradleUserHome\rider-symbol-graph`, uses a project cache under `D:\GradleBuilds\rider-symbol-graph`, and writes build output there as well. Install `D:\GradleBuilds\rider-symbol-graph\distributions\rider-symbol-graph-0.1.1.zip` from that external directory. This release targets Rider 2026.1.x (build 261).

If the machine uses another data drive, edit the two paths in `build-off-c.ps1` and `gradle.properties`.

The default shortcut can be changed under **Settings | Keymap | Symbol Graph | Show Symbol Graph**.

## Notes

The implementation uses IntelliJ PSI and `ReferencesSearch`, so usages are limited to files indexed by the IDE and naturally follow the language plugins installed in Rider. The graph intentionally caps references at 80 to keep the dialog responsive; this can be made configurable later.
