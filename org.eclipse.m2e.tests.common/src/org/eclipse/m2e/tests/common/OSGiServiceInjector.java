
package org.eclipse.m2e.tests.common;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;


public class OSGiServiceInjector implements MethodRule {
  @SuppressWarnings("unused") // ensure the felix.scr participates in the launch to enable OSGi DS
  private static org.apache.felix.scr.info.ScrInfo info;

  public static final OSGiServiceInjector INSTANCE = new OSGiServiceInjector();

  private OSGiServiceInjector() {
  }

  public @interface InjectService {
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        Bundle bundle = FrameworkUtil.getBundle(target.getClass());
        BundleContext context = bundle.getBundleContext();
        if(context == null) {
          throw new IllegalStateException("Test bundle <" + bundle.getSymbolicName() + "> not started");
        }
        // TODO: BundleContextOSGi ungets services immediately!
        // 'Manually' search for fields to inject and set them via reflection 
        // and use a map of service trackers like in the m2e.core activator?
        // Close the trackers after evaluation
        IEclipseContext serviceContext = EclipseContextFactory.getServiceContext(context);
        ContextInjectionFactory.inject(target, serviceContext);

        base.evaluate();
      }
    };
  }
}
