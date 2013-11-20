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

package org.eclipse.m2e.core.internal.embedder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.classrealm.ClassRealmConstituent;
import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.classrealm.ClassRealmRequest;


/**
 * EclipseArtifactFilterManager
 * 
 * @author igor
 */
@Component(role = ClassRealmManagerDelegate.class)
public class EclipseClassRealmManagerDelegate implements ClassRealmManagerDelegate {

  public static final String ROLE_HINT = EclipseClassRealmManagerDelegate.class.getName();

  @Requirement
  private PlexusContainer plexus;

  private final ArtifactVersion currentBuildApiVersion;

  public EclipseClassRealmManagerDelegate() {
    Properties props = new Properties();
    InputStream is = getClass().getResourceAsStream("/org/sonatype/plexus/build/incremental/version.properties"); //$NON-NLS-1$
    if(is != null) {
      try {
        props.load(is);
      } catch(IOException e) {
        e.printStackTrace();
      } finally {
        IOUtil.close(is);
      }
    }
    currentBuildApiVersion = new DefaultArtifactVersion(props.getProperty("api.version", "0.0.5")); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public void setupRealm(ClassRealm realm, ClassRealmRequest request) {
    if(supportsBuildApi(request.getConstituents())) {
      ClassRealm coreRealm = plexus.getContainerRealm();

      realm.importFrom(coreRealm, "org.codehaus.plexus.util.DirectoryScanner"); //$NON-NLS-1$
      realm.importFrom(coreRealm, "org.codehaus.plexus.util.AbstractScanner"); //$NON-NLS-1$
      realm.importFrom(coreRealm, "org.codehaus.plexus.util.Scanner"); //$NON-NLS-1$

      realm.importFrom(coreRealm, "org.sonatype.plexus.build.incremental"); //$NON-NLS-1$
    }
  }

  private boolean supportsBuildApi(List<ClassRealmConstituent> constituents) {
    for(Iterator<ClassRealmConstituent> it = constituents.iterator(); it.hasNext();) {
      ClassRealmConstituent constituent = it.next();
      if("org.sonatype.plexus".equals(constituent.getGroupId()) //$NON-NLS-1$
          && "plexus-build-api".equals(constituent.getArtifactId())) { //$NON-NLS-1$
        ArtifactVersion version = new DefaultArtifactVersion(constituent.getVersion());
        boolean compatible = currentBuildApiVersion.compareTo(version) >= 0;
        if(compatible) {
          // removing the JAR from the plugin realm to prevent discovery of the DefaultBuildContext
          it.remove();
        }
        return compatible;
      }
    }
    return false;
  }

}
