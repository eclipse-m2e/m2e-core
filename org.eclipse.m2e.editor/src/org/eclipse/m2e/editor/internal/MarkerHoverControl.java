/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.editor.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.IInformationControlExtension3;
import org.eclipse.jface.text.IInformationControlExtension5;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;

import org.eclipse.m2e.core.ui.internal.markers.MavenProblemResolution;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.pom.PomHyperlinkDetector;
import org.eclipse.m2e.editor.pom.PomTextHover;
import org.eclipse.m2e.editor.pom.PomHyperlinkDetector.ExpressionRegion;
import org.eclipse.m2e.editor.pom.PomHyperlinkDetector.ManagedArtifactRegion;
import org.eclipse.m2e.editor.pom.PomHyperlinkDetector.MarkerRegion;
import org.eclipse.m2e.editor.pom.PomTextHover.CompoundRegion;
import org.eclipse.m2e.internal.discovery.markers.MavenDiscoveryMarkerResolutionGenerator;


public class MarkerHoverControl extends AbstractInformationControl
    implements IInformationControlExtension2, IInformationControlExtension3, IInformationControlExtension5 {

  private CompoundRegion region;

  private Control focusControl;

  private Composite parent;

  private final DefaultMarkerAnnotationAccess markerAccess;

  public MarkerHoverControl(Shell shell, ToolBarManager toolbarManager) {
    super(shell, toolbarManager);
    markerAccess = new DefaultMarkerAnnotationAccess();
    create();
  }

  public MarkerHoverControl(Shell shell) {
    super(shell, EditorsUI.getTooltipAffordanceString());
    markerAccess = new DefaultMarkerAnnotationAccess();
    create();
  }

  /*
   * @see org.eclipse.jface.text.IInformationControlExtension2#setInput(java.lang.Object)
   */
  public void setInput(Object input) {
    assert input instanceof CompoundRegion;
    if(input instanceof CompoundRegion) {
      region = (CompoundRegion) input;
    } else {
      throw new IllegalStateException("Not CompoundRegion"); //$NON-NLS-1$
    }
    disposeDeferredCreatedContent();
    deferredCreateContent();
  }

  Shell getMyShell() {
    return super.getShell();
  }

  Control getRoot() {
    return parent;
  }

  /*
   * @see org.eclipse.jface.text.IInformationControlExtension#hasContents()
   */
  public boolean hasContents() {
    return region != null;
  }

  /*
   * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover.AbstractInformationControl#setFocus()
   */
  public void setFocus() {
    super.setFocus();
    if(focusControl != null) {
      focusControl.setFocus();
    }
  }

  /*
   * @see org.eclipse.jface.text.AbstractInformationControl#setVisible(boolean)
   */
  public final void setVisible(boolean visible) {
    if(!visible)
      disposeDeferredCreatedContent();
    super.setVisible(visible);
  }

  protected void disposeDeferredCreatedContent() {
    Control[] children = parent.getChildren();
    for(Control child : children) {
      child.dispose();
    }
    ToolBarManager toolBarManager = getToolBarManager();
    if(toolBarManager != null)
      toolBarManager.removeAll();
  }

  /*
   * @see org.eclipse.jface.text.AbstractInformationControl#createContent(org.eclipse.swt.widgets.Composite)
   */
  protected void createContent(Composite parent) {
    this.parent = parent;
    GridLayout layout = new GridLayout(1, false);
    layout.verticalSpacing = 0;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    parent.setLayout(layout);
  }

  @Override
  public Point computeSizeHint() {
    Point preferedSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

    Point constrains = getSizeConstraints();
    if(constrains == null)
      return preferedSize;

    int trimWidth = getShell().computeTrim(0, 0, 0, 0).width;
    Point constrainedSize = getShell().computeSize(constrains.x - trimWidth, SWT.DEFAULT, true);

    int width = Math.min(preferedSize.x, constrainedSize.x);
    int height = Math.max(preferedSize.y, constrainedSize.y);

    return new Point(width, height);
  }

  /**
   * Create content of the hover. This is called after the input has been set.
   */
  protected void deferredCreateContent() {
    if(region != null) {
      final ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
      GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
      scrolledComposite.setLayoutData(gridData);
      scrolledComposite.setExpandVertical(true);
      scrolledComposite.setExpandHorizontal(true);
      Composite composite = new Composite(scrolledComposite, SWT.NONE);
      GridLayout layout = new GridLayout(1, false);
      composite.setLayout(layout);
      scrolledComposite.setContent(composite);

      // force resize of scrolledComposite when its content height changes
      composite.addListener(SWT.Resize, new Listener() {
        int width = -1;

        public void handleEvent(Event e) {
          int newWidth = composite.getSize().x;
          if(newWidth != width) {
            scrolledComposite.setMinHeight(composite.computeSize(newWidth, SWT.DEFAULT).y);
            width = newWidth;
          }
        }
      });

      boolean lifecycleMarkers = false;
      for(IRegion reg : region.getRegions()) {
        if(reg instanceof PomHyperlinkDetector.MarkerRegion) {
          PomHyperlinkDetector.MarkerRegion markerReg = (PomHyperlinkDetector.MarkerRegion) reg;
          IMarker mark = markerReg.getAnnotation().getMarker();
          if(MavenDiscoveryMarkerResolutionGenerator.canResolve(mark)) {
            lifecycleMarkers = true;
            break;
          }
        }
      }
      fillToolbar(lifecycleMarkers);

      for(IRegion reg : region.getRegions()) {
        if(reg instanceof PomHyperlinkDetector.MarkerRegion) {
          final PomHyperlinkDetector.MarkerRegion markerReg = (PomHyperlinkDetector.MarkerRegion) reg;
          createAnnotationInformation(composite, markerReg);
          final IMarker mark = markerReg.getAnnotation().getMarker();

          if(MavenProblemResolution.hasResolutions(mark)) {
            List<IMarkerResolution> resolutions = MavenProblemResolution.getResolutions(mark);
            createResolutionsControl(composite, mark, resolutions);
          }
        }
        if(reg instanceof ManagedArtifactRegion) {
          final ManagedArtifactRegion man = (ManagedArtifactRegion) reg;
          Composite comp = createTooltipComposite(composite, PomTextHover.getLabelForRegion(man));
          //only create the hyperlink when the origin location for jumping is present.
          //in some cases (managed version comes from imported dependencies) we don't have the location and have nowhere to jump)
          if(PomHyperlinkDetector.canCreateHyperLink(man)) {
            Link link = createHyperlink(comp);
            link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
              dispose();
              PomHyperlinkDetector.createHyperlink(man).open();
            }));
          }

        }
        if(reg instanceof ExpressionRegion) {
          final ExpressionRegion expr = (ExpressionRegion) reg;
          Composite tooltipComposite = createTooltipComposite(composite, PomTextHover.getLabelForRegion(expr));
          if(PomHyperlinkDetector.canCreateHyperLink(expr)) {
            Link link = createHyperlink(tooltipComposite);
            link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
              dispose();
              PomHyperlinkDetector.createHyperlink(expr).open();
            }));
          }
        }
        if(region.getRegions().indexOf(reg) < region.getRegions().size() - 1) {
          createSeparator(composite);
        }
      }

      Point constraints = getSizeConstraints();
      Point contentSize = composite.computeSize(constraints != null ? constraints.x : SWT.DEFAULT, SWT.DEFAULT);

      composite.setSize(new Point(contentSize.x, contentSize.y)); //12 is the magic number for height of status line 

    }

    setColorAndFont(parent, parent.getForeground(), parent.getBackground(), JFaceResources.getDialogFont());

    parent.layout(true);
  }

  protected void fillToolbar(boolean includeLifecycle) {
    ToolBarManager toolBarManager = getToolBarManager();
    if(toolBarManager == null)
      return;
    toolBarManager.add(new OpenPreferencesAction(this, //
        MavenEditorImages.IMGD_WARNINGS, Messages.MarkerHoverControl_openWarningsPrefs, //
        "org.eclipse.m2e.core.ui.preferences.WarningsPreferencePage")); //$NON-NLS-1$

    if(includeLifecycle) {
      toolBarManager.add(new OpenPreferencesAction(this, //
          MavenEditorImages.IMGD_EXECUTION, Messages.MarkerHoverControl_openLifecyclePrefs, //
          "org.eclipse.m2e.core.preferences.LifecycleMappingPreferencePag")); //$NON-NLS-1$
      toolBarManager.add(new OpenPreferencesAction(this, //
          MavenEditorImages.IMGD_DISCOVERY, Messages.MarkerHoverControl_openDiscoveryPrefs, //
          "org.eclipse.m2e.discovery.internal.preferences.DiscoveryPreferencePage")); //$NON-NLS-1$
    }
    toolBarManager.update(true);
  }

  private Link createHyperlink(Composite parent) {
    Link link = new Link(parent, SWT.NONE);
    GridData data2 = new GridData(SWT.FILL, SWT.FILL, true, true);
    data2.horizontalIndent = 18;
    link.setLayoutData(data2);
    link.setText(Messages.PomTextHover_jump_to);
    return link;
  }

  private Composite createTooltipComposite(Composite parent, StyledString text) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    GridLayout layout = new GridLayout(2, false);
    layout.marginHeight = 2;
    layout.marginWidth = 2;
    layout.horizontalSpacing = 0;
    composite.setLayout(layout);

    //this paints the icon..
    final Canvas canvas = new Canvas(composite, SWT.NO_FOCUS);
    GridData gridData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
    gridData.widthHint = 17;
    gridData.heightHint = 16;
    canvas.setLayoutData(gridData);

    //and now comes the text
    StyledText styledtext = new StyledText(composite, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
    GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);

    styledtext.setLayoutData(data);
    styledtext.setText(text.getString());
    styledtext.setStyleRanges(text.getStyleRanges());

    new Label(composite, SWT.NONE);

    return composite;
  }

  private void setColorAndFont(Control control, Color foreground, Color background, Font font) {
    control.setForeground(foreground);
    control.setBackground(background);
    control.setFont(font);

    if(control instanceof Composite) {
      Control[] children = ((Composite) control).getChildren();
      for(Control child : children) {
        setColorAndFont(child, foreground, background, font);
      }
    }
  }

  private void createAnnotationInformation(Composite parent, final MarkerRegion annotation) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    GridLayout layout = new GridLayout(2, false);
    layout.marginHeight = 2;
    layout.marginWidth = 2;
    layout.horizontalSpacing = 0;
    composite.setLayout(layout);

    //this paints the icon..
    final Canvas canvas = new Canvas(composite, SWT.NO_FOCUS);
    GridData gridData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
    gridData.widthHint = 17;
    gridData.heightHint = 16;
    canvas.setLayoutData(gridData);
    canvas.addPaintListener(e -> {
      e.gc.setFont(null);
      markerAccess.paint(annotation.getAnnotation(), e.gc, canvas, new Rectangle(0, 0, 16, 16));
    });

    //and now comes the text
    StyledText text = new StyledText(composite, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
    GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
    text.setLayoutData(data);
    String annotationText = annotation.getAnnotation().getText();
    if(annotationText != null) {
      text.setText(annotationText);
    }
    if(annotation.isDefinedInParent()) {
      new Label(composite, SWT.NONE);

      Link link = new Link(composite, SWT.NONE);
      GridData data2 = new GridData(SWT.FILL, SWT.FILL, true, true);
      data2.horizontalIndent = 18;
      link.setLayoutData(data2);
      link.setText(Messages.MarkerHoverControl_openParentDefinition);
      link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
        PomHyperlinkDetector.createHyperlink(annotation).open();
        dispose();
      }));
    }

  }

  private void createSeparator(Composite parent) {
    Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
    gridData.verticalIndent = 2;
    separator.setLayoutData(gridData);
  }

  private void createResolutionsControl(Composite parent, IMarker mark, List<IMarkerResolution> resolutions) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.verticalSpacing = 2;
    layout.marginHeight = 0;
    composite.setLayout(layout);

    Label quickFixLabel = new Label(composite, SWT.NONE);
    GridData layoutData = new GridData(SWT.BEGINNING, SWT.TOP, false, false);
    layoutData.horizontalIndent = 4;
    quickFixLabel.setLayoutData(layoutData);
    String text;

    if(resolutions.size() == 1) {
      text = Messages.PomTextHover_one_quickfix;
    } else {
      text = NLS.bind(Messages.PomTextHover_more_quickfixes, String.valueOf(resolutions.size()));
    }
    quickFixLabel.setText(text);

    Composite composite2 = new Composite(parent, SWT.NONE);
    composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    GridLayout layout2 = new GridLayout(2, false);
    layout2.marginLeft = 5;
    layout2.verticalSpacing = 2;
    composite2.setLayout(layout2);

    List<Link> list = new ArrayList<>();
    for(IMarkerResolution r : resolutions) {
      list.add(createCompletionProposalLink(composite2, mark, r, 1));// Original link for single fix, hence pass 1 for count

    }
    final Link[] links = list.toArray(new Link[list.size()]);

    focusControl = links.length == 0 ? null : links[0];
    for(int i = 0; i < links.length; i++ ) {
      final int index = i;
      final Link link = links[index];
      link.addKeyListener(new KeyListener() {
        public void keyPressed(KeyEvent e) {
          switch(e.keyCode) {
            case SWT.ARROW_DOWN:
              if(index + 1 < links.length) {
                links[index + 1].setFocus();
              }
              break;
            case SWT.ARROW_UP:
              if(index > 0) {
                links[index - 1].setFocus();
              }
              break;
            default:
              break;
          }
        }

        public void keyReleased(KeyEvent e) {
        }
      });

    }
  }

  private Link createCompletionProposalLink(Composite parent, final IMarker mark, final IMarkerResolution proposal,
      int count) {
    final boolean isMultiFix = count > 1;
    if(isMultiFix) {
      new Label(parent, SWT.NONE); // spacer to fill image cell
      parent = new Composite(parent, SWT.NONE); // indented composite for multi-fix
      GridLayout layout = new GridLayout(2, false);
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      parent.setLayout(layout);
    }

    Label proposalImage = new Label(parent, SWT.NONE);
    proposalImage.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, false, false));
    Image image = null;
    if(proposal instanceof ICompletionProposal) {
      image = ((ICompletionProposal) proposal).getImage();
    } else if(proposal instanceof IMarkerResolution2) {
      image = ((IMarkerResolution2) proposal).getImage();
    }
    if(image != null) {
      proposalImage.setImage(image);

      proposalImage.addMouseListener(MouseListener.mouseUpAdapter(e -> {
        if(e.button == 1) {
          apply(proposal, mark, region.textViewer, region.textOffset);
        }
      }));
    }

    Link proposalLink = new Link(parent, SWT.WRAP);
    GridData layoutData = new GridData(SWT.BEGINNING, SWT.TOP, false, false);
    String linkText;
    if(isMultiFix) {
      linkText = NLS.bind(Messages.PomTextHover_category_fix, Integer.valueOf(count));
    } else {
      linkText = proposal.getLabel();
    }
    proposalLink.setText("<a>" + linkText + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
    proposalLink.setLayoutData(layoutData);
    proposalLink.addSelectionListener(
        SelectionListener.widgetSelectedAdapter(e -> apply(proposal, mark, region.textViewer, region.textOffset)));
    return proposalLink;
  }

  /**
   * {@inheritDoc} This default implementation returns <code>null</code>. Subclasses may override.
   */
  public IInformationControlCreator getInformationPresenterControlCreator() {
    return parent -> new MarkerHoverControl(parent, new ToolBarManager(SWT.FLAT));
  }

  private void apply(IMarkerResolution res, IMarker mark, ITextViewer viewer, int offset) {
    if(res instanceof ICompletionProposal) {
      apply((ICompletionProposal) res, viewer, offset, false);
    } else {
      dispose();
      res.run(mark);
    }
  }

  private void apply(ICompletionProposal p, ITextViewer viewer, int offset, boolean isMultiFix) {
    //Focus needs to be in the text viewer, otherwise linked mode does not work
    dispose();

    IRewriteTarget target = null;
    try {
      IDocument document = viewer.getDocument();

      if(viewer instanceof ITextViewerExtension) {
        ITextViewerExtension extension = (ITextViewerExtension) viewer;
        target = extension.getRewriteTarget();
      }

      if(target != null)
        target.beginCompoundChange();

      if(p instanceof ICompletionProposalExtension2) {
        ICompletionProposalExtension2 e = (ICompletionProposalExtension2) p;
        e.apply(viewer, (char) 0, isMultiFix ? SWT.CONTROL : SWT.NONE, offset);
      } else if(p instanceof ICompletionProposalExtension) {
        ICompletionProposalExtension e = (ICompletionProposalExtension) p;
        e.apply(document, (char) 0, offset);
      } else {
        p.apply(document);
      }

      Point selection = p.getSelection(document);
      if(selection != null) {
        viewer.setSelectedRange(selection.x, selection.y);
        viewer.revealRange(selection.x, selection.y);
      }
    } finally {
      if(target != null)
        target.endCompoundChange();
    }
  }

  private static final class OpenPreferencesAction extends Action {

    private final IInformationControl infoControl;

    private String prefsId;

    public OpenPreferencesAction(IInformationControl infoControl, ImageDescriptor imageDesc, String tooltip,
        String prefsId) {
      this.infoControl = infoControl;
      this.prefsId = prefsId;
      setImageDescriptor(imageDesc);
      setToolTipText(tooltip);
    }

    @Override
    public void run() {
      Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      infoControl.dispose();
      PreferencesUtil.createPreferenceDialogOn(shell, prefsId, null, null).open();
    }
  }
}
