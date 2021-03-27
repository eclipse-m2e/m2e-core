/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.pomeditor;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension3;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.sse.ui.internal.contentoutline.ConfigurableContentOutlinePage;

// TODO extract to specific bundle and extension
class MavenStructuredSourceTextEditor extends StructuredTextEditor {

	protected class MavenStructuredTextViewer extends StructuredTextViewer implements IAdaptable {

		public MavenStructuredTextViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler,
				boolean showAnnotationsOverview, int styles) {
			super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles);
		}

		public MavenProject getMavenProject() {
			return MavenStructuredSourceTextEditor.this.mavenPomEditor.getMavenProject();
		}

		public <T> T getAdapter(Class<T> adapter) {
			if (MavenProject.class.equals(adapter)) {
				return adapter.cast(getMavenProject());
			}
			return null;
		}

	}

	/**
	 *
	 */
	private final MavenPomEditor mavenPomEditor;

	/**
	 * @param mavenPomEditor
	 */
	MavenStructuredSourceTextEditor(MavenPomEditor mavenPomEditor) {
		this.mavenPomEditor = mavenPomEditor;
	}

	private long fModificationStamp = -1;

	private MavenProject mvnprj;

	protected void updateModificationStamp() {
		IDocumentProvider p = this instanceof ITextEditor ? ((ITextEditor) this).getDocumentProvider() : null;
		if (p == null)
			return;
		if (p instanceof IDocumentProviderExtension3) {
			fModificationStamp = p.getModificationStamp(this.mavenPomEditor.getEditorInput());
		}
	}

	/**
	 * we override the creation of StructuredTextViewer to have our own subclass
	 * created that drags along an instance of resolved MavenProject via
	 * implementing IMavenProjectCache
	 */
	protected StructuredTextViewer createStructedTextViewer(Composite parent, IVerticalRuler verticalRuler,
			int styles) {
		return new MavenStructuredTextViewer(parent, verticalRuler, getOverviewRuler(), isOverviewRulerVisible(),
				styles);
	}

	protected void sanityCheckState(IEditorInput input) {

		IDocumentProvider p = this instanceof ITextEditor ? ((ITextEditor) this).getDocumentProvider() : null;
		if (p == null)
			return;

		if (p instanceof IDocumentProviderExtension3) {

			IDocumentProviderExtension3 p3 = (IDocumentProviderExtension3) p;

			long stamp = p.getModificationStamp(input);
			if (stamp != fModificationStamp) {
				fModificationStamp = stamp;
				if (!p3.isSynchronized(input))
					handleEditorInputChanged();
			}

		} else {

			if (fModificationStamp == -1)
				fModificationStamp = p.getSynchronizationStamp(input);

			long stamp = p.getModificationStamp(input);
			if (stamp != fModificationStamp) {
				fModificationStamp = stamp;
				if (stamp != p.getSynchronizationStamp(input))
					handleEditorInputChanged();
			}
		}

		updateState(this.getEditorInput());
		updateStatusField(ITextEditorActionConstants.STATUS_CATEGORY_ELEMENT_STATE);
	}

	private boolean oldDirty;

	public boolean isDirty() {
		boolean dirty = super.isDirty();
		if (oldDirty != dirty) {
			oldDirty = dirty;
			updatePropertyDependentActions();
		}
		return dirty;
	}

	@Override
	public void dispose() {
		Object outlinePage = this.getAdapter(IContentOutlinePage.class);
		if (outlinePage instanceof ConfigurableContentOutlinePage) {
			((ConfigurableContentOutlinePage) outlinePage).setEditorPart(null);
		}
		super.dispose();
	}

}
