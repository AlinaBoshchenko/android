/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.templates

import com.android.sdklib.IAndroidTarget
import com.android.sdklib.SdkVersionInfo
import com.android.testutils.TestUtils.getKotlinVersionForTests
import com.android.testutils.VirtualTimeScheduler
import com.android.tools.analytics.TestUsageTracker
import com.android.tools.analytics.UsageTracker.cleanAfterTesting
import com.android.tools.analytics.UsageTracker.setWriterForTest
import com.android.tools.idea.npw.platform.Language
import com.android.tools.idea.sdk.AndroidSdks
import com.android.tools.idea.templates.Parameter.Type
import com.android.tools.idea.templates.TemplateMetadata.ATTR_ANDROIDX_SUPPORT
import com.android.tools.idea.templates.TemplateMetadata.ATTR_BUILD_API
import com.android.tools.idea.templates.TemplateMetadata.ATTR_BUILD_API_STRING
import com.android.tools.idea.templates.TemplateMetadata.ATTR_HAS_APPLICATION_THEME
import com.android.tools.idea.templates.TemplateMetadata.ATTR_IS_LAUNCHER
import com.android.tools.idea.templates.TemplateMetadata.ATTR_IS_LIBRARY_MODULE
import com.android.tools.idea.templates.TemplateMetadata.ATTR_KOTLIN_VERSION
import com.android.tools.idea.templates.TemplateMetadata.ATTR_LANGUAGE
import com.android.tools.idea.templates.TemplateMetadata.ATTR_MIN_API
import com.android.tools.idea.templates.TemplateMetadata.ATTR_MIN_API_LEVEL
import com.android.tools.idea.templates.TemplateMetadata.ATTR_PACKAGE_NAME
import com.android.tools.idea.templates.TemplateMetadata.ATTR_TARGET_API
import com.android.tools.idea.templates.TemplateMetadata.ATTR_TARGET_API_STRING
import com.android.tools.idea.templates.TemplateMetadata.ATTR_THEME_EXISTS
import com.android.tools.idea.templates.TemplateMetadata.getBuildApiString
import com.android.tools.idea.testing.AndroidGradleTestCase
import com.android.tools.idea.testing.IdeComponents
import java.io.File
import kotlin.system.measureTimeMillis

typealias ProjectStateCustomizer = (templateMap: MutableMap<String, Any>, projectMap: MutableMap<String, Any>) -> Unit

/**
 * Base class for test for template instantiation.
 *
 * Remaining work on template test:
 * - Start using new NewProjectModel etc to initialise TemplateParameters and set parameter values.
 * - Fix clean model syncing, and hook up clean lint checks.
 * - Test more combinations of parameters.
 * - Test all combinations of build tools.
 */
open class TemplateTestBase : AndroidGradleTestCase() {
  /**
   * A UsageTracker implementation that allows introspection of logged metrics in tests.
   */
  private lateinit var usageTracker: TestUsageTracker

  override fun createDefaultProject() = false

  override fun setUp() {
    super.setUp()
    usageTracker = TestUsageTracker(VirtualTimeScheduler())
    setWriterForTest(usageTracker)
    apiSensitiveTemplate = true

    // Replace the default RepositoryUrlManager with one that enables repository checks in tests. (myForceRepositoryChecksInTests)
    // This is necessary to fully resolve dynamic gradle coordinates such as ...:appcompat-v7:+ => appcompat-v7:25.3.1
    // keeping it exactly the same as they are resolved within the NPW flow.
    IdeComponents(null, testRootDisposable).replaceApplicationService(
      RepositoryUrlManager::class.java,
      RepositoryUrlManager(IdeGoogleMavenRepository, OfflineIdeGoogleMavenRepository, true))
  }

  override fun tearDown() {
    try {
      usageTracker.close()
      cleanAfterTesting()
    }
    finally {
      super.tearDown()
    }
  }

  /**
   * If true, check this template with all the interesting ([isInterestingApiLevel]) API versions.
   */
  protected var apiSensitiveTemplate = false

  protected val withKotlin = { templateMap: MutableMap<String, Any>, projectMap: MutableMap<String, Any> ->
    projectMap[ATTR_KOTLIN_VERSION] = getKotlinVersionForTests()
    projectMap[ATTR_LANGUAGE] = Language.KOTLIN.toString()
    templateMap[ATTR_LANGUAGE] = Language.KOTLIN.toString()
    templateMap[ATTR_PACKAGE_NAME] = "test.pkg.in" // Add in a Kotlin keyword ("in") in the package name to trigger escape code too
  }

  /**
   * Checks the given template in the given category
   *
   * @param category          the template category
   * @param name              the template name
   * @param createWithProject whether the template should be created as part of creating the project (only for activities), or whether it
   * should be added as as a separate template into an existing project (which is created first, followed by the template).
   * @param customizer        An instance of [ProjectStateCustomizer] used for providing template and project overrides.
   */
  protected open fun checkCreateTemplate(
    category: String, name: String, createWithProject: Boolean = false, customizer: ProjectStateCustomizer = { _, _ -> }
  ) {
    if (DISABLED) {
      return
    }
    ensureSdkManagerAvailable()
    val templateFile = findTemplate(category, name)
    if (isBroken(templateFile.name)) {
      return
    }
    val templateOverrides = mutableMapOf<String, Any>()
    val projectOverrides = mutableMapOf<String, Any>()
    customizer(templateOverrides, projectOverrides)
    val msToCheck = measureTimeMillis {
      checkTemplate(templateFile, createWithProject, templateOverrides, projectOverrides)
    }
    println("Checked ${templateFile.name} successfully in ${msToCheck}ms")
  }

  private fun checkTemplate(
    templateFile: File, createWithProject: Boolean, overrides: Map<String, Any>, projectOverrides: Map<String, Any>
  ) {
    require(!isBroken(templateFile.name))
    val sdkData = AndroidSdks.getInstance().tryToChooseAndroidSdk()
    val projectState = createNewProjectState(createWithProject, sdkData!!, getModuleTemplateForFormFactor(templateFile))
    val activityState = projectState.activityTemplateState.apply { setTemplateLocation(templateFile) }
    val moduleState = projectState.moduleTemplateState

    fun <T> Iterable<T>.takeOneIfOtherwiseAll(condition: Boolean) = if (condition) take(1) else take(Int.MAX_VALUE)

    // Iterate over all (valid) combinations of build target, minSdk and targetSdk
    // TODO: Assert that the SDK manager has a minimum set of SDKs installed needed to be certain the test is comprehensive
    // For now make sure there's at least one
    var ranTest = false
    val lowestMinApiForProject = (moduleState[ATTR_MIN_API] as String).toInt().coerceAtLeast(moduleState.template.metadata!!.minSdk)
    val targets = sdkData.targets.reversed()
      .filter { it.isPlatform && isInterestingApiLevel(it.version.apiLevel, MANUAL_BUILD_API, apiSensitiveTemplate) }
      .takeOneIfOtherwiseAll(TEST_JUST_ONE_BUILD_TARGET)
    for (target in targets) {
      val activityMetadata = activityState.template.metadata!!
      val moduleMetadata = moduleState.template.metadata!!
      val lowestSupportedApi = activityMetadata.minSdk.coerceAtLeast(lowestMinApiForProject)

      val interestingMinSdks = (lowestSupportedApi..SdkVersionInfo.HIGHEST_KNOWN_API)
        .filter { isInterestingApiLevel(it, MANUAL_MIN_API, apiSensitiveTemplate) }
        .takeOneIfOtherwiseAll(TEST_JUST_ONE_MIN_SDK)

      for (minSdk in interestingMinSdks) {
        val interestingTargetSdks = (minSdk..SdkVersionInfo.HIGHEST_KNOWN_API)
          .filter { isInterestingApiLevel(it, MANUAL_TARGET_API, apiSensitiveTemplate) }
          .takeOneIfOtherwiseAll(TEST_JUST_ONE_TARGET_SDK_VERSION)
          .filter {
            moduleMetadata.validateTemplate(minSdk, target.version.apiLevel) == null &&
            activityMetadata.validateTemplate(minSdk, target.version.apiLevel) == null
          }

        for (targetSdk in interestingTargetSdks) {
          // Should we try all options of theme with all platforms, or just try all platforms, with one setting for each?
          // Doesn't seem like we need to multiply, just pick the best setting that applies instead for each platform.
          val hasEnums = moduleMetadata.parameters.any { it.type == Type.ENUM }
          if (hasEnums && overrides.isEmpty()) {
            // TODO: Handle all enums here. None of the projects have this currently at this level.
            return fail("Not expecting enums at the root level")
          }
          var base = "${templateFile.name}_min_${minSdk}_target_${targetSdk}_build_${target.version.apiLevel}"
          if (overrides.isNotEmpty()) {
            base += "_overrides"
          }
          checkApiTarget(minSdk, targetSdk, target, projectState, base, activityState, overrides, projectOverrides)
          ranTest = true
        }
      }
    }
    assertTrue("Didn't run any tests! Make sure you have the right platforms installed.", ranTest)
  }

  /**
   * Checks creating the given project and template for the given SDK versions
   */
  protected fun checkApiTarget(
    minSdk: Int,
    targetSdk: Int,
    target: IAndroidTarget,
    projectState: TestNewProjectWizardState,
    projectNameBase: String,
    activityState: TestTemplateWizardState?,
    overrides: Map<String, Any>,
    projectOverrides: Map<String, Any>
  ) {
    val moduleState = projectState.moduleTemplateState
    val createActivity = moduleState[ATTR_CREATE_ACTIVITY] as Boolean? ?: true
    val templateState = (if (createActivity) projectState.activityTemplateState else activityState)!!

    moduleState.apply {
      put(ATTR_MIN_API, minSdk.toString())
      put(ATTR_MIN_API_LEVEL, minSdk)
      put(ATTR_TARGET_API, targetSdk)
      put(ATTR_TARGET_API_STRING, targetSdk.toString())
      put(ATTR_BUILD_API, target.version.apiLevel)
      put(ATTR_BUILD_API_STRING, getBuildApiString(target.version))
    }

    // Next check all other parameters, cycling through booleans and enums.
    var parameters = templateState.template.metadata!!.parameters
    if (!createActivity) {
      templateState.setParameterDefaults()
    }
    else {
      val moduleMetadata = moduleState.template.metadata!!
      parameters = parameters + moduleMetadata.parameters
    }
    templateState.putAll(overrides)
    moduleState.putAll(projectOverrides)
    
    for (parameter in parameters) {
      if (parameter.type === Type.SEPARATOR || parameter.type === Type.STRING) {
        // TODO: Consider whether we should attempt some strings here
        continue
      }
      if (!COMPREHENSIVE && SKIPPABLE_PARAMETERS.contains(parameter.id)) {
        continue
      }
      if (overrides.isNotEmpty() && overrides.containsKey(parameter.id)) {
        continue
      }

      // revert to this one after cycling
      val initial = parameter.getDefaultValue(templateState)
      if (parameter.type === Type.ENUM) {
        val options = parameter.options
        for (element in options) {
          val (optionId, optionMinSdk, optionMinBuildApi) = getOption(element)
          val projectMinApi = moduleState.getInt(ATTR_MIN_API_LEVEL)
          val projectBuildApi = moduleState.getInt(ATTR_BUILD_API)
          if (projectMinApi >= optionMinSdk && projectBuildApi >= optionMinBuildApi && optionId != initial) {
            templateState.put(parameter.id!!, optionId)
            val projectName = "${projectNameBase}_${parameter.id}_$optionId"
            checkProject(projectName, projectState, activityState)
            if (!COMPREHENSIVE) {
              break
            }
          }
        }
      }
      else {
        assert(parameter.type === Type.BOOLEAN)
        if (parameter.id == ATTR_IS_LAUNCHER && createActivity) {
          // Skipping this one: always true when launched from new project
          continue
        }
        // For boolean values, only run checkProject in the non-default setting.
        // The default value is already used when running checkProject in the default state for all variables.
        val value = !(initial as Boolean)
        templateState.put(parameter.id!!, value)
        val projectName = "${projectNameBase}_${parameter.id}_$value"
        checkProject(projectName, projectState, activityState)
      }
      templateState.put(parameter.id!!, initial!!)
    }
    val projectName = "${projectNameBase}_default"
    checkProject(projectName, projectState, activityState)
  }

  private fun checkProject(projectName: String, projectState: TestNewProjectWizardState, activityState: TestTemplateWizardState?) {
    val moduleState = projectState.moduleTemplateState

    val templateMetadata = activityState?.template?.metadata
    val checkLib = "Activity" == templateMetadata?.category && "Mobile" == templateMetadata.formFactor &&
               !moduleState.getBoolean(ATTR_CREATE_ACTIVITY)
    if (templateMetadata?.androidXRequired == true) {
      enableAndroidX(moduleState, activityState)
    }

    val language = Language.fromName(moduleState[ATTR_LANGUAGE] as String?, Language.JAVA)

    val projectChecker = ProjectChecker(CHECK_LINT, projectState, activityState, usageTracker, language)
    if (moduleState[ATTR_ANDROIDX_SUPPORT] != true) {
      // Make sure we test all templates against androidx
      enableAndroidX(moduleState, activityState)
      projectChecker.checkProject(projectName + "_x")
      disableAndroidX(moduleState, activityState)
    }
    // check that new Activities can be created on lib modules as well as app modules.
    if (!checkLib) {
      projectChecker.checkProject(projectName)
      return
    }
    moduleState.put(ATTR_IS_LIBRARY_MODULE, false)
    activityState!!.put(ATTR_IS_LIBRARY_MODULE, false)
    activityState.put(ATTR_HAS_APPLICATION_THEME, true)
    projectChecker.checkProject(projectName)
    moduleState.put(ATTR_IS_LIBRARY_MODULE, true)
    activityState.put(ATTR_IS_LIBRARY_MODULE, true)
    activityState.put(ATTR_HAS_APPLICATION_THEME, false)
    // For a library project a theme doesn't exist. This is derived in the IDE using FmGetApplicationThemeMethod
    moduleState.put(ATTR_THEME_EXISTS, false)
    projectChecker.checkProject(projectName + "_lib")
  }

  @MustBeDocumented
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  annotation class TemplateCheck
}

/**
 * Whether we should run comprehensive tests or not. This flag allows a simple run to just check a small set of
 * template combinations, and when the flag is set on the build server, a much more comprehensive battery of
 * checks to be performed.
 */
private val COMPREHENSIVE =
  System.getProperty("com.android.tools.idea.templates.TemplateTest.COMPREHENSIVE").orEmpty().toBoolean() ||
  "true".equals(System.getenv("com.android.tools.idea.templates.TemplateTest.COMPREHENSIVE"), true)
/**
 * Whether we should run these tests or not.
 */
internal val DISABLED =
  System.getProperty("DISABLE_STUDIO_TEMPLATE_TESTS").orEmpty().toBoolean() ||
  "true".equals(System.getenv("DISABLE_STUDIO_TEMPLATE_TESTS"), true)
/**
 * Whether we should enforce that lint passes cleanly on the projects
 */
internal const val CHECK_LINT = false // Needs work on closing projects cleanly
/**
 * Manual sdk version selections
 */
private val MANUAL_BUILD_API =
  System.getProperty("com.android.tools.idea.templates.TemplateTest.MANUAL_BUILD_API")?.toIntOrNull() ?: -1
private val MANUAL_MIN_API =
  System.getProperty("com.android.tools.idea.templates.TemplateTest.MANUAL_MIN_API")?.toIntOrNull() ?: -1
private val MANUAL_TARGET_API =
  System.getProperty("com.android.tools.idea.templates.TemplateTest.MANUAL_TARGET_API")?.toIntOrNull() ?: -1
/**
 * The following templates parameters are not very interesting (change only one small bit of text etc).
 * We can skip them when not running in comprehensive mode.
 * TODO(qumeric): update or remove
 */
private val SKIPPABLE_PARAMETERS = setOf<String>()
/**
 * Flags used to quickly check each template once (for one version), to get
 * quicker feedback on whether something is broken instead of waiting for
 * all the versions for each template first
 */
internal val TEST_FEWER_API_VERSIONS = !COMPREHENSIVE
private val TEST_JUST_ONE_MIN_SDK = !COMPREHENSIVE
private val TEST_JUST_ONE_BUILD_TARGET = !COMPREHENSIVE
private val TEST_JUST_ONE_TARGET_SDK_VERSION = !COMPREHENSIVE
// TODO: this is used only in TemplateTest. We should pass this value without changing template values.
internal const val ATTR_CREATE_ACTIVITY = "createActivity"
