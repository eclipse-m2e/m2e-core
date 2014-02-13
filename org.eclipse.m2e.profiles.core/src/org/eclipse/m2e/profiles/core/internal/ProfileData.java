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
 * Model wrapping Maven Profile informations.
 * 
 * @author Fred Bricon
 * @since 1.5.0
 */
public class ProfileData {
	private String id;
	private boolean autoActive;
	private boolean userSelected;
	private ProfileState activationState;
	private String source;
	
	/**
	 * Constructor
	 * @param id the profile id
	 */
	public ProfileData(String id) {
		this.id = id;
	}

	/**
	 * @return the Maven profile id
	 */
	public String getId() {
		return id;
	}

	 /**
   * Sets the Maven profile id
   */
	public void setId(String id) {
		this.id = id;
	}
	
	 /**
   * @return  <code>true</code> if the profile is active automatically
   */
	public boolean isAutoActive() {
		return autoActive;
	}

	public void setAutoActive(boolean autoActive) {
		this.autoActive = autoActive;
	}

	/**
	 * @return the activation state of the profile (Active, Inactive, Disabled, Unknown)
	 */
	public ProfileState getActivationState() {
		return activationState;
	}
	
	public void setActivationState(ProfileState activationState) {
		this.activationState = activationState;
	}
	
	/**
	 * @return the source defining the profile (settings.xml or artifactId defining the profile)
	 */
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return true if the profile was set in Eclipse UI
	 */
	public boolean isUserSelected() {
		return userSelected;
	}

	public void setUserSelected(boolean userSelected) {
		this.userSelected = userSelected;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((activationState == null) ? 0 : activationState.hashCode());
		result = prime * result + (autoActive ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + (userSelected ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProfileData other = (ProfileData) obj;
		if (activationState != other.activationState)
			return false;
		if (autoActive != other.autoActive)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (userSelected != other.userSelected)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ProfileData [id=" + id + ", autoActive=" + autoActive
				+ ", userSelected=" + userSelected + ", activationState="
				+ activationState + ", source=" + source + "]";
	}
}
