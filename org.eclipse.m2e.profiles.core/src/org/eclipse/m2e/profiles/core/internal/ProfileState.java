/*************************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Fred Bricon / JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.eclipse.m2e.profiles.core.internal;

/**
 * Enum of all possible Profile states :
 * <ul>
 * <li>Active : the profile is active</li>
 * <li>Inactive : the profile is not active</li>
 * <li>Disabled : the profile is not active</li>
 * </ul>
 * @author Fred Bricon
 * @since 1.5.0
 */
public enum ProfileState {
	Disabled(false), 
	Inactive(false), 
	Active(true); 
	
	private boolean active;
	
	ProfileState(boolean active) {
		this.active = active;
	}
	
	/**
	 * @return true if the Profile is active
	 */
	public boolean isActive() {
		return active;
	}
	
}
