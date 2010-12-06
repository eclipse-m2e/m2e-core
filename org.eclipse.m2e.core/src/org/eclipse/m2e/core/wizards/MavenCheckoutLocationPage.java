/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.wizards;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
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
import org.eclipse.swt.widgets.Text;

import org.apache.maven.model.Scm;

import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.scm.ScmHandlerFactory;
import org.eclipse.m2e.core.scm.ScmHandlerUi;
import org.eclipse.m2e.core.scm.ScmTag;
import org.eclipse.m2e.core.scm.ScmUrl;


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

    SelectionAdapter selectionAdapter = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updatePage();
      }
    };

    if(scmUrls == null || scmUrls.length < 2) {
      Label urlLabel = new Label(composite, SWT.NONE);
      urlLabel.setLayoutData(new GridData());
      urlLabel.setText(Messages.MavenCheckoutLocationPage_lblurl);

      scmTypeCombo = new Combo(composite, SWT.READ_ONLY);
      scmTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      scmTypeCombo.setData("name", "mavenCheckoutLocation.typeCombo"); //$NON-NLS-1$ //$NON-NLS-2$
      String[] types = ScmHandlerFactory.getTypes();
      for(int i = 0; i < types.length; i++ ) {
        scmTypeCombo.add(types[i]);
      }
      scmTypeCombo.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          String newScmType = scmTypeCombo.getText();
          if(!newScmType.equals(scmType)) {
            scmType = newScmType;
            scmUrlCombo.setText(""); //$NON-NLS-1$
            updatePage();
          }
        }
      });
      
      if(scmUrls!=null && scmUrls.length == 1) {
        try {
          scmType = ScmUrl.getType(scmUrls[0].getUrl());
        } catch(CoreException ex) {
        }
      }

      scmUrlCombo = new Combo(composite, SWT.NONE);
      scmUrlCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
      scmUrlCombo.setData("name", "mavenCheckoutLocation.urlCombo"); //$NON-NLS-1$ //$NON-NLS-2$

      scmUrlBrowseButton = new Button(composite, SWT.NONE);
      scmUrlBrowseButton.setLayoutData(new GridData());
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
      if(tag!=null) {
        headRevisionButton.setSelection(false);
        revisionText.setText(tag.getName());
      }
    }
    
    revisionText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updatePage();
      }
    });

    revisionBrowseButton = new Button(composite, SWT.NONE);
    GridData gd_revisionBrowseButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_revisionBrowseButton.verticalIndent = 3;
    revisionBrowseButton.setLayoutData(gd_revisionBrowseButton);
    revisionBrowseButton.setText(Messages.MavenCheckoutLocationPage_btnRevSelect);
    revisionBrowseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        String url = scmParentUrl;
        if(url==null) {
          return;
        }
        
        String scmType = scmTypeCombo.getText();
        
        ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);
        String revision = handlerUi.selectRevision(getShell(), scmUrls[0], revisionText.getText());
        if(revision!=null) {
          revisionText.setText(revision);
          headRevisionButton.setSelection(false);
          updatePage();
        }
      }
    });

    checkoutAllProjectsButton = new Button(composite, SWT.CHECK);
    GridData checkoutAllProjectsData = new GridData(SWT.LEFT, SWT.TOP, true, false, 5, 1);
    checkoutAllProjectsData.verticalIndent = 10;
    checkoutAllProjectsButton.setLayoutData(checkoutAllProjectsData);
    checkoutAllProjectsButton.setText(Messages.MavenCheckoutLocationPage_btnCheckout);
    checkoutAllProjectsButton.setSelection(true);
    checkoutAllProjectsButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updatePage();
      }
    });

    GridData advancedSettingsData = new GridData(SWT.FILL, SWT.TOP, true, false, 5, 1);
    advancedSettingsData.verticalIndent = 10;
    createAdvancedSettings(composite, advancedSettingsData);

    if(scmUrls!=null && scmUrls.length == 1) {
      scmTypeCombo.setText(scmType == null ? "" : scmType); //$NON-NLS-1$
      scmUrlCombo.setText(scmUrls[0].getProviderUrl());
    }

    if(scmUrls == null || scmUrls.length < 2) {
      scmUrlBrowseButton.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);
          // XXX should use null if there is no scmUrl selected
          ScmUrl currentUrl = scmUrls==null || scmUrls.length==0 ? new ScmUrl("scm:" + scmType + ":") : scmUrls[0]; //$NON-NLS-1$ //$NON-NLS-2$
          ScmUrl scmUrl = handlerUi.selectUrl(getShell(), currentUrl);
          if(scmUrl!=null) {
            scmUrlCombo.setText(scmUrl.getProviderUrl());
            if(scmUrls==null) {
              scmUrls = new ScmUrl[1];
            }
            scmUrls[0] = scmUrl;
            scmParentUrl = scmUrl.getUrl();
            updatePage();
          }
        }
      });
      
      scmUrlCombo.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          final String url = scmUrlCombo.getText();
          if(url.startsWith("scm:")) { //$NON-NLS-1$
            try {
              final String type = ScmUrl.getType(url);
              scmTypeCombo.setText(type);
              scmType = type;
              Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                  scmUrlCombo.setText(url.substring(type.length() + 5));
                }
              });
            } catch(CoreException ex) {
            }
            return;
          }
          
          if(scmUrls==null) {
            scmUrls = new ScmUrl[1];
          }
          
          ScmUrl scmUrl = new ScmUrl("scm:" + scmType + ":" + url); //$NON-NLS-1$ //$NON-NLS-2$
          scmUrls[0] = scmUrl;
          scmParentUrl = scmUrl.getUrl();
          updatePage();
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
    
    if(dialogSettings!=null && scmUrlCombo!=null) {
      String[] items = dialogSettings.getArray("scmUrl"); //$NON-NLS-1$
      if(items != null) {
        String text = scmUrlCombo.getText();
        scmUrlCombo.setItems(items);
        if (text.length() > 0) {
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
    if(dialogSettings != null && scmUrlCombo!=null) {
      Set<String> history = new LinkedHashSet<String>(MAX_HISTORY);
      
      String lastValue = scmUrlCombo.getText();
      if ( lastValue!=null && lastValue.trim().length() > 0 ) {
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
    boolean canSelectUrl = false ;
    boolean canSelectRevision = false;
    ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);
    if(handlerUi!=null) {
      canSelectUrl = handlerUi.canSelectUrl();
      canSelectRevision = handlerUi.canSelectRevision();
    }
    
    if(scmUrlBrowseButton!=null) {
      scmUrlBrowseButton.setEnabled(canSelectUrl);
    }

    revisionBrowseButton.setEnabled(canSelectRevision);

    boolean isHeadRevision = isHeadRevision();
    revisionLabel.setEnabled(!isHeadRevision);
    revisionText.setEnabled(!isHeadRevision);
    
    setPageComplete(isPageValid());
  }

  private boolean isPageValid() {
    setErrorMessage(null);
    
    if(scmUrls != null && scmUrls.length < 2) { 
      if(scmType == null) {
        setErrorMessage(Messages.MavenCheckoutLocationPage_error_empty);
        return false;
      }
    }

    ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);
    
    if(scmUrls == null || scmUrls.length < 2) {
      if(scmUrls == null || scmUrls.length == 0) {
        setErrorMessage(Messages.MavenCheckoutLocationPage_error_empty_url);
        return false;
      }
      
      if(handlerUi!=null && !handlerUi.isValidUrl(scmUrls[0].getUrl())) {
        setErrorMessage(Messages.MavenCheckoutLocationPage_error_url_empty);
        return false;
      }
    }
    
    if(!isHeadRevision()) {
      String revision = revisionText.getText().trim();
      if(revision.length()==0) {
        setErrorMessage(Messages.MavenCheckoutLocationPage_error_scm_empty);
        return false;
      }
      
      if(handlerUi!=null && !handlerUi.isValidRevision(null, revision)) {
        setErrorMessage(Messages.MavenCheckoutLocationPage_error_scm_invalid);
        return false;
      }      
    }
    
    return true;
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
    if(scmUrls==null) {
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
    ModifyListener listenerProxy = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        Event event = new Event();
        event.widget = e.widget;
        listener.widgetSelected(new SelectionEvent(event));
      }
    };
    scmUrlCombo.addModifyListener(listenerProxy);
    revisionText.addModifyListener(listenerProxy);
    headRevisionButton.addSelectionListener(listener);
  }

}
