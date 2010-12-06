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

package org.eclipse.m2e.model.edit.pom;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc --> A representation of the model object '
 * <em><b>Parent</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0 <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Parent#getArtifactId <em>Artifact Id
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Parent#getGroupId <em>Group Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Parent#getVersion <em>Version</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Parent#getRelativePath <em>Relative
 * Path</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getParent()
 * @model extendedMetaData="name='Parent' kind='elementOnly'"
 * @generated
 */
public interface Parent extends EObject {
	/**
	 * Returns the value of the '<em><b>Artifact Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The artifact id of the parent project to inherit from. <!-- end-model-doc
	 * -->
	 * 
	 * @return the value of the '<em>Artifact Id</em>' attribute.
	 * @see #setArtifactId(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getParent_ArtifactId()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='artifactId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getArtifactId();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Parent#getArtifactId
	 * <em>Artifact Id</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Artifact Id</em>' attribute.
	 * @see #getArtifactId()
	 * @generated
	 */
	void setArtifactId(String value);

	/**
	 * Returns the value of the '<em><b>Group Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The group id of the parent project to inherit from. <!-- end-model-doc
	 * -->
	 * 
	 * @return the value of the '<em>Group Id</em>' attribute.
	 * @see #setGroupId(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getParent_GroupId()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='groupId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getGroupId();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Parent#getGroupId <em>Group Id</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Group Id</em>' attribute.
	 * @see #getGroupId()
	 * @generated
	 */
	void setGroupId(String value);

	/**
	 * Returns the value of the '<em><b>Version</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The version of the parent project to inherit. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Version</em>' attribute.
	 * @see #setVersion(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getParent_Version()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='version' namespace='##targetNamespace'"
	 * @generated
	 */
	String getVersion();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Parent#getVersion <em>Version</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Version</em>' attribute.
	 * @see #getVersion()
	 * @generated
	 */
	void setVersion(String value);

	/**
	 * Returns the value of the '<em><b>Relative Path</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The relative path of the parent &lt;code&gt;pom.xml&lt;/code&gt; file
	 * within the check out. The default value is
	 * &lt;code&gt;../pom.xml&lt;/code&gt;. Maven looks for the parent pom first
	 * in the reactor of currently building projects, then in this location on
	 * the filesystem, then the local repository, and lastly in the remote repo.
	 * &lt;code&gt;relativePath&lt;/code&gt; allows you to select a different
	 * location, for example when your structure is flat, or deeper without an
	 * intermediate parent pom. However, the group ID, artifact ID and version
	 * are still required, and must match the file in the location given or it
	 * will revert to the repository for the POM. This feature is only for
	 * enhancing the development in a local checkout of that project.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Relative Path</em>' attribute.
	 * @see #setRelativePath(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getParent_RelativePath()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='relativePath' namespace='##targetNamespace'"
	 * @generated
	 */
	String getRelativePath();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Parent#getRelativePath
	 * <em>Relative Path</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Relative Path</em>' attribute.
	 * @see #getRelativePath()
	 * @generated
	 */
	void setRelativePath(String value);

} // Parent
