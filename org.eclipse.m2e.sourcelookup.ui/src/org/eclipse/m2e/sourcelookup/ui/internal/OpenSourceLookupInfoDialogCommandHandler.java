/*******************************************************************************
 * Copyright (c) 2011-2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.sourcelookup.ui.internal;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.launching.sourcelookup.advanced.AdvancedSourceLookupParticipant;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class OpenSourceLookupInfoDialogCommandHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

    if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
      return null;
    }

    Object debugElement = ((IStructuredSelection) selection).getFirstElement();

    final AdvancedSourceLookupParticipant sourceLookup = AdvancedSourceLookupParticipant.getSourceLookup(debugElement);

    if (debugElement != null && sourceLookup != null) {
      new SourceLookupInfoDialog(HandlerUtil.getActiveShell(event), debugElement, sourceLookup).open();
    }

    return null;
  }

}
