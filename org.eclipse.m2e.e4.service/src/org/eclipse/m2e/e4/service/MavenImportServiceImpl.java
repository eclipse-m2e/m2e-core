/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.e4.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.wizards.ImportMavenProjectsJob;
import org.eclipse.ui.IWorkingSet;

/**
 * MavenImportService
 *
 * @author Nikolaus Winter, comdirect bank AG
 */
@SuppressWarnings("restriction")
public class MavenImportServiceImpl implements MavenImportService {

	private MavenModelManager modelManager;

	@Override
	public void importProject(File projectFolder, boolean cleanEclipseFiles) {
		MavenProjectInfo projectInfo = null;

		try {
			File pomFile = new File(projectFolder, IMavenConstants.POM_FILE_NAME);
			Model mavenModel = this.modelManager.readMavenModel(pomFile);
			projectInfo = new MavenProjectInfo(mavenModel.getArtifactId(), pomFile, mavenModel, null);
		} catch (CoreException e) {
			MavenE4ServicePlugin.getDefault().log(IStatus.ERROR, "Error", e);
		}

		Collection<MavenProjectInfo> mavenProjectsToImport = new ArrayList<>(1);
		mavenProjectsToImport.add(projectInfo);

		if (cleanEclipseFiles) {
			removeEclipseFiles(projectFolder);
		}

		ImportMavenProjectsJob job = new ImportMavenProjectsJob(mavenProjectsToImport, new ArrayList<IWorkingSet>(),
				new ProjectImportConfiguration());
		job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
		job.schedule();
	}

	/**
	 * Removes Eclipse-Files from given Project Folder.
	 *
	 * @param projectFolder Project Folder
	 */
	private void removeEclipseFiles(File projectFolder) {
		new File(projectFolder, ".project").delete();
		new File(projectFolder, ".classpath").delete();
		new File(projectFolder, ".factorypath").delete();
		File settingsDirectory = new File(projectFolder, ".settings");
		if (settingsDirectory.isDirectory()) {
			File[] settingsFiles = settingsDirectory.listFiles();
			for (File settingsFile : settingsFiles) {
				settingsFile.delete();
			}
			settingsDirectory.delete();
		}
	}

	/**
	 * Registers MavenImportService at {@link IEclipseContext} (E4).
	 *
	 * @param context {@link IEclipseContext} (E4)
	 */
	@Inject
	public void setEclipseContext(IEclipseContext context) {
		context.set(MavenImportService.class, this);
		this.modelManager = MavenPlugin.getMavenModelManager();
		MavenE4ServicePlugin.getDefault().log(IStatus.INFO, "Registered MavenImportService");
	}
}
