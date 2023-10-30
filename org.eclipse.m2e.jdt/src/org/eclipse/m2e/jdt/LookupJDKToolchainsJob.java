/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.m2e.jdt;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.cli.MavenCli;
import org.apache.maven.toolchain.io.DefaultToolchainsReader;
import org.apache.maven.toolchain.io.ToolchainsReader;
import org.apache.maven.toolchain.java.JavaToolchainImpl;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;

public class LookupJDKToolchainsJob extends Job {

	private final IVMInstallType standardType;

	public LookupJDKToolchainsJob() {
		super(LookupJDKToolchainsJob.class.getSimpleName());
		this.standardType = JavaRuntime.getVMInstallType(StandardVMType.ID_STANDARD_VM_TYPE);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<File> toolchainFiles = List.of(MavenCli.DEFAULT_GLOBAL_TOOLCHAINS_FILE, MavenCli.DEFAULT_USER_TOOLCHAINS_FILE);
		ToolchainsReader reader = new DefaultToolchainsReader();
		for (File toolchainsFile : toolchainFiles) {
			if (toolchainsFile.isFile() && toolchainsFile.canRead()) {
				try {
					PersistedToolchains toolchains = reader.read(toolchainsFile, null);
					for (ToolchainModel toolchain : toolchains.getToolchains()) {
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						addToolchain(toolchain);
					}
				} catch(Exception e) {
					return Status.error(e.getMessage(), e);
				}
			}
		}
		return Status.OK_STATUS;
	}

	private Optional<File> getVMInstallation(ToolchainModel toolchain) {
		return Optional.ofNullable(toolchain)
			.filter(t -> "jdk".equals(t.getType()))
			.map(ToolchainModel::getConfiguration)
			.filter(Xpp3Dom.class::isInstance)
			.map(Xpp3Dom.class::cast)
			.map(dom -> dom.getChild(JavaToolchainImpl.KEY_JAVAHOME))
			.map(Xpp3Dom::getValue)
			.map(File::new)
			.filter(File::isDirectory);
	}

	private void addToolchain(ToolchainModel toolchain) {
		getVMInstallation(toolchain)
			.filter(f -> standardType.validateInstallLocation(f).isOK())
			.ifPresent(candidate -> {
				if (Arrays.stream(standardType.getVMInstalls()) //
					.map(IVMInstall::getInstallLocation) //
					.filter(Objects::nonNull)
					.noneMatch(install -> isSameCanonicalFile(candidate, install))) {
					VMStandin workingCopy = new VMStandin(standardType, candidate.getAbsolutePath());
					workingCopy.setInstallLocation(candidate);
					String name = candidate.getName();
					int i = 1;
					while (isDuplicateName(name)) {
						name = candidate.getName() + '(' + i++ + ')';
					}
					workingCopy.setName(name);
					IVMInstall newVM = workingCopy.convertToRealVM();
					// next lines workaround https://github.com/eclipse-jdt/eclipse.jdt.debug/issues/248
					if (!(newVM instanceof IVMInstall2 newVM2 && newVM2.getJavaVersion() != null)) {
						standardType.disposeVMInstall(newVM.getId());
					}
				}
			});
	}

	private static boolean isDuplicateName(String name) {
		return Stream.of(JavaRuntime.getVMInstallTypes()) //
			.flatMap(vmType -> Arrays.stream(vmType.getVMInstalls())) //
			.map(IVMInstall::getName) //
			.anyMatch(name::equals);
	}

	private static boolean isSameCanonicalFile(File f1, File f2) {
		try {
			return Objects.equals(f1.getCanonicalFile(), f2.getCanonicalFile());
		} catch (IOException ex) {
			return false;
		}
	}
}
