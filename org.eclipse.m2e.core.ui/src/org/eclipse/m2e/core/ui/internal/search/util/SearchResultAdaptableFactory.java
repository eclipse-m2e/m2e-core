/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.ui.internal.search.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.eclipse.ui.statushandlers.StatusManager;

import org.eclipse.m2e.core.search.ISearchResultGA;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;


/**
 * SearchResultAdaptableFactory
 *
 * @author Matthew Piggott
 */
public class SearchResultAdaptableFactory implements IAdapterFactory {
  private static final Class<?>[] ADAPTER_LIST = new Class[] {IDeferredWorkbenchAdapter.class};

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
   */
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if(adaptableObject instanceof ISearchResultGA && IDeferredWorkbenchAdapter.class.equals(adapterType)) {
      return adapterType.cast(new DeferredSearchResultsGAVEC((ISearchResultGA) adaptableObject));
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
   */
  public Class<?>[] getAdapterList() {
    return ADAPTER_LIST;
  }

  private static class DeferredSearchResultsGAVEC implements IDeferredWorkbenchAdapter {

    private ISearchResultGA searchResultGA;

    DeferredSearchResultsGAVEC(ISearchResultGA searchResultGA) {
      this.searchResultGA = searchResultGA;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object o) {
      return fetchChildren(o, new NullProgressMonitor());
    }

    private Object[] fetchChildren(Object o, IProgressMonitor monitor) {
      try {
        return searchResultGA.getProvider().getArtifacts(monitor, searchResultGA).toArray();
      } catch(CoreException e) {
        StatusManager.getManager().handle(e, M2EUIPluginActivator.PLUGIN_ID);
      }
      return new Object[0];
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
     */
    public ImageDescriptor getImageDescriptor(Object object) {
      return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
     */
    public String getLabel(Object o) {
      return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
     */
    public Object getParent(Object o) {
      return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.progress.IDeferredWorkbenchAdapter#fetchDeferredChildren(java.lang.Object, org.eclipse.ui.progress.IElementCollector, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
      try {
        Object[] children = fetchChildren(object, monitor);
        if(!monitor.isCanceled()) {
          collector.add(children, monitor);
        }
      } catch(OperationCanceledException e) {
        // Nothing to do
      }
      collector.done();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.progress.IDeferredWorkbenchAdapter#isContainer()
     */
    public boolean isContainer() {
      return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.progress.IDeferredWorkbenchAdapter#getRule(java.lang.Object)
     */
    public ISchedulingRule getRule(Object object) {
      return null;
    }
  }

}
