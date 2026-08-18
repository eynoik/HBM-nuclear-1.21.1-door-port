# HBM Door Port CI

- Result: **success**
- Source commit: `045676235d6619dbff59af67c50d294da1c6f08e`
- Java: 21
- Minecraft: 1.21.1
- NeoForge: 21.1.235

## Build log tail
```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 
> Task :neoFormListLibraries UP-TO-DATE
> Task :cacheVersionManifest1.21.1 FROM-CACHE
> Task :cacheVersionExecutableClient1.21.1 FROM-CACHE
> Task :cacheVersionExecutableServer1.21.1 FROM-CACHE
> Task :cacheVersionExtractedServer1.21.1 FROM-CACHE
> Task :cacheVersionMappingsClient1.21.1 FROM-CACHE
> Task :create1.21.1ClientExtraJar FROM-CACHE
> Task :selectRawArtifactNg_dummy_ng.net.minecraft_client_1.21.1_client-extra
> Task :neoFormListTransformLibraries UP-TO-DATE
> Task :neoFormStripClientFinals FROM-CACHE
> Task :neoFormStripClient FROM-CACHE
> Task :neoFormStripServer FROM-CACHE
> Task :neoFormMerge FROM-CACHE
> Task :neoFormMergeMappings FROM-CACHE
> Task :neoFormRename FROM-CACHE
> Task :neoFormDecompile FROM-CACHE
> Task :neoFormInject FROM-CACHE
> Task :neoFormPatch FROM-CACHE
> Task :neoFormPatchUserDev FROM-CACHE
> Task :neoFormTransformSource FROM-CACHE
> Task :neoFormRecompile FROM-CACHE
> Task :supplyRawJarForneoFormJoined1.21.1-20240808.144430
> Task :selectRawArtifactNg_dummy_ng.net.neoforged_neoforge_21.1.235
> Task :compileJava FROM-CACHE
> Task :processResources
> Task :classes
> Task :jar
> Task :jarJar SKIPPED
> Task :assemble
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :neoFormJoined1.21.1-20240808.144430DownloadAssets UP-TO-DATE
> Task :neoFormJoined1.21.1-20240808.144430ExtractNatives FROM-CACHE
> Task :writeMinecraftClasspathJunit FROM-CACHE
> Task :testJunit NO-SOURCE
> Task :check UP-TO-DATE
> Task :build
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/compile-1787065230051.json

BUILD SUCCESSFUL in 26s
29 actionable tasks: 5 executed, 21 from cache, 3 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```
