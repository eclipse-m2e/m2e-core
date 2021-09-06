
package org.eclipse.m2e.editor.dialogs;

import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.m2e.core.ui.internal.wizards.MavenProjectSelectionDialog;
import org.eclipse.m2e.editor.internal.Messages;


public class MavenModuleSelectionDialog extends MavenProjectSelectionDialog {
  protected Set<Object> knownModules;

  protected boolean pomUpdateRequired = false;

  public MavenModuleSelectionDialog(Shell parent, Set<Object> knownModules) {
    super(parent, true);
    this.knownModules = knownModules;
    setTitle(Messages.OverviewPage_selectModuleProjects);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Control control = super.createDialogArea(parent);

    final TreeViewer viewer = getViewer();
    viewer.setLabelProvider(new ProjectLabelProvider());
    viewer.getTree().addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      if(e.detail == SWT.CHECK) {
        TreeItem item = (TreeItem) e.item;
        Object data = item.getData();
        if(item.getChecked() && data instanceof IResource && knownModules.contains(((IResource) data).getLocation())) {
          item.setChecked(false);
        }
      }
    }));
    viewer.getTree().setFocus();

    final Button checkbox = new Button((Composite) control, SWT.CHECK);
    checkbox.setSelection(false);
    checkbox.setText(Messages.OverviewPage_updateModulePoms);
    checkbox.addSelectionListener(
        SelectionListener.widgetSelectedAdapter(e -> pomUpdateRequired = checkbox.getSelection()));

    return control;
  }

  public boolean isPomUpdateRequired() {
    return pomUpdateRequired;
  }

  protected class ProjectLabelProvider extends LabelProvider implements IColorProvider {
    private final ILabelProvider labelProvider = WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();

    @Override
    public String getText(Object element) {
      return labelProvider.getText(element);
    }

    @Override
    public Image getImage(Object element) {
      return labelProvider.getImage(element);
    }

    @Override
    public Color getForeground(Object element) {
      if(element instanceof IResource && knownModules.contains(((IResource) element).getLocation())) {
        return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
      }
      return null;
    }

    @Override
    public Color getBackground(Object element) {
      return null;
    }
  }
}
