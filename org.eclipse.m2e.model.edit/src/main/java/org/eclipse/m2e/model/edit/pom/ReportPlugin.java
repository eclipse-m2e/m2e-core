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

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc --> A representation of the model object '
 * <em><b>Report Plugin</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0 <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getGroupId <em>Group Id
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getArtifactId <em>
 * Artifact Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getVersion <em>Version
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getInherited <em>
 * Inherited</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getReportSets <em>Report
 * Sets</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getConfiguration <em>
 * Configuration</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getReportPlugin()
 * @model extendedMetaData="name='ReportPlugin' kind='elementOnly'"
 * @generated
 */
public interface ReportPlugin extends EObject {
	/**
	 * Returns the value of the '<em><b>Group Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The group ID of the reporting plugin in the repository. <!--
	 * end-model-doc -->
	 * 
	 * @return the value of the '<em>Group Id</em>' attribute.
	 * @see #isSetGroupId()
	 * @see #unsetGroupId()
	 * @see #setGroupId(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getReportPlugin_GroupId()
	 * @model unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData
	 *        ="kind='element' name='groupId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getGroupId();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getGroupId
	 * <em>Group Id</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Group Id</em>' attribute.
	 * @see #isSetGroupId()
	 * @see #unsetGroupId()
	 * @see #getGroupId()
	 * @generated
	 */
	void setGroupId(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getGroupId
	 * <em>Group Id</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @see #isSetGroupId()
	 * @see #getGroupId()
	 * @see #setGroupId(String)
	 * @generated
	 */
	void unsetGroupId();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getGroupId
	 * <em>Group Id</em>}' attribute is set. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Group Id</em>' attribute is set.
	 * @see #unsetGroupId()
	 * @see #getGroupId()
	 * @see #setGroupId(String)
	 * @generated
	 */
	boolean isSetGroupId();

	/**
	 * Returns the value of the '<em><b>Artifact Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The artifact ID of the reporting plugin in the repository. <!--
	 * end-model-doc -->
	 * 
	 * @return the value of the '<em>Artifact Id</em>' attribute.
	 * @see #setArtifactId(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getReportPlugin_ArtifactId()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='artifactId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getArtifactId();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getArtifactId
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
	 * Returns the value of the '<em><b>Version</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The version of the reporting plugin to be used. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Version</em>' attribute.
	 * @see #setVersion(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getReportPlugin_Version()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='version' namespace='##targetNamespace'"
	 * @generated
	 */
	String getVersion();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getVersion
	 * <em>Version</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Version</em>' attribute.
	 * @see #getVersion()
	 * @generated
	 */
	void setVersion(String value);

	/**
	 * Returns the value of the '<em><b>Inherited</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * Whether the configuration in this plugin should be made available to
	 * projects that inherit from this one. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Inherited</em>' attribute.
	 * @see #setInherited(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getReportPlugin_Inherited()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='inherited' namespace='##targetNamespace'"
	 * @generated
	 */
	String getInherited();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getInherited
	 * <em>Inherited</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Inherited</em>' attribute.
	 * @see #getInherited()
	 * @generated
	 */
	void setInherited(String value);

	/**
	 * Returns the value of the '<em><b>Report Sets</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.ReportSet}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0 Multiple
	 * specifications of a set of reports, each having (possibly) different
	 * configuration. This is the reporting parallel to an
	 * &lt;code&gt;execution&lt;/code&gt; in the build. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Report Sets</em>' containment reference
	 *         list.
	 * @see #isSetReportSets()
	 * @see #unsetReportSets()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getReportPlugin_ReportSets()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='reportSets' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<ReportSet> getReportSets();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getReportSets
	 * <em>Report Sets</em>}' containment reference list. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @see #isSetReportSets()
	 * @see #getReportSets()
	 * @generated
	 */
	void unsetReportSets();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getReportSets
	 * <em>Report Sets</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Report Sets</em>' containment
	 *         reference list is set.
	 * @see #unsetReportSets()
	 * @see #getReportSets()
	 * @generated
	 */
	boolean isSetReportSets();

	/**
	 * Returns the value of the '<em><b>Configuration</b></em>' reference. <!--
	 * begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Configuration</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Configuration</em>' reference.
	 * @see #setConfiguration(Configuration)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getReportPlugin_Configuration()
	 * @model
	 * @generated
	 */
	Configuration getConfiguration();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getConfiguration
	 * <em>Configuration</em>}' reference. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Configuration</em>' reference.
	 * @see #getConfiguration()
	 * @generated
	 */
	void setConfiguration(Configuration value);

} // ReportPlugin
