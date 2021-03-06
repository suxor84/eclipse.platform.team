/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * This class provides common behavior between the branch and date tag categories
 */
public abstract class TagCategory extends CVSModelElement {
	protected ICVSRepositoryLocation repository;
	
	public TagCategory(ICVSRepositoryLocation repository) {
		this.repository = repository;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.model.CVSModelElement#fetchChildren(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Object[] fetchChildren(Object o, IProgressMonitor monitor) throws CVSException {
		CVSTag[] tags = getTags(monitor);
		CVSTagElement[] elements = new CVSTagElement[tags.length];
		for (int i = 0; i < tags.length; i++) {
			elements[i] = new CVSTagElement(tags[i], repository);
		}
		return elements;
	}

	/**
	 * Return the tags that are to be displyed as children of this category
	 * @param monitor
	 * @return
	 */
	protected abstract CVSTag[] getTags(IProgressMonitor monitor) throws CVSException;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return repository;
	}
	
	/**
	 * Return the repository the given element belongs to.
	 */
	public ICVSRepositoryLocation getRepository(Object o) {
		return repository;
	}

	/**
	 * Returns an object which is an instance of the given class
	 * associated with this object. Returns <code>null</code> if
	 * no such object can be found.
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) return this;
		return null;
	}

}
