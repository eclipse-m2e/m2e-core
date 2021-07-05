
package org.eclipse.m2e.core.ui.internal.dialogs;

import java.beans.Beans;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;

import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;


public class InputHistory {
  /** the history limit */
  protected static final int MAX_HISTORY = 10;

  /** dialog settings to store input history */
  protected IDialogSettings dialogSettings;

  /** the Map of field ids to List of comboboxes that share the same history */
  private final Map<String, List<ControlWrapper>> comboMap;

  private final List<String> privileged;

  public InputHistory(String sectionName) {
    this(sectionName, new String[0]);
  }

  public InputHistory(String sectionName, String[] privileged) {
    comboMap = new HashMap<>();

    M2EUIPluginActivator plugin = M2EUIPluginActivator.getDefault();
    if(plugin != null) {
      IDialogSettings pluginSettings = plugin.getDialogSettings();
      dialogSettings = pluginSettings.getSection(sectionName);
      if(dialogSettings == null) {
        dialogSettings = pluginSettings.addNewSection(sectionName);
        pluginSettings.addSection(dialogSettings);
      }
    }
    assert privileged != null;
    this.privileged = Arrays.asList(privileged);
  }

  /** Loads the input history from the dialog settings. */
  public void load() {
    if(Beans.isDesignTime()) {
      return;
    }

    for(Map.Entry<String, List<ControlWrapper>> e : comboMap.entrySet()) {
      String id = e.getKey();
      Set<String> items = new LinkedHashSet<>();
      String[] itemsArr = dialogSettings.getArray(id);
      items.addAll(privileged);
      if(itemsArr != null) {
        items.addAll(Arrays.asList(itemsArr));
      }
      for(ControlWrapper wrapper : e.getValue()) {
        if(!wrapper.isDisposed()) {
          wrapper.setItems(items.toArray(new String[0]));
        }
      }
    }
  }

  /** Saves the input history into the dialog settings. */
  public void save() {
    if(Beans.isDesignTime()) {
      return;
    }

    for(Map.Entry<String, List<ControlWrapper>> e : comboMap.entrySet()) {
      String id = e.getKey();

      Set<String> history = new LinkedHashSet<>(MAX_HISTORY);

      for(ControlWrapper wrapper : e.getValue()) {
        wrapper.collect();
        String lastValue = wrapper.text;
        if(lastValue != null && lastValue.trim().length() > 0) {
          history.add(lastValue);
        }
      }

      ControlWrapper wrapper = e.getValue().iterator().next();
      String[] items = wrapper.items;
      if(items != null) {
        for(int j = 0; j < items.length && history.size() < MAX_HISTORY; j++ ) {
          // do not store the privileged items if they are not selected.
          // we eventually inject the same or different set next time
          if(!privileged.contains(items[j])) {
            history.add(items[j]);
          }
        }
      }

      dialogSettings.put(id, history.toArray(new String[history.size()]));
    }
  }

  /** Adds an input control to the list of fields to save. */
  public void add(Control combo) {
    add(null, combo);
  }

  /** Adds an input control to the list of fields to save. */
  public void add(String id, final Control combo) {
    if(combo != null) {
      if(id == null) {
        id = String.valueOf(combo.getData("name"));
      }
      List<ControlWrapper> combos = comboMap.get(id);
      if(combos == null) {
        combos = new ArrayList<>();
        comboMap.put(id, combos);
      }
      if(combo instanceof Combo) {
        combos.add(new ComboWrapper((Combo) combo));
      } else if(combo instanceof CCombo) {
        combos.add(new CComboWrapper((CCombo) combo));
      }
    }
  }

  abstract private class ControlWrapper {
    protected Control control;

    protected String text;

    protected String[] items;

    private boolean collected;

    protected ControlWrapper(Control control) {
      this.control = control;
      control.addDisposeListener(e -> collect());
    }

    protected void collect() {
      if(!collected && !isDisposed()) {
        text = getText();
        items = getItems();
      }
      collected = true;
    }

    protected boolean isDisposed() {
      return control.isDisposed();
    }

    abstract protected String getText();

    abstract protected String[] getItems();

    abstract protected void setItems(String[] items);
  }

  private class ComboWrapper extends ControlWrapper {
    private final Combo combo;

    protected ComboWrapper(Combo combo) {
      super(combo);
      this.combo = combo;
    }

    @Override
    protected String getText() {
      return combo.getText();
    }

    @Override
    protected String[] getItems() {
      return combo.getItems();
    }

    @Override
    protected void setItems(String[] items) {
      String value = combo.getText();
      combo.setItems(items);
      if(value.length() > 0) {
        // setItems() clears the text input, so we need to restore it
        combo.setText(value);
      } else if(items.length > 0) {
        combo.setText(items[0]);
      }
    }
  }

  private class CComboWrapper extends ControlWrapper {
    private final CCombo combo;

    protected CComboWrapper(CCombo combo) {
      super(combo);
      this.combo = combo;
    }

    @Override
    protected String getText() {
      return combo.getText();
    }

    @Override
    protected String[] getItems() {
      try {
        return combo.getItems();
      } catch(SWTException swtException) {
        //CCombo throws this if the list is disposed, but the combo itself is not disposed yet
        return new String[0];
      }
    }

    @Override
    protected void setItems(String[] items) {
      String value = combo.getText();
      combo.setItems(items);
      if(value.length() > 0) {
        // setItems() clears the text input, so we need to restore it
        combo.setText(value);
      }
    }
  }
}
