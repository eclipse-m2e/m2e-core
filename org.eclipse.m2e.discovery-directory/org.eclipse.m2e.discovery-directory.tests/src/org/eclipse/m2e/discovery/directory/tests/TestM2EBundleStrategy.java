
package org.eclipse.m2e.discovery.directory.tests;

import org.eclipse.m2e.internal.discovery.strategy.M2ERemoteBundleDiscoveryStrategy;


@SuppressWarnings("restriction")
public class TestM2EBundleStrategy extends M2ERemoteBundleDiscoveryStrategy {
  private static String DIRECTORY_XML_URL = "file:directory.xml";

  public TestM2EBundleStrategy() {
    super.setDirectoryUrl(DIRECTORY_XML_URL);
  }

  @Override
  public void setDirectoryUrl(String directoryUrl) {
    super.setDirectoryUrl(DIRECTORY_XML_URL);
  }

  // @Override
  // public void performDiscovery( IProgressMonitor monitor )
  // throws CoreException
  // {
  // if ( items == null || categories == null )
  // {
  // throw new IllegalStateException();
  // }
  // IExtensionPoint extensionPoint =
  // getExtensionRegistry().getExtensionPoint( ConnectorDiscoveryExtensionReader.EXTENSION_POINT_ID );
  // IExtension[] extensions = extensionPoint.getExtensions();
  // monitor.beginTask( "Loading local extensions", extensions.length == 0 ? 1 : extensions.length );
  // try
  // {
  // if ( extensions.length > 0 )
  // {
  // processExtensions( new SubProgressMonitor( monitor, extensions.length ), extensions );
  // }
  // }
  // finally
  // {
  // monitor.done();
  // }
  // }
  //
  // @Override
  // protected AbstractCatalogSource computeDiscoverySource(IContributor contributor) {
  // Policy policy = new Policy(true);
  // BundleDiscoverySource bundleDiscoverySource = new
  // BundleDiscoverySource(Platform.getBundle(contributor.getName()));
  // bundleDiscoverySource.setPolicy(policy);
  // return bundleDiscoverySource;
  // }
}
