/*************************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.eclipse.m2e.profiles.ui.internal.dialog;

import java.util.Collection;

/**
 * Utility class to manipulate Profiles
 *
 * @author Fred Bricon
 * @since 1.5.0
 */
public class ProfileUtil {

	private ProfileUtil(){}

	private static final String COMMA = ", ";

	/**
	* Turns a {@link ProfileSelection} collection as a String, joined by a comma separator.
	*/
	public static String toString(Collection<ProfileSelection> profiles) {
		StringBuilder sb = new StringBuilder();
		if(profiles != null && !profiles.isEmpty()) {
			boolean addComma = false;
			for (ProfileSelection ps : profiles) {
				if (ps !=null && Boolean.TRUE.equals(ps.getSelected())) {
					if (addComma) {
						sb.append(COMMA);
					}
					sb.append(ps.toMavenString());
					addComma = true;
				}
			}
		}
		return sb.toString();
	}

}
