load("//tools/base/bazel:bazel.bzl", "iml_module")

# managed by go/iml_to_build
iml_module(
    name = "intellij.android.plugin",
    srcs = ["src"],
    iml_files = ["intellij.android.plugin.iml"],
    # do not sort: must match IML order
    test_runtime_deps = [
        "//tools/idea/uast/uast-tests:intellij.platform.uast.tests",
        "//tools/idea/plugins/java-decompiler/plugin:intellij.java.decompiler",
        "//tools/idea/jvm/jvm-analysis-java-tests:intellij.jvm.analysis.java.tests",
        "//tools/idea/java/manifest:intellij.java.manifest",
        "//tools/idea/java/remote-servers/impl:intellij.java.remoteServers.impl",
        "//tools/idea:intellij.java.ui.tests",
        "//tools/idea/plugins/ui-designer:intellij.java.guiForms.designer",
        "//tools/idea/java/plugin:intellij.java.plugin",
        "//tools/idea/plugins/ui-designer-core:intellij.uiDesigner",
    ],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    runtime_deps = [
        "//tools/idea/platform/tasks-platform-impl:intellij.platform.tasks.impl",
        "//tools/idea/platform/credential-store:intellij.platform.credentialStore",
        "//tools/idea/jvm/jvm-analysis-impl:intellij.jvm.analysis.impl",
        "//tools/idea/java/ide-customization:intellij.java.ide.customization",
        "//tools/idea/java/ide-resources:intellij.java.ide.resources",
        "//tools/idea/java/compiler/instrumentation-util-8:intellij.java.compiler.instrumentationUtil.java8",
        "//tools/idea/plugins/maven/error-prone-compiler:intellij.maven.errorProne.compiler",
        "//tools/idea/plugins/IntelliLang/intellilang-jps-plugin:intellij.java.langInjection.jps",
        "//tools/adt/idea/adt-branding:intellij.android.adt.branding",
    ],
    # do not sort: must match IML order
    deps = [
        "//tools/adt/idea/android:intellij.android.core[module]",
        "//tools/adt/idea/sdk-updates:intellij.android.sdkUpdates[module]",
        "//tools/adt/idea/designer:intellij.android.designer[module]",
        "//tools/adt/idea/profilers-android:intellij.android.profilersAndroid[module]",
        "//tools/adt/idea/game-tools-starter:intellij.android.gameToolsStarter[module]",
        "//tools/adt/idea/android-adb:intellij.android.adb[module]",
        "//tools/adt/idea/android-debuggers:intellij.android.debuggers[module]",
        "//tools/adt/idea/android-lang:intellij.android.lang[module]",
        "//tools/adt/idea/project-system:intellij.android.projectSystem[module]",
        "//tools/adt/idea/project-system-gradle:intellij.android.projectSystem.gradle[module]",
        "//tools/adt/idea/android-transport:intellij.android.transport[module]",
        "//tools/adt/idea/android-layout-inspector[module]",
        "//tools/adt/idea/assistant[module]",
        "//tools/adt/idea/connection-assistant[module]",
        "//tools/adt/idea/whats-new-assistant[module]",
        "//tools/adt/idea/android-lang-databinding:intellij.android.lang-databinding[module]",
        "//tools/adt/idea/databinding:intellij.android.databinding[module]",
        "//tools/adt/idea/naveditor:intellij.android.naveditor[module]",
        "//tools/adt/idea/layout-inspector:intellij.android.layout-inspector[module]",
        "//tools/adt/idea/compose-designer:intellij.android.compose-designer[module]",
        "//tools/adt/idea/room:intellij.android.room[module]",
        "//tools/base/wizard/template-impl:studio.intellij.android.wizardTemplate.impl[module]",
    ],
)
