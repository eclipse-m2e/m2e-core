/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.m2e.internal.discovery.wizards;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.CopyUtils;
import org.eclipse.equinox.internal.p2.ui.dialogs.ILayoutConstants;
import org.eclipse.equinox.internal.p2.ui.dialogs.IResolutionErrorReportingPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.IUDetailsGroup;
import org.eclipse.equinox.internal.p2.ui.dialogs.InstallWizardPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.ResolutionStatusPage;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.p2.ui.viewers.IUColumnConfig;
import org.eclipse.equinox.internal.p2.ui.viewers.IUComparator;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.p2.ui.viewers.ProvElementComparer;
import org.eclipse.equinox.internal.p2.ui.viewers.ProvElementContentProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.AcceptLicensesWizardPage;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.m2e.internal.discovery.operation.RestartInstallOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;


/*
 * modified from org.eclipse.equinox.internal.p2.ui.dialogs.SelectableIUsPage 
 */
@SuppressWarnings("restriction")
public class DiscoverySelectableIUsPage extends ResolutionStatusPage implements IResolutionErrorReportingPage {

  private static final String DIALOG_SETTINGS_SECTION = "SelectableIUsPage"; //$NON-NLS-1$
  private RestartInstallOperation operation;

  private Object[] planSelections;

  private IWizardPage previousPage;

  IUElementListRoot root;
	Object[] initialSelections;
	CheckboxTableViewer tableViewer;
	IUDetailsGroup iuDetailsGroup;
	ProvElementContentProvider contentProvider;
	IUDetailsLabelProvider labelProvider;
	protected Display display;
	protected Policy policy;
	SashForm sashForm;

  public DiscoverySelectableIUsPage(ProvisioningUI ui, RestartInstallOperation operation, IUElementListRoot root,
      IInstallableUnit[] ius) {
    super("IUSelectionPage", ui, new MavenDiscoveryInstallWizard(ui, operation, operation.getIUs(), null, null)); //$NON-NLS-1$
    this.initialSelections = ius;
    this.root = root;
    this.operation = operation;
    setTitle(ProvUIMessages.InstallWizardPage_Title);
    setDescription(ProvUIMessages.PreselectedIUInstallWizard_Description);
    initializeResolutionModelElements(this.initialSelections);
    ((MavenDiscoveryInstallWizard) getProvisioningWizard()).setMainPage(this);
    ((MavenDiscoveryInstallWizard) getProvisioningWizard()).setResolutionResultsPage(createResolutionPage());
    updateStatus(root, operation);
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
	public void createControl(Composite parent) {
		display = parent.getDisplay();
		sashForm = new SashForm(parent, SWT.VERTICAL);
		FillLayout layout = new FillLayout();
		sashForm.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		sashForm.setLayoutData(data);
		initializeDialogUnits(sashForm);

		Composite composite = new Composite(sashForm, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		composite.setLayout(gridLayout);

		tableViewer = createTableViewer(composite);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		data.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH);
		Table table = tableViewer.getTable();
		table.setLayoutData(data);
		table.setHeaderVisible(true);
		activateCopy(table);
		IUColumnConfig[] columns = getColumnConfig();
		for (int i = 0; i < columns.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.LEFT, i);
			tc.setResizable(true);
			tc.setText(columns[i].getColumnTitle());
			tc.setWidth(columns[i].getWidthInPixels(table));
		}

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
        setDetailText(operation);
			}
		});

		tableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateSelection();
			}
		});

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(ProvUI.getIUColumnConfig());
		tableViewer.setComparator(comparator);
		tableViewer.setComparer(new ProvElementComparer());

		contentProvider = new ProvElementContentProvider();
		tableViewer.setContentProvider(contentProvider);
		labelProvider = new IUDetailsLabelProvider(null, ProvUI.getIUColumnConfig(), getShell());
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.setInput(root);
		setInitialCheckState();

		// Select and Deselect All buttons
		createSelectButtons(composite);

		// The text area shows a description of the selected IU, or error detail if applicable.
		iuDetailsGroup = new IUDetailsGroup(sashForm, tableViewer, convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH), true);

    updateStatus(root, operation);
		setControl(sashForm);
		sashForm.setWeights(getSashWeights());
		Dialog.applyDialogFont(sashForm);
	}

	private void createSelectButtons(Composite parent) {
		Composite buttonParent = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		buttonParent.setLayout(gridLayout);
		GridData data = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		buttonParent.setLayoutData(data);

		Button selectAll = new Button(buttonParent, SWT.PUSH);
		selectAll.setText(ProvUIMessages.SelectableIUsPage_Select_All);
		setButtonLayoutData(selectAll);
		selectAll.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				tableViewer.setAllChecked(true);
				updateSelection();
			}
		});

		Button deselectAll = new Button(buttonParent, SWT.PUSH);
		deselectAll.setText(ProvUIMessages.SelectableIUsPage_Deselect_All);
		setButtonLayoutData(deselectAll);
		deselectAll.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				tableViewer.setAllChecked(false);
				updateSelection();
			}
		});

		// dummy to take extra space
		Label dummy = new Label(buttonParent, SWT.NONE);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		dummy.setLayoutData(data);

		// separator underneath
		Label sep = new Label(buttonParent, SWT.HORIZONTAL | SWT.SEPARATOR);
		data = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		data.horizontalSpan = 3;
		sep.setLayoutData(data);
	}

	protected CheckboxTableViewer createTableViewer(Composite parent) {
		// The viewer allows selection of IU's for browsing the details,
		// and checking to include in the provisioning operation.
		CheckboxTableViewer v = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		return v;
	}

	public Object[] getCheckedIUElements() {
		if (tableViewer == null)
			return initialSelections;
		return tableViewer.getCheckedElements();
	}

	public Object[] getSelectedIUElements() {
		return ((IStructuredSelection) tableViewer.getSelection()).toArray();
	}

	protected Object[] getSelectedElements() {
		return ((IStructuredSelection) tableViewer.getSelection()).toArray();
	}

	protected IInstallableUnit[] elementsToIUs(Object[] elements) {
		IInstallableUnit[] theIUs = new IInstallableUnit[elements.length];
		for (int i = 0; i < elements.length; i++) {
			theIUs[i] = ProvUI.getAdapter(elements[i], IInstallableUnit.class);
		}
		return theIUs;
	}

	protected void setInitialCheckState() {
		if (initialSelections != null)
			tableViewer.setCheckedElements(initialSelections);
	}

	/*
	 * Overridden so that we don't call getNextPage().
	 * We use getNextPage() to start resolving the operation so
	 * we only want to do that when the next button is pressed.
	 * 
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	protected String getClipboardText(Control control) {
		StringBuffer buffer = new StringBuffer();
		Object[] elements = getSelectedElements();
		for (int i = 0; i < elements.length; i++) {
			if (i > 0)
				buffer.append(CopyUtils.NEWLINE);
			buffer.append(labelProvider.getClipboardText(elements[i], CopyUtils.DELIMITER));
		}
		return buffer.toString();
	}

	protected IInstallableUnit getSelectedIU() {
		java.util.List<IInstallableUnit> units = ElementUtils.elementsToIUs(getSelectedElements());
		if (units.size() == 0)
			return null;
		return units.get(0);
	}

	protected IUDetailsGroup getDetailsGroup() {
		return iuDetailsGroup;
	}

	protected boolean isCreated() {
		return tableViewer != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ResolutionStatusPage#updateCaches(org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot, org.eclipse.equinox.p2.operations.ProfileChangeOperation)
	 */
	protected void updateCaches(IUElementListRoot newRoot, ProfileChangeOperation op) {
    //resolvedOperation = op;
		if (newRoot != null && root != newRoot) {
			root = newRoot;
			if (tableViewer != null)
				tableViewer.setInput(newRoot);
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage#setCheckedElements(java.lang.Object[])
	 */
	public void setCheckedElements(Object[] elements) {
		if (tableViewer == null)
			initialSelections = elements;
		else
			tableViewer.setCheckedElements(elements);
	}

	protected SashForm getSashForm() {
		return sashForm;
	}

	protected String getDialogSettingsName() {
		return getWizard().getClass().getName() + "." + DIALOG_SETTINGS_SECTION; //$NON-NLS-1$
	}

	protected int getColumnWidth(int index) {
		return tableViewer.getTable().getColumn(index).getWidth();
	}

	void updateSelection() {
    setPageComplete(tableViewer.getCheckedElements().length == 0 || operation.getResolutionResult().isOK());
		getProvisioningWizard().operationSelectionsChanged(this);
	}

  private void setDetailText(ProfileChangeOperation resolvedOperation) {
    String detail = null;
    IInstallableUnit selectedIU = getSelectedIU();
    IUDetailsGroup detailsGroup = getDetailsGroup();

    // We either haven't resolved, or we failed to resolve and reported some error
    // while doing so.  
    if(resolvedOperation == null || !resolvedOperation.hasResolved()
        || getProvisioningWizard().statusOverridesOperation()) {
      // See if the wizard status knows something more about it.
      IStatus currentStatus = getProvisioningWizard().getCurrentStatus();
      if(!currentStatus.isOK()) {
        detail = currentStatus.getMessage();
        detailsGroup.enablePropertyLink(false);
      } else if(selectedIU != null) {
        detail = getIUDescription(selectedIU);
        detailsGroup.enablePropertyLink(true);
      } else {
        detail = ""; //$NON-NLS-1$
        detailsGroup.enablePropertyLink(false);
      }
      detailsGroup.setDetailText(detail);
      return;
    }

    // An IU is selected and we have resolved.  Look for information about the specific IU.
    if(selectedIU != null) {
      detail = resolvedOperation.getResolutionDetails(selectedIU);
      if(detail != null) {
        detailsGroup.enablePropertyLink(false);
        detailsGroup.setDetailText(detail);
        return;
      }
      // No specific error about this IU.  Show the overall error if it is in error.
      if(resolvedOperation.getResolutionResult().getSeverity() == IStatus.ERROR) {
        detail = resolvedOperation.getResolutionDetails();
        if(detail != null) {
          detailsGroup.enablePropertyLink(false);
          detailsGroup.setDetailText(detail);
          return;
        }
      }

      // The overall status is not an error, or else there was no explanatory text for an error.
      // We may as well just show info about this iu.
      detailsGroup.enablePropertyLink(true);
      detailsGroup.setDetailText(getIUDescription(selectedIU));
      return;
    }

    //No IU is selected, give the overall report
    detail = resolvedOperation.getResolutionDetails();
    detailsGroup.enablePropertyLink(false);
    if(detail == null)
      detail = ""; //$NON-NLS-1$
    detailsGroup.setDetailText(detail);
  }

  public IWizardPage getNextPage() {
    // If we are moving from the main page or error page, we may need to resolve before
    // advancing.
    // Do we need to resolve?
    if(operation == null
        || (operation != null && ((MavenDiscoveryInstallWizard) getProvisioningWizard()).shouldRecomputePlan(this))) {
      getProvisioningWizard().recomputePlan(getContainer());
    } else {
      // the selections have not changed from an IU point of view, but we want
      // to reinitialize the resolution model elements to ensure they are up to
      // date.
      initializeResolutionModelElements(planSelections);
    }
    IStatus status = operation.getResolutionResult();
    if(status == null || status.getSeverity() == IStatus.ERROR) {
      return this;
    } else if(status.getSeverity() == IStatus.CANCEL) {
      return this;
    } else {
      IWizardPage page = createResolutionPage();
      page.setWizard(getWizard());
      return page;
    }
  }

  public IWizardPage getPreviousPage() {
    if(previousPage != null) {
      return previousPage;
    }

    if(getWizard() == null) {
      return null;
    }

    return getWizard().getPreviousPage(this);
  }

  public void setPreviousPage(IWizardPage page) {
    previousPage = page;
  }

  private InstallPage createResolutionPage() {
    return new InstallPage(getProvisioningUI(), root, operation);
  }

  protected void initializeResolutionModelElements(Object[] selectedElements) {
    root = new IUElementListRoot();
    ArrayList<AvailableIUElement> list = new ArrayList<AvailableIUElement>(selectedElements.length);
    ArrayList<AvailableIUElement> selected = new ArrayList<AvailableIUElement>(selectedElements.length);
    for(int i = 0; i < selectedElements.length; i++ ) {
      IInstallableUnit iu = ElementUtils.getIU(selectedElements[i]);
      if(iu != null) {
        AvailableIUElement element = new AvailableIUElement(root, iu, operation.getProfileId(), false);
        list.add(element);
        selected.add(element);
      }
    }
    root.setChildren(list.toArray());
    planSelections = selected.toArray();
  }

  private static class InstallPage extends InstallWizardPage {
    private RestartInstallOperation operation;

    private AcceptLicensesWizardPage nextPage;

    public InstallPage(ProvisioningUI ui, IUElementListRoot root, RestartInstallOperation operation) {
      super(ui, new MavenDiscoveryInstallWizard(ui, operation, operation.getIUs(), null, null), root, operation);
      this.operation = operation;
    }

    public IWizardPage getNextPage() {
      if(nextPage == null) {
        nextPage = new AcceptLicensesWizardPage(ProvisioningUI.getDefaultUI().getLicenseManager(), operation.getIUs()
            .toArray(new IInstallableUnit[operation.getIUs().size()]), operation);
        nextPage.setWizard(getWizard());
      }
      return nextPage;
    }
  }
}
