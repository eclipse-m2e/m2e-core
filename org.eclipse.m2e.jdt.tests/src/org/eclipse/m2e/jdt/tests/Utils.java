package org.eclipse.m2e.jdt.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.m2e.jdt.internal.ClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.internal.MavenClasspathHelpers;

public class Utils {

  public static String read(IProject project, File fileFS) throws IOException, CoreException {
    IFile pomFileWS = project.getFile(fileFS.getName());
    byte[] bytes = new byte[(int) fileFS.length()];
    try (InputStream stream = pomFileWS.getContents()) {
      stream.read(bytes);
    }
    return new String(bytes);
  }
  
  public static Map<String, String> getJreContainerAttributes(IJavaProject javaProject) {
    IClasspathEntry jreEntry = MavenClasspathHelpers.getJREContainerEntry(javaProject);
    IClasspathEntryDescriptor jreEntryDescriptor = new ClasspathEntryDescriptor(jreEntry);
    return jreEntryDescriptor.getClasspathAttributes();
  }
  
  public static Map<String, String> getM2eContainerAttributes(IJavaProject javaProject) {
    IClasspathEntry m2eEntry = BuildPathManager.getMavenContainerEntry(javaProject);
    IClasspathEntryDescriptor m2eEntryDescriptor = new ClasspathEntryDescriptor(m2eEntry);
    return m2eEntryDescriptor.getClasspathAttributes();
  }
	  
}
