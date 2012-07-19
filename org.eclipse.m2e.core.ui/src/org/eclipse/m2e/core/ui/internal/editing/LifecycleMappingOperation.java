/*******************************************************************************
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - Work on Bug 350414
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.editing;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;

public class LifecycleMappingOperation implements Operation {
  

  private static final Logger log = LoggerFactory.getLogger(LifecycleMappingOperation.class);

  private static final String LIFECYCLE_PLUGIN_VERSION = LifecycleMappingFactory.LIFECYCLE_MAPPING_PLUGIN_VERSION;

  private static final String LIFECYCLE_PLUGIN_ARTIFACTID = LifecycleMappingFactory.LIFECYCLE_MAPPING_PLUGIN_ARTIFACTID;

  private static final String LIFECYCLE_PLUGIN_GROUPID = LifecycleMappingFactory.LIFECYCLE_MAPPING_PLUGIN_GROUPID;

  private String version;
  private String groupId;
  private String artifactId;

  private PluginExecutionAction action;
  private String[] goals;
  
  /**
   * If set to true, then the lifecycle mapping metadata is created
   * at the top level of the file, rather than within a plugin.
   * For use when not inside a pom
   */
  private boolean createAtTopLevel = false;

  public LifecycleMappingOperation(String pluginGroupId, String pluginArtifactId, String pluginVersion,
      PluginExecutionAction action, String[] goals) {
    this(pluginGroupId, pluginArtifactId, pluginVersion, action, goals, false);
  }
  
  public LifecycleMappingOperation(String pluginGroupId, String pluginArtifactId, String pluginVersion,
      PluginExecutionAction action, String[] goals, boolean createAtTopLevel) {
    this.artifactId = pluginArtifactId;
    this.groupId = pluginGroupId;
    this.version = pluginVersion;
    assert !PluginExecutionAction.configurator.equals(action);
    this.action = action;
    this.goals = goals;
    this.createAtTopLevel = createAtTopLevel;
  }

  public void process(Document document) {
    Element root = document.getDocumentElement();
    Element pluginExecutions;  // add the new plugins here 

    
    //now find the lifecycle stuff if it's there.
    if (createAtTopLevel) {
      if (root == null) {
        // probably an empty document
        root = document.createElement("lifecycleMappingMetadata"); //$NON-NLS-1$
        document.appendChild(root);
      }
      pluginExecutions = getChild(root, "pluginExecutions");  //$NON-NLS-1$
    } else {
      Element managedPlugins = getChild(root, BUILD, PLUGIN_MANAGEMENT, PLUGINS);
      Element lifecyclePlugin = findChild(managedPlugins, PLUGIN, 
          childEquals(GROUP_ID, LIFECYCLE_PLUGIN_GROUPID), 
          childEquals(ARTIFACT_ID, LIFECYCLE_PLUGIN_ARTIFACTID));
      
      
      if (lifecyclePlugin == null) {
        //not found, create
        lifecyclePlugin = PomHelper.createPlugin(managedPlugins, LIFECYCLE_PLUGIN_GROUPID, LIFECYCLE_PLUGIN_ARTIFACTID, LIFECYCLE_PLUGIN_VERSION);
  
        //mkleint: a bit scared to have this text localized, with chinese/japanese locales, it could write garbage into the pom file..
        Comment comment = document.createComment("This plugin's configuration is used to store Eclipse m2e settings only. It has no influence on the Maven build itself."); //$NON-NLS-1$
        managedPlugins.insertBefore(comment, lifecyclePlugin);
        format(comment);
      }
      
      pluginExecutions = getChild(lifecyclePlugin, CONFIGURATION, "lifecycleMappingMetadata", "pluginExecutions"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    //now find the plugin execution for the plugin we have..
    Element execution = null;
    for (Element exec : findChilds(pluginExecutions, "pluginExecution")) { //$NON-NLS-1$
      Element filter = findChild(exec, "pluginExecutionFilter",  //$NON-NLS-1$
          childEquals(GROUP_ID, groupId), 
          childEquals(ARTIFACT_ID, artifactId));
      //the action needs to match the action we want..
      Element actionEl = findChild(findChild(exec, "action"), action.toString()); //$NON-NLS-1$
      if (filter != null && actionEl != null) {
        String versionRange = getTextValue(getChild(filter, "versionRange")); //$NON-NLS-1$
        if (versionRange != null) { //  paranoid null check
          //now we shall do some smart matching on the existing versionRange and our version..
          //so far the "smart" thing involves just overwriting the range.
          try {
            VersionRange range = VersionRange.createFromVersionSpec(versionRange);
            if (!range.containsVersion(new DefaultArtifactVersion(version))) {
              Element rangeEl = findChild(filter, "versionRange"); //$NON-NLS-1$
              setText(rangeEl, "[" + version + ",)"); //$NON-NLS-1$ //$NON-NLS-2$
            }
          } catch(InvalidVersionSpecificationException e) {
            log.error("Failed to parse version range:" + versionRange, e); //$NON-NLS-1$
          }
        }
        execution = exec;
        break;
      }
    }
    if (execution == null) {
      execution = createPluginExecution(document, pluginExecutions);
    }
    //now enter/update the goal(s)..
    Element goalsEl = getChild(execution, "pluginExecutionFilter", GOALS); //$NON-NLS-1$
    List<String> toAddGoals = new ArrayList<String>(Arrays.asList(goals));
    for (Element existingGoal : findChilds(goalsEl, GOAL)) {
      String glValue = getTextValue(existingGoal);
      if (glValue != null && toAddGoals.contains(glValue)) {
        toAddGoals.remove(glValue);
      }
    }
    if (toAddGoals.size() > 0) {
      for (String goal : toAddGoals) {
        format(createElementWithText(goalsEl, GOAL, goal));
      }
    }
    
  }

  private Element createPluginExecution(Document document, Element parent) {
    Element exec = document.createElement("pluginExecution"); //$NON-NLS-1$
    parent.appendChild(exec);
    Element filter = document.createElement("pluginExecutionFilter"); //$NON-NLS-1$
    exec.appendChild(filter);
    createElementWithText(filter, GROUP_ID, groupId);
    createElementWithText(filter, ARTIFACT_ID, artifactId);
    createElementWithText(filter, "versionRange", "[" + version + ",)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    
    Element actionEl = document.createElement("action"); //$NON-NLS-1$
    exec.appendChild(actionEl);
    Element actionEl2 = document.createElement(action.toString());
    actionEl.appendChild(actionEl2);
    if(PluginExecutionAction.execute.equals(action)) {
      //mkleint: a bit scared to have this text localized, with chinese/japanese locales, it could write garbage into the pom file..
      actionEl2.appendChild(document.createComment("use <runOnIncremental>false</runOnIncremental>to only execute the mojo during full/clean build")); //$NON-NLS-1$
    }
    
    format(exec);
    return exec;
  }

}
