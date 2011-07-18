/*******************************************************************************
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/


package org.eclipse.m2e.editor.xml.internal.lifecycle;


import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionFilter;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Reader;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Writer;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.ui.internal.UpdateConfigurationJob;
import org.eclipse.m2e.editor.xml.internal.Messages;

public class WorkspaceLifecycleMappingProposal extends WorkbenchMarkerResolution implements ICompletionProposal, ICompletionProposalExtension5, IMarkerResolution {
  private static final Logger log = LoggerFactory.getLogger(WorkspaceLifecycleMappingProposal.class);


  private IQuickAssistInvocationContext context;
  private final IMarker marker;

  private final PluginExecutionAction action;
  
  public WorkspaceLifecycleMappingProposal(IQuickAssistInvocationContext context, MarkerAnnotation mark,
      PluginExecutionAction action) {
    this.context = context;
    marker = mark.getMarker();
    this.action = action;
  }
  
  public WorkspaceLifecycleMappingProposal(IMarker marker, PluginExecutionAction action) {
    this.marker = marker;
    this.action = action;
  }
  
  public void apply(final IDocument doc) {
    run(marker);
  }
  
  /**
   * @return
   */
  private static LifecycleMappingMetadataSource getWorkspacePreferencesMetadataSources() {
    LifecycleMappingMetadataSource source = new LifecycleMappingMetadataSource();
    String mapp = MavenPluginActivator.getDefault().getPluginPreferences().getString("XXX_mappings");
    if (mapp != null) {
      LifecycleMappingMetadataSourceXpp3Reader reader = new LifecycleMappingMetadataSourceXpp3Reader();
      try {
        source = reader.read(new StringReader(mapp));
      } catch(IOException ex) {
        // TODO Auto-generated catch block
        log.error(ex.getMessage(), ex);
      } catch(XmlPullParserException ex) {
        // TODO Auto-generated catch block
        log.error(ex.getMessage(), ex);
      }
    }
    return source;
  }
  

  private static void performIgnore(IMarker mark, LifecycleMappingMetadataSource source) throws IOException, CoreException {
    String pluginGroupId = mark.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""); //$NON-NLS-1$
    String pluginArtifactId = mark.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""); //$NON-NLS-1$
    String pluginVersion = mark.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, ""); //$NON-NLS-1$
    String goal = mark.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, ""); //$NON-NLS-1$
    String id = pluginGroupId + ":" + pluginArtifactId;
    MojoExecutionKey key = new MojoExecutionKey(pluginGroupId, pluginArtifactId, pluginVersion, goal, null, null);
    boolean found = false;
        for (PluginExecutionMetadata pem : source.getPluginExecutions()) {
          PluginExecutionFilter filter = pem.getFilter();
          if (PluginExecutionAction.ignore.equals(pem.getAction())) {
            if (filter.getGroupId().equals(pluginGroupId) && filter.getArtifactId().equals(pluginArtifactId)) {
              found = true;
              try {
                VersionRange range = VersionRange.createFromVersionSpec(filter.getVersionRange());
                DefaultArtifactVersion version = new DefaultArtifactVersion(pluginVersion);
                if (!range.containsVersion(version)) {
                  filter.setVersionRange("[" + pluginVersion + ",)");
                }
              } catch(InvalidVersionSpecificationException e) {
                log.error(e.getMessage(), e);
              }
              if (!filter.getGoals().contains(goal)) {
                filter.addGoal(goal);
              }
              break;
            }
          }
        }
    if (!found) {
      PluginExecutionMetadata pe = new PluginExecutionMetadata();
      PluginExecutionFilter fil  = new PluginExecutionFilter(pluginGroupId, pluginArtifactId, "[" + pluginVersion + ",)", goal);
      pe.setFilter(fil);
      source.addPluginExecution(pe);
      Xpp3Dom actionDom = new Xpp3Dom("action");
      actionDom.addChild(new Xpp3Dom(PluginExecutionAction.ignore.name()));
      pe.setActionDom(actionDom);
    }
    
    
    
  }



  public String getAdditionalProposalInfo() {
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public String getDisplayString() {
    String goal = marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, ""); //$NON-NLS-1$
    return PluginExecutionAction.ignore.equals(action) ? NLS.bind("Mark goal {0} in workspace as ignored in Eclipse build", goal)
        : NLS.bind(Messages.LifecycleMappingProposal_execute_label, goal);
  }

  public Image getImage() {
    return PluginExecutionAction.ignore.equals(action) ? PlatformUI.getWorkbench().getSharedImages()
        .getImage(org.eclipse.ui.ISharedImages.IMG_TOOL_DELETE) : PlatformUI.getWorkbench().getSharedImages()
        .getImage(org.eclipse.ui.ISharedImages.IMG_TOOL_FORWARD);
  }

  public Point getSelection(IDocument arg0) {
    return null;
  }

  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    if (context == null) {
      //no context in markerresolution, just to be sure..
      return null;
    }
    String pluginGroupId = marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""); //$NON-NLS-1$
    String pluginArtifactId = marker.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""); //$NON-NLS-1$
    String pluginVersion = marker.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, ""); //$NON-NLS-1$
    String goal = marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, ""); //$NON-NLS-1$
    String execution = marker.getAttribute(IMavenConstants.MARKER_ATTR_EXECUTION_ID, "-"); //$NON-NLS-1$
    String phase = marker.getAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, "-"); //$NON-NLS-1$
    String info = NLS.bind(Messages.LifecycleMappingProposal_all_desc, 
        new Object[] {goal, execution, phase, pluginGroupId + ":" + pluginArtifactId + ":" + pluginVersion,  //$NON-NLS-1$ //$NON-NLS-2$
        (PluginExecutionAction.ignore.equals(action)
            ? "This quickfix generates a plugin configuration snippet recognized by the m2e integration during project configuration. It marks the given goal as ignored for the purposes of the Eclipse build." 
            : Messages.LifecycleMappingProposal_execute_desc)});
    
    return info;
  }

  public String getLabel() {
    return getDisplayString();
  }

  public void run(final IMarker marker) {
    try {
      if(PluginExecutionAction.ignore.equals(action)) {
        LifecycleMappingMetadataSource source = getWorkspacePreferencesMetadataSources();
        performIgnore(marker, source);
        LifecycleMappingMetadataSourceXpp3Writer writer = new LifecycleMappingMetadataSourceXpp3Writer();
        StringWriter sw = new StringWriter();
        writer.write(sw, source);
        MavenPluginActivator.getDefault().getPluginPreferences().setValue("XXX_mappings", sw.toString());
        
        //now update the project
        new UpdateConfigurationJob(new IProject[] {marker.getResource().getProject()}).schedule();
        MavenPluginActivator.getDefault().savePluginPreferences();
        
      }
    } catch(IOException e) {
      log.error("Error generating code in pom.xml", e); //$NON-NLS-1$
    } catch(CoreException e) {
      log.error(e.getMessage(), e);
    }
  }

  public String getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IMarker[] findOtherMarkers(IMarker[] markers) {
    List<IMarker> handled = new ArrayList<IMarker>();
    
    for (IMarker marker : markers) {
      if (marker == this.marker) {
        continue;
      }
      String hint = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
      if ( hint != null && hint.equals(IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION)) {
        handled.add(marker);
      }
    }
    return handled.toArray(new IMarker[handled.size()]);
  }
  
  @Override
  public void run(IMarker[] markers, IProgressMonitor monitor) {
    try {
      if(PluginExecutionAction.ignore.equals(action)) {
        Set<IProject> prjs = new HashSet<IProject>();
        LifecycleMappingMetadataSource source = getWorkspacePreferencesMetadataSources();
        for (IMarker mark : markers) {
          performIgnore(mark, source);
          prjs.add(mark.getResource().getProject());
        }
        LifecycleMappingMetadataSourceXpp3Writer writer = new LifecycleMappingMetadataSourceXpp3Writer();
        StringWriter sw = new StringWriter();
        writer.write(sw, source);
        MavenPluginActivator.getDefault().getPluginPreferences().setValue("XXX_mappings", sw.toString());
        
        MavenPluginActivator.getDefault().savePluginPreferences();
        for (IMarker mark : markers) {
          mark.delete();
        }
        //now update the projects
        new UpdateConfigurationJob(prjs.toArray(new IProject[0])).schedule();
        
      }
    } catch(IOException e) {
      log.error("Error generating code in pom.xml", e); //$NON-NLS-1$
    } catch(CoreException e) {
      log.error(e.getMessage(), e);
    }
  }

}