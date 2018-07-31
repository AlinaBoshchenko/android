/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.gradle.structure.configurables

import com.android.tools.idea.gradle.structure.configurables.android.buildvariants.AndroidModuleBuildVariantsConfigurable
import com.android.tools.idea.gradle.structure.model.PsModule
import com.android.tools.idea.gradle.structure.model.android.PsAndroidModule
import com.android.tools.idea.structure.dialog.TrackedConfigurable
import com.google.wireless.android.sdk.stats.PSDEvent
import com.intellij.openapi.ui.NamedConfigurable

const val BUILD_VARIANTS_PERSPECTIVE_DISPLAY_NAME = "Build Variants"
const val BUILD_VARIANTS_PERSPECTIVE_PLACE_NAME = "build_variants.place"

class BuildVariantsPerspectiveConfigurable(context: PsContext)
  : BasePerspectiveConfigurable(context), TrackedConfigurable {

  override val leftConfigurable = PSDEvent.PSDLeftConfigurable.PROJECT_STRUCTURE_DIALOG_LEFT_CONFIGURABLE_BUILD_VARIANTS

  override fun getId() = "android.psd.build_variants"

  override fun createConfigurableFor(module: PsModule): NamedConfigurable<out PsModule>? =
      if (module is PsAndroidModule) createConfigurable(module) else null

  override val navigationPathName: String = BUILD_VARIANTS_PERSPECTIVE_PLACE_NAME

  override fun getDisplayName() = BUILD_VARIANTS_PERSPECTIVE_DISPLAY_NAME

  private fun createConfigurable(module: PsAndroidModule) =
      AndroidModuleBuildVariantsConfigurable(context, module).apply { history = myHistory }
}