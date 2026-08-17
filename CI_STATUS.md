# HBM Door Port CI

- Result: **success**
- Source commit: `2bf74c5d7a6dc6a578f936df2de66ad01bb48e2b`
- Java: 21
- Minecraft: 1.21.1
- NeoForge: 21.1.235

## Build log tail
```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 
> Task :neoFormListLibraries
> Task :cacheVersionManifest1.21.1
> Task :cacheVersionExecutableClient1.21.1
> Task :cacheVersionExecutableServer1.21.1
> Task :cacheVersionExtractedServer1.21.1
> Task :cacheVersionMappingsClient1.21.1
> Task :create1.21.1ClientExtraJar
> Task :selectRawArtifactNg_dummy_ng.net.minecraft_client_1.21.1_client-extra
> Task :neoFormListTransformLibraries
> Task :neoFormStripClientFinals
> Task :neoFormStripClient
> Task :neoFormStripServer
> Task :neoFormMerge
> Task :neoFormMergeMappings
> Task :neoFormRename
> Task :neoFormDecompile
> Task :neoFormInject
> Task :neoFormPatch
> Task :neoFormPatchUserDev
> Task :neoFormTransformSource
> Task :neoFormRecompile
Note: Some input files use or override a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
Note: Some input files use or override a deprecated API that is marked for removal.
Note: Recompile with -Xlint:removal for details.
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.

> Task :supplyRawJarForneoFormJoined1.21.1-20240808.144430
> Task :selectRawArtifactNg_dummy_ng.net.neoforged_neoforge_21.1.235

> Task :compileJava
/home/runner/work/HBM-nuclear-1.21.1-door-port/HBM-nuclear-1.21.1-door-port/src/main/java/com/hbmdoorsport/client/HbmDoorsClientGameEvents.java:11: warning: [removal] bus() in EventBusSubscriber has been deprecated and marked for removal
@EventBusSubscriber(modid = HbmDoorsPort.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
                                                                     ^
/home/runner/work/HBM-nuclear-1.21.1-door-port/HBM-nuclear-1.21.1-door-port/src/main/java/com/hbmdoorsport/client/HbmDoorsClientGameEvents.java:11: warning: [removal] Bus in EventBusSubscriber has been deprecated and marked for removal
@EventBusSubscriber(modid = HbmDoorsPort.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
                                                                                             ^
/home/runner/work/HBM-nuclear-1.21.1-door-port/HBM-nuclear-1.21.1-door-port/src/main/java/com/hbmdoorsport/client/HbmDoorsClient.java:9: warning: [removal] bus() in EventBusSubscriber has been deprecated and marked for removal
@EventBusSubscriber(modid = HbmDoorsPort.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
                                                                     ^
/home/runner/work/HBM-nuclear-1.21.1-door-port/HBM-nuclear-1.21.1-door-port/src/main/java/com/hbmdoorsport/client/HbmDoorsClient.java:9: warning: [removal] Bus in EventBusSubscriber has been deprecated and marked for removal
@EventBusSubscriber(modid = HbmDoorsPort.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
                                                                                             ^
4 warnings

> Task :processResources
> Task :classes
> Task :jar
> Task :jarJar SKIPPED
> Task :assemble
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :neoFormJoined1.21.1-20240808.144430DownloadAssets
> Task :neoFormJoined1.21.1-20240808.144430ExtractNatives
> Task :writeMinecraftClasspathJunit
> Task :testJunit NO-SOURCE
> Task :check UP-TO-DATE
> Task :build
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/compile-1787006929774.json

[Incubating] Problems report is available at: file:///home/runner/work/HBM-nuclear-1.21.1-door-port/HBM-nuclear-1.21.1-door-port/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 3m 46s
29 actionable tasks: 29 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```
