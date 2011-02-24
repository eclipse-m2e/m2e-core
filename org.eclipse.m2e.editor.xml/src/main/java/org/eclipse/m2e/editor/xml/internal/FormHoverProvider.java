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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension3;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.m2e.editor.xml.PomHyperlinkDetector;
import org.eclipse.m2e.editor.xml.PomHyperlinkDetector.MarkerRegion;
import org.eclipse.m2e.editor.xml.PomTextHover;

public final class FormHoverProvider {

  public static Execute createHoverRunnable(final Shell parentShell, final IMarker[] markers, final ISourceViewer sourceViewer) {
    if (markers.length > 0) {
    return new Execute() {
      public void run(Point position) {
        List<IMarker> markersSet = Arrays.asList(markers);
        PomTextHover.CompoundRegion compound = new PomTextHover.CompoundRegion(sourceViewer, 0);
        Iterator<Annotation> it = sourceViewer.getAnnotationModel().getAnnotationIterator();
        while (it.hasNext()) {
          Annotation ann = it.next();
          if (ann instanceof MarkerAnnotation) {
            MarkerAnnotation mann = (MarkerAnnotation) ann;
            if (markersSet.contains(mann.getMarker()));
              compound.addRegion(new PomHyperlinkDetector.MarkerRegion(0, 0, mann));
          }
        }
        final MarkerHoverControl mhc = new MarkerHoverControl(parentShell);
        final Display fDisplay= mhc.getMyShell().getDisplay();
        
        final Listener displayListener = new Listener() {
          public void handleEvent(Event event) {
            if (event.type == SWT.MouseMove) {
              if (!(event.widget instanceof Control) || event.widget.isDisposed())
                return;

              IInformationControl infoControl= mhc;
              if (infoControl != null && !infoControl.isFocusControl() && infoControl instanceof IInformationControlExtension3) {
                IInformationControlExtension3 iControl3= (IInformationControlExtension3) infoControl;
                Rectangle controlBounds= iControl3.getBounds();
                if (controlBounds != null) {
                  Point mouseLoc= event.display.map((Control) event.widget, null, event.x, event.y);
                  int margin= 20;
                  Geometry.expand(controlBounds, margin, margin, margin, margin);
                  if (!controlBounds.contains(mouseLoc)) {
                    mhc.setVisible(false);
                  }
                }

//              } else {
//                System.out.println("removing mouse move..");
//                /*
//                 * TODO: need better understanding of why/if this is needed.
//                 * Looks like the same panic code we have in org.eclipse.jface.text.AbstractHoverInformationControlManager.Closer.handleMouseMove(Event)
//                 */
//                if (fDisplay != null && !fDisplay.isDisposed())
//                  fDisplay.removeFilter(SWT.MouseMove, this);
              }

            } else if (event.type == SWT.FocusOut) {
              IInformationControl iControl= mhc;
              if (iControl != null && ! iControl.isFocusControl())
                mhc.setVisible(false);
            }
          }
        };
        
        mhc.setLocation(new Point(position.x, position.y));
        mhc.setSizeConstraints(400, 400);
        mhc.setInput(compound);
        Point hint = mhc.computeSizeHint();
        mhc.setSize(hint.x, Math.min(hint.y, 400));
        if (!fDisplay.getBounds().contains(position.x + hint.x, position.y)) {
          mhc.setLocation(new Point(position.x - (position.x + hint.x - fDisplay.getBounds().width), position.y));
        }
//        mhc.getMyShell().addShellListener(new ShellAdapter() {
//          public void shellActivated(ShellEvent e) {
//            mhc.setFocus();
//          }
//        });
        if (!fDisplay.isDisposed()) {
          fDisplay.addFilter(SWT.MouseMove, displayListener);
          fDisplay.addFilter(SWT.FocusOut, displayListener);
        }
        mhc.addDisposeListener(new DisposeListener() {
          public void widgetDisposed(DisposeEvent e) {
            fDisplay.removeFilter(SWT.MouseMove, displayListener);
            fDisplay.removeFilter(SWT.FocusOut, displayListener);
          }
        });
        mhc.setVisible(true);
      }
    };
    } else {
      return null;
    }
  }

  public static interface Execute {
    void run(Point location);
  }
}
