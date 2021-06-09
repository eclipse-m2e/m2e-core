/*******************************************************************************
 * Copyright (c) 2016 Anton Tanasenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Anton Tanasenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionFilter;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * AnnotationMappingMetadataSource
 *
 * @author atgsl
 */
public class AnnotationMappingMetadataSource implements MappingMetadataSource {

  private static final String SELF = ""; //$NON-NLS-1$

  private final MavenProject project;

  private final String projectId;

  private final List<PI> pis;

  public static AnnotationMappingMetadataSource get(MavenProject project) {
    List<PI> pis = parsePIs(project);
    if(!pis.isEmpty()) {
      return new AnnotationMappingMetadataSource(project, pis);
    }
    return null;
  }

  private AnnotationMappingMetadataSource(MavenProject project, List<PI> pis) {
    this.project = project;
    this.pis = pis;
    projectId = project.getModel().getLocation(SELF).getSource().getModelId();
  }

  public List<PluginExecutionMetadata> getPluginExecutionMetadata(MojoExecutionKey execution) {
    Xpp3Dom action = getAction(execution);
    if(action != null) {
      return Collections.singletonList(createMetadata(action));
    }
    return Collections.emptyList();
  }

  private Xpp3Dom getAction(MojoExecutionKey execution) {

    String key = Plugin.constructKey(execution.getGroupId(), execution.getArtifactId());
    Plugin plugin = getPlugin(project.getBuild(), key);
    Plugin mplugin = getPlugin(project.getPluginManagement(), key);

    String executionId = execution.getExecutionId();
    if(executionId != null) {

      // find pi for the executionId
      if(plugin != null) {
        PluginExecution pluginExecution = plugin.getExecutionsAsMap().get(executionId);
        if(pluginExecution != null) {
          Xpp3Dom action = getAction(pluginExecution);
          if(action != null)
            return action;
        }
      }
      if(mplugin != null) {
        PluginExecution pluginExecution = mplugin.getExecutionsAsMap().get(executionId);
        if(pluginExecution != null) {
          Xpp3Dom action = getAction(pluginExecution);
          if(action != null)
            return action;
        }
      }

      // find pi for the whole plugin
      if(plugin != null) {
        Xpp3Dom action = getAction(plugin);
        if(action != null)
          return action;
      }
      if(mplugin != null) {
        Xpp3Dom action = getAction(mplugin);
        if(action != null)
          return action;
      }
    }
    return null;
  }

  private Plugin getPlugin(PluginContainer plugins, String key) {
    return plugins == null ? null : plugins.getPluginsAsMap().get(key);
  }

  public LifecycleMappingMetadata getLifecycleMappingMetadata(String packagingType) throws DuplicateMappingException {
    return null;
  }

  private Xpp3Dom getAction(InputLocationTracker tracker) {
    InputLocation location = tracker.getLocation(SELF);

    if(location != null && location.getSource() != null && projectId.equals(location.getSource().getModelId())) {
      int l = location.getLineNumber();
      int c = location.getColumnNumber();
      for(PI pi : pis) {
        if(pi.l == l && pi.c == c) {
          return pi.action;
        }
      }
    }
    return null;
  }

  private PluginExecutionMetadata createMetadata(Xpp3Dom action) {
    Xpp3Dom actionDom = new Xpp3Dom("action"); //$NON-NLS-1$
    actionDom.addChild(action);

    PluginExecutionMetadata md = new PluginExecutionMetadata();
    md.setActionDom(actionDom);
    LifecycleMappingMetadataSource source = new LifecycleMappingMetadataSource();
    source.setSource(project);
    md.setSource(source);
    md.setFilter(new PluginExecutionFilter());
    return md;
  }

  private static List<PI> parsePIs(MavenProject project) {

    File pom = project.getFile();
    InputSource source = project.getModel().getLocation(SELF).getSource();

    List<PI> pis = new ArrayList<>();

    XmlPullParser parser = new MXParser();

    try (InputStream in = new FileInputStream(pom)) {
      parser.setInput(ReaderFactory.newXmlReader(in));

      Deque<State> stack = new LinkedList<>();

      int eventType = parser.getEventType();
      while(eventType != XmlPullParser.END_DOCUMENT) {

        if(eventType == XmlPullParser.START_TAG) {

          stack.push(new State(parser.getLineNumber(), parser.getColumnNumber()));

        } else if(eventType == XmlPullParser.END_TAG) {

          stack.pop();

        } else if(eventType == XmlPullParser.PROCESSING_INSTRUCTION && !stack.isEmpty()) {


          String text = parser.getText();
          if(text.startsWith("m2e ")) { //$NON-NLS-1$
            // found it
            Xpp3Dom dom = parse(text.substring(4));
            if(dom == null) {
              SourceLocation location = new SourceLocation(source.getLocation(), source.getModelId(),
                  parser.getLineNumber(), parser.getColumnNumber(), text.length() + 4);
              throw new LifecycleMappingConfigurationException(Messages.AnnotationMappingMetadataSource_UnsupportedInstructionFormat, location);
            }
            State s = stack.peek();
            PI pi = new PI(s.l, s.c, dom);
            pis.add(pi);
          }
        }

        eventType = parser.nextToken();
      }

    } catch(XmlPullParserException | IOException ex) {
      SourceLocation location = new SourceLocation(source.getLocation(), source.getModelId(), parser.getLineNumber(),
          parser.getColumnNumber(), 1);
      throw new LifecycleMappingConfigurationException(Messages.AnnotationMappingMetadataSource_ErrorParsingInstruction, location);
    }

    return pis;
  }

  private static final Splitter PI_SPLITTER = Splitter.on(CharMatcher.whitespace()).omitEmptyStrings().limit(2);

  private static final Splitter EXECUTE_SPLITTER = Splitter.on(',').omitEmptyStrings();

  private static final Map<String, String> EXECUTE_OPTIONS = new ImmutableMap.Builder<String, String>()
      .put("onConfiguration", LifecycleMappingFactory.ELEMENT_RUN_ON_CONFIGURATION) //$NON-NLS-1$
      .put("onIncremental", LifecycleMappingFactory.ELEMENT_RUN_ON_INCREMENTAL).build(); //$NON-NLS-1$

  private static Xpp3Dom parse(String pi) {

    List<String> split = PI_SPLITTER.splitToList(pi);

    PluginExecutionAction a = getAction(split.get(0));
    if(a == null) {
      return null;
    }
    switch(a) {
      case ignore:
        return new Xpp3Dom("ignore"); //$NON-NLS-1$

      case configurator:
        if(split.size() != 2) {
          return null;
        }
        Xpp3Dom conf = new Xpp3Dom("configurator"); //$NON-NLS-1$
        Xpp3Dom id = new Xpp3Dom("id"); //$NON-NLS-1$
        id.setValue(split.get(1));
        conf.addChild(id);
        return conf;

      case execute:
        Xpp3Dom exec = new Xpp3Dom("execute"); //$NON-NLS-1$
        if(split.size() > 1) {
          for(String option : EXECUTE_SPLITTER.split(split.get(1))) {
            String value = EXECUTE_OPTIONS.get(option);
            if(value == null) {
              return null;
            }
            Xpp3Dom opt = new Xpp3Dom(value);
            opt.setValue("true"); //$NON-NLS-1$
            exec.addChild(opt);
          }
        }
        return exec;

      default:
        return null;
    }
  }

  private static PluginExecutionAction getAction(String value) {
    for(PluginExecutionAction a : PluginExecutionAction.values()) {
      if(value.toLowerCase().equals(a.name())) {
        return a;
      }
    }
    return null;
  }

  private static class State {
    final int l;

    final int c;

    State(int l, int c) {
      this.l = l;
      this.c = c;
    }
  }

  private static class PI {
    final int l;

    final int c;

    final Xpp3Dom action;

    PI(int l, int c, Xpp3Dom action) {
      this.l = l;
      this.c = c;
      this.action = action;
    }
  }
}
