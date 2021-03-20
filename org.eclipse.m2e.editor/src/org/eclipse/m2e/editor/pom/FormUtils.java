/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.editor.pom;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.ui.internal.dialogs.MavenMessageDialog;
import org.eclipse.m2e.core.ui.internal.util.Util;
import org.eclipse.m2e.editor.internal.Messages;


/**
 * @author Eugene Kuleshov
 */
public abstract class FormUtils {
  private static final Logger log = LoggerFactory.getLogger(FormUtils.class);

  public static final int MAX_MSG_LENGTH = 80;

  /**
   * @deprecated use your own string.. this should not have been made ublic in the first place.
   */
  @Deprecated
  public static final String MORE_DETAILS = ""; //$NON-NLS-1$

  public static void decorateHeader(FormToolkit toolkit, Form form) {
    Util.proxy(toolkit, FormTooliktStub.class).decorateFormHeading(form);
  }

  /**
   * Stub interface for API added to FormToolikt in Eclipse 3.3
   */
  private interface FormTooliktStub {
    public void decorateFormHeading(Form form);
  }

  /**
   * @param form
   * @param message
   * @param severity
   * @return
   */
  public static boolean setMessage(ScrolledForm form, String message, int severity) {
    if(message != null && (message.length() > MAX_MSG_LENGTH || message.contains("\n"))) {
      String truncMsg = message;
      String[] lines = message.split("\n"); //$NON-NLS-1$
      if(lines.length > 0) {
        truncMsg = lines[0];
      } else {
        truncMsg = message.substring(0, MAX_MSG_LENGTH);
      }
      setMessageAndTTip(form, NLS.bind(Messages.FormUtils_click_for_details, truncMsg), message, severity);
      return true;
    }
    setMessageAndTTip(form, message, message, severity);
    return false;
  }

  public static void setMessageAndTTip(final ScrolledForm form, final String message, final String ttip,
      final int severity) {
    if(form.isDisposed()) {
      return;
    }
    form.getForm().setMessage(message, severity);
    addFormTitleListeners(createDefaultPerformer(form, message, ttip, severity), form);
  }

  /**
   * @param form
   * @param message
   * @param severity
   * @param runnable something that will be "run" once the user clicks the message area.
   */
  static void setMessageWithPerformer(ScrolledForm form, String message, int severity, Consumer<Point> runnable) {
    form.getForm().setMessage(message, severity);
    addFormTitleListeners(runnable, form);
  }

  public static String nvl(String s) {
    return s == null ? "" : s; //$NON-NLS-1$
  }

  public static String nvl(String s, String defaultValue) {
    return s == null ? defaultValue : s;
  }

  public static boolean isEmpty(String s) {
    return s == null || s.length() == 0;
  }

  public static boolean isEmpty(Text t) {
    return t == null || isEmpty(t.getText());
  }

  public static void setText(Text control, String text) {
    if(control != null && !control.isDisposed() && !control.getText().equals(text)) {
      control.setText(nvl(text));
      control.setSelection(nvl(text).length());
    }
  }

  public static void setText(CCombo control, String text) {
    if(control != null && !control.isDisposed() && !control.getText().equals(text)) {
      control.setText(nvl(text));
    }
  }

  public static void setButton(Button control, boolean selection) {
    if(control != null && !control.isDisposed() && control.getSelection() != selection) {
      control.setSelection(selection);
    }
  }

  public static void openHyperlink(String url) {
    if(!isEmpty(url) && (url.startsWith("http://") || url.startsWith("https://"))) { //$NON-NLS-1$ //$NON-NLS-2$
      url = url.trim();
      try {
        IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
        IWebBrowser browser = browserSupport.createBrowser(
            IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR, url, url, url);
        browser.openURL(new URL(url));
      } catch(PartInitException ex) {
        log.error(ex.getMessage(), ex);
      } catch(MalformedURLException ex) {
        log.error("Malformed url " + url, ex); //$NON-NLS-1$
      }
    }
  }

  public static void setEnabled(Composite composite, boolean enabled) {
    if(composite != null && !composite.isDisposed()) {
      composite.setEnabled(enabled);
      for(Control control : composite.getChildren()) {
        if(control instanceof Combo) {
          control.setEnabled(enabled);

        } else if(control instanceof CCombo) {
          control.setEnabled(enabled);

        } else if(control instanceof Hyperlink) {
          control.setEnabled(enabled);

        } else if(control instanceof Composite) {
          setEnabled((Composite) control, enabled);

        } else {
          control.setEnabled(enabled);

        }
      }
    }
  }

  /**
   * be very careful when using this method, see MNGECLIPSE-2674 ideally this would be replaced by a distributed system
   * where each component reacts to the readonly or not-readonly event and with the knowledge of the inner state decides
   * what gets enabled/disabled
   * 
   * @param composite
   * @param readonly
   * @deprecated so that you think hard before using it. Using it for disabling all controls is probably fine. Enabling
   *             all is NOT.
   */
  @Deprecated
  public static void setReadonly(Composite composite, boolean readonly) {
    if(composite != null) {
      for(Control control : composite.getChildren()) {
        if(control instanceof Text) {
          ((Text) control).setEditable(!readonly);

        } else if(control instanceof Combo) {
          ((Combo) control).setEnabled(!readonly);

        } else if(control instanceof CCombo) {
          ((CCombo) control).setEnabled(!readonly);

        } else if(control instanceof Button) {
          ((Button) control).setEnabled(!readonly);

        } else if(control instanceof Composite) {
          setReadonly((Composite) control, readonly);

        }
      }
    }
  }

  private static void cleanupMouseListeners(Control kid, int event) {
    Listener[] listeners = kid.getListeners(event);
    if(listeners != null) {
      for(Listener list : listeners) {
        kid.removeListener(event, list);
      }
    }
  }

  private static Consumer<Point> createDefaultPerformer(final ScrolledForm form, final String message,
      final String ttip, final int severity) {
    if(ttip != null && ttip.length() > 0 && message != null) {
      return point -> {
        int dialogSev = IMessageProvider.ERROR == severity ? MessageDialog.ERROR : MessageDialog.WARNING;
        MavenMessageDialog.openWithSeverity(form.getShell(), Messages.FormUtils_error_info,
            Messages.FormUtils_pom_error, ttip, dialogSev);
      };
    }
    return null;
  }

  private static void addFormTitleListeners(final Consumer<Point> runnable, final ScrolledForm form) {
    if(runnable != null) {
      final Composite head = form.getForm().getHead();
      Control[] kids = head.getChildren();
      for(final Control kid : kids) {
        //want to get the title region only
        //Note: doing this instead of adding a head 'client' control because that gets put 
        //on the second line of the title, and looks broken. instead, converting the title
        //into a url
        if(kid != form && kid instanceof Canvas) {
          cleanupMouseListeners(kid, SWT.MouseUp);
          cleanupMouseListeners(kid, SWT.MouseEnter);
          cleanupMouseListeners(kid, SWT.MouseExit);
          kid.addMouseListener(new MouseAdapter() {
            public void mouseUp(MouseEvent e) {
              runnable.accept(kid.toDisplay(new Point(e.x, e.y)));
            }
          });
          kid.addMouseTrackListener(new MouseTrackAdapter() {
            public void mouseEnter(MouseEvent e) {
              head.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
            }

            public void mouseExit(MouseEvent e) {
              head.setCursor(null);
            }
          });
        }
      }
    } else {
      //no ttip or message, make sure old listeners are cleaned up if errs are removed
      final Composite head = form.getForm().getHead();
      Control[] kids = head.getChildren();
      for(Control kid : kids) {
        //want to get the title region only
        //Note: doing this instead of adding a head 'client' control because that gets put 
        //on the second line of the title, and looks broken. instead, converting the title
        //into a url
        if(kid != form && kid instanceof Canvas) {
          cleanupMouseListeners(kid, SWT.MouseUp);
          cleanupMouseListeners(kid, SWT.MouseEnter);
          cleanupMouseListeners(kid, SWT.MouseExit);
        }
      }
    }
  }

  /**
   * copy pas
   * 
   * @param project
   * @param text
   * @return
   */
  //TODO copy pasted from PomTemplateContext
  static String simpleInterpolate(MavenProject project, String text) {
    if(text != null && text.contains("${")) { //$NON-NLS-1$
      //when expression is in the version but no project instance around
      // just give up.
      if(project == null) {
        return null;
      }
      Properties props = project.getProperties();
      RegexBasedInterpolator inter = new RegexBasedInterpolator();
      if(props != null) {
        inter.addValueSource(new PropertiesBasedValueSource(props));
      }
      inter.addValueSource(
          new PrefixedObjectValueSource(Arrays.asList(new String[] {"pom.", "project."}), project.getModel(), false)); //$NON-NLS-1$ //$NON-NLS-2$
      try {
        text = inter.interpolate(text);
      } catch(InterpolationException e) {
        text = null;
      }
    }
    return text;
  }

}
