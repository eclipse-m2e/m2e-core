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

package org.eclipse.m2e.core.ui.internal.console;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.ULocale;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Maven Console implementation
 * 
 * @author Dmitri Maximovich
 */
public class MavenConsoleImpl extends IOConsole implements MavenConsole, IPropertyChangeListener {

  private boolean initialized = false;

  // console is visible in the Console view
  private boolean visible = false;

  private ConsoleDocument consoleDocument;

  // created colors for each line type - must be disposed at shutdown
  private Color commandColor;

  private Color messageColor;

  private Color errorColor;

  // streams for each command type - each stream has its own color
  private IOConsoleOutputStream commandStream;

  private IOConsoleOutputStream messageStream;

  private IOConsoleOutputStream errorStream;

  private static final String TITLE = Messages.MavenConsoleImpl_title;

  private List<IMavenConsoleListener> listeners = new CopyOnWriteArrayList<IMavenConsoleListener>();

  public MavenConsoleImpl(ImageDescriptor imageDescriptor) {
    super(TITLE, imageDescriptor);
    this.setConsoleDocument(new ConsoleDocument());
  }

  protected void init() {
    super.init();

    //  Ensure that initialization occurs in the UI thread
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        JFaceResources.getFontRegistry().addListener(MavenConsoleImpl.this);
        initializeConsoleStreams(Display.getDefault());
        dumpConsole();
      }
    });
  }

  /*
   * Initialize three streams of the console. Must be called from the UI thread, so synchronization is unnecessary.
   */
  protected void initializeConsoleStreams(Display display) {
    if(!initialized) {
      setCommandStream(newOutputStream());
      setErrorStream(newOutputStream());
      setMessageStream(newOutputStream());

      // TODO convert this to use themes
      // install colors
      commandColor = new Color(display, new RGB(0, 0, 0));
      messageColor = new Color(display, new RGB(0, 0, 255));
      errorColor = new Color(display, new RGB(255, 0, 0));

      getCommandStream().setColor(commandColor);
      getMessageStream().setColor(messageColor);
      getErrorStream().setColor(errorColor);

      // install font
      setFont(JFaceResources.getFontRegistry().get("pref_console_font")); //$NON-NLS-1$

      initialized = true;
    }
  }

  /**
   * Is always called from main thread, so synchronization not necessary
   */
  protected void dumpConsole() {
    setVisible(true);
    ConsoleDocument.ConsoleLine[] lines = getConsoleDocument().getLines();
    for(int i = 0; i < lines.length; i++ ) {
      ConsoleDocument.ConsoleLine line = lines[i];
      appendLine(line.type, line.line);
    }
    getConsoleDocument().clear();
  }

  private void appendLine(final int type, final String line) {
    show(false);
    //the synchronization here caused a deadlock. since the writes are simply appending to the output stream
    //or the document, just doing it on the main thread to avoid deadlocks and or corruption of the 
    //document or output stream
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        if(isVisible()) {
          try {
            switch(type) {
              case ConsoleDocument.COMMAND:
                getCommandStream().write(line);
                getCommandStream().write('\n');
                break;
              case ConsoleDocument.MESSAGE:
                getMessageStream().write(line);
                getMessageStream().write('\n');
                break;
              case ConsoleDocument.ERROR:
                getErrorStream().write(line);
                getErrorStream().write('\n');
                break;
            }
          } catch(IOException ex) {
            // Don't log using slf4j - it will cause a cycle
            ex.printStackTrace();
          }
        } else {
          getConsoleDocument().appendConsoleLine(type, line);
        }
      }
    });
  }

  /**
   * Show the console.
   * 
   * @param showNoMatterWhat ignore preferences if <code>true</code>
   */
  public void show(boolean showNoMatterWhat) {
    if(showNoMatterWhat) {
      if(!isVisible()) {
        showConsole();
      } else {
        ConsolePlugin.getDefault().getConsoleManager().showConsoleView(this);
      }
    }
  }

  public void showConsole() {
    boolean exists = false;
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    for(IConsole element : manager.getConsoles()) {
      if(this == element) {
        exists = true;
      }
    }
    if(!exists) {
      manager.addConsoles(new IConsole[] {this});
    }
    manager.showConsoleView(this);
  }

  public void closeConsole() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    manager.removeConsoles(new IConsole[] {this});
    ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(this.newLifecycle());
  }

  public void propertyChange(PropertyChangeEvent event) {
    // font changed
    setFont(JFaceResources.getFontRegistry().get("pref_console_font")); //$NON-NLS-1$
  }

  private void bringConsoleToFront() {
    if(PlatformUI.isWorkbenchRunning()) {
      IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
      if(!isVisible()) {
        manager.addConsoles(new IConsole[] {this});
      }
      manager.showConsoleView(this);
    }
  }

  // Called when console is removed from the console view
  protected void dispose() {
    // Here we can't call super.dispose() because we actually want the partitioner to remain
    // connected, but we won't show lines until the console is added to the console manager
    // again.
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        setVisible(false);
        JFaceResources.getFontRegistry().removeListener(MavenConsoleImpl.this);
      }
    });
  }

  public void shutdown() {
    // Call super dispose because we want the partitioner to be
    // disconnected.
    super.dispose();
    if(commandColor != null) {
      commandColor.dispose();
    }
    if(messageColor != null) {
      messageColor.dispose();
    }
    if(errorColor != null) {
      errorColor.dispose();
    }
  }

  private DateFormat getDateFormat() {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, ULocale.getDefault());
  }

  // MavenConsole

  public void debug(String message) {
    if(!M2EUIPluginActivator.getDefault().getPreferenceStore().getBoolean(MavenPreferenceConstants.P_DEBUG_OUTPUT)) {
      return;
    }
    if(showConsoleOnOutput()) {
      bringConsoleToFront();
    }
    appendLine(ConsoleDocument.MESSAGE, getDateFormat().format(new Date()) + ": " + message);

    for(IMavenConsoleListener listener : listeners) {
      try {
        listener.loggingMessage(message);
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void info(String message) {
    if(showConsoleOnOutput()) {
      bringConsoleToFront();
    }
    appendLine(ConsoleDocument.MESSAGE, getDateFormat().format(new Date()) + ": " + message);

    for(IMavenConsoleListener listener : listeners) {
      try {
        listener.loggingMessage(message);
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void error(String message) {
    if(showConsoleOnError()) {
      bringConsoleToFront();
    }
    appendLine(ConsoleDocument.ERROR, getDateFormat().format(new Date()) + ": " + message); //$NON-NLS-1$

    for(IMavenConsoleListener listener : listeners) {
      try {
        listener.loggingError(message);
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }

  public boolean showConsoleOnError() {
    return M2EUIPluginActivator.getDefault().getPreferenceStore()
        .getBoolean(MavenPreferenceConstants.P_SHOW_CONSOLE_ON_ERR);
  }

  public boolean showConsoleOnOutput() {
    return M2EUIPluginActivator.getDefault().getPreferenceStore()
        .getBoolean(MavenPreferenceConstants.P_SHOW_CONSOLE_ON_OUTPUT);
  }

  public IConsoleListener newLifecycle() {
    return new MavenConsoleLifecycle();
  }

  /**
   * @param commandStream The commandStream to set.
   */
  protected void setCommandStream(IOConsoleOutputStream commandStream) {
    this.commandStream = commandStream;
  }

  /**
   * @return Returns the commandStream.
   */
  protected IOConsoleOutputStream getCommandStream() {
    return commandStream;
  }

  /**
   * @param messageStream The messageStream to set.
   */
  protected void setMessageStream(IOConsoleOutputStream messageStream) {
    this.messageStream = messageStream;
  }

  /**
   * @return Returns the messageStream.
   */
  protected IOConsoleOutputStream getMessageStream() {
    return messageStream;
  }

  /**
   * @param errorStream The errorStream to set.
   */
  protected void setErrorStream(IOConsoleOutputStream errorStream) {
    this.errorStream = errorStream;
  }

  /**
   * @return Returns the errorStream.
   */
  protected IOConsoleOutputStream getErrorStream() {
    return errorStream;
  }

  /**
   * @param visible The visible to set.
   */
  protected void setVisible(boolean visible) {
    this.visible = visible;
  }

  /**
   * @return Returns the visible.
   */
  protected boolean isVisible() {
    return visible;
  }

  /**
   * @param consoleDocument The consoleDocument to set.
   */
  private void setConsoleDocument(ConsoleDocument consoleDocument) {
    this.consoleDocument = consoleDocument;
  }

  /**
   * @return Returns the consoleDocument.
   */
  protected ConsoleDocument getConsoleDocument() {
    return consoleDocument;
  }

  /**
   * Used to notify this console of lifecycle methods <code>init()</code> and <code>dispose()</code>.
   */
  public class MavenConsoleLifecycle implements org.eclipse.ui.console.IConsoleListener {

    public void consolesAdded(IConsole[] consoles) {
      for(int i = 0; i < consoles.length; i++ ) {
        IConsole console = consoles[i];
        if(console == MavenConsoleImpl.this) {
          init();
        }
      }

    }

    public void consolesRemoved(IConsole[] consoles) {
      for(int i = 0; i < consoles.length; i++ ) {
        IConsole console = consoles[i];
        if(console == MavenConsoleImpl.this) {
          ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
          dispose();
        }
      }
    }

  }

  public void addMavenConsoleListener(IMavenConsoleListener listener) {
    listeners.remove(listener);
    listeners.add(listener);
  }

  public void removeMavenConsoleListener(IMavenConsoleListener listener) {
    listeners.remove(listener);
  }

}
