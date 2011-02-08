
package org.eclipse.m2e.editor.composites;

import static org.eclipse.m2e.editor.pom.FormUtils.nvl;
import static org.eclipse.m2e.editor.pom.FormUtils.setButton;
import static org.eclipse.m2e.editor.pom.FormUtils.setText;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.search.util.Packaging;
import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;
import org.eclipse.m2e.core.ui.internal.util.ProposalUtil;
import org.eclipse.m2e.core.ui.internal.wizards.WidthGroup;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.FormUtils;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.editor.pom.ValueProvider;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;


public class DependencyDetailComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  private MavenPomEditorPage editorPage;

  private FormToolkit toolkit;

  private WidthGroup detailsWidthGroup = new WidthGroup();

  protected Text groupIdText;

  protected Text artifactIdText;

  protected Text versionText;

  private Text classifierText;

  private CCombo typeCombo;

  private CCombo scopeCombo;

  private Text systemPathText;

  private Button selectSystemPathButton;

  private Button optionalButton;

  public DependencyDetailComposite(Composite parent, MavenPomEditorPage editorPage) {
    super(parent, SWT.NONE);

    this.editorPage = editorPage;
    toolkit = editorPage.getManagedForm().getToolkit();
//    toolkit = new FormToolkit(Display.getCurrent());

    GridLayout dependencyCompositeLayout = new GridLayout(3, false);
    dependencyCompositeLayout.marginWidth = 2;
    dependencyCompositeLayout.marginHeight = 2;
    setLayout(dependencyCompositeLayout);
    addControlListener(detailsWidthGroup);

    createControls();

    toolkit.paintBordersFor(this);
    toolkit.adapt(this);
  }
  /** mkleint: apparently this methods shall find the version in resolved pom for the given dependency
   * not sure if getBaseVersion is the way to go..
   * Note: duplicated in DependenciesComposite 
   * @param groupId
   * @param artifactId
   * @param monitor
   * @return
   */
  protected String getVersion(String groupId, String artifactId, IProgressMonitor monitor) {
    try {
      MavenProject mavenProject = editorPage.getPomEditor().readMavenProject(false, monitor);
      
      Artifact a = mavenProject.getArtifactMap().get(groupId + ":" + artifactId); //$NON-NLS-1$
      if(a != null) {
        return a.getBaseVersion();
      }
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
    return null;
  }

  private void createControls() {
    Label groupIdLabel = toolkit.createLabel(this, Messages.DependenciesComposite_lblGroupId, SWT.NONE);
    groupIdLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(groupIdLabel);

    groupIdText = toolkit.createText(this, null, SWT.NONE);
    GridData gd_groupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_groupIdText.horizontalIndent = 4;
    groupIdText.setLayoutData(gd_groupIdText);
    ProposalUtil.addGroupIdProposal(editorPage.getProject(), groupIdText, Packaging.ALL);
    M2EUIUtils.addRequiredDecoration(groupIdText);

    Hyperlink artifactIdHyperlink = toolkit.createHyperlink(this, Messages.DependenciesComposite_lblArtifactId,
        SWT.NONE);
    artifactIdHyperlink.setLayoutData(new GridData());
    artifactIdHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        final String groupId = groupIdText.getText();
        final String artifactId = artifactIdText.getText();
        final String version = versionText.getText();
        new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
          protected IStatus run(IProgressMonitor monitor) {
            OpenPomAction.openEditor(groupId, artifactId, //
                version != null ? version : getVersion(groupId, artifactId, monitor), //
                monitor);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    });

    detailsWidthGroup.addControl(artifactIdHyperlink);

    artifactIdText = toolkit.createText(this, null, SWT.NONE);
    GridData gd_artifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_artifactIdText.horizontalIndent = 4;
    artifactIdText.setLayoutData(gd_artifactIdText);
    ProposalUtil.addArtifactIdProposal(editorPage.getProject(), groupIdText, artifactIdText, Packaging.ALL);
    M2EUIUtils.addRequiredDecoration(artifactIdText);

    Label versionLabel = toolkit.createLabel(this, Messages.DependenciesComposite_lblVersion, SWT.NONE);
    versionLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(versionLabel);

    versionText = toolkit.createText(this, null, SWT.NONE);
    GridData versionTextData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    versionTextData.horizontalIndent = 4;
    versionTextData.widthHint = 200;
    versionText.setLayoutData(versionTextData);
    ProposalUtil.addVersionProposal(editorPage.getProject(), editorPage.getPomEditor().getMavenProject(), groupIdText, artifactIdText, versionText, Packaging.ALL);

    Label classifierLabel = toolkit.createLabel(this, Messages.DependenciesComposite_lblClassifier, SWT.NONE);
    classifierLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(classifierLabel);

    classifierText = toolkit.createText(this, null, SWT.NONE);
    GridData gd_classifierText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_classifierText.horizontalIndent = 4;
    gd_classifierText.widthHint = 200;
    classifierText.setLayoutData(gd_classifierText);
    ProposalUtil.addClassifierProposal(editorPage.getProject(), groupIdText, artifactIdText, versionText,
        classifierText, Packaging.ALL);

    Label typeLabel = toolkit.createLabel(this, Messages.DependenciesComposite_lblType, SWT.NONE);
    typeLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(typeLabel);

    typeCombo = new CCombo(this, SWT.FLAT);
    // FormUtils.addTypeProposal(groupIdText, artifactIdText, versionText, typeCombo, Packaging.ALL);

    // TODO retrieve artifact type from selected dependency 
    typeCombo.add("jar"); //$NON-NLS-1$
    typeCombo.add("war"); //$NON-NLS-1$
    typeCombo.add("rar"); //$NON-NLS-1$
    typeCombo.add("ear"); //$NON-NLS-1$
    typeCombo.add("par"); //$NON-NLS-1$
    typeCombo.add("ejb"); //$NON-NLS-1$
    typeCombo.add("ejb-client"); //$NON-NLS-1$
    typeCombo.add("test-jar"); //$NON-NLS-1$
    typeCombo.add("java-source"); //$NON-NLS-1$
    typeCombo.add("javadoc"); //$NON-NLS-1$
    typeCombo.add("maven-plugin"); //$NON-NLS-1$
    typeCombo.add("pom"); //$NON-NLS-1$

    GridData gd_typeText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_typeText.horizontalIndent = 4;
    gd_typeText.widthHint = 120;
    typeCombo.setLayoutData(gd_typeText);
    typeCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    toolkit.adapt(typeCombo, true, true);

    Label scopeLabel = toolkit.createLabel(this, Messages.DependenciesComposite_lblScope, SWT.NONE);
    scopeLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(scopeLabel);

    scopeCombo = new CCombo(this, SWT.READ_ONLY | SWT.FLAT);
    scopeCombo.add("compile"); //$NON-NLS-1$
    scopeCombo.add("test"); //$NON-NLS-1$
    scopeCombo.add("provided"); //$NON-NLS-1$
    scopeCombo.add("runtime"); //$NON-NLS-1$
    scopeCombo.add("system"); //$NON-NLS-1$
    // TODO should be only used on a dependency of type pom in the <dependencyManagement> section
    scopeCombo.add("import"); //$NON-NLS-1$

    GridData gd_scopeText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_scopeText.horizontalIndent = 4;
    gd_scopeText.widthHint = 120;
    scopeCombo.setLayoutData(gd_scopeText);
    scopeCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    toolkit.adapt(scopeCombo, true, true);

    Label systemPathLabel = toolkit.createLabel(this, Messages.DependenciesComposite_lblSystemPath, SWT.NONE);
    systemPathLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(systemPathLabel);

    systemPathText = toolkit.createText(this, null, SWT.NONE);
    GridData gd_systemPathText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_systemPathText.horizontalIndent = 4;
    gd_systemPathText.widthHint = 200;
    systemPathText.setLayoutData(gd_systemPathText);

    selectSystemPathButton = toolkit.createButton(this, Messages.DependenciesComposite_btnSelect, SWT.NONE);
    new Label(this, SWT.NONE);

    optionalButton = toolkit.createButton(this, Messages.DependenciesComposite_btnOptional, SWT.CHECK);
    GridData gd_optionalButton = new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1);
    gd_optionalButton.horizontalIndent = 4;
    optionalButton.setLayoutData(gd_optionalButton);

    setTabList(new Control[] {groupIdText, artifactIdText, versionText, classifierText, typeCombo, scopeCombo,
        systemPathText, selectSystemPathButton, optionalButton});
  }

  protected void update(Dependency dependency) {
    if(editorPage != null) {
      editorPage.removeNotifyListener(groupIdText);
      editorPage.removeNotifyListener(artifactIdText);
      editorPage.removeNotifyListener(versionText);
      editorPage.removeNotifyListener(classifierText);
      editorPage.removeNotifyListener(typeCombo);
      editorPage.removeNotifyListener(scopeCombo);
      editorPage.removeNotifyListener(systemPathText);
      editorPage.removeNotifyListener(optionalButton);
    }

    if(editorPage == null || dependency == null) {
      FormUtils.setEnabled(this, true);

      setText(groupIdText, ""); //$NON-NLS-1$
      setText(artifactIdText, ""); //$NON-NLS-1$
      setText(versionText, ""); //$NON-NLS-1$
      setText(classifierText, ""); //$NON-NLS-1$
      setText(typeCombo, ""); //$NON-NLS-1$
      setText(scopeCombo, ""); //$NON-NLS-1$
      setText(systemPathText, ""); //$NON-NLS-1$
      setButton(optionalButton, false);

      return;
    }

    FormUtils.setEnabled(this, true);
    FormUtils.setReadonly(this, editorPage.isReadOnly());

    setText(groupIdText, dependency.getGroupId());
    setText(artifactIdText, dependency.getArtifactId());
    setText(versionText, dependency.getVersion());
    setText(classifierText, dependency.getClassifier());
    setText(typeCombo, "".equals(nvl(dependency.getType())) ? "jar" : dependency.getType());
    setText(scopeCombo, "".equals(nvl(dependency.getScope())) ? "compile" : dependency.getScope());
    setText(systemPathText, dependency.getSystemPath());

    boolean optional = Boolean.parseBoolean(dependency.getOptional());
    if(optionalButton.getSelection() != optional) {
      optionalButton.setSelection(optional);
    }

    // set new listeners
    ValueProvider<Dependency> dependencyProvider = new ValueProvider.DefaultValueProvider<Dependency>(dependency);
    editorPage.setModifyListener(groupIdText, dependencyProvider, POM_PACKAGE.getDependency_GroupId(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(artifactIdText, dependencyProvider, POM_PACKAGE.getDependency_ArtifactId(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(versionText, dependencyProvider, POM_PACKAGE.getDependency_Version(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(classifierText, dependencyProvider, POM_PACKAGE.getDependency_Classifier(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(typeCombo, dependencyProvider, POM_PACKAGE.getDependency_Type(), "jar"); //$NON-NLS-1$
    editorPage.setModifyListener(scopeCombo, dependencyProvider, POM_PACKAGE.getDependency_Scope(), "compile"); //$NON-NLS-1$
    editorPage.setModifyListener(systemPathText, dependencyProvider, POM_PACKAGE.getDependency_SystemPath(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(optionalButton, dependencyProvider, POM_PACKAGE.getDependency_Optional(), "false");

    editorPage.registerListeners();
  }

  public void setGroupId(String groupId) {
    groupIdText.setText(nvl(groupId));
  }

  public String getGroupId() {
    return groupIdText.getText();
  }

  public void setArtifactId(String artifactId) {
    artifactIdText.setText(nvl(artifactId));
  }

  public String getArtifactId() {
    return artifactIdText.getText();
  }

  public void setVersion(String version) {
    versionText.setText(nvl(version));
  }

  public String getVersion() {
    return versionText.getText();
  }

  public void setType(String type) {
    typeCombo.setText(nvl(type));
  }

  public void setScope(String scope) {
    scopeCombo.setText(nvl(scope));
  }
}
