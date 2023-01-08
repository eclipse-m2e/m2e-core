/*************************************************************************************
 * Copyright (c) 2011-2018 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fred Bricon / JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.profiles.ui.internal.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.jobs.MavenJob;
import org.eclipse.m2e.core.internal.jobs.MavenWorkspaceJob;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.profiles.core.internal.IProfileManager;
import org.eclipse.m2e.profiles.core.internal.ProfileData;
import org.eclipse.m2e.profiles.core.internal.ProfileState;
import org.eclipse.m2e.profiles.ui.internal.Messages;
import org.eclipse.m2e.profiles.ui.internal.dialog.ProfileSelection;
import org.eclipse.m2e.profiles.ui.internal.dialog.SelectProfilesDialog;


/**
 * Handles profile selection commands.
 *
 * @author Fred Bricon
 * @since 1.5
 */
public class ProfileSelectionHandler extends AbstractHandler {

  private static final Logger log = LoggerFactory.getLogger(ProfileSelectionHandler.class);

  @Inject
  private IProfileManager profileManager;

  public ProfileSelectionHandler() {
    IEclipseContext serviceContext = EclipseContextFactory.getServiceContext(ProfileSelectionHandler.class);
    ContextInjectionFactory.inject(this, serviceContext);
  }

  /**
   * Opens the Maven profile selection Dialog window.
   */
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
    IProject[] projects = getSelectedProjects(event);
    return execute(window.getShell(), projects);
  }

  private IProject[] getSelectedProjects(ExecutionEvent event) {
    ISelection selection = HandlerUtil.getCurrentSelection(event);
    @SuppressWarnings("restriction")
    IProject[] projects = org.eclipse.m2e.core.ui.internal.actions.SelectionUtil.getProjects(selection, false);
    if(projects.length == 0) {
      IEditorInput input = HandlerUtil.getActiveEditorInput(event);
      if(input instanceof IFileEditorInput fileInput) {
        projects = new IProject[] {fileInput.getFile().getProject()};
      }
    }
    return projects;
  }

  public IStatus execute(Shell shell, IProject... projects) {
    Set<IMavenProjectFacade> facades = getMavenProjects(projects);
    if(facades.isEmpty()) {
      display(shell, Messages.ProfileSelectionHandler_Select_some_maven_projects);
      return null;
    }
    GetProfilesJob getProfilesJob = new GetProfilesJob(facades, profileManager);
    getProfilesJob.addJobChangeListener(onProfilesFetched(getProfilesJob, facades, profileManager, shell));
    getProfilesJob.setUser(true);
    getProfilesJob.schedule();
    return Status.OK_STATUS;
  }

  private IJobChangeListener onProfilesFetched(final GetProfilesJob getProfilesJob,
      final Set<IMavenProjectFacade> facades, final IProfileManager profileManager, final Shell shell) {

    return new JobChangeAdapter() {

      @Override
      public void done(IJobChangeEvent event) {
        if(getProfilesJob.getResult().isOK()) {
          shell.getDisplay().syncExec(() -> {
            List<ProfileSelection> sharedProfiles = getProfilesJob.getSharedProfiles();
            Map<IMavenProjectFacade, List<ProfileData>> allProfiles = getProfilesJob.getAllProfiles();
            final SelectProfilesDialog dialog = new SelectProfilesDialog(shell, facades, sharedProfiles);
            if(dialog.open() == Window.OK) {
              Job job = new UpdateProfilesJob(allProfiles, sharedProfiles, profileManager, dialog);
              job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
              job.schedule();
            }
          });

        }
      }
    };
  }

  private void display(Shell shell, String message) {
    MessageDialog.openInformation(shell, Messages.SelectProfilesDialog_Select_Maven_profiles, message);
  }

  /**
   * Returns an IMavenProjectFacade from the selected IResource, or from the active editor
   *
   * @param event
   * @return the selected IMavenProjectFacade
   */
  @SuppressWarnings("restriction")
  private Set<IMavenProjectFacade> getMavenProjects(IProject[] projects) {
    if(projects == null || projects.length == 0) {
      return Collections.emptySet();
    }
    Set<IMavenProjectFacade> facades = new HashSet<>(projects.length);
    try {
      IProgressMonitor monitor = new NullProgressMonitor();
      for(IProject p : projects) {
        if(p != null && p.isAccessible() && p.hasNature(org.eclipse.m2e.core.internal.IMavenConstants.NATURE_ID)) {
          IFile pom = p.getFile(org.eclipse.m2e.core.internal.IMavenConstants.POM_FILE_NAME);
          IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(pom, true, monitor);
          facades.add(facade);
        }
      }
    } catch(CoreException e) {
      log.error("Unable to select Maven projects", e);
    }

    return facades;
  }

  class GetProfilesJob extends MavenJob {

    private final IProfileManager profileManager;

    private final Set<IMavenProjectFacade> facades;

    private Map<IMavenProjectFacade, List<ProfileData>> allProfiles;

    private List<ProfileSelection> sharedProfiles;

    private GetProfilesJob(final Set<IMavenProjectFacade> facades, IProfileManager profileManager) {
      super(Messages.ProfileSelectionHandler_Loading_maven_profiles);
      this.facades = facades;
      this.profileManager = profileManager;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        this.allProfiles = getAllProfiles(facades, profileManager);
        this.sharedProfiles = getSharedProfiles(allProfiles);
      } catch(CoreException e) {
        return Status.error(Messages.ProfileSelectionHandler_Unable_to_open_profile_dialog, e);
      }
      return Status.OK_STATUS;
    }

    private List<ProfileSelection> getSharedProfiles(Map<IMavenProjectFacade, List<ProfileData>> projectProfilesMap) {

      List<ProfileData> currentSelection = null;
      List<List<ProfileData>> projectProfiles = new ArrayList<>(projectProfilesMap.values());
      int smallestSize = Integer.MAX_VALUE;
      for(List<ProfileData> profiles : projectProfiles) {
        int size = profiles.size();
        if(size < smallestSize) {
          smallestSize = size;
          currentSelection = profiles;
        }
      }
      projectProfiles.remove(currentSelection);

      // Init the smallest profiles selection possible
      List<ProfileSelection> selection = new ArrayList<>();
      if(currentSelection != null) {
        for(ProfileData p : currentSelection) {
          ProfileSelection ps = new ProfileSelection();
          ps.setId(p.getId());
          ps.setActivationState(p.getActivationState());
          ps.setAutoActive(p.isAutoActive());
          ps.setSource(p.getSource());
          ps.setSelected(p.isUserSelected());
          selection.add(ps);
        }
      }

      if(!projectProfiles.isEmpty()) {
        // Restrict to the common profiles only
        Iterator<ProfileSelection> ite = selection.iterator();

        while(ite.hasNext()) {
          ProfileSelection p = ite.next();
          for(List<ProfileData> statuses : projectProfiles) {
            ProfileData s = hasProfile(p.getId(), statuses);
            if(s == null) {
              // remove any non-common profile selection
              ite.remove();
              break;
            }
            // reset non common settings
            if(p.getAutoActive() != null && !p.getAutoActive().equals(s.isAutoActive())) {
              p.setAutoActive(null);
            }
            if(p.getSource() != null && !p.getSource().equals(s.getSource())) {
              p.setSource(Messages.ProfileSelectionHandler_multiple_definitions);
            }
            if(p.getSelected() != null && !p.getSelected().equals(s.isUserSelected())) {
              p.setSelected(null);
            }
            if(p.getActivationState() != null && !p.getActivationState().equals(s.getActivationState())) {
              p.setActivationState(null);
              p.setAutoActive(null);
            }
          }
        }
      }

      return selection;
    }

    private ProfileData hasProfile(String id, List<ProfileData> statuses) {
      for(ProfileData p : statuses) {
        if(id.equals(p.getId())) {
          return p;
        }
      }
      return null;
    }

    private Map<IMavenProjectFacade, List<ProfileData>> getAllProfiles(final Set<IMavenProjectFacade> facades,
        final IProfileManager profileManager) throws CoreException {
      Map<IMavenProjectFacade, List<ProfileData>> allProfiles = new HashMap<>(facades.size());
      IProgressMonitor monitor = new NullProgressMonitor();
      for(IMavenProjectFacade facade : facades) {
        allProfiles.put(facade, profileManager.getProfileDatas(facade, monitor));
      }
      return allProfiles;
    }

    public List<ProfileSelection> getSharedProfiles() {
      return sharedProfiles;
    }

    public Map<IMavenProjectFacade, List<ProfileData>> getAllProfiles() {
      return allProfiles;
    }
  }

  class UpdateProfilesJob extends MavenWorkspaceJob {

    private final Map<IMavenProjectFacade, List<ProfileData>> allProfiles;

    private final List<ProfileSelection> sharedProfiles;

    private final IProfileManager profileManager;

    private final SelectProfilesDialog dialog;

    private UpdateProfilesJob(Map<IMavenProjectFacade, List<ProfileData>> allProfiles,
        List<ProfileSelection> sharedProfiles, IProfileManager profileManager, SelectProfilesDialog dialog) {
      super(Messages.ProfileManager_Updating_maven_profiles);
      this.allProfiles = allProfiles;
      this.sharedProfiles = sharedProfiles;
      this.profileManager = profileManager;
      this.dialog = dialog;
    }

    public IStatus runInWorkspace(IProgressMonitor monitor) {
      try {
        SubMonitor progress = SubMonitor.convert(monitor, Messages.ProfileManager_Updating_maven_profiles, 100);
        SubMonitor subProgress = SubMonitor.convert(progress.newChild(5), allProfiles.size() * 100);
        for(Map.Entry<IMavenProjectFacade, List<ProfileData>> entry : allProfiles.entrySet()) {
          if(progress.isCanceled()) {
            throw new OperationCanceledException();
          }
          IMavenProjectFacade facade = entry.getKey();
          List<String> activeProfiles = getActiveProfiles(sharedProfiles, entry.getValue());

          profileManager.updateActiveProfiles(facade, activeProfiles, dialog.isOffline(), dialog.isForceUpdate(),
              subProgress.newChild(100));
        }
      } catch(CoreException ex) {
        log.error("Unable to update Maven profiles", ex);
        return ex.getStatus();
      }
      return Status.OK_STATUS;
    }

    private List<String> getActiveProfiles(List<ProfileSelection> sharedProfiles, List<ProfileData> availableProfiles) {
      List<String> ids = new ArrayList<>();

      for(ProfileData st : availableProfiles) {
        ProfileSelection selection = findSelectedProfile(st.getId(), sharedProfiles);
        String id = null;
        boolean isDisabled = false;
        if(selection == null) {
          // was not displayed. Use existing value.
          if(st.isUserSelected()) {
            id = st.getId();
            isDisabled = ProfileState.Disabled.equals(st.getActivationState());
          }
        } else {
          if(null == selection.getSelected()) {
            // Value was displayed but its state is unknown, use
            // previous state
            if(st.isUserSelected()) {
              id = st.getId();
              isDisabled = ProfileState.Disabled.equals(st.getActivationState());
            }
          } else {
            // Value was displayed and is consistent
            if(Boolean.TRUE.equals(selection.getSelected())) {
              id = selection.getId();
              isDisabled = ProfileState.Disabled.equals(selection.getActivationState());
            }
          }
        }

        if(id != null) {
          if(isDisabled) {
            id = "!" + id; //$NON-NLS-1$
          }
          ids.add(id);
        }
      }
      return ids;
    }

    private ProfileSelection findSelectedProfile(String id, List<ProfileSelection> sharedProfiles) {
      for(ProfileSelection sel : sharedProfiles) {
        if(id.equals(sel.getId())) {
          return sel;
        }
      }
      return null;
    }
  }
}
