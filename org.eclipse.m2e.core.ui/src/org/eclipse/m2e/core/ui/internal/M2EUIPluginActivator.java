
package org.eclipse.m2e.core.ui.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.console.MavenConsoleImpl;
import org.eclipse.m2e.core.ui.internal.search.util.IndexSearchEngine;
import org.eclipse.m2e.core.ui.internal.search.util.SearchEngine;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


public class M2EUIPluginActivator extends AbstractUIPlugin {
  
  public static final String PLUGIN_ID = "org.eclipse.m2e.core.ui";

  private static M2EUIPluginActivator instance;

  public M2EUIPluginActivator() {
    M2EUIPluginActivator.instance = this;
  }

  private MavenConsoleImpl console;

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
  }

  public static M2EUIPluginActivator getDefault() {
    return instance;
  }

  /**
   * Returns an Image for the file at the given relative path.
   */
  public static Image getImage(String path) {
    ImageRegistry registry = getDefault().getImageRegistry();
    Image image = registry.get(path);
    if(image == null) {
      registry.put(path, imageDescriptorFromPlugin(IMavenConstants.PLUGIN_ID, path));
      image = registry.get(path);
    }
    return image;
  }

  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(IMavenConstants.PLUGIN_ID, path);
  }

  public synchronized MavenConsoleImpl getMavenConsoleImpl() {
    if(console == null) {
      console = new MavenConsoleImpl(MavenImages.M2);
    }
    return console;
  }

  public boolean hasMavenConsoleImpl() {
    return console != null;
  }

  public SearchEngine getSearchEngine(IProject project) throws CoreException {
    return new IndexSearchEngine(MavenPlugin.getDefault().getIndexManager().getIndex(project));
  }
}
