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
 * <em><b>Build Base</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 3.0.0+ <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.BuildBase#getDefaultGoal <em>Default
 * Goal</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.BuildBase#getResources <em>Resources
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.BuildBase#getTestResources <em>Test
 * Resources</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.BuildBase#getDirectory <em>Directory
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.BuildBase#getFinalName <em>Final Name
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.BuildBase#getPluginManagement <em>
 * Plugin Management</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.BuildBase#getPlugins <em>Plugins
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.BuildBase#getFilters <em>Filters
 * </em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuildBase()
 * @model extendedMetaData="name='BuildBase' kind='elementOnly'"
 * @generated
 */
public interface BuildBase extends EObject {
	/**
	 * Returns the value of the '<em><b>Default Goal</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * The default goal (or phase in Maven 2) to execute when none is specified
	 * for the project.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Default Goal</em>' attribute.
	 * @see #setDefaultGoal(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuildBase_DefaultGoal()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='defaultGoal' namespace='##targetNamespace'"
	 * @generated
	 */
	String getDefaultGoal();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getDefaultGoal
	 * <em>Default Goal</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Default Goal</em>' attribute.
	 * @see #getDefaultGoal()
	 * @generated
	 */
	void setDefaultGoal(String value);

	/**
	 * Returns the value of the '<em><b>Resources</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Resource}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * This element describes all of the classpath resources such as properties
	 * files associated with a project. These resources are often included in
	 * the final package.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Resources</em>' containment reference list.
	 * @see #isSetResources()
	 * @see #unsetResources()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuildBase_Resources()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='resources' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Resource> getResources();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getResources
	 * <em>Resources</em>}' containment reference list. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #isSetResources()
	 * @see #getResources()
	 * @generated
	 */
	void unsetResources();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getResources
	 * <em>Resources</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Resources</em>' containment
	 *         reference list is set.
	 * @see #unsetResources()
	 * @see #getResources()
	 * @generated
	 */
	boolean isSetResources();

	/**
	 * Returns the value of the '<em><b>Test Resources</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Resource}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * This element describes all of the classpath resources such as properties
	 * files associated with a project's unit tests.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Test Resources</em>' containment reference
	 *         list.
	 * @see #isSetTestResources()
	 * @see #unsetTestResources()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuildBase_TestResources()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='testResources' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Resource> getTestResources();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getTestResources
	 * <em>Test Resources</em>}' containment reference list. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @see #isSetTestResources()
	 * @see #getTestResources()
	 * @generated
	 */
	void unsetTestResources();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getTestResources
	 * <em>Test Resources</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Test Resources</em>' containment
	 *         reference list is set.
	 * @see #unsetTestResources()
	 * @see #getTestResources()
	 * @generated
	 */
	boolean isSetTestResources();

	/**
	 * Returns the value of the '<em><b>Directory</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The directory where all files generated by the build are placed.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Directory</em>' attribute.
	 * @see #setDirectory(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuildBase_Directory()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='directory' namespace='##targetNamespace'"
	 * @generated
	 */
	String getDirectory();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getDirectory
	 * <em>Directory</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Directory</em>' attribute.
	 * @see #getDirectory()
	 * @generated
	 */
	void setDirectory(String value);

	/**
	 * Returns the value of the '<em><b>Final Name</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The filename (excluding the extension, and with no path information) that
	 * the produced artifact will be called. The default value is
	 * &lt;code&gt;${artifactId}-${version}&lt;/code&gt;.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Final Name</em>' attribute.
	 * @see #setFinalName(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuildBase_FinalName()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='finalName' namespace='##targetNamespace'"
	 * @generated
	 */
	String getFinalName();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getFinalName
	 * <em>Final Name</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Final Name</em>' attribute.
	 * @see #getFinalName()
	 * @generated
	 */
	void setFinalName(String value);

	/**
	 * Returns the value of the '<em><b>Plugin Management</b></em>' containment
	 * reference. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0
	 * 
	 * Default plugin information to be made available for reference by projects
	 * derived from this one. This plugin configuration will not be resolved or
	 * bound to the lifecycle unless referenced. Any local configuration for a
	 * given plugin will override the plugin's entire definition here.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Plugin Management</em>' containment
	 *         reference.
	 * @see #isSetPluginManagement()
	 * @see #unsetPluginManagement()
	 * @see #setPluginManagement(PluginManagement)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuildBase_PluginManagement()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='pluginManagement' namespace='##targetNamespace'"
	 * @generated
	 */
	PluginManagement getPluginManagement();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getPluginManagement
	 * <em>Plugin Management</em>}' containment reference. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Plugin Management</em>' containment
	 *            reference.
	 * @see #isSetPluginManagement()
	 * @see #unsetPluginManagement()
	 * @see #getPluginManagement()
	 * @generated
	 */
	void setPluginManagement(PluginManagement value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getPluginManagement
	 * <em>Plugin Management</em>}' containment reference. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @see #isSetPluginManagement()
	 * @see #getPluginManagement()
	 * @see #setPluginManagement(PluginManagement)
	 * @generated
	 */
	void unsetPluginManagement();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getPluginManagement
	 * <em>Plugin Management</em>}' containment reference is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Plugin Management</em>' containment
	 *         reference is set.
	 * @see #unsetPluginManagement()
	 * @see #getPluginManagement()
	 * @see #setPluginManagement(PluginManagement)
	 * @generated
	 */
	boolean isSetPluginManagement();

	/**
	 * Returns the value of the '<em><b>Plugins</b></em>' containment reference
	 * list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin}. <!-- begin-user-doc --> <!--
	 * end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The list of plugins to use.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Plugins</em>' containment reference list.
	 * @see #isSetPlugins()
	 * @see #unsetPlugins()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuildBase_Plugins()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='plugins' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Plugin> getPlugins();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getPlugins
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
	 * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getPlugins
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

	/**
	 * Returns the value of the '<em><b>Filters</b></em>' attribute list. The
	 * list contents are of type {@link java.lang.String}. <!-- begin-user-doc
	 * -->
	 * <p>
	 * If the meaning of the '<em>Filters</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Filters</em>' attribute list.
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuildBase_Filters()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 * @generated
	 */
	EList<String> getFilters();

} // BuildBase
