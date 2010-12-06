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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.model.edit.pom.Contributor;
import org.eclipse.m2e.model.edit.pom.Developer;
import org.eclipse.swt.graphics.Image;


/**
 * Label provider for Developer and Contributor elements
 * 
 * @author Dmitry Platonoff
 */
public class TeamLabelProvider extends LabelProvider {

  @Override
  public String getText(Object element) {
    if(element instanceof Developer) {
      Developer developer = (Developer) element; 
      return getText(developer.getName(), developer.getEmail(), developer.getOrganization());
    }
    else if(element instanceof Contributor) {
      Contributor contributor = (Contributor) element; 
      return getText(contributor.getName(), contributor.getEmail(), contributor.getOrganization());
    }
    return super.getText(element);
  }

  @Override
  public Image getImage(Object element) {
    return MavenEditorImages.IMG_PERSON;
  }

  private String getText(String name, String email, String organization) {
    StringBuilder sb = new StringBuilder();

    sb.append(isEmpty(name) ? "?" : name);

    if(!isEmpty(email)) {
      sb.append(" <").append(email).append('>');
    }

    if(!isEmpty(organization)) {
      sb.append(", ").append(organization);
    }

    return sb.toString();
  }

  private boolean isEmpty(String s) {
    return s == null || s.trim().length() == 0;
  }
}
