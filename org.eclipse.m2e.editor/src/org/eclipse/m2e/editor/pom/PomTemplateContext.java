/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.pom;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;

import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.search.util.ArtifactInfo;
import org.eclipse.m2e.core.ui.internal.search.util.Packaging;
import org.eclipse.m2e.core.ui.internal.search.util.SearchEngine;
import org.eclipse.m2e.core.ui.internal.util.XmlUtils;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.mojo.IMojoParameterMetadataProvider;
import org.eclipse.m2e.editor.mojo.MojoParameter;
import org.eclipse.m2e.editor.mojo.MojoParameterMetadataProvider;


/**
 * Context types.
 *
 * @author Lukas Krecan
 * @author Eugene Kuleshov
 */
public enum PomTemplateContext {

  UNKNOWN("unknown"), // //$NON-NLS-1$

  DOCUMENT("#document"), // //$NON-NLS-1$

  PROJECT("project"), // //$NON-NLS-1$

  BUILD("build"), // //$NON-NLS-1$

  PARENT("parent"), // //$NON-NLS-1$

  RELATIVE_PATH("relativePath"), // //$NON-NLS-1$

  DEPENDENCIES("dependencies"), // //$NON-NLS-1$

  DEPENDENCY_MANAGEMENT("dependencyManagement"), // //$NON-NLS-1$

  EXCLUSIONS("exclusions"), // //$NON-NLS-1$

  PLUGINS("plugins"), // //$NON-NLS-1$

  PLUGIN("plugin"), // //$NON-NLS-1$

  PLUGIN_MANAGEMENT("pluginManagement"), // //$NON-NLS-1$

  EXECUTIONS("executions"), // //$NON-NLS-1$

  PROFILES("profiles"), // //$NON-NLS-1$

  PROFILE("profile"), // //$NON-NLS-1$

  REPOSITORIES("repositories"), // //$NON-NLS-1$

  PROPERTIES("properties") { // //$NON-NLS-1$

    @Override
    protected void addTemplates(MavenProject project, IProject eclipsePrj, Collection<Template> templates,
        Node currentNode, String prefix) {

      // propose overridable properties

      Set<String> currentNodeProps = new HashSet<>();
      NodeList nodes = currentNode.getChildNodes();
      for(int i = 0; i < nodes.getLength(); i++ ) {
        currentNodeProps.add(nodes.item(i).getNodeName());
      }

      if(project != null) {
        Properties props = project.getProperties();
        if(props != null) {
          for(Map.Entry<Object, Object> e : props.entrySet()) {
            String name = e.getKey().toString();
            String value = e.getValue().toString();

            if(currentNodeProps.contains(name))
              continue;

            if(!name.startsWith(prefix))
              continue;

            String template = "<" + name + ">${" + value + "}</" + name + ">"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            String desc = NLS.bind(Messages.PomTemplateContext_property_override, value);
            templates.add(new PomTemplate(name, desc, getContextTypeId(), template, false)
                .image(MavenEditorImages.IMG_PROPERTY).relevance(2000));
          }
        }
      }

    }
  },

  CONFIGURATION("configuration") { //$NON-NLS-1$

    @Override
    public boolean handlesSubtree() {
      return true;
    }

    @Override
    protected void addTemplates(final MavenProject project, IProject eclipseprj, Collection<Template> proposals,
        Node node, String prefix) throws CoreException {
      // find configuration ancestor

      List<String> pathElements = new ArrayList<>();
      String configImpl = null;

      Element configNode = (Element) node;
      while(configNode != null && !getNodeName().equals(configNode.getNodeName())) {

        String impl = configNode.getAttribute("implementation");
        if(impl != null && !impl.trim().isEmpty()) {
          configImpl = impl;
        }

        if(configImpl == null) {
          pathElements.add(configNode.getNodeName());
        }
        configNode = (Element) configNode.getParentNode();
      }
      if(configNode == null) {
        return;
      }

      Collections.reverse(pathElements);
      String[] configPath = pathElements.toArray(new String[pathElements.size()]);

      Node configContainer = null;
      Node pluginSubNode = configNode;
      String containerName = pluginSubNode.getParentNode().getNodeName();
      if("execution".equals(containerName) //$NON-NLS-1$
          || "reportSet".equals(containerName)) { //$NON-NLS-1$
        configContainer = pluginSubNode.getParentNode();
        pluginSubNode = configContainer.getParentNode();
      }
      String groupId = getGroupId(pluginSubNode);
      if(groupId == null) {
        groupId = "org.apache.maven.plugins"; // TODO support other default groups //$NON-NLS-1$
      }
      String artifactId = getArtifactId(pluginSubNode);
      String version = extractVersion(project, eclipseprj, getVersion(pluginSubNode), groupId, artifactId,
          EXTRACT_STRATEGY_PLUGIN | EXTRACT_STRATEGY_SEARCH);
      if(version == null) {
        return;
      }

      // collect used mojo goals
      final Set<String> usedMojos = new HashSet<>();
      if("execution".equals(containerName)) { //$NON-NLS-1$
        Node goalsNode = getChildWithName(configContainer, "goals"); //$NON-NLS-1$
        if(goalsNode != null) {

          NodeList children = goalsNode.getChildNodes();
          int l = children.getLength();
          for(int i = 0; i < l; i++ ) {
            Node goalNode = children.item(i);
            if("goal".equals(goalNode.getNodeName())) { //$NON-NLS-1$
              String goal = XmlUtils.getTextValue(goalNode);
              if(goal != null && !goal.isEmpty()) {
                usedMojos.add(goal);
              }
            }
          }
        }
      }

      ArtifactKey pluginKey = new ArtifactKey(groupId, artifactId, version, null);
      IMojoParameterMetadataProvider prov = new MojoParameterMetadataProvider();
      MojoParameter result;
      if(configImpl != null) {
        result = prov.getClassConfiguration(pluginKey, configImpl);
      } else if(usedMojos.isEmpty()) {
        result = prov.getMojoConfiguration(pluginKey);
      } else {
        result = prov.getMojoConfiguration(pluginKey, usedMojos);
      }

      MojoParameter param = result.getContainer(configPath);

      if(param != null) {
        List<MojoParameter> nestedParameters = param.getNestedParameters();
        for(MojoParameter parameter : nestedParameters) {
          String name = parameter.getName();
          if(name.startsWith(prefix)) {

            String text = NLS.bind(Messages.PomTemplateContext_param, parameter.isRequired(), parameter.getType());

            String expression = parameter.getExpression();
            if(expression != null) {
              text += NLS.bind(Messages.PomTemplateContext_param_expr, expression);
            }

            String defaultValue = parameter.getDefaultValue();
            if(defaultValue != null) {
              text += NLS.bind(Messages.PomTemplateContext_param_def, defaultValue);
            }

            String description = parameter.getDescription();
            if(description != null) {
              String desc = description.trim();
              text += desc.startsWith("<p>") ? desc : "<br>" + desc; //$NON-NLS-1$ //$NON-NLS-2$
            }

            proposals.add(new PomTemplate(name, text, getContextTypeId(), //
                "<" + name + ">${cursor}</" + name + ">", false) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    .image(MavenEditorImages.IMG_PARAMETER).relevance(1900));
          }
        }

        if(param.isMap()) {

          if(prefix != null && !prefix.trim().isEmpty()) {
            proposals.add(new PomTemplate(NLS.bind(Messages.PomTemplateContext_insertParameter, prefix), "", //$NON-NLS-1$
                getContextTypeId(), "<" + prefix + ">${cursor}</" + prefix + ">", true) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    .image(MavenEditorImages.IMG_PARAMETER).relevance(1500));

          }

        }

        if(nestedParameters.size() == 1) {
          MojoParameter nestedParam = nestedParameters.get(0);
          if(nestedParam.isMultiple()) {
            boolean containsFiles = File.class.getSimpleName().equals(nestedParam.getType())
                || PomTemplateContext.fromNodeName(nestedParam.getName()).handlesFiles();
            if(containsFiles) {
              addFileTemplates(project, eclipseprj, proposals, node, prefix,
                  param.getName().toLowerCase().endsWith("directory"), nestedParam.getName());
            }
          }
        }

        boolean containsFiles = File.class.getSimpleName().equals(param.getType())
            || PomTemplateContext.fromNodeName(param.getName()).handlesFiles();
        if(containsFiles) {
          addFileTemplates(project, eclipseprj, proposals, node, prefix,
              param.getName().toLowerCase().endsWith("directory"), null);
        }

      }
    }
  },

  GROUP_ID("groupId") { //$NON-NLS-1$
    @Override
    public void addTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals, Node node,
        String prefix) {
      for(String groupId : getSearchEngine(eclipseprj).findGroupIds(prefix, getPackaging(node),
          getContainingArtifact(node))) {
        checkAndAdd(proposals, prefix, groupId);
      }
    }
  },

  ARTIFACT_ID("artifactId") { //$NON-NLS-1$
    @Override
    public void addTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals, Node node,
        String prefix) {
      String groupId = getGroupId(node);
      //#MNGECLIPSE-1832
      if((groupId == null || groupId.trim().length() == 0) && "plugin".equals(node.getParentNode().getNodeName())) {
        groupId = "org.apache.maven.plugins"; //$NON-NLS-1$
      }
      if(groupId != null) {
        for(String artifactId : getSearchEngine(eclipseprj).findArtifactIds(groupId, prefix, getPackaging(node),
            getContainingArtifact(node))) {
          checkAndAdd(proposals, prefix, artifactId, groupId + ":" + artifactId);
        }
      }
    }
  },

  VERSION("version") { //$NON-NLS-1$
    @Override
    public void addTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals, Node node,
        String prefix) {
      String groupId = getGroupId(node);
      //#MNGECLIPSE-1832
      if((groupId == null || groupId.trim().length() == 0) && "plugin".equals(node.getParentNode().getNodeName())) {
        groupId = "org.apache.maven.plugins"; //$NON-NLS-1$
      }
      String artifactId = getArtifactId(node);
      if(groupId != null && artifactId != null) {
        for(String version : getSearchEngine(eclipseprj).findVersions(groupId, artifactId, prefix,
            getPackaging(node))) {
          checkAndAdd(proposals, prefix, version, groupId + ":" + artifactId + ":" + version);
        }
      }
      //mkleint: this concept that all versions out there are equal is questionable..
      if("dependency".equals(node.getParentNode().getNodeName())) { //$NON-NLS-1$
        //see if we can complete the properties ending with .version

        List<String> keys = new ArrayList<>();
        String contextTypeId = getContextTypeId();
        MavenProject mvn = project;
        if(mvn != null) {
          //if groupid is the same, suggest ${project.version}
          if(groupId != null && groupId.equals(mvn.getGroupId())) {
            proposals.add(new Template("${project.version}", Messages.PomTemplateContext_project_version_hint, //$NON-NLS-1$
                contextTypeId, "$${project.version}", false));//$NON-NLS-1$
          }
          Properties props = mvn.getProperties();
          if(props != null) {
            for(Object key : props.keySet()) {
              //only add the properties following the .version convention
              if(key.toString().endsWith(".version") || key.toString().endsWith("Version")) { //$NON-NLS-1$ //$NON-NLS-2$
                keys.add(key.toString());
              }
            }
            //sort just properties
            Collections.sort(keys);
            if(!keys.isEmpty()) {
              for(String key : keys) {
                String expr = "${" + key + "}"; //$NON-NLS-1$ //$NON-NLS-2$
                proposals.add(new Template(expr, Messages.PomTemplateContext_expression_description, contextTypeId,
                    '$' + expr, false));
              }
            }
          }

        } else {
          //if we don't have the maven facade, it means the pom is probably broken.
          //all we can do is to try guess the groupid and come up with the project.version proposal eventually
          Element root = node.getOwnerDocument().getDocumentElement();
          if(root != null && "project".equals(root.getNodeName())) {//$NON-NLS-1$
            String currentgroupid = XmlUtils.getTextValue(XmlUtils.findChild(root, "groupId"));//$NON-NLS-1$
            if(currentgroupid == null) {
              Element parEl = XmlUtils.findChild(root, "parent");//$NON-NLS-1$
              if(parEl != null) {
                currentgroupid = XmlUtils.getTextValue(XmlUtils.findChild(parEl, "groupId"));//$NON-NLS-1$
              }
            }
            if(groupId != null && groupId.equals(currentgroupid)) {
              proposals.add(new Template("${project.version}", Messages.PomTemplateContext_project_version_hint, //$NON-NLS-1$
                  contextTypeId, "$${project.version}", false));//$NON-NLS-1$
            }
          }
        }
      }
    }
  },

  CLASSIFIER("classifier") { //$NON-NLS-1$
    @Override
    public void addTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals, Node node,
        String prefix) {
      String groupId = getGroupId(node);
      String artifactId = getArtifactId(node);
      String version = getVersion(node);
      if(groupId != null && artifactId != null && version != null) {
        for(String classifier : getSearchEngine(eclipseprj).findClassifiers(groupId, artifactId, version, prefix,
            getPackaging(node))) {
          checkAndAdd(proposals, prefix, classifier, groupId + ":" + artifactId + ":" + version + ":" + classifier);
        }
      }
    }
  },

  TYPE("type") { //$NON-NLS-1$
    @Override
    public void addTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals, Node node,
        String prefix) {
      String groupId = getGroupId(node);
      String artifactId = getArtifactId(node);
      String version = getVersion(node);
      if(groupId != null && artifactId != null && version != null) {
        for(String type : getSearchEngine(eclipseprj).findTypes(groupId, artifactId, version, prefix,
            getPackaging(node))) {
          checkAndAdd(proposals, prefix, type, groupId + ":" + artifactId + ":" + version + ":" + type);
        }
      }
    }
  },

  PACKAGING("packaging") { //$NON-NLS-1$
    @Override
    public void addTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals, Node node,
        String prefix) {
      // TODO only show "pom" packaging in root section
      checkAndAdd(proposals, prefix, "pom"); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "jar"); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "war"); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "ear"); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "ejb"); //$NON-NLS-1$
//      checkAndAdd(proposals, prefix, "eclipse-plugin"); //$NON-NLS-1$
//      checkAndAdd(proposals, prefix, "eclipse-feature"); //$NON-NLS-1$
//      checkAndAdd(proposals, prefix, "eclipse-update-site"); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "maven-plugin"); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "maven-archetype"); //$NON-NLS-1$
    }
  },

  SCOPE("scope") { //$NON-NLS-1$
    @Override
    public void addTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals, Node node,
        String prefix) {
      checkAndAdd(proposals, prefix, "compile"); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "test"); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "provided"); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "runtime"); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "system"); //$NON-NLS-1$

      if(getAncestor(node, "dependency", "dependencies", "dependencyManagement") != null) {// $NON-NLS-1$ $NON-NLS-2$ $NON-NLS-3$
        checkAndAdd(proposals, prefix, "import"); //$NON-NLS-1$
      }
    }
  },

  SYSTEM_PATH("systemPath"), //$NON-NLS-1$

  PHASE("phase") { //$NON-NLS-1$
    @Override
    public void addTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals, Node node,
        String prefix) {
      // TODO the following list should be derived from the packaging handler (the actual lifecycle)

      // Clean Lifecycle
      checkAndAdd(proposals, prefix, "pre-clean", Messages.PomTemplateContext_preclean); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "clean", Messages.PomTemplateContext_clean); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "post-clean", Messages.PomTemplateContext_postclean); //$NON-NLS-1$

      // Default Lifecycle
      checkAndAdd(proposals, prefix, "validate", Messages.PomTemplateContext_validate); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "generate-sources", Messages.PomTemplateContext_generatesources); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "process-sources", Messages.PomTemplateContext_processsources); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "generate-resources", Messages.PomTemplateContext_generateresources); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "process-resources", Messages.PomTemplateContext_processresources); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "compile", Messages.PomTemplateContext_compile); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "process-classes", Messages.PomTemplateContext_processclasses); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "generate-test-sources", Messages.PomTemplateContext_generatetestsources); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "process-test-sources", Messages.PomTemplateContext_processtestsources); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "generate-test-resources", Messages.PomTemplateContext_generatetestresources); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "process-test-resources", Messages.PomTemplateContext_processtestresources); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "test-compile", Messages.PomTemplateContext_testcompile); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "process-test-classes", Messages.PomTemplateContext_processtestclasses); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "test", Messages.PomTemplateContext_test); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "prepare-package", Messages.PomTemplateContext_preparepackage); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "package", Messages.PomTemplateContext_package); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "pre-integration-test", Messages.PomTemplateContext_preintegrationtest); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "integration-test", Messages.PomTemplateContext_integrationtest); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "post-integration-test", Messages.PomTemplateContext_postintegrationtest); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "verify", Messages.PomTemplateContext_verify); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "install", Messages.PomTemplateContext_install); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "deploy", Messages.PomTemplateContext_deploy); //$NON-NLS-1$

      // Site Lifecycle
      checkAndAdd(proposals, prefix, "pre-site", Messages.PomTemplateContext_presite); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "site", Messages.PomTemplateContext_site); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "post-site", Messages.PomTemplateContext_postsite); //$NON-NLS-1$
      checkAndAdd(proposals, prefix, "site-deploy", Messages.PomTemplateContext_sitedeploy); //$NON-NLS-1$
    }
  },

  GOAL("goal") { //$NON-NLS-1$
    @Override
    public void addTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals, Node node,
        String prefix) {
      if(!"goals".equals(node.getParentNode().getNodeName())) { //$NON-NLS-1$
        return;
      }
      node = node.getParentNode();
      if(!"execution".equals(node.getParentNode().getNodeName())) { //$NON-NLS-1$
        return;
      }
      node = node.getParentNode();
      if(!"executions".equals(node.getParentNode().getNodeName())) { //$NON-NLS-1$
        return;
      }
      node = node.getParentNode();

      String groupId = getGroupId(node);
      if(groupId == null) {
        groupId = "org.apache.maven.plugins"; //$NON-NLS-1$
      }
      String artifactId = getArtifactId(node);

      String version = extractVersion(project, eclipseprj, getVersion(node), groupId, artifactId,
          EXTRACT_STRATEGY_PLUGIN | EXTRACT_STRATEGY_SEARCH);
      if(version == null) {
        return;
      }

      PluginDescriptor descriptor = PomTemplateContextUtil.INSTANCE.getPluginDescriptor(groupId, artifactId, version);
      if(descriptor != null) {
        List<MojoDescriptor> mojos = descriptor.getMojos();
        if(mojos != null) {
          for(MojoDescriptor mojo : mojos) {
            checkAndAdd(proposals, prefix, mojo.getGoal(), mojo.getDescription());
          }
        }
      }
    }
  },

  MODULES("modules") { //$NON-NLS-1$
    @Override
    public void addTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals, Node node,
        String prefix) {
      addModuleTemplates(project, eclipseprj, proposals, node, prefix, true);
    }
  },

  MODULE("module") { //$NON-NLS-1$
    @Override
    public void addTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals, Node node,
        String prefix) {
      addModuleTemplates(project, eclipseprj, proposals, node, prefix, false);
    }
  },

  SOURCEDIRECTORY("sourceDirectory", "file"), //$NON-NLS-1$ //$NON-NLS-2$
  SCRIPTSOURCEDIRECTORY("scriptSourceDirectory", "file"), //$NON-NLS-1$ //$NON-NLS-2$
  TESTSOURCEDIRECTORY("testSourceDirectory", "file"), //$NON-NLS-1$ //$NON-NLS-2$
  OUTPUTDIRECTORY("outputDirectory", "file"), //$NON-NLS-1$ //$NON-NLS-2$
  TESTOUTPUTDIRECTORY("testOutputDirectory", "file"), //$NON-NLS-1$ //$NON-NLS-2$
  DIRECTORY("directory", "file"), //$NON-NLS-1$ //$NON-NLS-2$
  FILTER("filter", "file"), //$NON-NLS-1$ //$NON-NLS-2$

  LICENSES("licenses"); //$NON-NLS-1$

  private static final Logger log = LoggerFactory.getLogger(PomTemplateContext.class);

  private static final String PREFIX = "org.eclipse.m2e.editor.xml.templates.contextType."; //$NON-NLS-1$

  private final String nodeName;

  private final String contextSuffix;

  PomTemplateContext(String nodeName) {
    this(nodeName, nodeName);
  }

  PomTemplateContext(String nodeName, String contextSuffix) {
    this.nodeName = nodeName;
    this.contextSuffix = contextSuffix;
  }

  public boolean handlesSubtree() {
    return false;
  }

  public boolean handlesFiles() {
    return "file".equals(contextSuffix); //$NON-NLS-1$
  }

  /**
   * Return templates depending on the context type.
   */
  public Template[] getTemplates(MavenProject project, IProject eclipsePrj, Node node, String prefix) {
    Collection<Template> templates = new ArrayList<>();
    try {
      addTemplates(project, eclipsePrj, templates, node, prefix);
    } catch(CoreException e) {
      log.error(e.getMessage(), e);
    }
    return templates.toArray(new Template[templates.size()]);
  }

  /**
   * @param project
   * @param eclipsePrj only here because getSearchEngine() requires it as parameter.
   * @param templates
   * @param currentNode
   * @param prefix
   * @throws CoreException
   */
  protected void addTemplates(MavenProject project, IProject eclipsePrj, Collection<Template> templates,
      Node currentNode, String prefix) throws CoreException {
    if(handlesFiles()) {
      addFileTemplates(project, eclipsePrj, templates, currentNode, prefix, name().toLowerCase().endsWith("directory"),
          null);
    }
  }

  protected FileProposalContext getFileProposalContext(MavenProject project, IProject eclipsePrj, String prefix) {
    if(project == null && eclipsePrj == null) {
      return null;
    }

    File projectDir;
    if(project != null) {
      projectDir = project.getFile().getParentFile();
    } else {
      projectDir = new File(eclipsePrj.getLocationURI());
    }

    String parentPath;
    String prefixPath;
    int lastSep = prefix.lastIndexOf('/');
    if(lastSep != -1) {
      prefixPath = prefix.substring(0, lastSep) + '/';
      prefix = prefix.substring(lastSep + 1);
    } else {
      prefixPath = "";
    }
    String interpolated = simpleInterpolate(project, prefixPath);
    parentPath = interpolated == null ? prefixPath : interpolated;

    File parentDir;
    if(!new File(parentPath).isAbsolute()) {
      parentDir = new File(projectDir, parentPath);
    } else {
      parentDir = new File(parentPath);
    }
    if(!parentDir.isDirectory()) {
      return null;
    }
    return new FileProposalContext(projectDir, parentDir, prefixPath, prefix);
  }

  protected void addFileTemplates(MavenProject project, IProject eclipsePrj, Collection<Template> templates,
      Node currentNode, String prefix, boolean dirsOnly, String wrapperNode) {

    FileProposalContext pctx = getFileProposalContext(project, eclipsePrj, prefix);
    if(pctx == null) {
      return;
    }

    List<File> files = Arrays.asList(pctx.parentDir.listFiles());
    Collections.sort(files, Comparator.comparingInt((File r) -> r.isDirectory() ? 0 : 1)
        .thenComparing(Comparator.comparing(File::getName)));

    int rel = 4000;
    for(File f : files) {
      if(f.getName().startsWith(pctx.prefix)) {
        String value = pctx.prefixPath + f.getName();
        String template = value;
        boolean retrigger = false;

        if(f.isDirectory()) {
          if(hasContents(f, dirsOnly)) {
            template += '/';
            retrigger = true;
          }
        } else if(dirsOnly) {
          continue;
        }
        template = template.replace("$", "$$");

        if(wrapperNode != null) {
          template = '<' + wrapperNode + '>' + template + "${cursor}</" + wrapperNode + '>';
        }

        templates.add(new PomTemplate(f.getName(), "", getContextTypeId(), template, false).image(getFileIcon(f))
            .matchValue(value).relevance(rel-- ).retriggerOnApply(retrigger));
      }
    }
  }

  private boolean hasContents(File f, boolean dirsOnly) {
    // using nio is faster for large dirs compared to File#listFiles()
    boolean[] res = new boolean[] {false};
    Path thisPath = f.toPath();
    try {
      Files.walkFileTree(thisPath, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
          if(thisPath.equals(dir)) {
            return FileVisitResult.CONTINUE;
          }
          res[0] = true;
          return FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if(dirsOnly) {
            return FileVisitResult.CONTINUE;
          }
          res[0] = true;
          return FileVisitResult.TERMINATE;
        }
      });
    } catch(IOException ex) {
    }
    return res[0];
  }

  protected void addModuleTemplates(MavenProject project, IProject eclipseprj, Collection<Template> proposals,
      Node node, String prefix, boolean wrap) {
    if(project == null) {
      //shall not happen just double check.
      return;
    }
    FileProposalContext pctx = getFileProposalContext(project, eclipseprj, prefix);
    if(pctx == null) {
      return;
    }

    //MNGECLIPSE-2204 collect the existing values from the surrounding xml content only..
    // also, if it's a profile modules list, consider main modules as well
    Set<String> existings = new HashSet<>();
    Node moduleNode = node;
    if(moduleNode != null) {
      Node modulesNode;
      if("modules".equals(moduleNode.getLocalName())) {
        modulesNode = moduleNode;
      } else {
        modulesNode = moduleNode.getParentNode();
      }
      while(modulesNode != null) {
        for(Element el : XmlUtils.findChilds((Element) modulesNode, "module")) {
          if(el != moduleNode) {
            String val = XmlUtils.getTextValue(el);
            if(val != null) {
              existings.add(val);
            }
          }
        }
        Node profileProjectNode = getAncestor(modulesNode, "profile", "profiles", "project");
        if(profileProjectNode != null) {
          modulesNode = getChildWithName(profileProjectNode, "modules");
        } else {
          modulesNode = null;
        }
      }
    }

    Set<String> subProjects = new TreeSet<>();
    try {
      Path projectPath = pctx.projectDir.toPath().toRealPath();
      Path parentPath = pctx.parentDir.toPath().toRealPath();

      FileVisitor<Path> visitor = new SimpleFileVisitor<>() {

        boolean submodulesSearch;

        boolean submodulesFound;

        Path submodulesSearchBase;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

          if(submodulesSearch && submodulesFound) {
            // drop out quickly if we were searching for submodules and at least one was already found
            return FileVisitResult.SKIP_SIBLINGS;
          }

          String name = dir.getFileName().toString();
          if(name.startsWith(".")) {
            // skip dotfiles
            return FileVisitResult.SKIP_SUBTREE;
          }

          if(parentPath.equals(dir)) {
            // don't propose the dir we are looking under
            return FileVisitResult.CONTINUE;
          }

          if(projectPath.equals(dir) && pctx.prefixPath.startsWith("../")) {
            // skip this project's dir entirely when looking for modules in parent dir
            return FileVisitResult.SKIP_SUBTREE;
          }

          if(projectPath.startsWith(dir)) {
            // skip ancestors of current dir
            return FileVisitResult.CONTINUE;
          }

          //TODO polyglot?
          if(Arrays.asList("src", "target", "bin").contains(name) && dir.resolve("../pom.xml").toFile().exists()) {
            // skip recursing into certain the project dirs
            return FileVisitResult.SKIP_SUBTREE;
          }

          if(dir.resolve("pom.xml").toFile().exists()) {

            if(submodulesSearch) {
              // we were looking for submodules and found at least one
              submodulesFound = true;
              return FileVisitResult.SKIP_SIBLINGS;
            }

            // found a candidate
            String path = projectPath.relativize(dir).toString().replace('\\', '/');
            if(!existings.contains(path)) {
              subProjects.add(path);
            }

            // now we need to check for submodules, and if there are any, add a <path>/ proposal
            submodulesSearch = true;
            submodulesSearchBase = dir;
            submodulesFound = false;
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) {
          if(submodulesSearch && dir.equals(submodulesSearchBase)) {

            // finish search for submodules
            if(submodulesFound) {
              String path = projectPath.relativize(dir).toString().replace('\\', '/');
              subProjects.add(path + '/');
            }

            submodulesSearch = false;
            submodulesSearchBase = null;
            submodulesFound = false;
          }

          return FileVisitResult.CONTINUE;
        }
      };

      Files.walkFileTree(parentPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 5, visitor);
    } catch(IOException e) {
    }

    subProjects.removeAll(existings);

    int moduleRel = 8000;
    int submoduleRel = 4000;
    for(String path : subProjects) {
      if(path.startsWith(prefix)) {
        String value = path;
        String template = value;
        template = template.replace("$", "$$");

        Image image;
        String description;
        int rel;
        boolean retrigger = false;
        if(path.endsWith("/")) {
          image = MavenEditorImages.IMG_DISCOVERY;
          description = NLS.bind(Messages.PomTemplateContext_submodules, path);
          rel = submoduleRel-- ;
          retrigger = true;
        } else {
          image = getFileIcon(new File(pctx.projectDir, path));
          description = NLS.bind(Messages.PomTemplateContext_module, path);
          rel = moduleRel-- ;
        }
        if(wrap) {
          template = "<module>" + template + "${cursor}</module>";
        }

        proposals.add(new PomTemplate(value, description, getContextTypeId(), template, false).image(image)
            .relevance(rel).retriggerOnApply(retrigger));
      }
    }
  }

  protected static Image getFileIcon(File f) {
    IWorkspaceRoot wroot = ResourcesPlugin.getWorkspace().getRoot();
    IResource[] resources;
    if(f.isDirectory()) {
      resources = wroot.findContainersForLocationURI(f.toURI());
    } else {
      resources = wroot.findFilesForLocationURI(f.toURI());
    }
    IResource res = resources.length > 0 ? resources[0] : null;
    if(res != null) {
      IWorkbenchAdapter wbAdapter = res.getAdapter(IWorkbenchAdapter.class);
      if(wbAdapter == null) {
        return null;
      }
      ImageDescriptor id = wbAdapter.getImageDescriptor(res);
      return id != null ? MavenEditorImages.getImage(id) : null;
    }

    if(f.isDirectory()) {
      return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
    }
    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
  }

  protected String getNodeName() {
    return nodeName;
  }

  public String getContextTypeId() {
    return PREFIX + contextSuffix;
  }

  public static PomTemplateContext fromId(String contextTypeId) {
    for(PomTemplateContext context : values()) {
      if(context.getContextTypeId().equals(contextTypeId)) {
        return context;
      }
    }
    return UNKNOWN;
  }

  public static PomTemplateContext fromNodeName(String idSuffix) {
    for(PomTemplateContext context : values()) {
      if(context.getNodeName().equals(idSuffix)) {
        return context;
      }
    }
    return UNKNOWN;
  }

  public static PomTemplateContext fromNode(Node node) {
    PomTemplateContext context = PomTemplateContext.fromNodeName(node.getNodeName());

    // find an ancestor whose context impl can handle all of its subtree
    PomTemplateContext ancestorContext = context;
    while(!ancestorContext.handlesSubtree() && node != null) {
      ancestorContext = PomTemplateContext.fromNodeName(node.getNodeName());
      node = node.getParentNode();
    }
    if(ancestorContext.handlesSubtree()) {
      context = ancestorContext;
    }
    return context;
  }

  protected static SearchEngine getSearchEngine(IProject project) {
    if(searchEngineForTests != null) {
      return searchEngineForTests;
    }
    return M2EUIPluginActivator.getDefault().getSearchEngine(project);
  }

  private static SearchEngine searchEngineForTests;

  public static void setSearchEngineForTests(SearchEngine _searchEngineForTests) {
    searchEngineForTests = _searchEngineForTests;
  }

  /**
   * Returns containing artifactInfo for exclusions. Otherwise returns null.
   */
  protected ArtifactInfo getContainingArtifact(Node currentNode) {
    if(isExclusion(currentNode)) {
      Node node = currentNode.getParentNode().getParentNode();
      return getArtifactInfo(node);
    }
    return null;
  }

  /**
   * Returns artifact info from siblings of given node.
   */
  private ArtifactInfo getArtifactInfo(Node node) {
    return new ArtifactInfo(getGroupId(node), getArtifactId(node), getVersion(node), //
        getSiblingTextValue(node, "classifier"), getSiblingTextValue(node, "type")); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Returns required packaging.
   */
  protected Packaging getPackaging(Node currentNode) {
    if(isPlugin(currentNode)) {
      return Packaging.PLUGIN;
    } else if(isParent(currentNode)) {
      return Packaging.POM;
    }
    return Packaging.ALL;
  }

  /**
   * Returns true if user is editing plugin dependency.
   */
  private boolean isPlugin(Node currentNode) {
    return "plugin".equals(currentNode.getParentNode().getNodeName()); //$NON-NLS-1$
  }

  /**
   * Returns true if user is editing plugin dependency exclusion.
   */
  private boolean isExclusion(Node currentNode) {
    return "exclusion".equals(currentNode.getParentNode().getNodeName()); //$NON-NLS-1$
  }

  /**
   * Returns true if user is editing parent dependency.
   */
  private boolean isParent(Node currentNode) {
    return "parent".equals(currentNode.getParentNode().getNodeName()); //$NON-NLS-1$
  }

  protected String getGroupId(Node currentNode) {
    return getSiblingTextValue(currentNode, "groupId"); //$NON-NLS-1$
  }

  protected void checkAndAdd(Collection<Template> proposals, String prefix, String name) {
    checkAndAdd(proposals, prefix, name, name);
  }

  protected void checkAndAdd(Collection<Template> proposals, String prefix, String name, String description) {
    checkAndAdd(proposals, prefix, name, name, -1);
  }

  protected void checkAndAdd(Collection<Template> proposals, String prefix, String name, String description, int rel) {
    if(name.startsWith(prefix)) {
      proposals.add(new PomTemplate(name, description, getContextTypeId(), name, false).relevance(rel));
    }
  }

  /**
   * @param project
   * @param version
   * @param groupId
   * @param artifactId
   * @return
   * @throws CoreException
   */

  static int EXTRACT_STRATEGY_PLUGIN = 1;

  static int EXTRACT_STRATEGY_DEPENDENCY = 2;

  static int EXTRACT_STRATEGY_SEARCH = 4;

  static String extractVersion(MavenProject mp, IProject project, String version, String groupId, String artifactId,
      int strategy) {

    assert mp != null;
    version = simpleInterpolate(mp, version);

    if(version == null) {
      Packaging pack = Packaging.ALL;
      if((strategy & EXTRACT_STRATEGY_PLUGIN) != 0) {
        version = searchPM(mp, groupId, artifactId);
        pack = Packaging.PLUGIN;
      }
      if((strategy & EXTRACT_STRATEGY_DEPENDENCY) != 0) {
        version = searchDM(mp, groupId, artifactId);
      }
      if(version == null && (strategy & EXTRACT_STRATEGY_SEARCH) != 0) {
        Collection<String> versions = getSearchEngine(project).findVersions(groupId, artifactId, "", pack); //$NON-NLS-1$
        if(versions.isEmpty()) {
          return null;
        }
        version = versions.iterator().next();
      }
    }
    return version;
  }

  // TODO copy of this resides in FormUtils
  // TODO: This was previously just "static" not public static
  public static String simpleInterpolate(MavenProject project, String text) {
    if(text != null && text.contains("${")) { //$NON-NLS-1$
      //when expression is in the version but no project instance around
      // just give up.
      if(project == null) {
        return null;
      }
      Properties props = project.getProperties();
      RegexBasedInterpolator inter = new RegexBasedInterpolator();
      if(props != null) {
        inter.addValueSource(new PropertiesBasedValueSource(props));
      }
      inter.addValueSource(new PrefixedObjectValueSource(Arrays.asList("pom.", "project."), project.getModel(), false)); //$NON-NLS-1$ //$NON-NLS-2$
      try {
        text = inter.interpolate(text);
      } catch(InterpolationException e) {
        text = null;
      }
    }
    return text;
  }

  static String searchPM(MavenProject project, String groupId, String artifactId) {
    if(project == null) {
      return null;
    }
    String version = null;
    String id = Plugin.constructKey(groupId, artifactId);
    PluginManagement pm = project.getPluginManagement();
    if(pm != null) {
      for(Plugin pl : pm.getPlugins()) {
        if(id.equals(pl.getKey())) {
          version = pl.getVersion();
          break;
        }
      }
    }
    return version;
  }

  static String searchDM(MavenProject project, String groupId, String artifactId) {
    if(project == null) {
      return null;
    }
    String version = null;
    //see if we can find the dependency is in dependency management of resolved project.
    String id = groupId + ":" + artifactId + ":";
    DependencyManagement dm = project.getDependencyManagement();
    if(dm != null) {
      for(Dependency dep : dm.getDependencies()) {
        if(dep.getManagementKey().startsWith(id)) {
          version = dep.getVersion();
          break;
        }
      }
    }
    return version;
  }

  protected static String getArtifactId(Node currentNode) {
    return getSiblingTextValue(currentNode, "artifactId"); //$NON-NLS-1$
  }

  protected static String getVersion(Node currentNode) {
    return getSiblingTextValue(currentNode, "version"); //$NON-NLS-1$
  }

  private static String getSiblingTextValue(Node sibling, String name) {
    Node node = getSiblingWithName(sibling, name);
    return XmlUtils.getTextValue(node);
  }

  /**
   * Returns sibling with given name.
   */
  private static Node getSiblingWithName(Node node, String name) {
    return getChildWithName(node.getParentNode(), name);
  }

  /**
   * Returns child with given name
   */
  protected static Node getChildWithName(Node node, String name) {
    NodeList nodeList = node.getChildNodes();
    for(int i = 0; i < nodeList.getLength(); i++ ) {
      if(name.equals(nodeList.item(i).getNodeName())) {
        return nodeList.item(i);
      }
    }
    return null;
  }

  // TODO: This was protected previously
  public static Node getAncestor(Node node, String... names) {
    int i = 0;
    for(; i < names.length; i++ ) {
      Node parent = node.getParentNode();
      if(parent == null || !names[i].equals(parent.getNodeName()))
        return null;
      node = parent;
    }
    return i == names.length ? node : null;
  }

  private static class FileProposalContext {
    final File projectDir;

    final File parentDir;

    final String prefixPath;

    final String prefix;

    FileProposalContext(File projectDir, File parentDir, String prefixPath, String prefix) {
      this.projectDir = projectDir;
      this.parentDir = parentDir;
      this.prefixPath = prefixPath;
      this.prefix = prefix;
    }

  }
}
