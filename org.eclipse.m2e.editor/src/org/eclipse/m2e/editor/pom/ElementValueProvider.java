
package org.eclipse.m2e.editor.pom;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.m2e.core.ui.internal.editing.PomEdits;


public final class ElementValueProvider {

  private final String[] path;

  private String defaultValue;

  public ElementValueProvider(String... path) {
    this.path = path;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public Element find(Document document) {
    Element toRet = null;
    Element parent = document.getDocumentElement();
    for(String pathEl : path) {
      toRet = PomEdits.findChild(parent, pathEl);
      if(toRet == null) {
        return null;
      }
      parent = toRet;
    }
    return toRet;
  }

  public Element get(Document document) {
    return PomEdits.getChild(document.getDocumentElement(), path);
  }

  public String getValue(Document document) {
    return PomEdits.getTextValue(find(document));
  }

}
