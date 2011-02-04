/*******************************************************************************
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.ToolBarManager;
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
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;

import org.eclipse.m2e.editor.xml.PomHyperlinkDetector;
import org.eclipse.m2e.editor.xml.PomTextHover;
import org.eclipse.m2e.editor.xml.PomHyperlinkDetector.ExpressionRegion;
import org.eclipse.m2e.editor.xml.PomHyperlinkDetector.ManagedArtifactRegion;
import org.eclipse.m2e.editor.xml.PomTextHover.CompoundRegion;
import org.eclipse.m2e.editor.xml.PomTextHover.MarkerRegion;

public class MarkerHoverControl extends AbstractInformationControl implements IInformationControlExtension2, IInformationControlExtension3, IInformationControlExtension5 {
    
    private CompoundRegion region;
    private Control focusControl;
    private Composite parent;
    private final DefaultMarkerAnnotationAccess markerAccess;
    
    
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
      if (input instanceof CompoundRegion) {
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
      if (focusControl != null) {
        focusControl.setFocus();
      }
    }

    /*
     * @see org.eclipse.jface.text.AbstractInformationControl#setVisible(boolean)
     */
    public final void setVisible(boolean visible) {
      if (!visible)
        disposeDeferredCreatedContent();
      super.setVisible(visible);
    }

    protected void disposeDeferredCreatedContent() {
      Control[] children= parent.getChildren();
      for (int i= 0; i < children.length; i++) {
        children[i].dispose();
      }
      ToolBarManager toolBarManager= getToolBarManager();
      if (toolBarManager != null)
        toolBarManager.removeAll();
    }

    /*
     * @see org.eclipse.jface.text.AbstractInformationControl#createContent(org.eclipse.swt.widgets.Composite)
     */
    protected void createContent(Composite parent) {
      this.parent= parent;
      GridLayout layout= new GridLayout(1, false);
      layout.verticalSpacing= 0;
      layout.marginWidth= 0;
      layout.marginHeight= 0;
      parent.setLayout(layout);
    }

    /*
     * @see org.eclipse.jface.text.AbstractInformationControl#computeSizeHint()
     */
    public Point computeSizeHint() {
      Point preferedSize= getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

      Point constrains= getSizeConstraints();
      if (constrains == null)
        return preferedSize;

      Point constrainedSize= getShell().computeSize(constrains.x, SWT.DEFAULT, true);

      int width= Math.min(preferedSize.x, constrainedSize.x);
      int height= Math.max(preferedSize.y, constrainedSize.y);

      return new Point(width, height);
    }

    /**
     * Create content of the hover. This is called after
     * the input has been set.
     */
    protected void deferredCreateContent() {
//      fillToolbar();
      if (region != null) {
        final ScrolledComposite scrolledComposite= new ScrolledComposite(parent, SWT.V_SCROLL);
        GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
        scrolledComposite.setLayoutData(gridData);
        scrolledComposite.setExpandVertical(false);
        scrolledComposite.setExpandHorizontal(false);
        Composite composite = new Composite(scrolledComposite, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);
        scrolledComposite.setContent(composite);
        
        
        for (IRegion reg : region.getRegions()) {
          if (reg instanceof MarkerRegion) {
            createAnnotationInformation(composite, ((MarkerRegion)reg).getAnnotation());
            IMarker mark = ((MarkerRegion)reg).getAnnotation().getMarker();
            IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(mark);
            if (resolutions.length > 0) {
              createResolutionsControl(composite, mark, resolutions);
            }
          }
          if (reg instanceof ManagedArtifactRegion) {
            final ManagedArtifactRegion man = (ManagedArtifactRegion)reg;
            Link link = createHyperlink(composite, PomTextHover.getLabelForRegion(man));
            link.addSelectionListener(new SelectionAdapter() {
              @Override
              public void widgetSelected(SelectionEvent e) {
                dispose();
                PomHyperlinkDetector.createHyperlink(man).open();
              }
            });
            
          }
          if (reg instanceof ExpressionRegion) {
            final ExpressionRegion expr = (ExpressionRegion)reg;
            Link link = createHyperlink(composite, PomTextHover.getLabelForRegion(expr));
            link.addSelectionListener(new SelectionAdapter() {
              @Override
              public void widgetSelected(SelectionEvent e) {
                dispose();
                PomHyperlinkDetector.createHyperlink(expr).open();
              }
            });
          }
          if (region.getRegions().indexOf(reg) < region.getRegions().size() - 1) {
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

    private Link createHyperlink(Composite parent, StyledString text) {
      Composite composite= new Composite(parent, SWT.NONE);
      composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
      GridLayout layout= new GridLayout(2, false);
      layout.marginHeight= 2;
      layout.marginWidth= 2;
      layout.horizontalSpacing= 0;
      composite.setLayout(layout);

      //this paints the icon..
      final Canvas canvas= new Canvas(composite, SWT.NO_FOCUS);
      GridData gridData= new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
      gridData.widthHint= 17;
      gridData.heightHint= 16;
      canvas.setLayoutData(gridData);
      
      //and now comes the text
      StyledText styledtext= new StyledText(composite, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
      GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
      
      styledtext.setLayoutData(data);
      styledtext.setText(text.getString());
      styledtext.setStyleRanges(text.getStyleRanges());
      
      new Label(composite, SWT.NONE);
      
      Link link = new Link(composite, SWT.NONE); 
      GridData data2 = new GridData(SWT.FILL, SWT.FILL, true, true);
      data2.horizontalIndent = 18;
      link.setLayoutData(data2);
      link.setText(Messages.PomTextHover_jump_to);
      return link;
      
    }
  
    private void setColorAndFont(Control control, Color foreground, Color background, Font font) {
      control.setForeground(foreground);
      control.setBackground(background);
      control.setFont(font);

      if (control instanceof Composite) {
        Control[] children= ((Composite) control).getChildren();
        for (int i= 0; i < children.length; i++) {
          setColorAndFont(children[i], foreground, background, font);
        }
      }
    }

    private void createAnnotationInformation(Composite parent, final Annotation annotation) {
      Composite composite= new Composite(parent, SWT.NONE);
      composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
      GridLayout layout= new GridLayout(2, false);
      layout.marginHeight= 2;
      layout.marginWidth= 2;
      layout.horizontalSpacing= 0;
      composite.setLayout(layout);

      //this paints the icon..
      final Canvas canvas= new Canvas(composite, SWT.NO_FOCUS);
      GridData gridData= new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
      gridData.widthHint= 17;
      gridData.heightHint= 16;
      canvas.setLayoutData(gridData);
      canvas.addPaintListener(new PaintListener() {
        public void paintControl(PaintEvent e) {
          e.gc.setFont(null);
          markerAccess.paint(annotation, e.gc, canvas, new Rectangle(0, 0, 16, 16));
        }
      });
      
      //and now comes the text
      StyledText text= new StyledText(composite, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
      GridData data= new GridData(SWT.FILL, SWT.FILL, true, true);
      text.setLayoutData(data);
      String annotationText = annotation.getText();
      if (annotationText != null)
        text.setText(annotationText);
    }
    
    private void createSeparator(Composite parent) {
      Label separator= new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
      GridData gridData= new GridData(SWT.FILL, SWT.TOP, true, false);
      gridData.verticalIndent = 2;
      separator.setLayoutData(gridData);
    }

    private void createResolutionsControl(Composite parent, IMarker mark, IMarkerResolution[] resolutions) {
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      GridLayout layout= new GridLayout(1, false);
      layout.marginWidth = 0;
      layout.verticalSpacing = 2;
      layout.marginHeight = 0;
      composite.setLayout(layout);

      Label quickFixLabel= new Label(composite, SWT.NONE);
      GridData layoutData= new GridData(SWT.BEGINNING, SWT.TOP, false, false);
      layoutData.horizontalIndent= 4;
      quickFixLabel.setLayoutData(layoutData);
      String text;
      
      if (resolutions.length == 1) {
        text= Messages.PomTextHover_one_quickfix;
      } else {
        text= NLS.bind(Messages.PomTextHover_more_quickfixes, String.valueOf(resolutions.length));
      }
      quickFixLabel.setText(text);
      

      Composite composite2= new Composite(parent, SWT.NONE);
      composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      GridLayout layout2= new GridLayout(2, false);
      layout2.marginLeft= 5;
      layout2.verticalSpacing= 2;
      composite2.setLayout(layout2);
      
      List<Link> list= new ArrayList<Link>();
      for (int i= 0; i < resolutions.length; i++) {
        list.add(createCompletionProposalLink(composite2, mark, resolutions[i], 1));// Original link for single fix, hence pass 1 for count
        
      }
      final Link[] links = list.toArray(new Link[list.size()]);


      focusControl= links[0];
      for (int i= 0; i < links.length; i++) {
        final int index= i;
        final Link link= links[index];
        link.addKeyListener(new KeyListener() {
          public void keyPressed(KeyEvent e) {
            switch (e.keyCode) {
              case SWT.ARROW_DOWN:
                if (index + 1 < links.length) {
                  links[index + 1].setFocus();
                }
                break;
              case SWT.ARROW_UP:
                if (index > 0) {
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
    
    private Link createCompletionProposalLink(Composite parent, final IMarker mark, final IMarkerResolution proposal, int count) {
      final boolean isMultiFix= count > 1;
      if (isMultiFix) {
        new Label(parent, SWT.NONE); // spacer to fill image cell
        parent= new Composite(parent, SWT.NONE); // indented composite for multi-fix
        GridLayout layout= new GridLayout(2, false);
        layout.marginWidth= 0;
        layout.marginHeight= 0;
        parent.setLayout(layout);
      }
      
      Label proposalImage= new Label(parent, SWT.NONE);
      proposalImage.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, false, false));
      Image image= null; 
      if (proposal instanceof ICompletionProposal) {
        image = ((ICompletionProposal)proposal).getImage();
      } else if (proposal instanceof IMarkerResolution2) {
        image = ((IMarkerResolution2)proposal).getImage();
      }
      if (image != null) {
        proposalImage.setImage(image);

        proposalImage.addMouseListener(new MouseListener() {

          public void mouseDoubleClick(MouseEvent e) {
          }

          public void mouseDown(MouseEvent e) {
          }

          public void mouseUp(MouseEvent e) {
            if (e.button == 1) {
              apply(proposal, mark, region.textViewer, region.textOffset);
            }
          }
        });
      }

      Link proposalLink = new Link(parent, SWT.WRAP);
      GridData layoutData= new GridData(SWT.BEGINNING, SWT.TOP, false, false);
      String linkText;
      if (isMultiFix) {
        linkText = NLS.bind(Messages.PomTextHover_category_fix, new Integer(count));
      } else {
        linkText = proposal.getLabel();
      }
      proposalLink.setText("<a>" + linkText + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
      proposalLink.setLayoutData(layoutData);
      proposalLink.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          apply(proposal, mark, region.textViewer, region.textOffset);
        }
      });
      return proposalLink;
    }
    
    /**
     * {@inheritDoc}
     * This default implementation returns <code>null</code>. Subclasses may override.
     */
    public IInformationControlCreator getInformationPresenterControlCreator() {
      return new IInformationControlCreator() {
        /*
         * @see org.eclipse.jface.text.IInformationControlCreator#createInformationControl(org.eclipse.swt.widgets.Shell)
         */
        public IInformationControl createInformationControl(Shell parent) {
          return new MarkerHoverControl(parent);
        }
      };
    }
    
    private void apply(IMarkerResolution res, IMarker mark, ITextViewer viewer, int offset) {
      if (res instanceof ICompletionProposal) {
        apply((ICompletionProposal)res, viewer, offset, false);
      } else {
        dispose();
        res.run(mark);
      }
    }

    private void apply(ICompletionProposal p, ITextViewer viewer, int offset, boolean isMultiFix) {
      //Focus needs to be in the text viewer, otherwise linked mode does not work
      dispose();

      IRewriteTarget target= null;
      try {
        IDocument document= viewer.getDocument();

        if (viewer instanceof ITextViewerExtension) {
          ITextViewerExtension extension= (ITextViewerExtension) viewer;
          target= extension.getRewriteTarget();
        }

        if (target != null)
          target.beginCompoundChange();

        if (p instanceof ICompletionProposalExtension2) {
          ICompletionProposalExtension2 e= (ICompletionProposalExtension2) p;
          e.apply(viewer, (char) 0, isMultiFix ? SWT.CONTROL : SWT.NONE, offset);
        } else if (p instanceof ICompletionProposalExtension) {
          ICompletionProposalExtension e= (ICompletionProposalExtension) p;
          e.apply(document, (char) 0, offset);
        } else {
          p.apply(document);
        }

        Point selection= p.getSelection(document);
        if (selection != null) {
          viewer.setSelectedRange(selection.x, selection.y);
          viewer.revealRange(selection.x, selection.y);
        }
      } finally {
        if (target != null)
          target.endCompoundChange();
      }
    }
    
  }