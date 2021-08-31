/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.search.util;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;


/**
 * ControlDecoration renders an image decoration near a control. It allows clients to specify an image and a position
 * for the image relative to the control. A ControlDecoration may be assigned description text, which can optionally be
 * shown when the user hovers over the image. Clients can decorate any kind of control.
 * <p>
 * Decoration images always appear on the left or right side of the field, never above or below it. Decorations can be
 * positioned at the top, center, or bottom of either side of the control. Future implementations may provide additional
 * positioning options for decorations.
 * <p>
 * ControlDecoration renders the image adjacent to the specified (already created) control, with no guarantee that it
 * won't be clipped or otherwise obscured or overlapped by adjacent controls, including another ControlDecoration placed
 * in the same location. Clients should ensure that there is adequate space adjacent to the control to show the
 * decoration properly.
 * <p>
 * Clients using ControlDecoration should typically ensure that enough margin space is reserved for a decoration by
 * altering the layout data margins, although this is not assumed or required by the ControlDecoration implementation.
 * <p>
 * This class is intended to be instantiated and used by clients. It is not intended to be subclassed by clients.
 *
 * @since 3.3
 * @see FieldDecoration
 * @see FieldDecorationRegistry
 */
public class ControlDecoration {
  /**
   * Debug flag for tracing
   */
  private static boolean DEBUG = false;

  /**
   * Cached platform flags for dealing with platform-specific issues.
   */
  static boolean CARBON = "carbon".equals(SWT.getPlatform()); //$NON-NLS-1$

  /**
   * The associated control
   */
  Control control;

  /**
   * The composite on which to render the decoration and hook mouse events, or null if we are hooking all parent
   * composites.
   */
  private final Composite composite;

  /**
   * The associated image.
   */
  private Image image;

  /**
   * The associated description text.
   */
  private String descriptionText;

  /**
   * The position of the decoration.
   */
  private final int position;

  /**
   * The decoration's visibility flag
   */
  boolean visible = true;

  /**
   * Boolean indicating whether the decoration should only be shown when the control has focus
   */
  boolean showOnlyOnFocus = false;

  /**
   * Boolean indicating whether the decoration should show its description text in a hover when the user hovers over the
   * decoration.
   */
  boolean showHover = true;

  /**
   * Margin width used between the decorator and the control.
   */
  private int marginWidth = 0;

  /**
   * Registered selection listeners.
   */
  ListenerList<SelectionListener> selectionListeners = new ListenerList<>();

  /**
   * Registered menu detect listeners.
   */
  ListenerList<MenuDetectListener> menuDetectListeners = new ListenerList<>();

  /**
   * The focus listener
   */
  private FocusListener focusListener;

  /**
   * The dispose listener
   */
  private DisposeListener disposeListener;

  /**
   * The paint listener installed for drawing the decoration
   */
  private PaintListener paintListener;

  /**
   * The mouse listener installed for tracking the hover
   */
  private MouseTrackListener mouseTrackListener;

  /**
   * The mouse move listener installed for tracking the hover
   */
  MouseMoveListener mouseMoveListener;

  /**
   * The untyped listener installed for notifying external listeners
   */
  private Listener compositeListener;

  /**
   * Control that we last installed a move listener on. We only want one at a time.
   */
  Control moveListeningTarget = null;

  /**
   * Debug counter used to match add and remove listeners
   */
  private int listenerInstalls = 0;

  /**
   * The current rectangle used for tracking mouse moves
   */
  Rectangle decorationRectangle;

  /**
   * An internal flag tracking whether we have focus. We use this rather than isFocusControl() so that we can set the
   * flag as soon as we get the focus callback, rather than having to do an asyncExec in the middle of a focus callback
   * to ensure that isFocusControl() represents the outcome of the event.
   */
  boolean hasFocus = false;

  /**
   * The hover used for showing description text
   */
  private Hover hover;

  /**
   * The hover used to show a decoration image's description.
   */
  class Hover {
    private static final String EMPTY = ""; //$NON-NLS-1$

    /**
     * Offset of info hover arrow from the left or right side.
     */
    private final int hao = 10;

    /**
     * Width of info hover arrow.
     */
    private final int haw = 8;

    /**
     * Height of info hover arrow.
     */
    private final int hah = 10;

    /**
     * Margin around info hover text.
     */
    int hm = 2;

    /**
     * This info hover's shell.
     */
    Shell hoverShell;

    /**
     * The info hover text.
     */
    String text = EMPTY;

    /**
     * The region used to manage the shell shape
     */
    Region region;

    /**
     * Boolean indicating whether the last computed polygon location had an arrow on left. (true if left, false if
     * right).
     */
    boolean arrowOnLeft = true;

    /*
     * Create a hover parented by the specified shell.
     */
    Hover(Shell parent) {
      final Display display = parent.getDisplay();
      hoverShell = new Shell(parent, SWT.NO_TRIM | SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
      hoverShell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
      hoverShell.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
      hoverShell.addPaintListener(pe -> {
        pe.gc.drawText(text, hm, hm);
        if(!CARBON) {
          pe.gc.drawPolygon(getPolygon(true));
        }
      });
      hoverShell.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseDown(MouseEvent e) {
          hideHover();
        }
      });
    }

    /*
     * Compute a polygon that represents a hover with an arrow pointer. If
     * border is true, compute the polygon inset by 1-pixel border. Consult
     * the arrowOnLeft flag to determine which side the arrow is on.
     */
    int[] getPolygon(boolean border) {
      Point e = getExtent();
      int b = border ? 1 : 0;
      if(arrowOnLeft) {
        return new int[] {0, 0, e.x - b, 0, e.x - b, e.y - b, hao + haw, e.y - b, hao + haw / 2, e.y + hah - b, hao,
            e.y - b, 0, e.y - b, 0, 0};
      }
      return new int[] {0, 0, e.x - b, 0, e.x - b, e.y - b, e.x - hao - b, e.y - b, e.x - hao - haw / 2, e.y + hah - b,
          e.x - hao - haw, e.y - b, 0, e.y - b, 0, 0};
    }

    /*
     * Dispose the hover, it is no longer needed. Dispose any resources
     * allocated by the hover.
     */
    void dispose() {
      if(!hoverShell.isDisposed()) {
        hoverShell.dispose();
      }
      if(region != null) {
        region.dispose();
      }
    }

    /*
     * Set the visibility of the hover.
     */
    void setVisible(boolean visible) {
      if(visible) {
        if(!hoverShell.isVisible()) {
          hoverShell.setVisible(true);
        }
      } else {
        if(hoverShell.isVisible()) {
          hoverShell.setVisible(false);
        }
      }
    }

    /*
     * Set the text of the hover to the specified text. Recompute the size
     * and location of the hover to hover near the decoration rectangle,
     * pointing the arrow toward the target control.
     */
    void setText(String t, Rectangle decorationRectangle, Control targetControl) {
      if(t == null) {
        t = EMPTY;
      }
      if(!t.equals(text)) {
        Point oldSize = getExtent();
        text = t;
        hoverShell.redraw();
        Point newSize = getExtent();
        if(!oldSize.equals(newSize)) {
          // set a flag that indicates the direction of arrow
          arrowOnLeft = decorationRectangle.x <= targetControl.getLocation().x;
          setNewShape();
        }
      }

      Point extent = getExtent();
      int y = -extent.y - hah + 1;
      int x = arrowOnLeft ? -hao + haw / 2 : -extent.x + hao + haw / 2;

      hoverShell.setLocation(control.getParent().toDisplay(decorationRectangle.x + x, decorationRectangle.y + y));
    }

    /*
     * Return whether or not the hover (shell) is visible.
     */
    boolean isVisible() {
      return hoverShell.isVisible();
    }

    /*
     * Compute the extent of the hover for the current text.
     */
    Point getExtent() {
      GC gc = new GC(hoverShell);
      Point e = gc.textExtent(text);
      gc.dispose();
      e.x += hm * 2;
      e.y += hm * 2;
      return e;
    }

    /*
     * Compute a new shape for the hover shell.
     */
    void setNewShape() {
      Region oldRegion = region;
      region = new Region();
      region.add(getPolygon(false));
      hoverShell.setRegion(region);
      if(oldRegion != null) {
        oldRegion.dispose();
      }

    }
  }

  /**
   * Construct a ControlDecoration for decorating the specified control at the specified position relative to the
   * control. Render the decoration on top of any Control that happens to appear at the specified location.
   * <p>
   * SWT constants are used to specify the position of the decoration relative to the control. The position should
   * include style bits describing both the vertical and horizontal orientation. <code>SWT.LEFT</code> and
   * <code>SWT.RIGHT</code> describe the horizontal placement of the decoration relative to the control, and the
   * constants <code>SWT.TOP</code>, <code>SWT.CENTER</code>, and <code>SWT.BOTTOM</code> describe the vertical
   * alignment of the decoration relative to the control. Decorations always appear on either the left or right side of
   * the control, never above or below it. For example, a decoration appearing on the left side of the field, at the
   * top, is specified as SWT.LEFT | SWT.TOP. If no position style bits are specified, the control decoration will be
   * positioned to the left and center of the control (<code>SWT.LEFT | SWT.CENTER</code>).
   * </p>
   *
   * @param control the control to be decorated
   * @param position bit-wise or of position constants (<code>SWT.TOP</code>, <code>SWT.BOTTOM</code>,
   *          <code>SWT.LEFT</code>, <code>SWT.RIGHT</code>, and <code>SWT.CENTER</code>).
   */
  public ControlDecoration(Control control, int position) {
    this(control, position, null);

  }

  /**
   * Construct a ControlDecoration for decorating the specified control at the specified position relative to the
   * control. Render the decoration only on the specified Composite or its children. The decoration will be clipped if
   * it does not appear within the visible bounds of the composite or its child composites.
   * <p>
   * SWT constants are used to specify the position of the decoration relative to the control. The position should
   * include style bits describing both the vertical and horizontal orientation. <code>SWT.LEFT</code> and
   * <code>SWT.RIGHT</code> describe the horizontal placement of the decoration relative to the control, and the
   * constants <code>SWT.TOP</code>, <code>SWT.CENTER</code>, and <code>SWT.BOTTOM</code> describe the vertical
   * alignment of the decoration relative to the control. Decorations always appear on either the left or right side of
   * the control, never above or below it. For example, a decoration appearing on the left side of the field, at the
   * top, is specified as SWT.LEFT | SWT.TOP. If no position style bits are specified, the control decoration will be
   * positioned to the left and center of the control (<code>SWT.LEFT | SWT.CENTER</code>).
   * </p>
   *
   * @param control the control to be decorated
   * @param position bit-wise or of position constants (<code>SWT.TOP</code>, <code>SWT.BOTTOM</code>,
   *          <code>SWT.LEFT</code>, <code>SWT.RIGHT</code>, and <code>SWT.CENTER</code>).
   * @param composite The SWT composite within which the decoration should be rendered. The decoration will be clipped
   *          to this composite, but it may be rendered on a child of the composite. The decoration will not be visible
   *          if the specified composite or its child composites are not visible in the space relative to the control,
   *          where the decoration is to be rendered. If this value is <code>null</code>, then the decoration will be
   *          rendered on whichever composite (or composites) are located in the specified position.
   */
  public ControlDecoration(Control control, int position, Composite composite) {
    this.position = position;
    this.control = control;
    this.composite = composite;

    addControlListeners();

  }

  /**
   * Adds the listener to the collection of listeners who will be notified when the platform-specific context menu
   * trigger has occurred, by sending it one of the messages defined in the <code>MenuDetectListener</code> interface.
   * <p>
   * The <code>widget</code> field in the SelectionEvent will contain the Composite on which the decoration is rendered
   * that received the click. The <code>x</code> and <code>y</code> fields will be in coordinates relative to the
   * display. The <code>data</code> field will contain the decoration that received the event.
   * </p>
   *
   * @param listener the listener which should be notified
   * @see org.eclipse.swt.events.MenuDetectListener
   * @see org.eclipse.swt.events.MenuDetectEvent
   * @see #removeMenuDetectListener
   */
  public void addMenuDetectListener(MenuDetectListener listener) {
    menuDetectListeners.add(listener);
  }

  /**
   * Removes the listener from the collection of listeners who will be notified when the platform-specific context menu
   * trigger has occurred.
   *
   * @param listener the listener which should no longer be notified. This message has no effect if the listener was not
   *          previously added to the receiver.
   * @see org.eclipse.swt.events.MenuDetectListener
   * @see #addMenuDetectListener
   */
  public void removeMenuDetectListener(MenuDetectListener listener) {
    menuDetectListeners.remove(listener);
  }

  /**
   * Adds the listener to the collection of listeners who will be notified when the decoration is selected, by sending
   * it one of the messages defined in the <code>SelectionListener</code> interface.
   * <p>
   * <code>widgetSelected</code> is called when the decoration is selected (by mouse click).
   * <code>widgetDefaultSelected</code> is called when the decoration is double-clicked.
   * </p>
   * <p>
   * The <code>widget</code> field in the SelectionEvent will contain the Composite on which the decoration is rendered
   * that received the click. The <code>x</code> and <code>y</code> fields will be in coordinates relative to that
   * widget. The <code>data</code> field will contain the decoration that received the event.
   * </p>
   *
   * @param listener the listener which should be notified
   * @see org.eclipse.swt.events.SelectionListener
   * @see org.eclipse.swt.events.SelectionEvent
   * @see #removeSelectionListener
   */
  public void addSelectionListener(SelectionListener listener) {
    selectionListeners.add(listener);
  }

  /**
   * Removes the listener from the collection of listeners who will be notified when the decoration is selected.
   *
   * @param listener the listener which should no longer be notified. This message has no effect if the listener was not
   *          previously added to the receiver.
   * @see org.eclipse.swt.events.SelectionListener
   * @see #addSelectionListener
   */
  public void removeSelectionListener(SelectionListener listener) {
    selectionListeners.remove(listener);
  }

  /**
   * Dispose this ControlDecoration. Unhook any listeners that have been installed on the target control. This method
   * has no effect if the receiver is already disposed.
   */
  public void dispose() {
    if(control == null) {
      return;
    }
    if(hover != null) {
      hover.dispose();
      hover = null;
    }
    removeControlListeners();
    control = null;
  }

  /**
   * Get the control that is decorated by the receiver.
   *
   * @return the Control decorated by the receiver. May be <code>null</code> if the control has been uninstalled.
   */
  public Control getControl() {
    return control;
  }

  /**
   * Add any listeners needed on the target control and on the composite where the decoration is to be rendered.
   */
  private void addControlListeners() {
    disposeListener = event -> dispose();
    printAddListener(control, "DISPOSE"); //$NON-NLS-1$
    control.addDisposeListener(disposeListener);

    focusListener = new FocusListener() {
      @Override
      public void focusGained(FocusEvent event) {
        hasFocus = true;
        if(showOnlyOnFocus) {
          update();
        }
      }

      @Override
      public void focusLost(FocusEvent event) {
        hasFocus = false;
        if(showOnlyOnFocus) {
          update();
        }
      }
    };
    printAddListener(control, "FOCUS"); //$NON-NLS-1$
    control.addFocusListener(focusListener);

    // Listener for painting the decoration
    paintListener = event -> {
      Control control = (Control) event.widget;
      Rectangle rect = getDecorationRectangle(control);
      if(shouldShowDecoration()) {
        event.gc.drawImage(getImage(), rect.x, rect.y);
      }
    };

    // Listener for tracking the end of a hover. Only installed
    // after a hover begins.
    mouseMoveListener = event -> {
      if(showHover) {
        if(!decorationRectangle.contains(event.x, event.y)) {
          hideHover();
          // No need to listen any longer
          printRemoveListener(event.widget, "MOUSEMOVE"); //$NON-NLS-1$
          ((Control) event.widget).removeMouseMoveListener(mouseMoveListener);
          moveListeningTarget = null;
        }
      }
    };

    // Listener for tracking the beginning of a hover. Always installed.
    mouseTrackListener = new MouseTrackListener() {
      @Override
      public void mouseExit(MouseEvent event) {
        // Just in case we didn't catch it before.
        Control target = (Control) event.widget;
        if(target == moveListeningTarget) {
          printRemoveListener(target, "MOUSEMOVE"); //$NON-NLS-1$
          target.removeMouseMoveListener(mouseMoveListener);
          moveListeningTarget = null;
        }
        hideHover();
      }

      @Override
      public void mouseHover(MouseEvent event) {
        if(showHover) {
          decorationRectangle = getDecorationRectangle((Control) event.widget);
          if(decorationRectangle.contains(event.x, event.y)) {
            showHoverText(getDescriptionText());
            Control target = (Control) event.widget;
            if(moveListeningTarget == null) {
              printAddListener(target, "MOUSEMOVE"); //$NON-NLS-1$
              target.addMouseMoveListener(mouseMoveListener);
              moveListeningTarget = target;
            } else if(target != moveListeningTarget) {
              printRemoveListener(moveListeningTarget, "MOUSEMOVE"); //$NON-NLS-1$
              moveListeningTarget.removeMouseMoveListener(mouseMoveListener);
              printAddListener(target, "MOUSEMOVE"); //$NON-NLS-1$
              target.addMouseMoveListener(mouseMoveListener);
              moveListeningTarget = target;
            } else {
              // It is already installed on this control.
            }
          }
        }
      }

      @Override
      public void mouseEnter(MouseEvent event) {
        // Nothing to do until a hover occurs.
      }
    };

    compositeListener = event -> {
      // Don't forward events if decoration is not showing
      if(!visible) {
        return;
      }
      // Notify listeners if any are registered.
      switch(event.type) {
        case SWT.MouseDown:
          if(!selectionListeners.isEmpty())
            notifySelectionListeners(event);
          break;
        case SWT.MouseDoubleClick:
          if(!selectionListeners.isEmpty())
            notifySelectionListeners(event);
          break;
        case SWT.MenuDetect:
          if(!menuDetectListeners.isEmpty())
            notifyMenuDetectListeners(event);
          break;
      }
    };

    // We do not know which parent in the control hierarchy
    // is providing the decoration space, so hook all the way up, until
    // the shell or the specified parent composite is reached.
    Composite c = control.getParent();
    while(c != null) {
      installCompositeListeners(c);
      if(composite != null && composite == c) {
        // We just installed on the specified composite, so stop.
        c = null;
      } else if(c instanceof Shell) {
        // We just installed on a shell, so don't go further
        c = null;
      } else {
        c = c.getParent();
      }
    }
    // force a redraw of the decoration area so our paint listener
    // is notified.
    update();
  }

  /*
   * Install the listeners used to paint and track mouse events on the
   * composite.
   */
  private void installCompositeListeners(Composite c) {
    if(!c.isDisposed()) {
      printAddListener(c, "PAINT"); //$NON-NLS-1$
      c.addPaintListener(paintListener);
      printAddListener(c, "MOUSETRACK"); //$NON-NLS-1$
      c.addMouseTrackListener(mouseTrackListener);
      printAddListener(c, "SWT.MenuDetect"); //$NON-NLS-1$
      c.addListener(SWT.MenuDetect, compositeListener);
      printAddListener(c, "SWT.MouseDown"); //$NON-NLS-1$
      c.addListener(SWT.MouseDown, compositeListener);
      printAddListener(c, "SWT.MouseDoubleClick"); //$NON-NLS-1$
      c.addListener(SWT.MouseDoubleClick, compositeListener);
    }
  }

  /*
   * Remove the listeners used to paint and track mouse events on the
   * composite.
   */
  private void removeCompositeListeners(Composite c) {
    if(!c.isDisposed()) {
      printRemoveListener(c, "PAINT"); //$NON-NLS-1$
      c.removePaintListener(paintListener);
      printRemoveListener(c, "MOUSETRACK"); //$NON-NLS-1$
      c.removeMouseTrackListener(mouseTrackListener);
      printRemoveListener(c, "SWT.MenuDetect"); //$NON-NLS-1$
      c.removeListener(SWT.MenuDetect, compositeListener);
      printRemoveListener(c, "SWT.MouseDown"); //$NON-NLS-1$
      c.removeListener(SWT.MouseDown, compositeListener);
      printRemoveListener(c, "SWT.MouseDoubleClick"); //$NON-NLS-1$
      c.removeListener(SWT.MouseDoubleClick, compositeListener);
    }
  }

  void notifySelectionListeners(Event event) {
    if(!(event.widget instanceof Control)) {
      return;
    }
    if(getDecorationRectangle((Control) event.widget).contains(event.x, event.y)) {
      SelectionEvent clientEvent = new SelectionEvent(event);
      clientEvent.data = this;
      if(getImage() != null) {
        clientEvent.height = getImage().getBounds().height;
        clientEvent.width = getImage().getBounds().width;
      }
      Object[] listeners;
      switch(event.type) {
        case SWT.MouseDoubleClick:
          if(event.button == 1) {
            listeners = selectionListeners.getListeners();
            for(Object listener : listeners) {
              ((SelectionListener) listener).widgetDefaultSelected(clientEvent);
            }
          }
          break;
        case SWT.MouseDown:
          if(event.button == 1) {
            listeners = selectionListeners.getListeners();
            for(Object listener : listeners) {
              ((SelectionListener) listener).widgetSelected(clientEvent);
            }
          }
          break;
      }
    }
  }

  void notifyMenuDetectListeners(Event event) {
    if(getDecorationRectangle(null).contains(event.x, event.y)) {
      MenuDetectEvent clientEvent = new MenuDetectEvent(event);
      clientEvent.data = this;
      Object[] listeners = menuDetectListeners.getListeners();
      for(Object listener : listeners) {
        ((MenuDetectListener) listener).menuDetected(clientEvent);

      }
    }
  }

  /**
   * Show the specified text using the same hover dialog as is used to show decorator descriptions. When
   * {@link #setShowHover(boolean)} has been set to <code>true</code>, a decoration's description text will be shown in
   * an info hover over the field's control whenever the mouse hovers over the decoration. This method can be used to
   * show a decoration's description text at other times (such as when the control receives focus), or to show other
   * text associated with the field. The hover will not be shown if the decoration is hidden.
   *
   * @param text the text to be shown in the info hover, or <code>null</code> if no text should be shown.
   */
  public void showHoverText(String text) {
    if(control == null) {
      return;
    }
    showHoverText(text, control);
  }

  /**
   * Hide any hover popups that are currently showing on the control. When {@link #setShowHover(boolean)} has been set
   * to <code>true</code>, a decoration's description text will be shown in an info hover over the field's control as
   * long as the mouse hovers over the decoration, and will be hidden when the mouse exits the decoration. This method
   * can be used to hide a hover, whether it was shown explicitly using {@link #showHoverText(String)}, or was showing
   * because the user was hovering in the decoration.
   * <p>
   * This message has no effect if there is no current hover.
   */
  public void hideHover() {
    if(hover != null) {
      hover.setVisible(false);
    }
  }

  /**
   * Show the control decoration. This message has no effect if the decoration is already showing. If
   * {@link #setShowOnlyOnFocus(boolean)} is set to <code>true</code>, the decoration will only be shown if the control
   * has focus.
   */
  public void show() {
    if(!visible) {
      visible = true;
      update();
    }
  }

  /**
   * Hide the control decoration and any associated hovers. This message has no effect if the decoration is already
   * hidden.
   */
  public void hide() {
    if(visible) {
      visible = false;
      hideHover();
      update();
    }
  }

  /**
   * Get the description text that may be shown in a hover for this decoration.
   *
   * @return the text to be shown as a description for the decoration, or <code>null</code> if none has been set.
   */
  public String getDescriptionText() {
    return descriptionText;
  }

  /**
   * Set the image shown in this control decoration. Update the rendered decoration.
   *
   * @param text the text to be shown as a description for the decoration, or <code>null</code> if none has been set.
   */
  public void setDescriptionText(String text) {
    this.descriptionText = text;
    update();
  }

  /**
   * Get the image shown in this control decoration.
   *
   * @return the image to be shown adjacent to the control, or <code>null</code> if one has not been set.
   */
  public Image getImage() {
    return image;
  }

  /**
   * Set the image shown in this control decoration. Update the rendered decoration.
   *
   * @param image the image to be shown adjacent to the control. Should never be <code>null</code>.
   */
  public void setImage(Image image) {
    this.image = image;
    update();
  }

  /**
   * Get the boolean that controls whether the decoration is shown only when the control has focus. The default value of
   * this setting is <code>false</code>.
   *
   * @return <code>true</code> if the decoration should only be shown when the control has focus, and <code>false</code>
   *         if it should always be shown. Note that if the control is not capable of receiving focus (
   *         <code>SWT.NO_FOCUS</code>), then the decoration will never show when this value is <code>true</code>.
   */
  public boolean getShowOnlyOnFocus() {
    return showOnlyOnFocus;
  }

  /**
   * Set the boolean that controls whether the decoration is shown only when the control has focus. The default value of
   * this setting is <code>false</code>.
   *
   * @param showOnlyOnFocus <code>true</code> if the decoration should only be shown when the control has focus, and
   *          <code>false</code> if it should always be shown. Note that if the control is not capable of receiving
   *          focus (<code>SWT.NO_FOCUS</code>), then the decoration will never show when this value is
   *          <code>true</code>.
   */
  public void setShowOnlyOnFocus(boolean showOnlyOnFocus) {
    this.showOnlyOnFocus = showOnlyOnFocus;
    update();
  }

  /**
   * Get the boolean that controls whether the decoration's description text should be shown in a hover when the user
   * hovers over the decoration. The default value of this setting is <code>true</code>.
   *
   * @return <code>true</code> if a hover popup containing the decoration's description text should be shown when the
   *         user hovers over the decoration, and <code>false</code> if a hover should not be shown.
   */
  public boolean getShowHover() {
    return showHover;
  }

  /**
   * Set the boolean that controls whether the decoration's description text should be shown in a hover when the user
   * hovers over the decoration. The default value of this setting is <code>true</code>.
   *
   * @param showHover <code>true</code> if a hover popup containing the decoration's description text should be shown
   *          when the user hovers over the decoration, and <code>false</code> if a hover should not be shown.
   */
  public void setShowHover(boolean showHover) {
    this.showHover = showHover;
    update();
  }

  /**
   * Get the margin width in pixels that should be used between the decorator and the horizontal edge of the control.
   * The default value of this setting is <code>0</code>.
   *
   * @return the number of pixels that should be reserved between the horizontal edge of the control and the adjacent
   *         edge of the decoration.
   */
  public int getMarginWidth() {
    return marginWidth;
  }

  /**
   * Set the margin width in pixels that should be used between the decorator and the horizontal edge of the control.
   * The default value of this setting is <code>0</code>.
   *
   * @param marginWidth the number of pixels that should be reserved between the horizontal edge of the control and the
   *          adjacent edge of the decoration.
   */
  public void setMarginWidth(int marginWidth) {
    this.marginWidth = marginWidth;
    update();
  }

  /**
   * Something has changed, requiring redraw. Redraw the decoration and update the hover text if appropriate.
   */
  protected void update() {
    if(control == null || control.isDisposed()) {
      return;
    }
    Rectangle rect = getDecorationRectangle(control.getShell());
    // Redraw this rectangle in all children
    control.getShell().redraw(rect.x, rect.y, rect.width, rect.height, true);
    control.getShell().update();
    if(hover != null && getDescriptionText() != null) {
      hover.setText(getDescriptionText(), getDecorationRectangle(control.getParent()), control);
    }
  }

  /*
   * Show the specified text in the hover, positioning the hover near the
   * specified control.
   */
  private void showHoverText(String text, Control hoverNear) {
    // If we aren't to show a hover, don't do anything.
    if(!showHover) {
      return;
    }

    // If we are not visible, don't show the hover.
    if(!visible) {
      return;
    }
    // If there is no text, don't do anything.
    if(text == null) {
      hideHover();
      return;
    }

    // If there is no control, nothing to do
    if(control == null) {
      return;
    }
    // Create the hover if it's not showing
    if(hover == null) {
      hover = new Hover(hoverNear.getShell());
    }
    hover.setText(text, getDecorationRectangle(control.getParent()), control);
    hover.setVisible(true);
  }

  /*
   * Remove any listeners installed on the controls.
   */
  private void removeControlListeners() {
    if(control == null) {
      return;
    }
    printRemoveListener(control, "FOCUS"); //$NON-NLS-1$
    control.removeFocusListener(focusListener);
    focusListener = null;

    printRemoveListener(control, "DISPOSE"); //$NON-NLS-1$
    control.removeDisposeListener(disposeListener);
    disposeListener = null;

    Composite c = control.getParent();
    while(c != null) {
      removeCompositeListeners(c);
      if(composite != null && composite == c) {
        // We previously installed listeners only to the specified
        // composite, so stop.
        c = null;
      } else if(c instanceof Shell) {
        // We previously installed listeners only up to the first Shell
        // encountered, so stop.
        c = null;
      } else {
        c = c.getParent();
      }
    }
    paintListener = null;
    mouseTrackListener = null;
    compositeListener = null;

    // We may have a remaining mouse move listener installed
    if(moveListeningTarget != null) {
      printRemoveListener(moveListeningTarget, "MOUSEMOVE"); //$NON-NLS-1$
      moveListeningTarget.removeMouseMoveListener(mouseMoveListener);
      moveListeningTarget = null;
      mouseMoveListener = null;
    }
    if(DEBUG) {
      if(listenerInstalls > 0) {
        System.out.println("LISTENER LEAK>>>CHECK TRACE ABOVE"); //$NON-NLS-1$
      } else if(listenerInstalls < 0) {
        System.out.println("REMOVED UNREGISTERED LISTENERS>>>CHECK TRACE ABOVE"); //$NON-NLS-1$
      } else {
        System.out.println("ALL INSTALLED LISTENERS WERE REMOVED."); //$NON-NLS-1$
      }
    }
  }

  /**
   * Return the rectangle in which the decoration should be rendered, in coordinates relative to the specified control.
   * If the specified control is null, return the rectangle in display coordinates.
   *
   * @param targetControl the control whose coordinates should be used
   * @return the rectangle in which the decoration should be rendered
   */
  protected Rectangle getDecorationRectangle(Control targetControl) {
    if(getImage() == null || control == null) {
      return new Rectangle(0, 0, 0, 0);
    }
    // Compute the bounds first relative to the control's parent.
    Rectangle imageBounds = getImage().getBounds();
    Rectangle controlBounds = control.getBounds();
    int x, y;
    // Compute x
    if((position & SWT.RIGHT) == SWT.RIGHT) {
      x = controlBounds.x + controlBounds.width + marginWidth;
    } else {
      // default is left
      x = controlBounds.x - imageBounds.width - marginWidth;
    }
    // Compute y
    if((position & SWT.TOP) == SWT.TOP) {
      y = controlBounds.y;
    } else if((position & SWT.BOTTOM) == SWT.BOTTOM) {
      y = controlBounds.y + control.getBounds().height - imageBounds.height;
    } else {
      // default is center
      y = controlBounds.y + (control.getBounds().height - imageBounds.height) / 2;
    }

    // Now convert to coordinates relative to the target control.
    Point globalPoint = control.getParent().toDisplay(x, y);
    Point targetPoint;
    if(targetControl == null) {
      targetPoint = globalPoint;
    } else {
      targetPoint = targetControl.toControl(globalPoint);
    }
    return new Rectangle(targetPoint.x, targetPoint.y, imageBounds.width, imageBounds.height);
  }

  /*
   * Return true if the decoration should be shown, false if it should not.
   */
  boolean shouldShowDecoration() {
    if(!visible) {
      return false;
    }
    if(control == null || control.isDisposed() || getImage() == null) {
      return false;
    }

    if(!control.isVisible()) {
      return false;
    }
    if(showOnlyOnFocus) {
      return hasFocus;
    }
    return true;
  }

  /*
   * If in debug mode, print info about adding the specified listener.
   */
  void printAddListener(Widget widget, String listenerType) {
    listenerInstalls++ ;
    if(DEBUG) {
      System.out.println("Added listener>>>" + listenerType + " to>>>" + widget); //$NON-NLS-1$//$NON-NLS-2$
    }
  }

  /*
   * If in debug mode, print info about adding the specified listener.
   */
  void printRemoveListener(Widget widget, String listenerType) {
    listenerInstalls-- ;
    if(DEBUG) {
      System.out.println("Removed listener>>>" + listenerType + " from>>>" + widget); //$NON-NLS-1$//$NON-NLS-2$
    }
  }
}
