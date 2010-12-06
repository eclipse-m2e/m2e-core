package org.eclipse.m2e.editor.composites;

import org.eclipse.swt.graphics.Color;

public class ManageDependencyLabelProvider extends DependencyLabelProvider {
  @Override
  public Color getForeground(Object element) {
    /*
     * Super class shows scope=compile dependencies as greyed out, which we
     * don't want to do here.
     */
    return null;
  }
}
