/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.editor.lemminx.bnd;

import java.util.function.Function;
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
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import aQute.bnd.help.Syntax;

/**
 * Extension to provide bnd instruction autocompletion to maven
 */
public class BndLemminxPlugin implements IXMLExtension {

	@Override
	public void start(InitializeParams params, XMLExtensionsRegistry registry) {
		Logger logger = Logger.getLogger("bnd");
		logger.log(Level.INFO, "Loading bnd-lemminx extension");
		registry.registerCompletionParticipant(new ICompletionParticipant() {

			@Override
			public void onAttributeName(boolean generateValue, ICompletionRequest completionRequest,
					ICompletionResponse response, CancelChecker checker) throws Exception {
			}

			@Override
			public void onAttributeValue(String valuePrefix, ICompletionRequest completionRequest,
					ICompletionResponse response, CancelChecker checker) throws Exception {
			}

			@Override
			public void onDTDSystemId(String valuePrefix, ICompletionRequest completionRequest,
					ICompletionResponse response, CancelChecker checker) throws Exception {
			}

			@Override
			public void onTagOpen(ICompletionRequest completionRequest, ICompletionResponse response,
					CancelChecker checker) throws Exception {
			}

			@Override
			public void onXMLContent(ICompletionRequest completionRequest, ICompletionResponse response,
					CancelChecker checker) throws Exception {
				try {
					DOMDocument xmlDocument = completionRequest.getXMLDocument();
					DOMNode node = xmlDocument.findNodeBefore(completionRequest.getOffset());
					logger.log(Level.INFO, "onXMLContent: " + node);
					if (isBndInstructionNode(node)) {
						addCompletion(response, syntax -> syntax.getHeader() + ": ");
					} else if (isFelixInstructionNode(node)) {
						addCompletion(response, syntax -> {
							String header = syntax.getHeader();
							if (header.startsWith("-")) {
								header = "_" + header.substring(1);
							}
							return String.format("<%s>${0}</%s>", header, header);
						});
					}
				} catch (Exception e) {
					logger.log(Level.WARNING, "err=" + e);
				}
			}

			private void addCompletion(ICompletionResponse response, Function<Syntax, String> insert) {
				Syntax.HELP.values().stream().forEach(syntax -> {
					CompletionItem item = new CompletionItem();
					item.setLabel(syntax.getHeader());
					item.setDocumentation(syntax.getLead());
					item.setDetail(syntax.getExample());
					item.setInsertText(insert.apply(syntax));
					item.setKind(CompletionItemKind.Property);
					item.setInsertTextFormat(InsertTextFormat.Snippet);
					response.addCompletionItem(item);
				});
			}
		});
	}

	private static boolean isBndInstructionNode(DOMNode node) {
		if (node != null) {
			if (node.getNodeName().equals("bnd")) {
				return true;
			}
			return isBndInstructionNode(node.getParentNode());
		}
		return false;
	}

	private static boolean isFelixInstructionNode(DOMNode node) {
		if (node != null) {
			if (node.getNodeName().equals("instructions")) {
				return true;
			}
			return isFelixInstructionNode(node.getParentNode());
		}
		return false;
	}

	@Override
	public void stop(XMLExtensionsRegistry registry) {
		// nothing special to do...
	}

	@Override
	public void doSave(ISaveContext context) {
		IXMLExtension.super.doSave(context);
	}

}