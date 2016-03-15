/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.gradle.structure.configurables.ui;

import com.android.tools.idea.gradle.structure.configurables.PsContext;
import com.android.tools.idea.gradle.structure.model.PsModule;
import com.android.tools.idea.gradle.structure.model.PsProject;
import com.android.tools.idea.gradle.util.ui.LabeledComboBoxAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.containers.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ModulesComboBoxAction extends LabeledComboBoxAction {
  @NotNull private final PsProject myProject;
  @NotNull private final PsContext myContext;

  public ModulesComboBoxAction(@NotNull PsProject project, @NotNull PsContext context) {
    super("Module: ");
    myProject = project;
    myContext = context;
  }

  @Override
  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setIcon(AllIcons.Nodes.Module);
    presentation.setText(myContext.getSelectedModule());
  }

  @Override
  @NotNull
  protected DefaultActionGroup createPopupActionGroup(JComponent button) {
    final DefaultActionGroup group = new DefaultActionGroup();
    myProject.forEachModule(new Predicate<PsModule>() {
      @Override
      public boolean apply(@Nullable PsModule module) {
        if (module == null) {
          return false;
        }
        group.add(new ModuleAction(module));
        return true;
      }
    });
    return group;
  }

  private class ModuleAction extends DumbAwareAction {
    @NotNull private final String myModuleName;

    ModuleAction(@NotNull PsModule module) {
      super(module.getName(), "", module.getIcon());
      myModuleName = module.getName();
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      myContext.setSelectedModule(myModuleName, this);
    }
  }
}
