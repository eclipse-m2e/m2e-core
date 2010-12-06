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
 * <em><b>Dependency</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 3.0.0+ <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Dependency#getGroupId <em>Group Id
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Dependency#getArtifactId <em>Artifact
 * Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Dependency#getVersion <em>Version
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Dependency#getType <em>Type</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Dependency#getClassifier <em>
 * Classifier</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Dependency#getScope <em>Scope</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Dependency#getSystemPath <em>System
 * Path</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Dependency#getExclusions <em>
 * Exclusions</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Dependency#getOptional <em>Optional
 * </em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getDependency()
 * @model extendedMetaData="name='Dependency' kind='elementOnly'"
 * @generated
 */
public interface Dependency extends EObject {
	/**
	 * Returns the value of the '<em><b>Group Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * The project group that produced the dependency, e.g.
	 * &lt;code&gt;org.apache.maven&lt;/code&gt;.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Group Id</em>' attribute.
	 * @see #setGroupId(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getDependency_GroupId()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='groupId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getGroupId();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getGroupId
	 * <em>Group Id</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Group Id</em>' attribute.
	 * @see #getGroupId()
	 * @generated
	 */
	void setGroupId(String value);

	/**
	 * Returns the value of the '<em><b>Artifact Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * The unique id for an artifact produced by the project group, e.g.
	 * &lt;code&gt;maven-artifact&lt;/code&gt;.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Artifact Id</em>' attribute.
	 * @see #setArtifactId(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getDependency_ArtifactId()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='artifactId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getArtifactId();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getArtifactId
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
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * The version of the dependency, e.g. &lt;code&gt;3.2.1&lt;/code&gt;. In
	 * Maven 2, this can also be specified as a range of versions.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Version</em>' attribute.
	 * @see #setVersion(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getDependency_Version()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='version' namespace='##targetNamespace'"
	 * @generated
	 */
	String getVersion();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getVersion
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
	 * Returns the value of the '<em><b>Type</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The type of dependency. This defaults to &lt;code&gt;jar&lt;/code&gt;.
	 * While it usually represents the extension on the filename of the
	 * dependency, that is not always the case. A type can be mapped to a
	 * different extension and a classifier. The type often correspongs to the
	 * packaging used, though this is also not always the case. Some examples
	 * are &lt;code&gt;jar&lt;/code&gt;, &lt;code&gt;war&lt;/code&gt;,
	 * &lt;code&gt;ejb-client&lt;/code&gt; and
	 * &lt;code&gt;test-jar&lt;/code&gt;. New types can be defined by plugins
	 * that set &lt;code&gt;extensions&lt;/code&gt; to
	 * &lt;code&gt;true&lt;/code&gt;, so this is not a complete list.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see #isSetType()
	 * @see #unsetType()
	 * @see #setType(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getDependency_Type()
	 * @model unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData
	 *        ="kind='element' name='type' namespace='##targetNamespace'"
	 * @generated
	 */
	String getType();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getType <em>Type</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Type</em>' attribute.
	 * @see #isSetType()
	 * @see #unsetType()
	 * @see #getType()
	 * @generated
	 */
	void setType(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getType <em>Type</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetType()
	 * @see #getType()
	 * @see #setType(String)
	 * @generated
	 */
	void unsetType();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getType <em>Type</em>}'
	 * attribute is set. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Type</em>' attribute is set.
	 * @see #unsetType()
	 * @see #getType()
	 * @see #setType(String)
	 * @generated
	 */
	boolean isSetType();

	/**
	 * Returns the value of the '<em><b>Classifier</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The classifier of the dependency. This allows distinguishing two
	 * artifacts that belong to the same POM but were built differently, and is
	 * appended to the filename after the version. For example,
	 * &lt;code&gt;jdk14&lt;/code&gt; and &lt;code&gt;jdk15&lt;/code&gt;.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Classifier</em>' attribute.
	 * @see #setClassifier(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getDependency_Classifier()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='classifier' namespace='##targetNamespace'"
	 * @generated
	 */
	String getClassifier();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getClassifier
	 * <em>Classifier</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Classifier</em>' attribute.
	 * @see #getClassifier()
	 * @generated
	 */
	void setClassifier(String value);

	/**
	 * Returns the value of the '<em><b>Scope</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The scope of the dependency - &lt;code&gt;compile&lt;/code&gt;,
	 * &lt;code&gt;runtime&lt;/code&gt;, &lt;code&gt;test&lt;/code&gt;,
	 * &lt;code&gt;system&lt;/code&gt;, and &lt;code&gt;provided&lt;/code&gt;.
	 * Used to calculate the various classpaths used for compilation, testing,
	 * and so on. It also assists in determining which artifacts to include in a
	 * distribution of this project. For more information, see &lt;a href="http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html"
	 * &gt;the dependency mechanism&lt;/a&gt;.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Scope</em>' attribute.
	 * @see #setScope(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getDependency_Scope()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='scope' namespace='##targetNamespace'"
	 * @generated
	 */
	String getScope();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getScope <em>Scope</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Scope</em>' attribute.
	 * @see #getScope()
	 * @generated
	 */
	void setScope(String value);

	/**
	 * Returns the value of the '<em><b>System Path</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * FOR SYSTEM SCOPE ONLY. Note that use of this property is
	 * &lt;b&gt;discouraged&lt;/b&gt; and may be replaced in later versions.
	 * This specifies the path on the filesystem for this dependency. Requires
	 * an absolute path for the value, not relative. Use a property that gives
	 * the machine specific absolute path, e.g.
	 * &lt;code&gt;${java.home}&lt;/code&gt;.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>System Path</em>' attribute.
	 * @see #setSystemPath(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getDependency_SystemPath()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='systemPath' namespace='##targetNamespace'"
	 * @generated
	 */
	String getSystemPath();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getSystemPath
	 * <em>System Path</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>System Path</em>' attribute.
	 * @see #getSystemPath()
	 * @generated
	 */
	void setSystemPath(String value);

	/**
	 * Returns the value of the '<em><b>Exclusions</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Exclusion}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * Lists a set of artifacts that should be excluded from this dependency's
	 * artifact list when it comes to calculating transitive dependencies.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Exclusions</em>' containment reference
	 *         list.
	 * @see #isSetExclusions()
	 * @see #unsetExclusions()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getDependency_Exclusions()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='exclusions' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Exclusion> getExclusions();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getExclusions
	 * <em>Exclusions</em>}' containment reference list. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #isSetExclusions()
	 * @see #getExclusions()
	 * @generated
	 */
	void unsetExclusions();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getExclusions
	 * <em>Exclusions</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Exclusions</em>' containment
	 *         reference list is set.
	 * @see #unsetExclusions()
	 * @see #getExclusions()
	 * @generated
	 */
	boolean isSetExclusions();

	/**
	 * Returns the value of the '<em><b>Optional</b></em>' attribute. The
	 * default value is <code>"false"</code>. <!-- begin-user-doc --> <!--
	 * end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * Indicates the dependency is optional for use of this library. While the
	 * version of the dependency will be taken into account for dependency
	 * calculation if the library is used elsewhere, it will not be passed on
	 * transitively.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Optional</em>' attribute.
	 * @see #isSetOptional()
	 * @see #unsetOptional()
	 * @see #setOptional(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getDependency_Optional()
	 * @model default="false" unsettable="true"
	 *        dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='optional' namespace='##targetNamespace'"
	 * @generated
	 */
	String getOptional();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getOptional
	 * <em>Optional</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Optional</em>' attribute.
	 * @see #isSetOptional()
	 * @see #unsetOptional()
	 * @see #getOptional()
	 * @generated
	 */
	void setOptional(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getOptional
	 * <em>Optional</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @see #isSetOptional()
	 * @see #getOptional()
	 * @see #setOptional(String)
	 * @generated
	 */
	void unsetOptional();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency#getOptional
	 * <em>Optional</em>}' attribute is set. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Optional</em>' attribute is set.
	 * @see #unsetOptional()
	 * @see #getOptional()
	 * @see #setOptional(String)
	 * @generated
	 */
	boolean isSetOptional();

} // Dependency
