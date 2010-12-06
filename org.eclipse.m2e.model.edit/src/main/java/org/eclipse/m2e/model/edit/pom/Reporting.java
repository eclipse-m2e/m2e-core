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
 * <em><b>Reporting</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0 Section for management of reports and their
 * configuration. <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Reporting#getExcludeDefaults <em>
 * Exclude Defaults</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Reporting#getOutputDirectory <em>
 * Output Directory</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Reporting#getPlugins <em>Plugins
 * </em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getReporting()
 * @model extendedMetaData="name='Reporting' kind='elementOnly'"
 * @generated
 */
public interface Reporting extends EObject {
	/**
	 * Returns the value of the '<em><b>Exclude Defaults</b></em>' attribute.
	 * The default value is <code>"false"</code>. <!-- begin-user-doc --> <!--
	 * end-user-doc --> <!-- begin-model-doc --> 4.0.0 If true, then the default
	 * reports are not included in the site generation. This includes the
	 * reports in the "Project Info" menu. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Exclude Defaults</em>' attribute.
	 * @see #isSetExcludeDefaults()
	 * @see #unsetExcludeDefaults()
	 * @see #setExcludeDefaults(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getReporting_ExcludeDefaults()
	 * @model default="false" unsettable="true"
	 *        dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='excludeDefaults' namespace='##targetNamespace'"
	 * @generated
	 */
	String getExcludeDefaults();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Reporting#getExcludeDefaults
	 * <em>Exclude Defaults</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Exclude Defaults</em>' attribute.
	 * @see #isSetExcludeDefaults()
	 * @see #unsetExcludeDefaults()
	 * @see #getExcludeDefaults()
	 * @generated
	 */
	void setExcludeDefaults(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Reporting#getExcludeDefaults
	 * <em>Exclude Defaults</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isSetExcludeDefaults()
	 * @see #getExcludeDefaults()
	 * @see #setExcludeDefaults(String)
	 * @generated
	 */
	void unsetExcludeDefaults();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Reporting#getExcludeDefaults
	 * <em>Exclude Defaults</em>}' attribute is set. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Exclude Defaults</em>' attribute is
	 *         set.
	 * @see #unsetExcludeDefaults()
	 * @see #getExcludeDefaults()
	 * @see #setExcludeDefaults(String)
	 * @generated
	 */
	boolean isSetExcludeDefaults();

	/**
	 * Returns the value of the '<em><b>Output Directory</b></em>' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
	 * 4.0.0
	 * 
	 * Where to store all of the generated reports. The default is
	 * &lt;code&gt;${project.build.directory}/site&lt;/code&gt; .
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Output Directory</em>' attribute.
	 * @see #setOutputDirectory(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getReporting_OutputDirectory()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='outputDirectory' namespace='##targetNamespace'"
	 * @generated
	 */
	String getOutputDirectory();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Reporting#getOutputDirectory
	 * <em>Output Directory</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Output Directory</em>' attribute.
	 * @see #getOutputDirectory()
	 * @generated
	 */
	void setOutputDirectory(String value);

	/**
	 * Returns the value of the '<em><b>Plugins</b></em>' containment reference
	 * list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin}. <!-- begin-user-doc
	 * --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0 The reporting
	 * plugins to use and their configuration. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Plugins</em>' containment reference list.
	 * @see #isSetPlugins()
	 * @see #unsetPlugins()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getReporting_Plugins()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='plugins' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<ReportPlugin> getPlugins();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Reporting#getPlugins
	 * <em>Plugins</em>}' containment reference list. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #isSetPlugins()
	 * @see #getPlugins()
	 * @generated
	 */
	void unsetPlugins();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Reporting#getPlugins
	 * <em>Plugins</em>}' containment reference list is set. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Plugins</em>' containment reference
	 *         list is set.
	 * @see #unsetPlugins()
	 * @see #getPlugins()
	 * @generated
	 */
	boolean isSetPlugins();

} // Reporting
