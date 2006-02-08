/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import java.util.*;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.FileStateTypedElement;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A abstract implementation of {@link ISynchronizationCompareAdapter}. Most of the methods
 * are no-ops except for the {@link #asCompareInput(ISynchronizationContext, Object) }
 * which will convert file objects to an appropriate compare input.
 * <p>
 * Clients may subclass this class.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public class SynchronizationCompareAdapter implements ISynchronizationCompareAdapter {

	private static class ResourceDiffCompareInput extends DiffNode implements IModelCompareInput, IAdaptable {

		private final IDiff node;

		public ResourceDiffCompareInput(IDiff node) {
			super(getCompareKind(node), getAncestor(node), getLeftContributor(node), getRightContributor(node));
			this.node = node;
		}
		
		private static int getCompareKind(IDiff node) {
			switch (node.getKind()) {
			case IDiff.CHANGE:
				return Differencer.CHANGE;
			case IDiff.ADD:
				return Differencer.ADDITION;
			case IDiff.REMOVE:
				return Differencer.DELETION;
			}
			return 0;
		}
		
		private static ITypedElement getRightContributor(IDiff node) {
			// For a resource diff, use the after state
			if (node instanceof IResourceDiff) {
				IResourceDiff rd = (IResourceDiff) node;
				return asTypedElement(rd.getAfterState());
			}
			if (node instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) node;
				IResourceDiff diff = (IResourceDiff)twd.getRemoteChange();
				// If there is a remote change, use the after state
				if (diff != null)
					return getRightContributor(diff);
				// There's no remote change so use the before state of the local
				diff = (IResourceDiff)twd.getLocalChange();
				return asTypedElement(diff.getBeforeState());
				
			}
			return null;
		}

		private static ITypedElement getLeftContributor(final IDiff node) {
			// The left contributor is always the local resource
			final IResource resource = ResourceDiffTree.getResourceFor(node);
			return new LocalResourceTypedElement(resource) {
				public boolean isEditable() {
					if(! resource.exists() && isOutgoingDeletion(node)) {
						return false;
					}
					return super.isEditable();
				}

				private boolean isOutgoingDeletion(IDiff node) {
					if (node instanceof IThreeWayDiff) {
						IThreeWayDiff twd = (IThreeWayDiff) node;
						return twd.getKind() == IDiff.REMOVE && twd.getDirection() == IThreeWayDiff.OUTGOING;
					}
					return false;
				}
			};
		}

		private static ITypedElement getAncestor(IDiff node) {
			if (node instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) node;
				IResourceDiff diff = (IResourceDiff)twd.getLocalChange();
				if (diff == null)
					diff = (IResourceDiff)twd.getRemoteChange();
				return asTypedElement(diff.getBeforeState());
				
			}
			return null;
		}

		private static ITypedElement asTypedElement(IFileRevision state) {
			if (state == null)
				return null;
			return new FileStateTypedElement(state);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.mapping.IModelCompareInput#prepareInput(org.eclipse.compare.CompareConfiguration, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void prepareInput(CompareConfiguration configuration, IProgressMonitor monitor) throws CoreException {
			Utils.updateLabels(node, configuration);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.mapping.IModelCompareInput#getCompareModel()
		 */
		public ISaveableCompareModel getSaveableModel() {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
		 */
		public Object getAdapter(Class adapter) {
			if (adapter == IFile.class || adapter == IResource.class) {
				if (node instanceof IResourceDiff) {
					IResourceDiff rd = (IResourceDiff) node;
					return (IFile)rd.getResource();
				}
				if (node instanceof IThreeWayDiff) {
					IThreeWayDiff twd = (IThreeWayDiff) node;
					IResourceDiff diff = (IResourceDiff)twd.getRemoteChange();
					// If there is a remote change, use the after state
					if (diff != null)
						return (IFile)diff.getResource();
					// There's no remote change so use the before state of the local
					diff = (IResourceDiff)twd.getLocalChange();
					return (IFile)diff.getResource();
					
				}
			}
			return null;
		}
		
	}

	/**
	 * Default implementaton that is capable of returning a compare input for objects
	 * that adapt to {@link IFile}. Subclasses shoudl override if compare inputs are
	 * availabel for other types of model elements.
	 * @see org.eclipse.team.ui.mapping.ICompareAdapter#asCompareInput(org.eclipse.team.ui.mapping.ISynchronizationContext, java.lang.Object)
	 */
	public ICompareInput asCompareInput(ISynchronizationContext context, Object o) {
		IResource resource = Utils.getResource(o);
		if (resource != null) {
			if (resource.getType() == IResource.FILE) {
				IDiff node = context.getDiffTree().getDiff(resource);
				if (node != null)
					return new ResourceDiffCompareInput(node);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ICompareAdapter#hasCompareInput(org.eclipse.team.core.mapping.ISynchronizationContext, java.lang.Object)
	 */
	public boolean hasCompareInput(ISynchronizationContext context, Object object) {
		return asCompareInput(context, object) != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IResourceMappingPersistenceAdapter#getName(org.eclipse.core.resources.mapping.ResourceMapping)
	 */
	public String getName(ResourceMapping mapping) {
		Object object = mapping.getModelObject();
		IWorkbenchAdapter adapter = (IWorkbenchAdapter) Utils.getAdapter(
				object, IWorkbenchAdapter.class);
		if (adapter != null) {
			String label = adapter.getLabel(object);
			if (label != null)
				return label;
		}
		IResource resource = Utils.getResource(object);
		if (resource != null)
			return resource.getName();
		if (object instanceof ModelProvider) {
			ModelProvider provider = (ModelProvider) object;
			return provider.getDescriptor().getLabel();
		}
		return ""; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IResourceMappingPersistenceAdapter#getFullPath(org.eclipse.core.resources.mapping.ResourceMapping)
	 */
	public IPath getFullPath(ResourceMapping mapping) {
		Object object = mapping.getModelObject();
		IWorkbenchAdapter adapter = (IWorkbenchAdapter) Utils.getAdapter(
				object, IWorkbenchAdapter.class);
		if (adapter != null) {
			List segments = new ArrayList();
			Object parent = object;
			do {
				String segment = adapter.getLabel(parent);
				if (segment != null && segment.length() > 0)
					segments.add(0, segment);
				parent = adapter.getParent(parent);
			} while (parent != null);
			if (!segments.isEmpty()) {
				IPath path = Path.EMPTY;
				for (Iterator iter = segments.iterator(); iter.hasNext();) {
					String segment = (String) iter.next();
					path = path.append(segment);
				}
				return path;
			}
		}
		return new Path(getName(mapping));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter#save(org.eclipse.core.resources.mapping.ResourceMapping[], org.eclipse.ui.IMemento)
	 */
	public void save(ResourceMapping[] mappings, IMemento memento) {
		// Do nothing by default
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter#restore(org.eclipse.ui.IMemento)
	 */
	public ResourceMapping[] restore(IMemento memento) {
		return new ResourceMapping[0];
	}
}