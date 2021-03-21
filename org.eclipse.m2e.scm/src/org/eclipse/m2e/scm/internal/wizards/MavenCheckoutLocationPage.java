/*******************************************************************************
 * Copyright (c) 2008-2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.scm.internal.wizards;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import org.apache.maven.model.Scm;

import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.IMavenDiscovery;
import org.eclipse.m2e.core.ui.internal.wizards.AbstractMavenWizardPage;
import org.eclipse.m2e.scm.ScmTag;
import org.eclipse.m2e.scm.ScmUrl;
import org.eclipse.m2e.scm.internal.Messages;
import org.eclipse.m2e.scm.internal.ScmHandlerFactory;
import org.eclipse.m2e.scm.spi.ScmHandlerUi;


/**
 * @author Eugene Kuleshov
 */
public class MavenCheckoutLocationPage extends AbstractMavenWizardPage {

  String scmType;

  ScmUrl[] scmUrls;

  String scmParentUrl;

  Combo scmTypeCombo;

  Combo scmUrlCombo;

  Button scmUrlBrowseButton;

  Button headRevisionButton;

  Label revisionLabel;

  Text revisionText;

  Button revisionBrowseButton;

  private Button checkoutAllProjectsButton;

  private Link m2eMarketplace;

  protected MavenCheckoutLocationPage(ProjectImportConfiguration projectImportConfiguration) {
    super("MavenCheckoutLocationPage", projectImportConfiguration);
    setTitle(Messages.MavenCheckoutLocationPage_title);
    setDescription(Messages.MavenCheckoutLocationPage_description);
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(5, false);
    gridLayout.verticalSpacing = 0;
    composite.setLayout(gridLayout);
    setControl(composite);

    SelectionListener selectionAdapter = SelectionListener.widgetSelectedAdapter(e -> updatePage());

    if(scmUrls == null || scmUrls.length < 2) {
      Label urlLabel = new Label(composite, SWT.NONE);
      urlLabel.setText(Messages.MavenCheckoutLocationPage_lblurl);

      scmTypeCombo = new Combo(composite, SWT.READ_ONLY);
      GridData gd_scmTypeCombo = new GridData(SWT.FILL, SWT.CENTER, false, false);
      gd_scmTypeCombo.widthHint = 80;
      scmTypeCombo.setLayoutData(gd_scmTypeCombo);
      scmTypeCombo.setData("name", "mavenCheckoutLocation.typeCombo"); //$NON-NLS-1$ //$NON-NLS-2$
      String[] types = ScmHandlerFactory.getTypes();
      for(String type : types) {
        scmTypeCombo.add(type);
      }
      scmTypeCombo.addModifyListener(e -> {
        String newScmType = scmTypeCombo.getText();
        if(!newScmType.equals(scmType)) {
          scmType = newScmType;
          scmUrlCombo.setText(""); //$NON-NLS-1$
          updatePage();
        }
      });

      if(scmUrls != null && scmUrls.length == 1) {
        try {
          scmType = ScmUrl.getType(scmUrls[0].getUrl());
        } catch(CoreException ex) {
        }
      }

      scmUrlCombo = new Combo(composite, SWT.NONE);
      scmUrlCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
      scmUrlCombo.setData("name", "mavenCheckoutLocation.urlCombo"); //$NON-NLS-1$ //$NON-NLS-2$

      scmUrlBrowseButton = new Button(composite, SWT.NONE);
      scmUrlBrowseButton.setText(Messages.MavenCheckoutLocationPage_btnBrowse);
    }

    headRevisionButton = new Button(composite, SWT.CHECK);
    GridData headRevisionButtonData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 5, 1);
    headRevisionButtonData.verticalIndent = 5;
    headRevisionButton.setLayoutData(headRevisionButtonData);
    headRevisionButton.setText(Messages.MavenCheckoutLocationPage_btnHead);
    headRevisionButton.setSelection(true);
    headRevisionButton.addSelectionListener(selectionAdapter);

    revisionLabel = new Label(composite, SWT.RADIO);
    GridData revisionButtonData = new GridData();
    revisionButtonData.horizontalIndent = 10;
    revisionLabel.setLayoutData(revisionButtonData);
    revisionLabel.setText(Messages.MavenCheckoutLocationPage_lblRevision);
    // revisionButton.addSelectionListener(selectionAdapter);

    revisionText = new Text(composite, SWT.BORDER);
    GridData revisionTextData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    revisionTextData.widthHint = 115;
    revisionTextData.verticalIndent = 3;
    revisionText.setLayoutData(revisionTextData);

    if(scmUrls != null) {
      ScmTag tag = scmUrls[0].getTag();
      if(tag != null) {
        headRevisionButton.setSelection(false);
        revisionText.setText(tag.getName());
      }
    }

    revisionText.addModifyListener(e -> updatePage());

    revisionBrowseButton = new Button(composite, SWT.NONE);
    GridData gd_revisionBrowseButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_revisionBrowseButton.verticalIndent = 3;
    revisionBrowseButton.setLayoutData(gd_revisionBrowseButton);
    revisionBrowseButton.setText(Messages.MavenCheckoutLocationPage_btnRevSelect);
    revisionBrowseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      String url = scmParentUrl;
      if(url == null) {
        return;
      }

      String scmType = scmTypeCombo.getText();

      ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);
      String revision = handlerUi.selectRevision(getShell(), scmUrls[0], revisionText.getText());
      if(revision != null) {
        revisionText.setText(revision);
        headRevisionButton.setSelection(false);
        updatePage();
      }
    }));

    checkoutAllProjectsButton = new Button(composite, SWT.CHECK);
    GridData checkoutAllProjectsData = new GridData(SWT.LEFT, SWT.TOP, true, false, 5, 1);
    checkoutAllProjectsData.verticalIndent = 10;
    checkoutAllProjectsButton.setLayoutData(checkoutAllProjectsData);
    checkoutAllProjectsButton.setText(Messages.MavenCheckoutLocationPage_btnCheckout);
    checkoutAllProjectsButton.setSelection(true);
    checkoutAllProjectsButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updatePage()));

    GridData advancedSettingsData = new GridData(SWT.FILL, SWT.TOP, true, false, 5, 1);
    advancedSettingsData.verticalIndent = 10;
    createAdvancedSettings(composite, advancedSettingsData);

    if(scmUrls != null && scmUrls.length == 1) {
      scmTypeCombo.setText(scmType == null ? "" : scmType); //$NON-NLS-1$
      scmUrlCombo.setText(scmUrls[0].getProviderUrl());
    }

    if(scmUrls == null || scmUrls.length < 2) {
      scmUrlBrowseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
        ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);
        // XXX should use null if there is no scmUrl selected
        ScmUrl currentUrl = scmUrls == null || scmUrls.length == 0 ? new ScmUrl("scm:" + scmType + ":") : scmUrls[0]; //$NON-NLS-1$ //$NON-NLS-2$
        ScmUrl scmUrl = handlerUi.selectUrl(getShell(), currentUrl);
        if(scmUrl != null) {
          scmUrlCombo.setText(scmUrl.getProviderUrl());
          if(scmUrls == null) {
            scmUrls = new ScmUrl[1];
          }
          scmUrls[0] = scmUrl;
          scmParentUrl = scmUrl.getUrl();
          updatePage();
        }
      }));

      scmUrlCombo.addModifyListener(e -> {
        final String url = scmUrlCombo.getText().trim();
        if(url.startsWith("scm:")) { //$NON-NLS-1$
          try {
            final String type = ScmUrl.getType(url);
            scmTypeCombo.setText(type);
            scmType = type;
            Display.getDefault().asyncExec(() -> scmUrlCombo.setText(url.substring(type.length() + 5)));
          } catch(CoreException ex) {
          }
          return;
        }

        if(scmUrls == null) {
          scmUrls = new ScmUrl[1];
        }

        ScmUrl scmUrl = new ScmUrl("scm:" + scmType + ":" + url); //$NON-NLS-1$ //$NON-NLS-2$
        scmUrls[0] = scmUrl;
        scmParentUrl = scmUrl.getUrl();
        updatePage();
      });
    }
    if(Platform.getBundle("org.eclipse.m2e.discovery") != null) {
      m2eMarketplace = new Link(composite, SWT.NONE);
      m2eMarketplace.setLayoutData(new GridData(SWT.END, SWT.END, true, true, 5, 1));
      m2eMarketplace.setText(Messages.MavenCheckoutLocationPage_linkMarketPlace);
      m2eMarketplace.addSelectionListener(new SelectionListener() {

        public void widgetSelected(SelectionEvent e) {
          IWizardContainer container = getWizard().getContainer();
          if(container instanceof WizardDialog) {
            ((WizardDialog) container).close();
          }
          IExtensionRegistry registry = RegistryFactory.getRegistry();
          IExtensionPoint point = registry.getExtensionPoint("org.eclipse.m2e.core.ui.discoveryLaunch");
          if(point != null) {
            IExtension[] extension = point.getExtensions();
            if(extension.length > 0) {
              for(IConfigurationElement element : extension[0].getConfigurationElements()) {
                if(element.getName().equals("launcher")) {
                  try {
                    ((IMavenDiscovery) element.createExecutableExtension("class"))
                        .launch(Display.getCurrent().getActiveShell());
                    break;
                  } catch(CoreException e1) {
                    //
                  }
                }
              }
            }
          }
        }

        public void widgetDefaultSelected(SelectionEvent e) {

        }
      });
    }

    updatePage();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.wizards.AbstractMavenWizardPage#setVisible(boolean)
   */
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    if(scmType == null && scmTypeCombo != null && scmTypeCombo.getItems().length == 1
        && !scmTypeCombo.getItem(0).isEmpty()) {
      scmTypeCombo.select(0);
    }

    if(dialogSettings != null && scmUrlCombo != null) {
      String[] items = dialogSettings.getArray("scmUrl"); //$NON-NLS-1$
      if(items != null) {
        String text = scmUrlCombo.getText();
        scmUrlCombo.setItems(items);
        if(text.length() > 0) {
          // setItems() clears the text input, so we need to restore it
          scmUrlCombo.setText(text);
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.wizards.AbstractMavenWizardPage#dispose()
   */
  public void dispose() {
    if(dialogSettings != null && scmUrlCombo != null) {
      Set<String> history = new LinkedHashSet<String>(MAX_HISTORY);

      String lastValue = scmUrlCombo.getText();
      if(lastValue != null && lastValue.trim().length() > 0) {
        history.add("scm:" + scmType + ":" + lastValue); //$NON-NLS-1$ //$NON-NLS-2$
      }

      String[] items = scmUrlCombo.getItems();
      for(int j = 0; j < items.length && history.size() < MAX_HISTORY; j++ ) {
        history.add(items[j]);
      }

      dialogSettings.put("scmUrl", history.toArray(new String[history.size()])); //$NON-NLS-1$
    }

    super.dispose();
  }

  public IWizardContainer getContainer() {
    return super.getContainer();
  }

  void updatePage() {
    boolean canSelectUrl = false;
    boolean canSelectRevision = false;
    ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);
    if(handlerUi != null) {
      canSelectUrl = handlerUi.canSelectUrl();
      canSelectRevision = handlerUi.canSelectRevision();
    }

    if(scmUrlBrowseButton != null) {
      scmUrlBrowseButton.setEnabled(canSelectUrl);
      scmUrlBrowseButton.setVisible(canSelectUrl);
    }
    revisionBrowseButton.setEnabled(canSelectRevision);
    revisionBrowseButton.setVisible(canSelectRevision);

    boolean isHeadRevision = isHeadRevision();
    revisionLabel.setEnabled(!isHeadRevision);
    revisionText.setEnabled(!isHeadRevision);

    setPageComplete(isPageValid());
  }

  private boolean isPageValid() {
    setErrorMessage(null);

    boolean emptyUrl = isEmptyScmUrl(scmUrls);

    if(scmType == null && emptyUrl) {
      setErrorMessage(Messages.MavenCheckoutLocationPage_error_empty);
      return false;
    }

    if(emptyUrl) {
      setErrorMessage(Messages.MavenCheckoutLocationPage_error_empty_url);
      return false;
    }

    ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);

    if(handlerUi != null && !handlerUi.isValidUrl(scmUrls[0].getUrl())) {
      setErrorMessage(Messages.MavenCheckoutLocationPage_error_url_empty);
      return false;
    }

    if(!isHeadRevision()) {
      String revision = revisionText.getText().trim();
      if(revision.length() == 0) {
        setErrorMessage(Messages.MavenCheckoutLocationPage_error_scm_empty);
        return false;
      }

      if(handlerUi != null && !handlerUi.isValidRevision(null, revision)) {
        setErrorMessage(Messages.MavenCheckoutLocationPage_error_scm_invalid);
        return false;
      }
    }

    return true;
  }

  private boolean isEmptyScmUrl(ScmUrl[] scmUrls) {
    if(scmUrls == null || scmUrls.length == 0) {
      return true;
    }
    String type = null;
    String url = scmUrls[0].getUrl();
    if(url != null) {
      try {
        type = ScmUrl.getType(url);
      } catch(CoreException ignore) {
      }
    }
    return url == null || url.isEmpty() || ("scm:" + type + ":").equals(url);
  }

  public void setParent(String parentUrl) {
    this.scmParentUrl = parentUrl;
  }

  public void setUrls(ScmUrl[] urls) {
    this.scmUrls = urls;
  }

  public ScmUrl[] getUrls() {
    return scmUrls;
  }

  public Scm[] getScms() {
    if(scmUrls == null) {
      return new Scm[0];
    }

    String revision = getRevision();
    Scm[] scms = new Scm[scmUrls.length];
    for(int i = 0; i < scms.length; i++ ) {
      Scm scm = new Scm();
      scm.setConnection(scmUrls[i].getUrl());
      scm.setTag(revision);
      scms[i] = scm;
    }
    return scms;
  }

  public boolean isCheckoutAllProjects() {
    return checkoutAllProjectsButton.getSelection();
  }

  public boolean isHeadRevision() {
    return headRevisionButton.getSelection();
  }

  public String getRevision() {
    if(isHeadRevision()) {
      return "HEAD"; //$NON-NLS-1$
    }
    return revisionText.getText().trim();
  }

  public void addListener(final SelectionListener listener) {
    ModifyListener listenerProxy = e -> {
      Event event = new Event();
      event.widget = e.widget;
      listener.widgetSelected(new SelectionEvent(event));
    };
    scmUrlCombo.addModifyListener(listenerProxy);
    revisionText.addModifyListener(listenerProxy);
    headRevisionButton.addSelectionListener(listener);
  }

}
