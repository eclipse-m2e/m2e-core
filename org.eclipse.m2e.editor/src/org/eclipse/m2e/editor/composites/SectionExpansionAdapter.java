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

package org.eclipse.m2e.editor.composites;

import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.Section;

/**
 * An expansion adapter that collapses all the sections in the same row.
 */
public class SectionExpansionAdapter extends ExpansionAdapter {
  private boolean inProgress = false;
  private Section[] sections;
  
  public SectionExpansionAdapter(Section[] sections) {
    this.sections = sections;
    for(Section section : sections) {
      section.addExpansionListener(this);
    }
  }

  public void expansionStateChanged(ExpansionEvent e) {
    if(!inProgress && e.getSource() instanceof Section) {
      inProgress = true;
      boolean expand = ((Section)e.getSource()).isExpanded();
      
      for(Section section : sections) {
        section.setExpanded(expand);
      }

      inProgress = false;
    }
  }
}
