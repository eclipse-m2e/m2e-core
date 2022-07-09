/*******************************************************************************
 * Copyright (c) 2022, 2023 Hannes Wellmann and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Hannes Wellmann - initial API and implementation
 *******************************************************************************/

package org.apache.maven.cli;

import java.io.File;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.maven.execution.MavenExecutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenCLIParser {
	private MavenCLIParser() {
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(MavenCLIParser.class);

	public static void populateCLIOptions(MavenExecutionRequest request) {
		try {
			File multiModuleProjectDirectory = request.getMultiModuleProjectDirectory();
			CommandLine cl;
			CliRequest cliRequest = new CliRequest(new String[0], null);
			if (multiModuleProjectDirectory != null) {
				cliRequest.multiModuleProjectDirectory = multiModuleProjectDirectory;
				new MavenCli().cli(cliRequest);
				cl = cliRequest.commandLine;
			} else {
				cl = new CLIManager().parse(cliRequest.getArgs());
			}
			MavenCli.populateProperties(cl, request.getSystemProperties(), request.getUserProperties());

			populateProfilesProfiles(request, cl);
		} catch (Exception e) {
			LOGGER.error("Failed to populate request for", e);
		}
	}

	private static final Pattern COMMA = Pattern.compile(",");

	private static void populateProfilesProfiles(MavenExecutionRequest request, CommandLine mavenConfig) {
		// Based on MavenCli.populateRequest() section 'Profile Activation'
		String[] profileOptionValues = mavenConfig.getOptionValues(CLIManager.ACTIVATE_PROFILES);
		if (profileOptionValues != null) {
			for (String profileValue : profileOptionValues) {
				COMMA.splitAsStream(profileValue).map(String::trim).forEach(profileAction -> {
					if (profileAction.startsWith("-") || profileAction.startsWith("!")) {
						request.addInactiveProfile(profileAction.substring(1));
					} else if (profileAction.startsWith("+")) {
						request.addActiveProfile(profileAction.substring(1));
					} else {
						request.addActiveProfile(profileAction);
					}
				});
			}
		}
	}

	// TODO: parse more?

}
