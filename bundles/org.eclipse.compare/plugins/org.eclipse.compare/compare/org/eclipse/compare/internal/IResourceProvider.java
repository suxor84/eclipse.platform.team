/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.core.resources.IResource;

public interface IResourceProvider {

	/**
	 * Returns the corresponding resource for this object or <code>null</code>.
	 *
	 * @return the corresponding resource or <code>null</code>
	 */
	IResource getResource();
}