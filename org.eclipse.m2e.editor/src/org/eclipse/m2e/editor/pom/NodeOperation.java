/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.editor.pom;

import org.w3c.dom.Node;

import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;


/**
 * A non-editing operation on top of the DOM document, to be used with XmlUtils.performOnRootElement and
 * XmlUtils.performOnCurrentElement
 * 
 * @author mkleint
 * @param <T>
 */
public interface NodeOperation<T extends Node> {

  void process(T node, IStructuredDocument structuredDocument);
}
