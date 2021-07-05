/*******************************************************************************
 * Copyright (c) 2008-2011 Sonatype, Inc.
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

package org.eclipse.m2e.core.ui.internal.editing;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;


/**
 * This class creates an org.eclipse.ltk.core.refactoring.DocumentChange instance based on old and new text values
 *
 * @author Anton Kraev
 */
public class ChangeCreator {
  private static final Logger log = LoggerFactory.getLogger(ChangeCreator.class);

  private final String label;

  private final IDocument oldDocument;

  private final IDocument newDocument;

  private final IFile oldFile;

  public ChangeCreator(IFile oldFile, IDocument oldDocument, IDocument newDocument, String label) {
    this.newDocument = newDocument;
    this.oldDocument = oldDocument;
    this.oldFile = oldFile;
    this.label = label;
  }

  public TextChange createChange() throws Exception {
    TextChange change = oldFile == null ? new DocumentChange(label, oldDocument) : new TextFileChange(label, oldFile);
    // change.setSaveMode(TextFileChange.FORCE_SAVE);
    change.setEdit(new MultiTextEdit());
    Object leftSide = new LineComparator(oldDocument);
    Object rightSide = new LineComparator(newDocument);

    RangeDifference[] differences = RangeDifferencer.findDifferences((IRangeComparator) leftSide,
        (IRangeComparator) rightSide);
    for(RangeDifference curr : differences) {
      if(curr.leftLength() == 0 && curr.rightLength() == 0)
        continue;

      int rightOffset = newDocument.getLineOffset(curr.rightStart());
      int rightLength = curr.rightLength() == 0 ? 0 : newDocument.getLineOffset(curr.rightEnd() - 1) - rightOffset
          + newDocument.getLineLength(curr.rightEnd() - 1);

      int leftOffset = oldDocument.getLineOffset(curr.leftStart());
      int leftLength = curr.leftLength() == 0 ? 0 : oldDocument.getLineOffset(curr.leftEnd() - 1) - leftOffset
          + oldDocument.getLineLength(curr.leftEnd() - 1);

      String newText = newDocument.get(rightOffset, rightLength);
      addEdit(change, curr.leftStart(), new ReplaceEdit(leftOffset, leftLength, newText));
    }
    return change;
  }

  private void addEdit(TextChange change, int startLine, TextEdit edit) {
    change.addTextEditGroup(new TextEditGroup("Line " + (startLine + 1), edit)); //$NON-NLS-1$
    change.addEdit(edit);
  }

  public static class LineComparator implements IRangeComparator {
    private final IDocument document;

    private final ArrayList<Integer> hashes;

    /**
     * Create a line comparator for the given document.
     *
     * @param document
     */
    public LineComparator(IDocument document) {
      this.document = document;
      this.hashes = new ArrayList<>(Arrays.asList(new Integer[document.getNumberOfLines()]));
    }

    /*
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#getRangeCount()
     */
    @Override
    public int getRangeCount() {
      return document.getNumberOfLines();
    }

    /*
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#rangesEqual(int, org.eclipse.compare.rangedifferencer.IRangeComparator, int)
     */
    @Override
    public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
      try {
        return getHash(thisIndex).equals(((LineComparator) other).getHash(otherIndex));
      } catch(BadLocationException e) {
        log.error("Problem comparing", e);
        return false;
      }
    }

    /*
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#skipRangeComparison(int, int, org.eclipse.compare.rangedifferencer.IRangeComparator)
     */
    @Override
    public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
      return false;
    }

    /**
     * @param line the number of the line in the document to get the hash for
     * @return the hash of the line
     * @throws BadLocationException if the line number is invalid
     */
    private Integer getHash(int line) throws BadLocationException {
      Integer hash = hashes.get(line);
      if(hash == null) {
        IRegion lineRegion;
        lineRegion = document.getLineInformation(line);
        String lineContents = document.get(lineRegion.getOffset(), lineRegion.getLength());
        hash = computeDJBHash(lineContents);
        hashes.set(line, hash);
      }
      return hash;
    }

    /**
     * Compute a hash using the DJB hash algorithm
     *
     * @param string the string for which to compute a hash
     * @return the DJB hash value of the string
     */
    private int computeDJBHash(String string) {
      int hash = 5381;
      int len = string.length();
      for(int i = 0; i < len; i++ ) {
        hash = (hash << 5) + hash + string.charAt(i);
      }
      return hash;
    }
  }

}
