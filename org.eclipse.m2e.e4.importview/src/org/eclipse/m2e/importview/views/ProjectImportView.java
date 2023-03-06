/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.importview.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.wizards.ImportMavenProjectsJob;
import org.eclipse.m2e.e4.importview.MavenE4ImportViewPlugin;
import org.eclipse.m2e.e4.importview.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

/**
 * This view can be used to import Maven Projects into Eclipse Workspace.
 *
 * @author Nikolaus Winter, comdirect bank AG
 */
@SuppressWarnings("restriction")
public class ProjectImportView extends ViewPart {

	public static final String ID = "org.eclipse.m2e.importview.views.ProjectImportView";

	private String rootDirectory;
	private List<MavenProjectInfo> projectsToImport = new ArrayList<>();
	private List<String> savedRootDirectories;

	// UI Elements
	private Combo rootDirectoryCombo;
	private TreeViewer projectTreeViewer;
	private Text filterText;
	private ListViewer projectImportListViewer;
	private Button removeEclipseFilesCheckbox;

	@Override
	public void createPartControl(final Composite parent) {
		parent.setLayout(new GridLayout(3, false));

		createLeftPanel(parent);
		createCenterPanel(parent);
		createRightPanel(parent);
	}

	private void createLeftPanel(final Composite parent) {
		final int totalWidth = 7;

		Composite left = new Composite(parent, SWT.NONE);
		left.setLayout(new GridLayout(totalWidth, false));
		GridData leftCompositeLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		left.setLayoutData(leftCompositeLayoutData);

		final Label selectRootDirectoryLabel = new Label(left, SWT.NONE);
		selectRootDirectoryLabel.setLayoutData(new GridData());
		selectRootDirectoryLabel.setText(Messages.labelRootDirectory);

		rootDirectoryCombo = new Combo(left, SWT.READ_ONLY);
		rootDirectoryCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		rootDirectoryCombo.addSelectionListener(new RootDirectoryComboSelectionHandler(parent));
		if (savedRootDirectories != null && !savedRootDirectories.isEmpty()) {
			for (String savedRootDirectory : savedRootDirectories) {
				rootDirectoryCombo.add(savedRootDirectory);
			}
		}

		final Button browseButton = new Button(left, SWT.NONE);
		browseButton.setText(Messages.buttonBrowseRootDirectory);
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		browseButton.addSelectionListener(new BrowseForDirectoryHandler(parent));

		final Button removeRootDirectoryButton = new Button(left, SWT.NONE);
		removeRootDirectoryButton.setText(Messages.buttonRemoveRootDirectory);
		removeRootDirectoryButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		removeRootDirectoryButton.setToolTipText(Messages.buttonRemoveRootDirectoryTooltip);
		removeRootDirectoryButton.addSelectionListener(new RemoveRootDirectoryHandler());

		final Button reloadButton = new Button(left, SWT.NONE);
		reloadButton.setImage(
				MavenE4ImportViewPlugin.getDefault().getImageRegistry().get(MavenE4ImportViewPlugin.ICON_RELOAD));
		reloadButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		reloadButton.setToolTipText(Messages.buttonReloadTooltip);
		reloadButton.addSelectionListener(new ReloadRepoHandler());

		final Button collapseButton = new Button(left, SWT.NONE);
		collapseButton.setImage(
				MavenE4ImportViewPlugin.getDefault().getImageRegistry().get(MavenE4ImportViewPlugin.ICON_COLLAPSE));
		collapseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		collapseButton.setToolTipText(Messages.buttonCollapseToLevel1Tooltip);
		collapseButton.addSelectionListener(new CollapseTreeHandler());

		final Button expandButton = new Button(left, SWT.NONE);
		expandButton.setImage(
				MavenE4ImportViewPlugin.getDefault().getImageRegistry().get(MavenE4ImportViewPlugin.ICON_EXPAND));
		expandButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		expandButton.setToolTipText(Messages.buttonExpandAllTooltip);
		expandButton.addSelectionListener(new ExpandTreeHandler());

		final Label filterLabel = new Label(left, SWT.NONE);
		filterLabel.setLayoutData(new GridData());
		filterLabel.setText(Messages.labelFilterProjects);

		filterText = new Text(left, SWT.BORDER + SWT.SEARCH + SWT.ICON_CANCEL);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, totalWidth - 1, 1));
		filterText.addModifyListener(new FilterChangedHandler());

		final Label projectsLabel = new Label(left, SWT.NONE);
		projectsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		projectsLabel.setText(Messages.labelProjectTreeViewer);

		projectTreeViewer = new TreeViewer(left, SWT.BORDER | SWT.MULTI);
		projectTreeViewer.setContentProvider(new ProjectSelectionTreeContentProvider());
		projectTreeViewer.setLabelProvider(new ProjectSelectionLabelProvider());
		projectTreeViewer.setComparator(new ProjectSelectionViewerComparator());
		projectTreeViewer.addDoubleClickListener(new SelectProjectByDoubleClickHandler());

		final Tree projectTree = projectTreeViewer.getTree();
		GridData projectTreeData = new GridData(SWT.FILL, SWT.FILL, true, true, totalWidth - 1, 1);
		projectTree.setLayoutData(projectTreeData);
	}

	private void createCenterPanel(final Composite parent) {
		final int totalWidth = 1;

		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(totalWidth, false));

		final Button addAllButton = new Button(center, SWT.NONE);
		addAllButton.setImage(
				MavenE4ImportViewPlugin.getDefault().getImageRegistry().get(MavenE4ImportViewPlugin.ICON_ARROW_RIGHT));
		addAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		addAllButton.setToolTipText(Messages.buttonAddAllTooltip);
		addAllButton.addSelectionListener(new AddAllSelectedProjectsToImportListHandler());

		final Button removeAllButton = new Button(center, SWT.NONE);
		removeAllButton.setImage(
				MavenE4ImportViewPlugin.getDefault().getImageRegistry().get(MavenE4ImportViewPlugin.ICON_ARROW_LEFT));
		removeAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		removeAllButton.setToolTipText(Messages.buttonRemoveAllTooltip);
		removeAllButton.addSelectionListener(new RemoveAllSelectedProjectsFromImportListHandler());
	}

	private void createRightPanel(final Composite parent) {
		final int totalWidth = 4;

		Composite right = new Composite(parent, SWT.NONE);
		right.setLayout(new GridLayout(totalWidth, false));
		GridData rightCompositeLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		right.setLayoutData(rightCompositeLayoutData);

		final Label projectImportLabel = new Label(right, SWT.NONE);
		projectImportLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, totalWidth, 1));
		projectImportLabel.setText(Messages.labelProjectImportList);

		projectImportListViewer = new ListViewer(right, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		// TODO: Prüfen, ob der Content Provider nicht ggf. zu speziell ist???
		projectImportListViewer.setContentProvider(new ProjectSelectionTreeContentProvider());
		projectImportListViewer.setLabelProvider(new ProjectSelectionLabelProvider());
		projectImportListViewer.setInput(this.projectsToImport);
		projectImportListViewer.addDoubleClickListener(new DeselectProjectByDoubleClickHandler());

		final org.eclipse.swt.widgets.List projectList = projectImportListViewer.getList();
		GridData projectListData = new GridData(SWT.FILL, SWT.FILL, true, true, totalWidth, 1);
		projectList.setLayoutData(projectListData);

		final Button clearProjectListButton = new Button(right, SWT.NONE);
		clearProjectListButton.setText(Messages.buttonClearProjectList);
		clearProjectListButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		clearProjectListButton.addSelectionListener(new ClearProjectsToImportListHandler());

		final Button importButton = new Button(right, SWT.NONE);
		importButton.setText(Messages.buttonImportProjects);
		importButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		importButton.addSelectionListener(new ImportProjectsHandler());

		final Button exportSelectionButton = new Button(right, SWT.NONE);
		exportSelectionButton.setImage(MavenE4ImportViewPlugin.getDefault().getImageRegistry()
				.get(MavenE4ImportViewPlugin.ICON_SAVE_IMPORT_SELECTION));
		exportSelectionButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		exportSelectionButton.setToolTipText(Messages.buttonExportListTooltip);
		exportSelectionButton.addSelectionListener(new ExportSelectionHandler(parent));

		final Button importSelectionButton = new Button(right, SWT.NONE);
		importSelectionButton.setImage(MavenE4ImportViewPlugin.getDefault().getImageRegistry()
				.get(MavenE4ImportViewPlugin.ICON_LOAD_IMPORT_SELECTION));
		importSelectionButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		importSelectionButton.setToolTipText(Messages.buttonImportListTooltip);
		importSelectionButton.addSelectionListener(new ImportSelectionHandler(parent));

		removeEclipseFilesCheckbox = new Button(right, SWT.CHECK);
		removeEclipseFilesCheckbox.setText(Messages.labelRemoveEclipseFiles);
		removeEclipseFilesCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, totalWidth, 1));
		// FIXME: remember last state
		removeEclipseFilesCheckbox.setSelection(true);
	}

	@Override
	public void setFocus() {
		rootDirectoryCombo.setFocus();
	}

	private void reloadProjectSelectionList() {
		if (!StringUtils.isEmpty(rootDirectory)) {
			loadProjectSelectionList(rootDirectory);
		} else {
			projectTreeViewer.setInput(null);
		}
	}

	/**
	 * Reads the project(s) from the given root location and fills the project
	 * selection tree viewer.
	 *
	 * @param location root location of Maven Project Folder
	 *
	 * @return Has a non-empty project list been loaded?
	 */
	private boolean loadProjectSelectionList(String location) {
		if (StringUtils.isEmpty(location)) {
			return false;
		}

		File workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		MavenModelManager modelManager = MavenPlugin.getMavenModelManager();
		LocalProjectScanner scanner = new LocalProjectScanner(workspaceRoot, location.trim(), false, modelManager);

		// TODO: show progress to user (no null progress monitor, instead go async)
		try {
			scanner.run(new NullProgressMonitor());
		} catch (InterruptedException e) {
			MavenE4ImportViewPlugin.getDefault().log(IStatus.ERROR, "Scanning of projects interrupted.", e);
			return false;
		}

		// FIXME: Sort adjacent projects alphabetically

		List<MavenProjectInfo> projectList = scanner.getProjects();

		if (projectList == null || projectList.isEmpty()) {
			return false;
		}

		projectTreeViewer.setInput(projectList);
		projectTreeViewer.expandAll();

		return true;
	}

	private void warn(String msg) {
		MavenE4ImportViewPlugin.getDefault().log(IStatus.WARNING, msg);
	}

	/**
	 * Adds all selected {@link MavenProjectInfo} to the list to import.
	 */
	private void addAllSelectedProjectsToImportList() {
		ITreeSelection selection = (ITreeSelection) projectTreeViewer.getSelection();
		Iterator<?> iterator = selection.iterator();
		while (iterator.hasNext()) {
			MavenProjectInfo projectInfo = (MavenProjectInfo) iterator.next();
			addProjectToImportList(projectInfo);
		}
		projectTreeViewer.setSelection(null);
		projectTreeViewer.refresh();
		projectImportListViewer.refresh();
	}

	/**
	 * Removes all selected {@link MavenProjectInfo} from the list to import.
	 */
	private void removeAllSelectedProjectsFromImportList() {
		IStructuredSelection selection = (IStructuredSelection) projectImportListViewer.getSelection();
		Iterator<?> iterator = selection.iterator();
		while (iterator.hasNext()) {
			MavenProjectInfo projectInfo = (MavenProjectInfo) iterator.next();
			removeProjectFromImportList(projectInfo);
		}
		projectImportListViewer.setSelection(null);
		projectImportListViewer.refresh();
		projectTreeViewer.refresh();
	}

	/**
	 * Adds given {@link MavenProjectInfo} to the list to import.
	 *
	 * @param projectInfo Maven project to add
	 */
	private void addProjectToImportList(MavenProjectInfo projectInfo) {
		if (!projectsToImport.contains(projectInfo)) {
			projectsToImport.add(projectInfo);
		}
	}

	/**
	 * Removes given {@link MavenProjectInfo} from the list to import.
	 *
	 * @param projectInfo Maven project to add
	 */
	private void removeProjectFromImportList(MavenProjectInfo projectInfo) {
		if (projectsToImport.contains(projectInfo)) {
			projectsToImport.remove(projectInfo);
		}
	}

	/**
	 * Handles Event "Browse for Directory"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class BrowseForDirectoryHandler extends SelectionAdapter {
		private final Composite parent;

		private BrowseForDirectoryHandler(Composite parent) {
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.NONE);
			dialog.setText(Messages.selectRootDirectoryDialogText);
			dialog.setMessage(Messages.selectRootDirectoryDialogMessage);
			String currentRootDirectory = rootDirectoryCombo.getText();
			if (currentRootDirectory.length() == 0) {
				currentRootDirectory = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
			}
			dialog.setFilterPath(currentRootDirectory);
			String newRootDirectory = dialog.open();
			if (newRootDirectory != null) {
				boolean nonEmptyListLoaded = loadProjectSelectionList(newRootDirectory);
				if (!nonEmptyListLoaded) {
					MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(),
							Messages.selectRootDirectoryMessageNoProjectsFoundTitle,
							Messages.selectRootDirectoryMessageNoProjectsFoundText, SWT.NONE);
					return;
				}
				if (rootDirectoryCombo.indexOf(newRootDirectory) != -1) {
					rootDirectoryCombo.remove(newRootDirectory);
				}
				rootDirectoryCombo.add(newRootDirectory, 0);
				rootDirectoryCombo.setText(newRootDirectory);
				rootDirectory = newRootDirectory;
			}
		}
	}

	/**
	 * Handles Event "Remove Root Directory"
	 */
	private final class RemoveRootDirectoryHandler extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			int selection = rootDirectoryCombo.getSelectionIndex();
			if (selection >= 0) {
				rootDirectoryCombo.remove(selection);
			}
			rootDirectoryCombo.select(0);

			rootDirectory = rootDirectoryCombo.getText();
			reloadProjectSelectionList();
		}
	}

	/**
	 * Handles Event "Reload"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class ReloadRepoHandler extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			reloadProjectSelectionList();
		}
	}

	/**
	 * Handles Event "collapse Tree"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class CollapseTreeHandler extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			projectTreeViewer.collapseAll();
			projectTreeViewer.expandToLevel(2);
		}
	}

	/**
	 * Handles Event "collapse Tree"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class ExpandTreeHandler extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			projectTreeViewer.expandAll();
		}
	}

	/**
	 * Handles Event "Item from Root Directory Combo is selected"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class RootDirectoryComboSelectionHandler extends SelectionAdapter {

		private final Composite parent;

		private RootDirectoryComboSelectionHandler(Composite parent) {
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			String selectedRootDirectory = rootDirectoryCombo.getText();
			if (selectedRootDirectory != null && selectedRootDirectory.equals(rootDirectory)) {
				return;
			}
			boolean nonEmptyListLoaded = loadProjectSelectionList(selectedRootDirectory);
			if (!nonEmptyListLoaded) {
				MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(),
						Messages.selectRootDirectoryMessageNoProjectsFoundTitle,
						Messages.selectRootDirectoryMessageNoProjectsFoundText, SWT.NONE);
				rootDirectoryCombo.setText(rootDirectory);
				return;
			}
			rootDirectory = selectedRootDirectory;
		}

	}

	/**
	 * Handles Event "Filter text changed"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class FilterChangedHandler implements ModifyListener {

		@Override
		public void modifyText(ModifyEvent e) {
			final String newText = filterText.getText();
			if (newText.trim().isEmpty()) {
				projectTreeViewer.resetFilters();
				projectTreeViewer.expandAll();
				return;
			}
			// FIXME: should not be case sensitive!
			projectTreeViewer.setFilters(new MavenProjectInfoFilter(newText));
			projectTreeViewer.expandAll();
		}
	}

	/**
	 * Handles Event "Add all selected projects to import list"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class AddAllSelectedProjectsToImportListHandler extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent event) {
			addAllSelectedProjectsToImportList();
		}
	}

	/**
	 * Handles Event "Remove all selected projects to import list"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class RemoveAllSelectedProjectsFromImportListHandler extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent event) {
			removeAllSelectedProjectsFromImportList();
		}
	}

	// TODO: QA
	/**
	 * Handles Event "Import all projects of import list"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class ImportProjectsHandler extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent event) {
			List<MavenProjectInfo> projectsToImport2 = getProjectsToImport();
			if (removeEclipseFilesCheckbox.getSelection()) {
				for (Object name : projectsToImport2) {
					MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) name;
					removeEclipseFiles(mavenProjectInfo.getPomFile().getParentFile());
				}
			}
			ImportMavenProjectsJob job = new ImportMavenProjectsJob(projectsToImport2, new ArrayList<IWorkingSet>(),
					new ProjectImportConfiguration());
			job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
			job.schedule();
			projectsToImport.clear();
			projectImportListViewer.refresh();
		}

		private List<MavenProjectInfo> getProjectsToImport() {
			List<MavenProjectInfo> projectList = new ArrayList<>();
			Set<String> projectsInWorkspace = getGroupIdAndArtifactIdOfAllProjectsInWorkspace();
			Iterator<MavenProjectInfo> iterator = projectsToImport.iterator();
			MavenProjectInfo mavenProjectInfo;
			Model mavenModel;
			while (iterator.hasNext()) {
				mavenProjectInfo = iterator.next();
				mavenModel = mavenProjectInfo.getModel();
				String groupId = getGroupId(mavenModel);
				if (!projectsInWorkspace.contains(String.format("%s:%s", groupId, mavenModel.getArtifactId()))) {
					projectList.add(mavenProjectInfo);
				} else {
				}
			}
			return projectList;
		}

		private String getGroupId(Model mavenModel) {
			if (mavenModel.getGroupId() != null) {
				return mavenModel.getGroupId();
			}
			if (mavenModel.getParent() != null) {
				return mavenModel.getParent().getGroupId();
			}
			return null;
		}

		private Set<String> getGroupIdAndArtifactIdOfAllProjectsInWorkspace() {
			HashSet<String> result = new HashSet<>();
			IMavenProjectFacade[] mavenProjectFacades = MavenPlugin.getMavenProjectRegistry().getProjects();
			for (IMavenProjectFacade mavenProjectFacade : mavenProjectFacades) {
				result.add(String.format("%s:%s", mavenProjectFacade.getArtifactKey().getGroupId(),
						mavenProjectFacade.getArtifactKey().getArtifactId()));
			}
			return result;
		}

		private void removeEclipseFiles(File projectFolder) {
			new File(projectFolder, ".project").delete();
			new File(projectFolder, ".classpath").delete();
			File settingsDirectory = new File(projectFolder, ".settings");
			if (settingsDirectory.isDirectory()) {
				File[] settingsFiles = settingsDirectory.listFiles();
				for (File settingsFile : settingsFiles) {
					settingsFile.delete();
				}
				settingsDirectory.delete();
			}
		}

	}

	/**
	 * Handles Event "Import Selection"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class ImportSelectionHandler extends SelectionAdapter {

		private final Composite parent;

		private ImportSelectionHandler(Composite parent) {
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent event) {

			if (ProjectImportView.this.rootDirectory == null) {
				MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(), Messages.importSelectionMessageTitle,
						Messages.importSelectionMessageNoRoot, SWT.NONE);
				return;
			}
			File root = new File(ProjectImportView.this.rootDirectory);
			if (!root.isDirectory()) {
				MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(), Messages.importSelectionMessageTitle,
						Messages.importSelectionMessageNoRoot, SWT.NONE);
				return;
			}

			FileDialog saveFileDialog = new FileDialog(parent.getShell(), SWT.NONE);
			saveFileDialog.setText("Please select file location and name");
			saveFileDialog.setFileName("project-list.txt");
			// FIXME ???
			saveFileDialog.setOverwrite(true);
			saveFileDialog.open();

			String[] fileNames = saveFileDialog.getFileNames();
			if (fileNames.length != 1) {
				MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(), Messages.importSelectionMessageTitle,
						"Genau ein Filename muss ausgew\u00e4hlt werden.", SWT.NONE);
				MavenE4ImportViewPlugin.getDefault().log(IStatus.ERROR,
						"There must be exactly one file name: " + Arrays.toString(fileNames));
				return;
			}

			File file = new File(new File(saveFileDialog.getFilterPath()), fileNames[0]);

			MavenE4ImportViewPlugin.getDefault().log(IStatus.INFO, "file: " + file);

			if (!file.exists()) {
				MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(), Messages.importSelectionMessageTitle,
						"Die ausgew\u00e4hlte Datei existiert nicht.", SWT.NONE);
				MavenE4ImportViewPlugin.getDefault().log(IStatus.ERROR, "File does not exist: " + file);
				return;
			}

			projectImportListViewer.setSelection(null);

			BufferedReader fileReader = null;
			boolean errorDialogShown = false;
			try {
				fileReader = new BufferedReader(new FileReader(file));
				String line;
				Set<String> pomsToSelect = new HashSet<>();
				while ((line = fileReader.readLine()) != null) {
					File projectToAdd = new File(root, line);
					pomsToSelect.add(projectToAdd.getAbsolutePath());
					if (!projectToAdd.exists()) {
						String errMsgStart = "Das angegebene Projekt kann nicht gefunden werden: ";
						warn(errMsgStart + projectToAdd.getAbsolutePath());
						// open dialog only once
						if (!errorDialogShown) {
							MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(),
									Messages.importSelectionMessageTitle,
									errMsgStart + "\r\n\r\n" + projectToAdd.getAbsolutePath()
											+ "\r\n\r\nIst das Root Directory richtig?",
									SWT.NONE);
							errorDialogShown = true;
						}
					}
					MavenE4ImportViewPlugin.getDefault().log(IStatus.INFO, "Importing line: " + line);
					pomsToSelect.add(new File(root, line).getAbsolutePath());
					projectTreeViewer.resetFilters();
					projectTreeViewer.expandAll();
					addAllProjectsToImportList(projectTreeViewer.getTree().getItem(0), pomsToSelect);
				}
				projectImportListViewer.refresh();
			} catch (IOException e) {
				MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(), Messages.importSelectionMessageTitle,
						Messages.importSelectionMessageIOError, SWT.NONE);
				MavenE4ImportViewPlugin.getDefault().log(IStatus.ERROR, "Error while saving project import list.", e);
			} finally {
				try {
					fileReader.close();
				} catch (IOException e) {
					MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(),
							Messages.importSelectionMessageTitle, Messages.importSelectionMessageIOError, SWT.NONE);
					MavenE4ImportViewPlugin.getDefault().log(IStatus.ERROR, "Error while saving project import list.",
							e);
				}
			}

		}

		private void addAllProjectsToImportList(TreeItem treeItem, Set<String> pomPaths) {
			MavenProjectInfo projectInfo = (MavenProjectInfo) treeItem.getData();
			if (projectInfo != null && projectInfo.getPomFile() != null
					&& pomPaths.contains(projectInfo.getPomFile().getAbsolutePath())) {
				addProjectToImportList(projectInfo);
			}
			TreeItem[] items = treeItem.getItems();
			for (TreeItem item : items) {
				addAllProjectsToImportList(item, pomPaths);
			}
		}

	}

	/**
	 * Handles Event "Export Selection"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class ExportSelectionHandler extends SelectionAdapter {

		private final Composite parent;

		private ExportSelectionHandler(Composite parent) {
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent event) {

			if (projectsToImport.isEmpty()) {
				MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(), Messages.exportSelectionMessageTitle,
						Messages.exportSelectionMessageNoProjectsSelected, SWT.NONE);
				return;
			}

			FileDialog saveFileDialog = new FileDialog(parent.getShell(), SWT.NONE);
			saveFileDialog.setText("Please select file location and name");
			saveFileDialog.setFileName("project-list.txt");
			// FIXME ???
			saveFileDialog.setOverwrite(true);
			saveFileDialog.open();

			String[] fileNames = saveFileDialog.getFileNames();
			if (fileNames.length != 1) {
				return;
			}

			File file = new File(new File(saveFileDialog.getFilterPath()), fileNames[0]);

			StringBuffer fileContent = new StringBuffer();
			Iterator<MavenProjectInfo> iterator = projectsToImport.iterator();
			MavenProjectInfo mavenProjectInfo;
			while (iterator.hasNext()) {
				mavenProjectInfo = iterator.next();
				fileContent.append(getProjectPath(mavenProjectInfo) + "/pom.xml\n");
			}

			FileWriter fileWriter = null;
			try {
				fileWriter = new FileWriter(file);
				fileWriter.write(fileContent.toString());
				fileWriter.flush();
			} catch (IOException e) {
				MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(), Messages.exportSelectionMessageTitle,
						Messages.exportSelectionMessageIOError, SWT.NONE);
				MavenE4ImportViewPlugin.getDefault().log(IStatus.ERROR, "Error while saving project import list.", e);
			} finally {
				try {
					fileWriter.close();
				} catch (IOException e) {
					MessageDialog.open(MessageDialog.WARNING, this.parent.getShell(),
							Messages.exportSelectionMessageTitle, Messages.exportSelectionMessageIOError, SWT.NONE);
					MavenE4ImportViewPlugin.getDefault().log(IStatus.ERROR, "Error while saving project import list.",
							e);
				}
			}

		}

		private String getProjectPath(MavenProjectInfo projectInfo) {
			String folderName = projectInfo.getPomFile().getParentFile().getName();
			if (projectInfo.getParent() == null) {
				return "";
			}
			return getProjectPath(projectInfo.getParent()) + "/" + folderName;

		}
	}

	/**
	 * Handles Event "Double Click on Project in Tree"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 *
	 */
	private final class SelectProjectByDoubleClickHandler implements IDoubleClickListener {

		@Override
		public void doubleClick(DoubleClickEvent event) {
			addAllSelectedProjectsToImportList();
		}

	}

	/**
	 * Handles Event "Double Click on List of Projects to import"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 *
	 */
	private final class DeselectProjectByDoubleClickHandler implements IDoubleClickListener {

		@Override
		public void doubleClick(DoubleClickEvent event) {
			removeAllSelectedProjectsFromImportList();
		}

	}

	/**
	 * Handles Event "Clear projects to import list"
	 *
	 * @author Nikolaus Winter, comdirect bank AG
	 */
	private final class ClearProjectsToImportListHandler extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			projectsToImport.clear();
			projectImportListViewer.refresh();
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (memento == null) {
			return;
		}
		String rootDirectories = memento.getTextData();
		if (rootDirectories == null) {
			return;
		}
		StringTokenizer tokenizer = new StringTokenizer(rootDirectories, "#");
		if (tokenizer.countTokens() == 0) {
			return;
		}
		savedRootDirectories = new ArrayList<>();
		while (tokenizer.hasMoreTokens()) {
			savedRootDirectories.add(tokenizer.nextToken());
		}

	}

	@Override
	public void saveState(IMemento memento) {
		String[] rootDirectories = rootDirectoryCombo.getItems();
		StringBuffer textData = new StringBuffer();
		for (String rootDirectorie : rootDirectories) {
			textData.append("#");
			textData.append(rootDirectorie);
		}
		memento.putTextData(textData.toString());
		super.saveState(memento);
	}
}
