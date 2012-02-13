/*******************************************************************************
 * Copyright (c) 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElement;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.format;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.insertAt;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.setText;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.jface.text.Region;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;

import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.editor.xml.internal.dialogs.SPDXLicense;


@SuppressWarnings("restriction")
public class AddLicensePomOperation implements Operation {

  private final SPDXLicense license;

  private final PomTemplateContext context;

  private final Region region;

  private int generatedOffset = -1;

  private int generatedLength = -1;

  public AddLicensePomOperation(SPDXLicense license, PomTemplateContext context, Region region) {
    if(license == null) {
      throw new NullPointerException();
    }
    if(context != PomTemplateContext.PROJECT && context != PomTemplateContext.LICENSES) {
      throw new IllegalArgumentException();
    }
    if(context == PomTemplateContext.LICENSES && region == null) {
      throw new IllegalArgumentException();
    }

    this.license = license;
    this.context = context;
    this.region = region;
  }

  public void process(Document doc) {
    Element element;
    if(context == PomTemplateContext.PROJECT) {
      element = createLicenses(doc);
    } else {
      element = createLicense(doc);
    }
    format(element);

    if(element instanceof IndexedRegion) {
      generatedOffset = ((IndexedRegion) element).getStartOffset();
      generatedLength = ((IndexedRegion) element).getEndOffset() - generatedOffset;
    }
  }

  private Element createLicenses(Document doc) {
    Element licensesDom;
    if(region != null) {
      licensesDom = insertAt(doc.createElement("licenses"), region.getOffset());
    } else {
      licensesDom = getChild(doc.getDocumentElement(), "licenses");
    }

    setLicense(createElement(licensesDom, "license"));

    return licensesDom;
  }

  private Element createLicense(Document doc) {
    Element licenseDom = insertAt(doc.createElement("license"), region.getOffset());

    setLicense(licenseDom);

    return licenseDom;
  }

  void setLicense(Element licenseDom) {
    setText(getChild(licenseDom, "name"), license.getName());
    setText(getChild(licenseDom, "url"), license.getURL());
  }

  public Point getSelection() {
    return new Point(generatedOffset, generatedLength);
  }
}
