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

package org.eclipse.m2e.editor.pom;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.ui.dialogs.MavenMessageDialog;
import org.eclipse.m2e.core.util.Util;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
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


/**
 * @author Eugene Kuleshov
 */
public abstract class FormUtils {
  public static final int MAX_MSG_LENGTH = 80;

  /**
   * @deprecated use your own string.. this should not have been made ublic in the first place.
   */
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
    if(message != null && message.length() > MAX_MSG_LENGTH) {
      String truncMsg = message;
      String[] lines = message.split("\n"); //$NON-NLS-1$
      if(lines.length > 0) {
        truncMsg = lines[0];
      } else {
        truncMsg = message.substring(0, MAX_MSG_LENGTH);
      }
      setMessageAndTTip(form, NLS.bind(Messages.FormUtils_click_for_details, truncMsg), message, severity);
      return true;
    } else {
      setMessageAndTTip(form, message, message, severity);
      return false;
    }
  }

  public static void setMessageAndTTip(final ScrolledForm form, final String message, final String ttip,
      final int severity) {
    form.getForm().setMessage(message, severity);
    addFormTitleListeners(form, message, ttip, severity);
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
        IWebBrowser browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR
            | IWorkbenchBrowserSupport.LOCATION_BAR, url, url, url);
        browser.openURL(new URL(url));
      } catch(PartInitException ex) {
        MavenLogger.log(ex);
      } catch(MalformedURLException ex) {
        MavenLogger.log("Malformed url " + url, ex); //$NON-NLS-1$
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
   * be very careful when using this method, see MNGECLIPSE-2674
   * ideally this would be replaced by a distributed system where each component reacts to the 
   * readonly or not-readonly event and with the knowledge of the inner state decides what gets enabled/disabled
   *  
   * @param composite
   * @param readonly
   * @deprecated so that you think hard before using it. Using it for disabling all controls is probably fine. Enabling all is NOT.
   */
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

  private static void addFormTitleListeners(final ScrolledForm form, final String message, final String ttip,
      final int severity) {
    if(ttip != null && ttip.length() > 0 && message != null && severity == IMessageProvider.ERROR) {
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
          kid.addMouseListener(new MouseAdapter() {
            public void mouseUp(MouseEvent e) {
              MavenMessageDialog.openInfo(form.getShell(), Messages.FormUtils_error_info, Messages.FormUtils_pom_error, ttip);
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

}
