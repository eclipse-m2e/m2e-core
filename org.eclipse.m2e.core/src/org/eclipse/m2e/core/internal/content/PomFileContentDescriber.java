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

package org.eclipse.m2e.core.internal.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.eclipse.core.runtime.content.IContentDescription;

import org.eclipse.m2e.core.internal.Messages;


/**
 * A content describer for POM files.
 * 
 * @see org.eclipse.ant.internal.core.contentDescriber.AntBuildfileContentDescriber
 * @author Herve Boutemy
 * @since 0.9.6
 */
public final class PomFileContentDescriber extends XMLContentDescriber {
  /**
   * Determines the validation status for the given contents.
   * 
   * @param contents the contents to be evaluated
   * @return one of the following:<ul>
   * <li><code>VALID</code></li>,
   * <li><code>INVALID</code></li>,
   * <li><code>INDETERMINATE</code></li>
   * </ul>
   * @throws IOException
   */
  private int checkCriteria(InputSource contents) throws IOException {
    PomHandler pomHandler = new PomHandler();
    try {
      if(!pomHandler.parseContents(contents)) {
        return INDETERMINATE;
      }
    } catch(SAXException e) {
      // we may be handed any kind of contents... it is normal we fail to parse
      return INDETERMINATE;
    } catch(ParserConfigurationException e) {
      // some bad thing happened - force this describer to be disabled
      throw new RuntimeException(Messages.PomFileContentDescriber_error);
    }

    // Check to see if we matched our criteria.
    if(pomHandler.hasRootProjectElement()) {
      if(pomHandler.hasArtifactIdElement()) {
        //project and artifactId element 
        return VALID;
      }
      //only a top level project element: maybe a POM file, but maybe an Ant buildfile, a site descriptor, ...
      return INDETERMINATE;
    }
    return INDETERMINATE;
  }

  @Override
  public int describe(InputStream contents, IContentDescription description) throws IOException {
    // call the basic XML describer to do basic recognition
    if(super.describe(contents, description) == INVALID) {
      return INVALID;
    }
    // super.describe will have consumed some chars, need to rewind   
    contents.reset();
    // Check to see if we matched our criteria.   
    return checkCriteria(new InputSource(contents));
  }

  @Override
  public int describe(Reader contents, IContentDescription description) throws IOException {
    // call the basic XML describer to do basic recognition
    if(super.describe(contents, description) == INVALID) {
      return INVALID;
    }
    // super.describe will have consumed some chars, need to rewind
    contents.reset();
    // Check to see if we matched our criteria.
    return checkCriteria(new InputSource(contents));
  }
}
