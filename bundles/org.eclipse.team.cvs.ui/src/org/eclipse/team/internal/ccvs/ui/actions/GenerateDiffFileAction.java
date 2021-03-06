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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard;
import org.eclipse.ui.PlatformUI;

/**
 * Action to generate a patch file using the CVS diff command.
 * 
 * NOTE: This is a temporary action and should eventually be replaced
 * by a create patch command in the compare viewer.
 */
public class GenerateDiffFileAction extends WorkspaceTraversalAction{
    
	/** (Non-javadoc)
	 * Method declared on IActionDelegate.
	 */
	public void execute(IAction action) {

		try {
			final IResource [][] resources = new IResource[][] { null };
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						resources[0] = getDeepResourcesToPatch(monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
			GenerateDiffFileWizard.run(getTargetPart(), resources[0]);
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.LOG_NONTEAM_EXCEPTIONS);
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	 private IResource[] getDeepResourcesToPatch(IProgressMonitor monitor) throws CoreException {
	        ResourceMapping[] mappings = getCVSResourceMappings();
	        List roots = new ArrayList();
	        for (int i = 0; i < mappings.length; i++) {
	            ResourceMapping mapping = mappings[i];
	            ResourceTraversal[] traversals = mapping.getTraversals(
	            		SubscriberResourceMappingContext.createContext(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber()), 
	            		monitor);
	            for (int j = 0; j < traversals.length; j++) {
	                ResourceTraversal traversal = traversals[j];
	                IResource[] resources = traversal.getResources();
	                if (traversal.getDepth() == IResource.DEPTH_INFINITE) {
	                    roots.addAll(Arrays.asList(resources));
	                } else if (traversal.getDepth() == IResource.DEPTH_ZERO) {
	                    collectShallowFiles(resources, roots);
	                } else if (traversal.getDepth() == IResource.DEPTH_ONE) {
	                    collectShallowFiles(resources, roots);
	                    for (int k = 0; k < resources.length; k++) {
	                        IResource resource = resources[k];
	                        if (resource.getType() != IResource.FILE) {
	                            collectShallowFiles(members(resource), roots);
	                        }
	                    }
	                }
	            }
	        }
	        return (IResource[]) roots.toArray(new IResource[roots.size()]);
	    }
	 
	   private void collectShallowFiles(IResource[] resources, List roots) {
	        for (int k = 0; k < resources.length; k++) {
	            IResource resource = resources[k];
	            if (resource.getType() == IResource.FILE)
	                roots.add(resource);
	        }
	    }
	   
	   private IResource[] members(IResource resource) throws CoreException {
	        return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().members(resource);
	    }
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForMultipleResources()
	 */
	protected boolean isEnabledForMultipleResources() {
			return true;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getId()
	 */
	public String getId() {
		return ICVSUIConstants.CMD_CREATEPATCH;
	}
}
