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

package org.eclipse.m2e.editor.xml;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.ui.views.contentoutline.XMLContentOutlineConfiguration;


/**
 * @author Eugene Kuleshov
 */
public class PomContentOutlineConfiguration extends XMLContentOutlineConfiguration {

  public ILabelProvider getLabelProvider(TreeViewer viewer) {
    return new PomLabelProvider(super.getLabelProvider(viewer));
  }

  /**
   * POM label provider
   */
  private final class PomLabelProvider implements ILabelProvider {
    
    private static final String TARGET_PATH = "targetPath"; //$NON-NLS-1$

    private static final String DIRECTORY = "directory"; //$NON-NLS-1$

    private static final String REPORT_SET = "reportSet"; //$NON-NLS-1$

    private static final String PROPERTIES = "properties"; //$NON-NLS-1$

    private static final String REPORTING = "reporting"; //$NON-NLS-1$

    private static final String BUILD = "build"; //$NON-NLS-1$

    private static final String EXCLUDE = "exclude"; //$NON-NLS-1$

    private static final String INCLUDE = "include"; //$NON-NLS-1$

    private static final String FILTER = "filter"; //$NON-NLS-1$

    private static final String TEST_RESOURCE = "testResource"; //$NON-NLS-1$

    private static final String RESOURCE = "resource"; //$NON-NLS-1$

    private static final String TEST_RESOURCES = "testResources"; //$NON-NLS-1$

    private static final String RESOURCES = "resources"; //$NON-NLS-1$

    private static final String GOAL = "goal"; //$NON-NLS-1$

    private static final String EXECUTION = "execution"; //$NON-NLS-1$

    private static final String PLUGIN = "plugin"; //$NON-NLS-1$

    private static final String PLUGINS = "plugins"; //$NON-NLS-1$

    private static final String SNAPSHOT_REPOSITORY = "snapshotRepository"; //$NON-NLS-1$

    private static final String PLUGIN_REPOSITORY = "pluginRepository"; //$NON-NLS-1$

    private static final String REPOSITORY = "repository"; //$NON-NLS-1$

    private static final String SITE = "site"; //$NON-NLS-1$

    private static final String CONTRIBUTOR = "contributor"; //$NON-NLS-1$

    private static final String DEVELOPER = "developer"; //$NON-NLS-1$

    private static final String PROFILE = "profile"; //$NON-NLS-1$

    private static final String PROFILES = "profiles"; //$NON-NLS-1$

    private static final String MODULE = "module"; //$NON-NLS-1$

    private static final String EXTENSION = "extension"; //$NON-NLS-1$

    private static final String EXCLUSION = "exclusion"; //$NON-NLS-1$

    private static final String MODULES = "modules"; //$NON-NLS-1$

    private static final String EXTENSIONS = "extensions"; //$NON-NLS-1$

    private static final String EXCLUSIONS = "exclusions"; //$NON-NLS-1$

    private static final String DEPENDENCIES = "dependencies"; //$NON-NLS-1$

    private static final String PARENT = "parent"; //$NON-NLS-1$

    private static final String SCOPE = "scope"; //$NON-NLS-1$

    private static final String TYPE = "type"; //$NON-NLS-1$

    private static final String CLASSIFIER = "classifier"; //$NON-NLS-1$

    private static final String DEPENDENCY = "dependency"; //$NON-NLS-1$

    private static final String ID = "id"; //$NON-NLS-1$

    private static final String EMAIL = "email"; //$NON-NLS-1$

    private static final String NAME = "name"; //$NON-NLS-1$

    private static final String VERSION = "version"; //$NON-NLS-1$

    private static final String GROUP_ID = "groupId"; //$NON-NLS-1$

    private static final String ARTIFACT_ID = "artifactId"; //$NON-NLS-1$

    private static final String NAMESPACE_POM = "http://maven.apache.org/POM/4.0.0"; //$NON-NLS-1$

    private static final int MAX_LABEL_LENGTH = 120;
    
    private final ILabelProvider labelProvider;
  
    private PomLabelProvider(ILabelProvider labelProvider) {
      this.labelProvider = labelProvider;
    }
  
    public Image getImage(Object element) {
      Node node = (Node) element;
      String namespace = node.getNamespaceURI();
      String nodeName = node.getNodeName();
      
      if(node.getNodeType()==Node.COMMENT_NODE) {
        return labelProvider.getImage(element);
      }
      
      if(NAMESPACE_POM.equals(namespace)) {
        if(PARENT.equals(nodeName)) {
          return MvnImages.IMG_JAR;
        
        } else if(DEPENDENCIES.equals(nodeName) //
            || EXCLUSIONS.equals(nodeName) //
            || EXTENSIONS.equals(nodeName) //
            || MODULES.equals(nodeName)) {
          return MvnImages.IMG_JARS;
          
        } else if(DEPENDENCY.equals(nodeName) //
            || EXCLUSION.equals(nodeName) //
            || EXTENSION.equals(nodeName) //
            || MODULE.equals(nodeName)) {
          // TODO show folder if module is in the workspace
          return MvnImages.IMG_JAR;
        
        } else if(REPOSITORY.equals(nodeName) || PLUGIN_REPOSITORY.equals(nodeName)
            || SNAPSHOT_REPOSITORY.equals(nodeName) || SITE.equals(nodeName)) {
          return MvnImages.IMG_REPOSITORY;
          
        } else if(PROFILES.equals(nodeName)) {
          return MvnImages.IMG_PROFILES;
          
        } else if(PROFILE.equals(nodeName)) {
          return MvnImages.IMG_PROFILE;
          
        } else if(DEVELOPER.equals(nodeName) || CONTRIBUTOR.equals(nodeName)) {
          return MvnImages.IMG_PERSON;
          
        } else if(PLUGINS.equals(nodeName)) {
          return MvnImages.IMG_PLUGINS;
          
        } else if(PLUGIN.equals(nodeName)) {
          return MvnImages.IMG_PLUGIN;
        
        } else if(EXECUTION.equals(nodeName)) {
          return MvnImages.IMG_EXECUTION;
          
        } else if(GOAL.equals(nodeName)) {
          return MvnImages.IMG_GOAL;
          
        } else if(RESOURCES.equals(nodeName) //
            || TEST_RESOURCES.equals(nodeName)) {
          return MvnImages.IMG_RESOURCES;
          
        } else if(RESOURCE.equals(nodeName) //
            || TEST_RESOURCE.equals(nodeName)) {
          return MvnImages.IMG_RESOURCE;
          
        } else if(FILTER.equals(nodeName)) {
          return MvnImages.IMG_FILTER;
          
        } else if(INCLUDE.equals(nodeName)) {
          return MvnImages.IMG_INCLUDE;
          
        } else if(EXCLUDE.equals(nodeName)) {
          return MvnImages.IMG_EXCLUDE;
          
        } else if(BUILD.equals(nodeName)) {
          return MvnImages.IMG_BUILD;
          
        } else if(REPORTING.equals(nodeName)) {
          return MvnImages.IMG_REPORT;
          
        } else if(PROPERTIES.equals(nodeName)) {
          return MvnImages.IMG_PROPERTIES;
          
        } else if(PROPERTIES.equals(node.getParentNode().getNodeName())) {
          return MvnImages.IMG_PROPERTY;

        // } else if("mailingList".equals(nodeName)) {
        //   return MvnImages.IMG_MAIL;
        
        }
        
        return MvnImages.IMG_ELEMENT;
      }
      
      return labelProvider.getImage(element);
    }
  
    public String getText(Object element) {
      String text = labelProvider.getText(element);
  
      Node node = (Node) element;
      String namespace = node.getNamespaceURI();
      String nodeName = node.getNodeName();
      
      if(node.getNodeType()==Node.COMMENT_NODE) {
        return cleanText(node);
      }
      
      if(NAMESPACE_POM.equals(namespace)) {
        if(PARENT.equals(nodeName)) {
          return getLabel(text, node, GROUP_ID, ARTIFACT_ID, VERSION);
        
        } else if(DEPENDENCY.equals(nodeName)) {
          return getLabel(text, node, GROUP_ID, ARTIFACT_ID, VERSION, CLASSIFIER, TYPE, SCOPE);
        
        } else if(EXCLUSION.equals(nodeName)) {
          return getLabel(text, node, GROUP_ID, ARTIFACT_ID);
        
        } else if(EXTENSION.equals(nodeName)) {
          return getLabel(text, node, GROUP_ID, ARTIFACT_ID, VERSION);
          
        } else if(REPOSITORY.equals(nodeName) || PLUGIN_REPOSITORY.equals(nodeName)
            || SNAPSHOT_REPOSITORY.equals(nodeName) || SITE.equals(nodeName) || PROFILE.equals(nodeName)
            || EXECUTION.equals(nodeName)) {
          return getLabel(text, node, ID);
          
        } else if("mailingList".equals(nodeName)) { //$NON-NLS-1$
          return getLabel(text, node, NAME);
          
        } else if(DEVELOPER.equals(nodeName)) {
          return getLabel(text, node, ID, NAME, EMAIL);
          
        } else if(CONTRIBUTOR.equals(nodeName)) {
          return getLabel(text, node, NAME, EMAIL);
          
        } else if(PLUGIN.equals(nodeName)) {
          return getLabel(text, node, GROUP_ID, ARTIFACT_ID, VERSION);
        
        } else if(RESOURCE.equals(nodeName) || TEST_RESOURCE.equals(nodeName)) {
          return getLabel(text, node, DIRECTORY, TARGET_PATH);
          
        } else if(REPORT_SET.equals(nodeName)) {
          return getLabel(text, node, ID);
          
        } else if(EXECUTION.equals(nodeName)) {
          return getLabel(text, node, ID);
          
        }
        
        NodeList childNodes = node.getChildNodes();
        if(childNodes.getLength()==1) {
          Node item = childNodes.item(0);
          short nodeType = item.getNodeType();
          if(nodeType==Node.TEXT_NODE || nodeType==Node.COMMENT_NODE) {
            String nodeText = item.getNodeValue();
            if(nodeText.length()>0) {
              return text + "  " + cleanText(item); //$NON-NLS-1$
            }
          }
        }
      }
      
      return text;
    }
  
    public boolean isLabelProperty(Object element, String name) {
      return labelProvider.isLabelProperty(element, name);
    }
  
    public void addListener(ILabelProviderListener listener) {
      labelProvider.addListener(listener);
    }
  
    public void removeListener(ILabelProviderListener listener) {
      labelProvider.removeListener(listener);
    }
  
    public void dispose() {
      labelProvider.dispose();
    }
  
    private String getLabel(String text, Node node, String... names) {
      StringBuilder sb = new StringBuilder(text).append("  "); //$NON-NLS-1$
      String sep = ""; //$NON-NLS-1$
      for(String name : names) {
        String value = getValue(node, name);
        if(value!=null) {
          sb.append(sep).append(value);
          sep = " : "; //$NON-NLS-1$
        }
      }
      
      return sb.toString();
    }
  
    private String getValue(Node node, String name) {
      NodeList childNodes = node.getChildNodes();
      for(int i = 0; i < childNodes.getLength(); i++ ) {
        Node item = childNodes.item(i);
        if(item.getNodeType()==Node.ELEMENT_NODE && name.equals(item.getNodeName())) {
          NodeList nodes = item.getChildNodes();
          if(nodes.getLength()==1) {
            String value = nodes.item(0).getNodeValue().trim();
            if(value.length()>0) {
              return value;
            }
          }
          return null;
        }
      }
      return null;
    }
  
    private String cleanText(Node node) {
      String value = node.getNodeValue();
      if (value==null) {
        return ""; //$NON-NLS-1$
      }
      
      value = value.replaceAll("\\s", " ").replaceAll("(\\s){2,}", " ").trim(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      if (value.length() > MAX_LABEL_LENGTH) {
        value = value.substring(0, 120) + Dialog.ELLIPSIS;
      }
      
      return value;
    }
  }

}

