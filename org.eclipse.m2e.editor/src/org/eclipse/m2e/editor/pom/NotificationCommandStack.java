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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.jface.text.IUndoManager;

/**
 * @author Anton Kraev
 */
public class NotificationCommandStack extends BasicCommandStack {

  private MavenPomEditor editor;
  private List<MavenPomEditorPage> pages;
  private boolean isDirty = false;

  public NotificationCommandStack(MavenPomEditor editor) {
    this.editor = editor;
    this.pages = editor.getPages();
  }
  
  @Override
  public void execute(Command command) {
    processCommand(command, false);
    
    IUndoManager undoManager = editor.getSourcePage().getTextViewer().getUndoManager();
    undoManager.beginCompoundChange();
    try {
      super.execute(command);
    } finally {
      undoManager.endCompoundChange();
    }
    
    processCommand(command, true);
    fireDirty();
  }

  private void processCommand(Command command, boolean add) {
    if (command instanceof CompoundCommand) {
      CompoundCommand compoundCommand = (CompoundCommand) command;
      Iterator<Command> commands = compoundCommand.getCommandList().iterator();
      while (commands.hasNext()) {
        processCommand(commands.next(), add);
      }
    }
    
    if (command instanceof AddCommand) {
      AddCommand addCommand = (AddCommand) command;
      Iterator<?> it = addCommand.getCollection().iterator();
      while (it.hasNext()) {
        processListeners(it.next(), add);
      }
    }

    if (command instanceof SetCommand) {
      SetCommand setCommand = (SetCommand) command;
      processListeners(setCommand.getValue(), add);
    }

    if (command instanceof RemoveCommand) {
      RemoveCommand removeCommand = (RemoveCommand) command;
      Collection<?> collection = removeCommand.getCollection();
      if(collection!=null) {
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
          Object next = it.next();
          if (next instanceof EObject) {
            EObject object = (EObject) next;
            for (int i=0; i<pages.size(); i++) {
              object.eAdapters().remove(pages.get(i));
            }
          }
        }
      }
    }
  }

  private void processListeners(Object next, boolean add) {
    if (next instanceof EObject) {
      EObject object = (EObject) next;
      for (int i=0; i<pages.size(); i++) {
        if (add) {
          if (!object.eAdapters().contains(pages.get(i)))
            object.eAdapters().add(pages.get(i));
        } else {
          object.eAdapters().remove(pages.get(i));
        }
      }
    }
  }
  
  @Override
  public void redo() {
    super.redo();
    fireDirty();
  }

  private void fireDirty() {
    if (isDirty != isSaveNeeded()) {
      editor.editorDirtyStateChanged();
    }
    isDirty = isSaveNeeded();
  }

  @Override
  public void undo() {
    super.undo();
    fireDirty();
  }

}
