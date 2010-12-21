/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.cli;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;

public class WorkspaceState {
	private static Properties state;

	public static synchronized Properties getState()  {
		if (state == null) {
			state = new Properties();
			try {
				String location = System.getProperty("m2eclipse.workspace.state");
		    	if (location != null) {
		    		BufferedInputStream in = new BufferedInputStream(new FileInputStream(location));
		    		try {
		    			state.load(in);
		    		} finally {
		    			in.close();
		    		}
		    	}
			} catch (IOException e) {
				// XXX log
			}
		}
		return state;
	}

  public static boolean resolveArtifact(Artifact artifact) {
    File file = findArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(),
        artifact.getBaseVersion());

    if(file == null) {
      return false;
    }

    artifact.setFile(file);
    artifact.setResolved(true);
    return true;
  }

  public static File findArtifact(String groupId, String artifactId, String type, String baseVersion) {
    Properties state = getState();
    if(state == null) {
      return null;
    }

    String key = groupId + ':' + artifactId + ':' + type + ':' + baseVersion;
    String value = state.getProperty(key);

    if(value == null || value.length() == 0) {
      return null;
    }

    File file = new File(value);
    if(!file.exists()) {
      return null;
    }

    return file;
  }

}
