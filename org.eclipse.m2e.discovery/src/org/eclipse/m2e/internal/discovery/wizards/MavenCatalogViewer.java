/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.statushandlers.StatusManager;

import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.internal.discovery.DiscoveryActivator;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.Messages;


@SuppressWarnings("restriction")
public class MavenCatalogViewer extends CatalogViewer {
  public static final Logger log = LoggerFactory.getLogger(MavenCatalogViewer.class);

  private Set<String> installedFeatures;

  private boolean noneApplicable;

  /*
   * Outside of tests the shellProvider should generally be a WizardPage which allows setting the header.
   */
  public MavenCatalogViewer(Catalog catalog, IShellProvider shellProvider, IRunnableContext context,
      CatalogConfiguration configuration) {
    super(catalog, shellProvider, context, configuration);
  }

  protected void postDiscovery(IProgressMonitor monitor) {
    final SubMonitor subMon = SubMonitor.convert(monitor, getCatalog().getItems().size() * 3);
    try {
      for(CatalogItem connector : getCatalog().getItems()) {
        connector.setInstalled(installedFeatures != null
            && installedFeatures.containsAll(connector.getInstallableUnits()));
        subMon.worked(1);
      }

      if(getCatalog().getItems().size() == installedFeatures.size()) {
        handleStatus(new Status(IStatus.ERROR, DiscoveryActivator.PLUGIN_ID, Messages.MavenCatalogViewer_allInstalled));
      } else {
        final MavenCatalogConfiguration config = (MavenCatalogConfiguration) getConfiguration();
        final Collection<String> selectedPackagingTypes = config.getSelectedPackagingTypes();
        final Collection<MojoExecutionKey> selectedMojos = config.getSelectedMojos();
        final Collection<String> selectedLifecycleIds = config.getSelectedLifecycleIds();
        final Collection<String> selectedConfiguratorIds = config.getSelectedConfiguratorIds();

        if(!selectedConfiguratorIds.isEmpty() || !selectedLifecycleIds.isEmpty() || !selectedMojos.isEmpty()
            || !selectedPackagingTypes.isEmpty()) {
          noneApplicable = true;
          shellProvider.getShell().getDisplay().syncExec(new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run() {
              for(CatalogItem ci : getCatalog().getItems()) {
                boolean selected = false;
                subMon.worked(2);

                LifecycleMappingMetadataSource src = MavenDiscovery.getLifecycleMappingMetadataSource(ci);
                if(src != null) {
                  for(String packagingType : selectedPackagingTypes) {
                    if(hasPackaging(src, packagingType)) {
                      selected = true;
                      select(ci);
                      break;
                    }
                  }
                  if(selected) {
                    continue;
                  }
                  for(MojoExecutionKey mojoExecution : selectedMojos) {
                    if(matchesFilter(src, mojoExecution)) {
                      selected = true;
                      select(ci);
                      break;
                    }
                  }
                  if(selected) {
                    continue;
                  }
                }

                List<String> projectConfigurators = new ArrayList<String>();
                List<String> mappingStrategies = new ArrayList<String>();
                MavenDiscovery.getProvidedProjectConfigurators(ci, projectConfigurators, mappingStrategies);

                for(String configuratorId : selectedConfiguratorIds) {
                  if(projectConfigurators.contains(configuratorId)) {
                    selected = true;
                    select(ci);
                    break;
                  }
                }
                if(selected) {
                  continue;
                }

                for(String lifecycleId : selectedLifecycleIds) {
                  if(mappingStrategies.contains(lifecycleId)) {
                    select(ci);
                    break;
                  }
                }
              }
              if(noneApplicable) {
                handleStatus(new Status(IStatus.ERROR, DiscoveryActivator.PLUGIN_ID,
                    Messages.MavenCatalogViewer_noApplicableMarketplaceItems));
              }
            }
          });
        }
      }
    } finally {
      subMon.done();
    }
  }

  @Override
  public void updateCatalog() {
    boolean wasCancelled = false;
    boolean wasError = false;
    final IStatus[] result = new IStatus[1];
    try {
      context.run(true, true, new IRunnableWithProgress() {
        @SuppressWarnings("synthetic-access")
        public void run(IProgressMonitor monitor) throws InterruptedException {
          SubMonitor submon = SubMonitor.convert(monitor, 100);
          try {
            if(installedFeatures == null) {
              installedFeatures = getInstalledFeatures(submon.newChild(10));
            }
            result[0] = getCatalog().performDiscovery(submon.newChild(80));
            if(monitor.isCanceled()) {
              throw new InterruptedException();
            }
            if(!getCatalog().getItems().isEmpty()) {
              postDiscovery(submon.newChild(10));
            }
          } finally {
            submon.done();
          }
        }
      });
    } catch(InvocationTargetException e) {
      result[0] = computeStatus(e, Messages.MavenCatalogViewer_unexpectedException);
    } catch(InterruptedException e) {
      // cancelled by user so nothing to do here.
      wasCancelled = true;
    }
    if(result[0] != null && !result[0].isOK()) {
      handleStatus(result[0]);
      wasError = true;
    }
    if(getCatalog() != null) {
      catalogUpdated(wasCancelled, wasError);
      verifyUpdateSiteAvailability();
    }
    // help UI tests
    viewer.setData("discoveryComplete", Boolean.TRUE); //$NON-NLS-1$
  }

  private void select(CatalogItem ci) {
    modifySelection(ci, true);
    ci.addTag(MavenDiscovery.APPLICABLE_TAG);
    noneApplicable = false;
  }

  private void handleStatus(final IStatus status) {
    if(status.isOK()) {
      return;
    }

    if(shellProvider instanceof WizardPage) {
      shellProvider.getShell().getDisplay().asyncExec(new Runnable() {
        public void run() {
          // Display the error in the wizard header
          int messageType = IMessageProvider.INFORMATION;
          if(status.matches(IStatus.ERROR)) {
            messageType = IMessageProvider.ERROR;
          } else if(status.matches(IStatus.WARNING)) {
            messageType = IMessageProvider.WARNING;
          }
          ((WizardPage) shellProvider).setMessage(status.getMessage(), messageType);
          StatusManager.getManager().handle(status);
        }
      });
    } else {
      StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
    }
  }

  private static boolean matchesFilter(LifecycleMappingMetadataSource src, MojoExecutionKey mojoExecution) {
    for(PluginExecutionMetadata p : src.getPluginExecutions()) {
      if(p.getFilter().match(mojoExecution)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasPackaging(LifecycleMappingMetadataSource lifecycleMappingMetadataSource,
      String packagingType) {
    for(LifecycleMappingMetadata lifecycleMappingMetadata : lifecycleMappingMetadataSource.getLifecycleMappings()) {
      if(packagingType.equals(lifecycleMappingMetadata.getPackagingType())) {
        return true;
      }
    }
    return false;
  }
}
