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

package org.eclipse.m2e.editor.xml.internal;

import java.io.File;
import java.util.List;
import java.util.Stack;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.project.MavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectCache;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
/**
 * 
 * @author mkleint
 */
public class XmlUtils {

  public static Element findChildElement(Element parent, String name) {
    return MavenMarkerManager.findChildElement(parent, name);
  }

  public static List<Element> findChildElements(Element parent, String name) {
    return MavenMarkerManager.findChildElements(parent, name);
  }

  public static String getElementTextValue(Node element) {
    return MavenMarkerManager.getElementTextValue(element);
  }

  /**
   * what is this method supposed to do? for the sourceViewer find the associated file on disk and for
   * that one find the IProject it belongs to. The required condition for the IProject instance is that
   * project relative path of the file shall only be pom.xml (thus no nested, unopened maven pom). 
   * So that when MavenPlugin.getDefault().getMavenProjectManager().getProject(prj); is called later on
   * the instance, it actually returns the maven model facade for the pom.xml backing the sourceViewer.
   * @param sourceViewer
   * @return
   */
  public static IProject extractProject(ITextViewer sourceViewer) {
    ITextFileBuffer buf = FileBuffers.getTextFileBufferManager().getTextFileBuffer(sourceViewer.getDocument());
    if (buf == null) {
      //eg. for viewers of pom files in local repository
      return null;
    }
    IFileStore folder = buf.getFileStore();
    File file = new File(folder.toURI());
    IPath path = Path.fromOSString(file.getAbsolutePath());
    Stack<IResource> stack = new Stack<IResource>();
    //here we need to find the most inner project to the path.
    //we do so by shortening the path and remembering all the resources identified.
    // at the end we pick the last one from the stack. is there a catch to it?
    IResource ifile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
    if (ifile != null) {
      stack.push(ifile);
    } else {
      while(path.segmentCount() > 1) {
        ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        if(ifile != null) {
          stack.push(ifile);
        }
        path = path.removeFirstSegments(1);
      }
    }
    IResource res = stack.empty() ? null : stack.pop();
    if (res != null) {
      IProject prj = res.getProject();
    //the project returned is in a way unrelated to nested child poms that don't have an opened project,
    //in that case we pass along a wrong parent/aggregator
      if (res.getProjectRelativePath().segmentCount() != 1) { 
        //if the project were the pom's project, the relative path would be just "pom.xml", if it's not just throw it out of the window..
        prj = null;
      }
      return prj;
    }
    return null;
  }
  
  public static MavenProject extractMavenProject(ITextViewer sourceViewer) {
    //TODO we might want to eventually reduce our dependency on IProject
    
    //first try checking for latest mavenproject in the facade
    //TODO is there a reliable way to check for other model's updates?
    IProject prj = extractProject(sourceViewer);
    MavenProject mp = extractMavenProject(prj);
    if (mp == null) {
      //if not found, look in the sourceViewer's cache
      if (sourceViewer instanceof IMavenProjectCache) {
        mp = ((IMavenProjectCache)sourceViewer).getMavenProject();
      }
    } else {
      //if found, update the sourceViewer's cache
      if (sourceViewer instanceof IMavenProjectCache) {
        ((IMavenProjectCache)sourceViewer).setMavenProject(mp);
      }
    }
    return mp;
  }
  /**
   * you are encouraged to use the extractMavenProject(ITextViewer) method instead
   * @param project
   * @return
   */
  public static MavenProject extractMavenProject(IProject project) {
    //TODO we might want to eventually reduce our dependency on IProject
    if (project != null) {
      IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getProject(project);
      if (facade != null) {
        return facade.getMavenProject();
      }
    }
    return null;
  }

  /**
   * converts an InputLocation to a file path on the local disk, null if not available.
   * still the input source's model value can be used further..
   * @param location
   * @return
   */
  public static  File fileForInputLocation(InputLocation location) {
    InputSource source = location.getSource();
    if (source != null) {
      //MNGECLIPSE-2539 apparently if maven can't resolve the model from local storage,
      //the location will be empty. not only applicable to local repo models but
      //apparently also to models in workspace not reachable by relativePath 
      String loc = source.getLocation();
      File file = null;
      if (loc != null) {
        file = new File(loc);
      } else {
        //try to find pom by coordinates..
        String modelId = source.getModelId();
        String[] splitStrings = modelId.split(":");
        assert splitStrings.length == 3;
        IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject(splitStrings[0], splitStrings[1], splitStrings[2]);
        if (facade != null) {
          file = facade.getPomFile();
        } else {
          //if not in the workspace, try looking into the local repository.
          IMaven maven = MavenPlugin.getDefault().getMaven();
          try {
            String path = maven.getArtifactPath(maven.getLocalRepository(), splitStrings[0], splitStrings[1], splitStrings[2], "pom", null);
            if (path != null) {
              file = new File(maven.getLocalRepositoryPath(), path);
            }
          } catch(CoreException e) {
            MavenLogger.log("Failed to calculate local repository path of artifact", e);
          }
        }
      }
      return file;
    }
    return null;
  }

  /**
   * originally copied from org.eclipse.wst.xml.ui.internal.hyperlink.XMLHyperlinkDetector

   * this method grabs the IDOMModel for the IDocument, performs the passed operation on the node at the offset
   * and then releases the IDOMModel 
   * 
   * operation's Node value is also an instance of IndexedRegion
   * 
   * @param offset
   */
  public static void performOnCurrentElement(IDocument document, int offset, NodeOperation<Node> operation) {
    assert document != null;
    assert operation != null;
    // get the current node at the offset (returns either: element,
    // doctype, text)
    IStructuredModel sModel = null;
    try {
      sModel = StructuredModelManager.getModelManager().getExistingModelForRead(document);
      if (sModel != null) {
        IndexedRegion inode = sModel.getIndexedRegion(offset);
        if (inode == null) {
          inode = sModel.getIndexedRegion(offset - 1);
        }
        if (inode instanceof Node) {
          operation.process((Node) inode);
        } 
      }
    }
    finally {
      if (sModel != null) {
        sModel.releaseFromRead();
      }
    }
  }

  /**
   * this method grabs the IDOMModel for the IDocument, performs the passed operation on the root element of the document
   * and then releases the IDOMModel 
   * 
   * root Element value is also an instance of IndexedRegion
   * @param doc
   * @param operation
   */
  public static void performOnRootElement(IDocument doc, NodeOperation<Element> operation) {
    assert doc != null;
    assert operation != null;
    IDOMModel domModel = null;
    try {
      domModel = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(doc);
      IStructuredDocument document = domModel.getStructuredDocument();
      Element root = domModel.getDocument().getDocumentElement();
      operation.process(root);
    } finally {
      if (domModel != null) {
        domModel.releaseFromRead();
      }
    }
  }

  public static IStructuredDocument getDocument(IMarker marker) {
    if (marker.getResource().getType() == IResource.FILE)
    {
      IDOMModel domModel = null;
      try {
        domModel = (IDOMModel)StructuredModelManager.getModelManager().getModelForRead((IFile)marker.getResource());
        return domModel.getStructuredDocument();
      } catch(Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } finally {
        if (domModel != null) {
          domModel.releaseFromRead();
        }
      }
    }
    return null;
  }

  /*
   * calculates the path of the node up in the hierarchy, example of result is project/build/plugins/plugin
   * level parameter designates the number of parents to climb eg. for level 2 the result would be plugins/plugin
   * level -1 means all the way to the top. 
   */
  public static String pathUp(Node node, int level) {
    StringBuffer buf = new StringBuffer();
    int current = level;
    while (node != null && current > 0) {
      if (node instanceof Element) {
        if (buf.length() > 0) {
          buf.insert(0, "/");
        }
        buf.insert(0, node.getNodeName());
        current = current -1;
      }
      node = node.getParentNode();
    }
    return buf.toString();
  }

}
