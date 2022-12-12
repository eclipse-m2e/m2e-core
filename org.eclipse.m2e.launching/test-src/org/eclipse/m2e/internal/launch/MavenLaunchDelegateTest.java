/********************************************************************************
 * Copyright (c) 2023, 2023 konradwindszus and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   konradwindszus - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.internal.launch;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;


@SuppressWarnings("restriction")
public class MavenLaunchDelegateTest {

  private static final class DummyJRE extends AbstractVMInstall {

    private static final IVMInstallType DUMMY_TYPE = new StandardVMType();

    public DummyJRE(String id) {
      super(DUMMY_TYPE, id);
    }

    public String getJavaVersion() {
      return getId();
    }

    public String toString() {
      return "DummyJRE [" + getId() + "]";
    }

  }

  @Test
  public void testGetBestMatchingVM() throws InvalidVersionSpecificationException {
    Collection<IVMInstall> allJREs = new ArrayList<>();
    allJREs.add(new DummyJRE("17.0.4"));
    allJREs.add(new DummyJRE("11.0.7"));
    allJREs.add(new DummyJRE("11.0.1"));
    allJREs.add(new DummyJRE("1.8.0"));

    assertEquals(new DummyJRE("11.0.7"),
        MavenLaunchDelegate.getBestMatchingVM(VersionRange.createFromVersionSpec("11"), allJREs));

    assertEquals(new DummyJRE("11.0.7"),
        MavenLaunchDelegate.getBestMatchingVM(VersionRange.createFromVersionSpec("[11,)"), allJREs));

    assertEquals(new DummyJRE("17.0.4"),
        MavenLaunchDelegate.getBestMatchingVM(VersionRange.createFromVersionSpec("[11,18)"), allJREs));

    assertEquals(new DummyJRE("1.8.0"),
        MavenLaunchDelegate.getBestMatchingVM(VersionRange.createFromVersionSpec("[1.8,9)"), allJREs));
  }

}
