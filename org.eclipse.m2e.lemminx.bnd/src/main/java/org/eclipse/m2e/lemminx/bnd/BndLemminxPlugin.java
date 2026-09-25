/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.lemminx.bnd;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.completion.ICompletionParticipant;
import org.eclipse.lemminx.services.extensions.completion.ICompletionRequest;
import org.eclipse.lemminx.services.extensions.completion.ICompletionResponse;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import aQute.bnd.help.Syntax;

public class BndLemminxPlugin implements IXMLExtension {

	// TODO LemminxClasspathExtensionProvider that puts our jar on the classpath +
	// bnd dependencies!

	@Override
	public void start(InitializeParams params, XMLExtensionsRegistry registry) {
		Logger logger = Logger.getLogger("bnd");
		logger.log(Level.INFO, "Hello From BND Extension");
		registry.registerCompletionParticipant(new ICompletionParticipant() {

			@Override
			public void onAttributeName(boolean arg0, ICompletionRequest arg1, ICompletionResponse arg2,
					CancelChecker arg3) throws Exception {
				logger.log(Level.INFO, "onAttributeName");
			}

			@Override
			public void onAttributeValue(String arg0, ICompletionRequest arg1, ICompletionResponse arg2,
					CancelChecker arg3) throws Exception {
				logger.log(Level.INFO, "onAttributeValue");
			}

			@Override
			public void onDTDSystemId(String arg0, ICompletionRequest arg1, ICompletionResponse arg2,
					CancelChecker arg3) throws Exception {
				logger.log(Level.INFO, "onDTDSystemId");
			}

			@Override
			public void onTagOpen(ICompletionRequest arg0, ICompletionResponse arg1, CancelChecker arg2)
					throws Exception {
				logger.log(Level.INFO, "onTagOpen");
			}

			@Override
			public void onXMLContent(ICompletionRequest completionRequest, ICompletionResponse response,
					CancelChecker checker) throws Exception {

				logger.log(Level.INFO, "onXMLContent");
				try {
					// FIXME CDATA do not trigger completion:
					// https://github.com/eclipse/lemminx/issues/1694
					DOMDocument xmlDocument = completionRequest.getXMLDocument();
					DOMNode node = xmlDocument.findNodeBefore(completionRequest.getOffset());
					if (isBndNode(node)) {
						// FIXME get the text to give better completion proposals, see:
						// https://github.com/eclipse/lemminx/issues/1695
//					if (node != null && node.getNodeName().equals("bnd")) {
//						logger.log(Level.INFO, "text content=" + node.getTextContent());
//						String substring = xmlDocument.getText().substring(node.getStart(), node.getEnd());
//						logger.log(Level.INFO, "substring=" + substring);
//					} else {
//						logger.log(Level.INFO,
//								"node=" + node + ", start=" + node.getStart() + ", end=" + node.getEnd()
//										+ " --> text content=" + node.getTextContent());
//
//						// Syntax.HELP.values().stream().map(syntax -> {
//					}
						Syntax.HELP.values().stream().forEach(syntax -> {
							CompletionItem item = new CompletionItem();
							item.setLabel(syntax.getHeader());
							item.setInsertText(syntax.getHeader() + ": ");
							response.addCompletionItem(item);
						});
					}
				} catch (Exception e) {
					logger.log(Level.INFO, "err=" + e);
				}
			}

			private boolean isBndNode(DOMNode node) {
				if (node != null) {
					if (node.getNodeName().equals("bnd")) {
						return true;
					}
					return isBndNode(node.getParentNode());
				}
				return false;
			}

		});
	}

	@Override
	public void stop(XMLExtensionsRegistry registry) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSave(ISaveContext context) {
		// TODO Auto-generated method stub
		IXMLExtension.super.doSave(context);
	}

}