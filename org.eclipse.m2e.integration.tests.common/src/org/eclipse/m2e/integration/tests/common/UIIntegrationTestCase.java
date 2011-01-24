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

package org.eclipse.m2e.integration.tests.common;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withMnemonic;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.text.StringStartsWith;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.util.PrefUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;

import org.codehaus.plexus.util.IOUtil;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.internal.index.NexusIndexManager;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.integration.tests.common.matchers.ContainsMnemonic;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.tests.common.JobHelpers;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


/**
 * @author rseddon
 * @author Marvin Froeder
 */
@SuppressWarnings("restriction")
@RunWith(SWTBotJunit4ClassRunner.class)
public abstract class UIIntegrationTestCase {
  private static final String DEFAULT_PROJECT_GROUP = "org.sonatype.test";

  public static final String PACKAGE_EXPLORER_VIEW_ID = "org.eclipse.jdt.ui.PackageExplorer";

  protected static SonatypeSWTBot bot;

  protected static final IProgressMonitor monitor = new NullProgressMonitor();

  private static JobHelpers.IJobMatcher EDITOR_JOB_MATCHER = new JobHelpers.IJobMatcher() {
    public boolean matches(Job job) {
      // wait for the job from MavenPomEditor.doSave()
      return (job instanceof UIJob) && "Saving".equals(job.getName());
    }
  };

  @BeforeClass
  public final static void beforeClass() throws Exception {
    bot = new SonatypeSWTBot();

    SWTBotPreferences.KEYBOARD_LAYOUT = "EN_US";
    SWTBotPreferences.TIMEOUT = 10 * 1000;

    // close the Welcome view if it's open
    try {
      SWTBotView view = bot.activeView();
      if(view != null && "org.eclipse.ui.internal.introview".equals(view.getViewReference().getId())) {
        view.close();
      }
    } catch(WidgetNotFoundException e) {
      // no active view
    }

    SWTBotShell[] shells = bot.shells();
    for(SWTBotShell shell : shells) {
      final Shell widget = shell.widget;
      Object parent = UIThreadRunnable.syncExec(shell.display, new Result<Object>() {
        public Object run() {
          return widget.isDisposed() ? null : widget.getParent();
        }
      });

      if(parent == null) {
        continue;
      }

      shell.close();
    }

    List<? extends SWTBotEditor> editors = bot.editors();
    for(SWTBotEditor e : editors) {
      e.close();
    }

    // Clean out projects left over from previous test runs.
    clearProjects();

    // Turn off eclipse features which make tests unreliable.
    WorkbenchPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.RUN_IN_BACKGROUND, true);

    PrefUtil.getAPIPreferenceStore().setValue(IWorkbenchPreferenceConstants.ENABLE_ANIMATIONS, false);

    // fullScreen();
    MavenPlugin.getDefault(); // force m2e to load so its indexing jobs will
    // be scheduled.

    openPerspective("org.eclipse.jdt.ui.JavaPerspective");

    closeView("org.eclipse.ui.views.ContentOutline");
    closeView("org.eclipse.mylyn.tasks.ui.views.tasks");
  }

  @Before
  public final void waitMenu() {
    // The following seems to be needed to run SWTBot tests in Xvfb (the 'fake' X-server).
    // see http://dev.eclipse.org/mhonarc/newsLists/news.eclipse.swtbot/msg01134.html
    UIThreadRunnable.syncExec(new VoidResult() {
      public void run() {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().forceActive();
      }
    });

    bot.waitUntil(new DefaultCondition() {

      public boolean test() throws Exception {
        return bot.menu("File").isEnabled();
      }

      public String getFailureMessage() {
        return "Menu bar not available";
      }
    });
  }

  @AfterClass
  public final static void sleep() throws Exception {
    removeServer();
    clearProjects();
    takeScreenShot("cleared projects");
  }

  @After
  public final void finalShot() throws IOException {
    takeScreenShot(getClass().getSimpleName());
  }

  public static File takeScreenShot(String classifier) throws IOException {
    File parent = new File("target/screenshots");
    parent.mkdirs();
    File output = getCanonicalFile(File.createTempFile("swtbot-", "-" + classifier + ".png", parent));
    output.getParentFile().mkdirs();
    SWTUtils.captureScreenshot(output.getAbsolutePath());
    return output;
  }

  protected static File getCanonicalFile(File file) {
    try {
      return file.getCanonicalFile();
    } catch(IOException e) {
      return file.getAbsoluteFile();
    }
  }

  public static File takeScreenShot() throws IOException {
    return takeScreenShot("screen");
  }

  public static Exception takeScreenShot(Throwable e) throws Exception {
    File shot = takeScreenShot("exception");
    throw new Exception(e.getMessage() + " - " + shot, e);
  }

  protected void importZippedProject(File f) throws Exception {
    try {
      bot.menu("File").menu("Import...").click();
      SWTBotShell shell = bot.shell("Import");
      try {
        shell.activate();

        bot.tree().expandNode("General").select("Existing Projects into Workspace");
        bot.button("Next >").click();
        // bot.button("Select root directory:").click();
        bot.radio("Select archive file:").click();
        bot.text(1).setText(f.getCanonicalPath());

        bot.button("Refresh").click();
        bot.button("Finish").click();
      } catch(Throwable e) {
        takeScreenShot(e);
      } finally {
        SwtbotUtil.waitForClose(shell);
      }

      waitForAllBuildsToComplete();
    } finally {
      f.delete();
    }
  }

  protected static void waitForAllBuildsToComplete() {
    waitForAllEditorsToSave();
    JobHelpers.waitForJobsToComplete();
  }

  protected static void waitForAllLaunchesToComplete(int maxWaitMillis) {
    waitForAllEditorsToSave();
    JobHelpers.waitForLaunchesToComplete(maxWaitMillis);
  }

  protected static void waitForAllEditorsToSave() {
    JobHelpers.waitForJobs(EDITOR_JOB_MATCHER, 30 * 1000);
  }

  protected void createNewFolder(String projectName, String folderName) {
    // Add a new src folder with simple source file
    SWTBotTree tree = selectProject(projectName, true);

    ContextMenuHelper.clickContextMenu(tree, "New", "Folder");

    SWTBotShell shell = bot.shell("New Folder");
    try {
      shell.activate();

      bot.textWithLabel("Folder name:").setText(folderName);
      bot.button("Finish").click();

    } finally {
      SwtbotUtil.waitForClose(shell);
    }
  }

  protected File importMavenProjects(String pluginId, String projectPath) throws Exception {
    File tempDir = unzipProject(pluginId, projectPath);
    waitForAllBuildsToComplete();
    // try {
    // getUI().click(new ButtonLocator("Cancel"));
    // // if there is a dialog up here, take a screenshot but get rid of it
    // // - so we can keep going
    // ScreenCapture.createScreenCapture();
    // } catch (Exception e) {
    // // make sure that there are no dialogs up here
    // }fi
    try {
      bot.menu("File").menu("Import...").click();

      SWTBotShell shell = bot.shell("Import");
      try {
        shell.activate();

        bot.tree().expandNode("Maven").select("Existing Maven Projects");
        bot.button("Next >").click();
        bot.comboBoxWithLabel("Root Directory:").setText(tempDir.getCanonicalPath());

        bot.button("Refresh").click();
        bot.button("Finish").click();
      } finally {
        SwtbotUtil.waitForClose(shell);
      }

      waitForAllBuildsToComplete();

    } catch(Exception ex) {
      deleteDirectory(tempDir);
      throw ex;
    }

    return tempDir;
  }

  protected void openResource(String resourceName) {
    bot.menu("Navigate").menu("Open Resource...").click();
    SWTBotShell shell = bot.shell("Open Resource");
    try {
      shell.activate();

      bot.text().setText(resourceName);
      bot.button("Open").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }
  }

  protected void openType(String typeName) {
    bot.menu("Navigate").menu("Open Type...").click();
    SWTBotShell shell = bot.shell("Open Type");
    try {
      shell.activate();

      bot.text().setText(typeName);
      bot.button("OK").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }
  }

  protected void checkoutProjectsFromSVN(String url) throws Exception {
    bot.menu("File").menu("Import...").click();

    SWTBotShell shell = bot.shell("Import");
    try {
      shell.activate();

      bot.tree().expandNode("Maven").select("Check out Maven Projects from SCM");
      bot.button("Next >").click();
      // for some reason, in eclipse 3.5.1 and WT, the direct combo
      // selection
      // is
      // not triggering the UI events, so the finish button never gets
      // enabled
      // getUI().click(new ComboItemLocator("svn", new
      // NamedWidgetLocator("mavenCheckoutLocation.typeCombo")));
      // getUI().setFocus(
      // new NamedWidgetLocator("mavenCheckoutLocation.typeCombo"));
      // for (int i = 0; i < 9; i++) {
      // getUI().keyClick(WT.ARROW_DOWN);
      // }
      try {
        bot.comboBoxWithLabel("SCM URL:").setSelection("svn");
      } catch(RuntimeException ex) {
        throw new RuntimeException("Available options: " + Arrays.asList(bot.comboBoxWithLabel("SCM URL:").items()), ex);
      }
      bot.comboBox(1).setText(url);

      bot.button("Finish").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    waitForAllBuildsToComplete();
  }

  public void importZippedProject(String pluginID, String pluginPath) throws Exception {
    File f = copyPluginResourceToTempFile(pluginID, pluginPath);
    try {
      importZippedProject(f);
    } finally {
      f.delete();
    }
  }

  protected IViewPart showView(final String id) throws Exception {
    IViewPart part = (IViewPart) UIThreadTask.executeOnEventQueue(new UIThreadTask() {
      public Object runEx() throws Exception {
        IViewPart part = getActivePage().showView(id);

        return part;
      }
    });

    waitForAllBuildsToComplete();
    Assert.assertFalse(part == null);

    SWTBotView view = bot.viewById(id);
    view.show();

    return part;
  }

  protected static SWTBotView openView(final String id) {
    SWTBotView view;
    try {
      view = bot.viewById(id);
    } catch(WidgetNotFoundException e) {
      IViewPart part;
      try {
        part = (IViewPart) UIThreadTask.executeOnEventQueue(new UIThreadTask() {
          public Object runEx() throws Exception {
            IViewPart part = getActivePage().showView(id);

            return part;
          }
        });
      } catch(Exception ex) {
        throw new RuntimeException(ex);
      }

      Assert.assertFalse(part == null);

      view = bot.viewById(id);
    }
    view.show();

    return view;
  }

  protected void updateProjectConfiguration(String projectName) throws Exception {
    SWTBotTree tree = selectProject(projectName, true);

    ContextMenuHelper.clickContextMenu(tree, "Maven", "Update Project Configuration");

    waitForAllBuildsToComplete();
  }

  protected void openIssueTracking(String projectName) throws Exception {
    SWTBotTree tree = selectProject("test-project", true);

    ContextMenuHelper.clickContextMenu(tree, "Maven", "Open Issue Tracker");

    waitForAllBuildsToComplete();
  }

  protected SWTBotTree selectProject(String projectName) {
    return selectProject(projectName, true);
  }

  protected SWTBotTree selectProject(String projectName, boolean searchForIt) {
    SWTBotTree tree = bot.viewById(PACKAGE_EXPLORER_VIEW_ID).bot().tree();
    SWTBotTreeItem treeItem = null;
    try {
      treeItem = tree.getTreeItem(projectName);
    } catch(WidgetNotFoundException ex) {
      if(searchForIt) {
        SWTBotTreeItem[] allItems = tree.getAllItems();
        for(SWTBotTreeItem item : allItems) {
          // workaround required due to SVN/CVS that does add extra
          // informations to project name
          if(item.getText().contains(projectName)) {
            treeItem = item;
            break;
          }
        }
      }

      if(treeItem == null) {
        throw ex;
      }
    }
    treeItem.select();
    return tree;
  }

  protected List<SWTBotTreeItem> findItems(SWTBotTreeItem tree, Matcher<String> matcher) {
    List<SWTBotTreeItem> items = new ArrayList<SWTBotTreeItem>();
    SWTBotTreeItem[] allItems = tree.getItems();
    for(SWTBotTreeItem item : allItems) {
      if(matcher.matches(item.getText())) {
        items.add(item);
      }
    }

    return items;
  }

  protected SWTBotTreeItem findItem(SWTBotTreeItem tree, Matcher<String> matcher) {
    List<SWTBotTreeItem> items = findItems(tree, matcher);
    Assert.assertEquals(1, items.size());
    return items.get(0);
  }

  protected SWTBotTreeItem selectNode(SWTBotTree tree, Matcher<String> matcher) {
    SWTBotTreeItem treeItem = null;
    SWTBotTreeItem[] allItems = tree.getAllItems();
    for(SWTBotTreeItem item : allItems) {
      if(matcher.matches(item.getText())) {
        treeItem = item;
        break;
      }
    }

    if(treeItem != null) {
      treeItem.select();
    }
    return treeItem;
  }

  protected void installTomcat6() throws Exception {

    String tomcatInstallLocation = System.getProperty(TOMCAT_INSTALL_LOCATION_PROPERTY);
    if(tomcatInstallLocation == null) {
      tomcatInstallLocation = DEFAULT_TOMCAT_INSTALL_LOCATION;
    }

    Assert.assertTrue("Can't locate tomcat installation: " + tomcatInstallLocation,
        new File(tomcatInstallLocation).exists());
    // Install the Tomcat server

    Thread.sleep(5000);

    showView(SERVERS_VIEW_ID);
    SWTBotView serversView = bot.viewById(SERVERS_VIEW_ID);

    SWTBotTree tree = serversView.bot().tree();
    Assert.assertEquals("Server view already contains a server " + tree.getAllItems(), 0, tree.getAllItems().length);

    ContextMenuHelper.clickContextMenu(tree, "New", "Server");

    SWTBotShell shell = bot.shell("New Server");
    try {
      shell.activate();

      bot.tree().expandNode("Apache").select("Tomcat v6.0 Server");
      bot.button("Next >").click();

      SWTBotButton b = bot.button("Finish");
      if(!b.isEnabled()) {
        // First time...
        bot.textWithLabel("Tomcat installation &directory:").setText(tomcatInstallLocation);
      }
      b.click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    waitForAllBuildsToComplete();
  }

  protected void deployProjectsIntoTomcat() throws Exception {
    // Deploy the test project into tomcat
    SWTBotView serversView = bot.viewById(SERVERS_VIEW_ID);
    serversView.show();
    serversView.setFocus();

    SWTBotTree tree = serversView.bot().tree().select(0);
    if(isEclipseVersion(3, 5)) {
      ContextMenuHelper.clickContextMenu(tree, "Add and Remove...");
    } else {
      ContextMenuHelper.clickContextMenu(tree, "Add and Remove Projects...");
    }
    String title = isEclipseVersion(3, 5) ? "Add and Remove..." : "Add and Remove Projects";

    SWTBotShell shell = bot.shell(title);
    try {
      shell.activate();
      bot.button("Add All >>").click();
      bot.button("Finish").click();
    } catch(Throwable ex) {
      takeScreenShot(ex);
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    ContextMenuHelper.clickContextMenu(tree, "Start");

    waitForAllBuildsToComplete();
    if(!waitForServer(8080, 10000)) {
      ContextMenuHelper.clickContextMenu(tree, Matchers.anyOf(withMnemonic("Start"), withMnemonic("Restart")));
      waitForAllBuildsToComplete();
      waitForServer(8080, 10000);
    }
  }

  protected static void shutdownServer() {
    try {
      // shutdown the server
      SWTBotView serversView = bot.viewById(SERVERS_VIEW_ID);
      serversView.show();
      SWTBotTree tree = serversView.bot().tree().select(0);

      ContextMenuHelper.clickContextMenu(tree, "Stop");

      waitForAllBuildsToComplete();

      SWTBotShell shell = bot.shell("Terminate Server");
      try {
        shell.activate();
        bot.button("OK").click();
      } finally {
        SwtbotUtil.waitForClose(shell);
      }
    } catch(WidgetNotFoundException ex) {
      // this only happen when server takes too long to stop
    }
  }

  public static void removeServer() {
    // shutdown the server
    try {
      SWTBotView serversView = bot.viewById(SERVERS_VIEW_ID);
      SWTBotTree tree = serversView.bot().tree();

      for(int i = 0; i < tree.getAllItems().length; i++ ) {
        SWTBotTree server = tree.select(0);

        // stop it first
        try {
          ContextMenuHelper.clickContextMenu(server, "Stop");
        } catch(Exception e) {
          // was not started
        }
        waitForAllBuildsToComplete();

        ContextMenuHelper.clickContextMenu(server, "Delete");

        SWTBotShell shell = bot.shell("Delete Server");
        try {
          shell.activate();

          bot.button("OK").click();
        } finally {
          SwtbotUtil.waitForClose(shell);
        }
      }

      waitForAllBuildsToComplete();
    } catch(WidgetNotFoundException e) {
      // not an issue, mean this is not a server test
      return;
    } catch(SWTException e) {
      if(e.getCause() instanceof WidgetNotFoundException) {
        return; // not a problem
      } else {
        throw e;
      }
    }
  }

  protected void restartServer(boolean republish) throws Exception {
    // shutdown the server
    SWTBotView serversView = bot.viewById(SERVERS_VIEW_ID);
    serversView.show();

    SWTBotTree tree;
    try {
      tree = serversView.bot().tree().select(0);
    } catch(WidgetNotFoundException ex) {
      takeScreenShot(ex);
      throw ex;
    }

    shutdownServer();

    if(republish) {
      ContextMenuHelper.clickContextMenu(tree, "Publish");
      waitForAllBuildsToComplete();
    }

    ContextMenuHelper.clickContextMenu(tree, "Start");
    waitForAllBuildsToComplete();
  }

  protected void findText(String src) {
    findTextWithWrap(src, false);
  }

  public static final String FIND_REPLACE = "Find/Replace";

  protected void findTextWithWrap(String src, boolean wrap) {
    bot.menu("Edit").menu("Find/Replace...").click();

    SWTBotShell shell = bot.shell(FIND_REPLACE);
    try {
      shell.activate();

      bot.comboBoxWithLabel("Find:").setText(src);
      if(wrap) {
        bot.checkBox("Wrap search").select();
      } else {
        bot.checkBox("Wrap search").deselect();
      }

      bot.button("Find").click();
      bot.button("Close").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }
  }

  protected void replaceText(String src, String target) {
    replaceTextWithWrap(src, target, false);
  }

  protected void replaceTextWithWrap(String src, String target, boolean wrap) {
    bot.menu("Edit").menu("Find/Replace...").click();

    SWTBotShell shell = bot.shell(FIND_REPLACE);
    try {
      shell.activate();

      bot.comboBoxWithLabel("Find:").setText(src);
      bot.comboBoxWithLabel("Replace with:").setText(target);

      if(wrap) {
        bot.checkBox("Wrap search").select();
      } else {
        bot.checkBox("Wrap search").deselect();
      }

      bot.button("Replace All").click();

      bot.button("Close").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

  }

  public static boolean isEclipseVersion(int major, int minor) {
    Bundle bundle = ResourcesPlugin.getPlugin().getBundle();
    String version = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
    Version v = org.osgi.framework.Version.parseVersion(version);
    return v.getMajor() == major && v.getMinor() == minor;
  }

  protected static IWorkbenchPage getActivePage() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    return workbench.getWorkbenchWindows()[0].getActivePage();
  }

  protected File copyPluginResourceToTempFile(String plugin, String file) throws MalformedURLException, IOException {
    URL url = FileLocator.find(Platform.getBundle(plugin), new Path("/" + file), null);
    return copyPluginResourceToTempFile(plugin, url);
  }

  protected File copyPluginResourceToTempFile(String plugin, URL url) throws MalformedURLException, IOException {
    File f = File.createTempFile("temp", "." + new Path(url.getFile()).getFileExtension());
    InputStream is = new BufferedInputStream(url.openStream());
    FileOutputStream os = new FileOutputStream(f);
    try {
      IOUtil.copy(is, os);
    } finally {
      is.close();
      os.close();
    }

    return f;
  }

  /**
   * Import a project and assert it has no markers of SEVERITY_ERROR
   */
  protected File doImport(String pluginId, String projectPath) throws Exception {
    return doImport(pluginId, projectPath, true);
  }

  protected File doImport(String pluginId, String projectPath, boolean assertNoErrors) throws Exception {
    File tempDir = importMavenProjects(pluginId, projectPath);
    if(assertNoErrors) {
      assertProjectsHaveNoErrors();
    }
    return tempDir;
  }

  protected void assertProjectsHaveNoErrors() throws Exception {
    StringBuffer messages = new StringBuffer();
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    int count = 0;
    for(IProject project : projects) {
      if("Servers".equals(project.getName())) {
        continue;
      }
      if(count >= 10) {
        break;
      }
      IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
      for(int i = 0; i < markers.length; i++ ) {
        if(markers[i].getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_ERROR) {
          count++ ;
          messages.append('\t');
          if(messages.length() > 0) {
            messages.append(System.getProperty("line.separator"));
          }
          messages.append(project.getName() + ":" + markers[i].getAttribute(IMarker.LOCATION, "unknown location") + " "
              + markers[i].getAttribute(IMarker.MESSAGE, "unknown message"));
        }
      }
    }
    if(count > 0) {
      Assert.fail("One or more compile errors found:" + System.getProperty("line.separator") + messages);
    }
  }

  private static void unzipFile(String pluginId, String pluginPath, File dest) throws IOException {
    URL url = FileLocator.find(Platform.getBundle(pluginId), new Path("/" + pluginPath), null);
    InputStream is = new BufferedInputStream(url.openStream());
    ZipInputStream zis = new ZipInputStream(is);
    try {
      ZipEntry entry = zis.getNextEntry();
      while(entry != null) {
        File f = new File(dest, entry.getName());
        if(entry.isDirectory()) {
          f.mkdirs();
        } else {
          if(!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
          }
          OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
          try {
            IOUtil.copy(zis, os);
          } finally {
            os.close();
          }
        }
        zis.closeEntry();
        entry = zis.getNextEntry();
      }
    } finally {
      zis.close();
    }
  }

  public static File unzipProject(String pluginId, String pluginPath) throws Exception {
    File tempDir = createTempDir("sonatype");
    unzipFile(pluginId, pluginPath, tempDir);
    return tempDir;
  }

  protected static File createTempDir(String prefix) throws IOException {
    File temp = null;
    temp = File.createTempFile(prefix, "");
    if(!temp.delete()) {
      throw new IOException("Unable to delete temp file:" + temp.getName());
    }
    if(!temp.mkdir()) {
      throw new IOException("Unable to create temp dir:" + temp.getName());
    }
    return temp;
  }

  private void deleteDirectory(File dir) {
    File[] fileArray = dir.listFiles();
    if(fileArray != null) {
      for(int i = 0; i < fileArray.length; i++ ) {
        if(fileArray[i].isDirectory())
          deleteDirectory(fileArray[i]);
        else
          fileArray[i].delete();
      }
    }
    dir.delete();
  }

  // Location of tomcat 6 installation which can be used by Eclipse WTP tests
  private static final String DEFAULT_TOMCAT_INSTALL_LOCATION = "target/tomcat/apache-tomcat-6.0.24";

  // Set this system property to override DEFAULT_TOMCAT_INSTALL_LOCATION
  private static final String TOMCAT_INSTALL_LOCATION_PROPERTY = "tomcat.install.location";

  public static final String SERVERS_VIEW_ID = "org.eclipse.wst.server.ui.ServersView";

  public static final String TOMCAT_SERVER_NAME = "Tomcat.*";

  public static void clearProjects() throws Exception {
    WorkspaceHelpers.cleanWorkspace();
  }

  protected MavenPomEditor openPomFile(String name) throws Exception {

    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(name));

    final IEditorInput editorInput = new FileEditorInput(file);
    MavenPomEditor editor = (MavenPomEditor) UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        IEditorPart part = getActivePage().openEditor(editorInput, "org.eclipse.m2e.editor.MavenPomEditor", true);
        if(part instanceof MavenPomEditor) {
          return part;
        }
        return null;
      }
    });

    waitForAllBuildsToComplete();

    return editor;
  }

  protected Model getModel(final MavenPomEditor editor) throws Exception {
    Model model = (Model) UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        return editor.readProjectDocument();
      }
    });
    return model;
  }

  /**
   * Create an archetype project and assert that it has proper natures & builders, and no error markers
   */
  protected IProject createArchetypeProject(final String archetypeName, String projectName) throws Exception {
    try {
      IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      Assert.assertFalse(project.exists());

      bot.menu("File").menu("New").menu("Project...").click();

      SWTBotShell shell = bot.shell("New Project");
      try {
        shell.activate();

        bot.tree().expandNode("Maven").select("Maven Project");
        // click the first next button
        bot.button("Next >").click();
        bot.checkBox("Create a simple project (skip archetype selection)").deselect();

        // then the first page with only 'default' values
        bot.button("Next >").click();
        bot.comboBoxWithId("name", "catalogsCombo").setSelection(0);

        bot.waitUntil(SwtbotUtil.waitForLoad(bot.table()));

        // now select the quickstart row
        bot.table().select(bot.table().indexOf(archetypeName, 1));

        // and then click next
        bot.button("Next >").click();

        // then fill in the last page details
        bot.comboBoxWithLabel("Group Id:").setText(DEFAULT_PROJECT_GROUP);
        bot.comboBoxWithLabel("Artifact Id:").setText(projectName);

        bot.button("Finish").click();
      } catch(Throwable ex) {
        throw new Exception("Failed to create project for archetype:" + archetypeName + " - " + takeScreenShot(), ex);
      } finally {
        SwtbotUtil.waitForClose(shell);
      }

      waitForAllBuildsToComplete();

      project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      Assert.assertTrue(project.exists());
      assertProjectsHaveNoErrors();
      Assert.assertTrue("archtype project \"" + archetypeName + "\" created without Maven nature",
          project.hasNature(IMavenConstants.NATURE_ID));

      selectProject(projectName, true);

      return project;
    } catch(Throwable ex) {
      throw new Exception("Failed to create project for archetype:" + archetypeName + " - " + takeScreenShot(), ex);
    }
  }

  /**
   * Create an archetype project and assert that it has proper natures & builders, and no error markers
   */
  protected IProject createSimpleMavenProject(String projectName) throws Exception {
    try {
      IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      Assert.assertFalse(project.exists());

      bot.menu("File").menu("New").menu("Project...").click();

      SWTBotShell shell = bot.shell("New Project");
      try {
        shell.activate();

        bot.tree().expandNode("Maven").select("Maven Project");

        bot.button("Next >").click();

        bot.checkBox("Create a simple project (skip archetype selection)").click();

        bot.button("Next >").click();

        // then fill in the last page details
        bot.comboBoxWithLabel("Group Id:").setText(DEFAULT_PROJECT_GROUP);
        bot.comboBoxWithLabel("Artifact Id:").setText(projectName);

        bot.button("Finish").click();
      } finally {
        SwtbotUtil.waitForClose(shell);
      }

      waitForAllBuildsToComplete();

      project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      Assert.assertTrue(project.exists());
      assertProjectsHaveNoErrors();
      Assert.assertTrue(projectName + " created without Maven nature", project.hasNature(IMavenConstants.NATURE_ID));

      selectProject(projectName, true);

      return project;
    } catch(Throwable ex) {
      throw new Exception("Failed to create project for archetype:" + projectName + " - " + takeScreenShot(), ex);
    }
  }

  protected IEditorPart openFile(IProject project, String relPath) throws Exception {

    final IFile f = project.getFile(relPath);

    IEditorPart editor = (IEditorPart) UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        return IDE.openEditor(getActivePage(), f, true);
      }
    });

    return editor;
  }

  private static boolean xmlPrefsSet = false;

  protected void setXmlPrefs() throws Exception {
    if(!xmlPrefsSet && isEclipseVersion(3, 5)) {
      // Disable new xml completion behavior to preserver compatibility
      // with previous versions.
      bot.menu("Window").menu("Preferences").click();

      SWTBotShell shell = bot.shell("Preferences");
      try {
        shell.activate();

        bot.tree().expandNode("XML").expandNode("XML Files").expandNode("Editor").select("Typing");

        bot.checkBox("Insert a matching end tag").select();

        bot.button("OK").click();

        xmlPrefsSet = true;

      } finally {
        SwtbotUtil.waitForClose(shell);
      }
    }
  }

  protected IProject createQuickstartProject(String projectName) throws Exception {
    return createArchetypeProject("maven-archetype-quickstart", projectName);
  }

  protected void save() {
    bot.menu("File").menu("Save").click();
  }

  protected static void assertWizardError(final String message) throws Exception {
    assertWizardMessage(message, WizardPage.ERROR);
  }

  protected static void assertWizardMessage(final String message) throws Exception {
    assertWizardMessage(message, WizardPage.INFORMATION);
  }

  protected static void assertWizardMessage(final String message, final int severity) throws Exception {
    bot.sleep(1000);
    final AssertionError[] error = new AssertionError[] {null};
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        try {
          Object data = bot.activeShell().widget.getData();
          assertTrue("Current dialog is expected to be a wizard", data instanceof WizardDialog);

          boolean error = severity == WizardPage.ERROR;

          IWizardPage page = ((WizardDialog) data).getCurrentPage();
          String wizardMessage = error ? page.getErrorMessage() : page.getMessage();
          String prefix = error ? "Wizard error " : "Wizard message ";

          if(message == null) {
            assertNull(prefix + "should be null", wizardMessage);
          } else {
            assertNotNull(prefix + "should not be null", wizardMessage);
            assertEquals(prefix + "is not as expected", message, wizardMessage.trim());
          }
        } catch(AssertionError e) {
          error[0] = e;
        }
      }
    });
    if(error[0] != null) {
      takeScreenShot(error[0]);
    }
  }

  protected void addDependency(IProject project, String groupId, String artifactId, String version) {
    addDependency(project.getName(), groupId, artifactId, version);
  }

  @SuppressWarnings("unchecked")
  protected void addDependency(String projectName, String groupId, String artifactId, String version) {
    ContextMenuHelper.clickContextMenu(selectProject(projectName), "Maven", "Add Dependency");

    SWTBotShell shell = bot.shell("Add Dependency");
    try {
      shell.activate();

      bot.text().setText(artifactId);
      SWTBotTreeItem node = bot.tree().getTreeItem(ContainsMnemonic.containsMnemonic(groupId),
          ContainsMnemonic.containsMnemonic(artifactId));
      node.expand();
      String[] selection = findNodeName(node, startsWith(version));
      assertEquals("The matcher is expected to find one node", 1, selection.length);
      node.getNode(selection[0]).doubleClick();

      // bot.button("OK").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }
  }

  protected String[] findNodeName(SWTBotTreeItem node, Matcher<String> matcher) {
    List<String> nodes = new ArrayList<String>();
    List<String> items = node.getNodes();
    for(String text : items) {
      if(matcher.matches(text)) {
        nodes.add(text);
      }
    }
    return nodes.toArray(new String[0]);
  }

  protected void cleanProjects() {
    bot.menu("Project").menu("Clean...").click();

    SWTBotShell shell = bot.shell("Clean");
    try {
      shell.activate();
      bot.radio("Clean all projects").click();
      bot.button("ok").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    waitForAllBuildsToComplete();
  }

  protected static void closeView(final String id) throws Exception {
    IViewPart view = (IViewPart) UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        IViewPart view = getActivePage().findView(id);

        return view;
      }
    });

    if(view != null) {
      bot.viewById(id).close();
    }
  }

  protected static void openPerspective(final String id) throws Exception {
    // select Java perspective
    bot.perspectiveById(id).activate();

    UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
        IPerspectiveDescriptor perspective = perspectiveRegistry.findPerspectiveWithId(id);
        getActivePage().setPerspective(perspective);

        return null;
      }
    });
  }

  protected void switchToExternalMaven() throws Exception {
    MavenRuntime newRuntime = MavenRuntimeManager.createExternalRuntime("C:\\apache-maven-2.1.0");
    List<MavenRuntime> currRuntimes = MavenPlugin.getDefault().getMavenRuntimeManager().getMavenRuntimes();
    ArrayList<MavenRuntime> list = new ArrayList<MavenRuntime>(currRuntimes);
    list.add(newRuntime);
    MavenPlugin.getDefault().getMavenRuntimeManager().setRuntimes(list);
    MavenPlugin.getDefault().getMavenRuntimeManager().setDefaultRuntime(newRuntime);
  }

  protected void updateLocalIndex() throws Exception {
    SWTBotView view = openView("org.eclipse.m2e.core.views.MavenRepositoryView");
    SWTBotTree tree = view.bot().tree();
    findItem(tree.expandNode("Local Repositories"), StringStartsWith.startsWith("Local Repository")).select();
    ContextMenuHelper.clickContextMenu(tree, "Rebuild Index");

    SWTBotShell shell = bot.shell("Rebuild Index");
    try {
      shell.activate();
      bot.button("OK").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    waitForAllBuildsToComplete();

    showView("org.eclipse.m2e.core.views.MavenRepositoryView");
  }

  protected void excludeArtifact(String projectName, String jarName) throws Exception {
    SWTBotTree tree = selectProject(projectName);
    findItem(tree.expandNode(projectName).expandNode("Maven Dependencies"), StringStartsWith.startsWith(jarName))
        .select();
    ContextMenuHelper.clickContextMenu(tree, "Maven", "Exclude Maven Artifact...");
    SWTBotShell shell = bot.shell("Exclude Maven Artifact");
    try {
      shell.activate();
      bot.button("OK").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    waitForAllBuildsToComplete();
  }

  protected static boolean waitForServer(int port, int timeout) {
    Socket socket = new Socket();
    try {
      socket.bind(null);
    } catch(IOException e) {
      return false;
    }
    try {
      for(int i = 0; i <= timeout / 100; i++ ) {
        try {
          socket.connect(new InetSocketAddress(InetAddress.getByName(null), port), 100);
          return true;
        } catch(IOException e) {
          // ignored, retry
        }
      }
      return false;
    } finally {
      try {
        socket.close();
      } catch(IOException e) {
        // ignored
      }
    }
  }

  protected String retrieveWebPage(String urlString) throws IOException, InterruptedException {
    int i = 0;
    do {
      URL url = new URL(urlString);
      URLConnection conn;
      try {
        conn = url.openConnection();
      } catch(IOException e) {
        continue;
      }
      conn.setDoInput(true);
      try {
        conn.connect();
      } catch(IOException e) {
        Thread.sleep(1000);
        continue;
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      IOUtil.copy(conn.getInputStream(), out);

      try {
        conn.getInputStream().close();
      } catch(IOException e) {
        // not relevant
      }

      return new String(out.toByteArray(), "UTF-8");
    } while(i < 10);

    return null;
  }

  protected void copy(final String str) throws Exception {
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        Clipboard clipboard = new Clipboard(Display.getDefault());
        TextTransfer transfer = TextTransfer.getInstance();
        clipboard.setContents(new String[] {str}, new Transfer[] {transfer});
        clipboard.dispose();
      }
    });
  }

  protected static String setUserSettings(String settingsFile) {
    if(settingsFile != null) {
      settingsFile = new File(settingsFile).getAbsolutePath();
    }
    IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
    String oldUserSettingsFile = mavenConfiguration.getUserSettingsFile();
    mavenConfiguration.setUserSettingsFile(settingsFile);
    return oldUserSettingsFile;
  }

  protected static String getUserSettings() {
    IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
    return mavenConfiguration.getUserSettingsFile();
  }

  protected static void updateRepositoryRegistry() {
    try {
      ((RepositoryRegistry) MavenPlugin.getDefault().getRepositoryRegistry()).updateRegistry(monitor);
    } catch(CoreException e) {
      throw new IllegalStateException(e);
    }
  }

  protected static void updateIndex(String repoUrl) {
    IRepositoryRegistry repositoryRegistry = MavenPlugin.getDefault().getRepositoryRegistry();
    for(IRepository repository : repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS)) {
      if(repository.getUrl().equals(repoUrl)) {
        try {
          NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
          indexManager.updateIndex(repository, true, monitor);
        } catch(CoreException e) {
          throw new IllegalStateException(e);
        }
      }
    }
  }

}
