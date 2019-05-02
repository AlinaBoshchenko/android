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
package com.android.tools.idea.uibuilder.handlers.constraint;

import com.android.tools.idea.uibuilder.scout.Scout;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MarginPopup extends JPanel {

  public static class MarginValue {

    private int myValue = Scout.DEFAULT_MARGIN;
    @NotNull private String myDisplayValue = myValue + "dp";

    public void setValue(int value, String resName) {
      myValue = value;
      Scout.setMargin(value);
      if (resName != null) {
        myDisplayValue = DEFAULT_RES_DISPLAY;
        Scout.setMarginResource(resName);
      } else {
        myDisplayValue = value + "dp";
        Scout.setMarginResource(null);
      }
    }

    public int getValue() {
      return myValue;
    }

    /**
     * @return shows valid display value of default margin. (e.g. "@ ..." or "38dp"
     */
    @NotNull
    public String getDisplayValue() {
      return myDisplayValue;
    }
  }

  private static final float DEFAULT_FONT_SIZE = 12f;
  private static final String DEFAULT_RES_DISPLAY = "@ ...";
  private final JBTextField myDefaultMargin = new JBTextField("Default Margin : ");
  private final JBTextField myTextField = new JBTextField();
  private final JButton[] myHistoryButtons = new JButton[3];
  private final JButton myResourcePickerButton = new JButton();
  private final int[] myDefaultValues = {0, 8, 16, 24};
  private final int[] myHistoryValues = {-1, -1, -1};

  ActionListener myListener;
  private JBPopup myPopup;
  private final MarginValue myValue = new MarginValue();

  public MarginValue getMargin() {
    return myValue;
  }

  public void setPopup(@Nullable JBPopup popup) {
    for (Component component : getComponents()) {
      updateFontsForPresentationMode(component);
    }

    myPopup = popup;
  }

  private ActionListener myDefaultListener = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      JButton b = (JButton)e.getSource();
      String s = b.getText();
      try {
        myValue.setValue(Integer.parseInt(s), null);
      }
      catch (NumberFormatException e1) {
        myValue.setValue(0, null);
      }
      updateText();
      if (myListener != null) {
        myListener.actionPerformed(e);
      }
      cancel();
    }
  };

  public void cancel() {
    if (myPopup != null) {
      myPopup.cancel();
    }
    myPopup = null;
  }

  public void updateText() {
    myTextField.setText(String.valueOf(myValue.getValue()));
  }

  private boolean isADefault(int value) {
    for (int i = 0; i < myDefaultValues.length; i++) {
      if (myDefaultValues[i] == value) {
        return true;
      }
    }
    return false;
  }

  private ActionListener mTextListener = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      saveNewValue(e);
      SwingUtilities.getWindowAncestor(MarginPopup.this).setVisible(false);
    }
  };

  public void setActionListener(ActionListener actionListener) {
    myListener = actionListener;
  }

  void saveNewValue(ActionEvent e) {
    try {
      myValue.setValue(Integer.parseInt(myTextField.getText()), null);
      int value = myValue.getValue();
      if (!isADefault(value)) {

        for (int i = 0; i < myHistoryValues.length; i++) {
          int old = myHistoryValues[i];
          myHistoryValues[i] = value;
          if (value > 0) {
            myHistoryButtons[i].setText(Integer.toString(value));
          }
          value = old;
        }
      }
    }
    catch (NumberFormatException e1) {
      e1.printStackTrace();
    }
    if (myListener != null) {
      myListener.actionPerformed(e);
    }
  }

  public MarginPopup(final ActionListener resourcePickerActionListener) {
    super(new GridBagLayout());
    setBackground(JBColor.background());

    GridBagConstraints gc = new GridBagConstraints();
    myDefaultMargin.setEditable(false);
    myDefaultMargin.setFocusable(false);
    myDefaultMargin.setBorder(BorderFactory.createEmptyBorder());

    gc.gridx = 0;
    gc.gridy = 0;
    gc.gridwidth = 2;
    gc.insets = JBUI.insets(10, 5, 5, 0);
    gc.fill = GridBagConstraints.BOTH;
    myDefaultMargin.setHorizontalAlignment(SwingConstants.RIGHT);
    add(myDefaultMargin, gc);

    gc.gridx = 2;
    gc.gridy = 0;
    gc.gridwidth = 1;
    gc.insets = JBUI.insets(10, 5, 5, 0);
    gc.fill = GridBagConstraints.BOTH;
    myTextField.setHorizontalAlignment(SwingConstants.RIGHT);
    myTextField.setText(Integer.toString(myValue.getValue()));
    add(myTextField, gc);

    gc.gridx = 3;
    gc.gridwidth = 1;
    gc.insets.bottom = JBUI.scale(0);
    gc.insets.left = JBUI.scale(2);
    gc.insets.right = JBUI.scale(5);
    gc.insets.top = JBUI.scale(2);
    add(new JBLabel("dp"), gc);

    gc.gridwidth = 1;

    gc.gridy = 1;
    gc.insets.bottom = JBUI.scale(0);
    gc.insets.left = JBUI.scale(0);
    gc.insets.right = JBUI.scale(0);
    gc.insets.top = JBUI.scale(0);
    ((AbstractDocument)myTextField.getDocument()).setDocumentFilter(new DocumentFilter() {
      @Override
      public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        for (int i = 0; i < text.length(); i++) {
          if (!Character.isDigit(text.charAt(i))) {
            return;
          }
        }
        super.replace(fb, offset, length, text, attrs);
      }
    });
    gc.fill = GridBagConstraints.HORIZONTAL;
    myTextField.addActionListener(mTextListener);
    Insets margin = JBUI.insets(0, 0, 0, 0);
    for (int i = 0; i < 4; i++) {
      JButton b = new JButton("" + myDefaultValues[i]);
      b.setMargin(margin);
      b.addActionListener(myDefaultListener);
      b.setBackground(JBColor.background());
      gc.gridx = i;
      gc.insets.left = JBUI.scale((i == 0) ? 5 : 0);
      gc.insets.right = JBUI.scale((i == 3) ? 5 : 0);
      add(b, gc);
    }
    gc.gridy = 2;
    gc.insets.bottom = JBUI.scale(7);
    for (int i = 0; i < myHistoryButtons.length; i++) {
      myHistoryButtons[i] = new JButton("XXX");
      myHistoryButtons[i].setMargin(margin);
      myHistoryButtons[i].setBackground(JBColor.background());
      if (myHistoryValues[i] > 0) {
        myHistoryButtons[i].setText(Integer.toString(myHistoryValues[i]));
      }
      else {
        myHistoryButtons[i].setText("");
      }
      myHistoryButtons[i].addActionListener(myDefaultListener);

      gc.gridx = i;
      gc.insets.left = JBUI.scale((i == 0) ? 5 : 0);
      gc.insets.right = JBUI.scale((i == 3) ? 5 : 0);
      add(myHistoryButtons[i], gc);
    }
    myResourcePickerButton.setMargin(margin);
    myResourcePickerButton.setText("@ ...");
    myResourcePickerButton.addActionListener(resourcePickerActionListener);
    gc.gridx = 3;
    gc.insets.left = JBUI.scale(0);
    gc.insets.right = JBUI.scale(5);
    add(myResourcePickerButton, gc);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
      }

      @Override
      public void componentHidden(ComponentEvent e) {
        saveNewValue(null);
      }
    });
  }

  public JComponent getTextField() {
    return myTextField;
  }

  private void updateFontsForPresentationMode(Component component) {
    Font font = component.getFont();
    component.setFont(font.deriveFont((float) JBUI.scaleFontSize(DEFAULT_FONT_SIZE)));
  }
}
