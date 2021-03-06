/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.mapping.MergeActionHandler;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.ide.IDE;

public abstract class ResourceMergeActionHandler extends MergeActionHandler implements IDiffChangeListener, IPropertyChangeListener {

	public ResourceMergeActionHandler(ISynchronizePageConfiguration configuration) {
		super(configuration);
		getSynchronizationContext().getDiffTree().addDiffChangeListener(this);
		configuration.addPropertyChangeListener(this);
	}

	/**
	 * Prompt to save all dirty editors and return whether to proceed
	 * or not.
	 * @return whether to proceed
	 * or not
	 */
	public final boolean saveDirtyEditors() {
		if(needsToSaveDirtyEditors()) {
			if(!saveAllEditors(getTargetResources(), confirmSaveOfDirtyEditor())) {
				return false;
			}
		}
		return true;
	}
	
	private IResource[] getTargetResources() {
		IStructuredSelection selection = getStructuredSelection();
		Object[] objects = selection.toArray();
		Set roots = new HashSet();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			ResourceMapping mapping = Utils.getResourceMapping(object);
			if (mapping != null) {
				try {
					ResourceTraversal[] traversals = mapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, null);
					for (int j = 0; j < traversals.length; j++) {
						ResourceTraversal traversal = traversals[j];
						IResource[] resources = traversal.getResources();
						for (int k = 0; k < resources.length; k++) {
							IResource resource = resources[k];
							roots.add(resource);
						}
					}
				} catch (CoreException e) {
					TeamUIPlugin.log(e);
				}
			}
		}
		return (IResource[]) roots.toArray(new IResource[roots.size()]);
	}

	/**
	 * Save all dirty editors in the workbench that are open on files that may
	 * be affected by this operation. Opens a dialog to prompt the user if
	 * <code>confirm</code> is true. Return true if successful. Return false
	 * if the user has canceled the command. Must be called from the UI thread.
	 * @param resources the root resources being operated on
	 * @param confirm prompt the user if true
	 * @return boolean false if the operation was canceled.
	 */
	public final boolean saveAllEditors(IResource[] resources, boolean confirm) {
		return IDE.saveAllEditors(resources, confirm);
	}

	/**
	 * Return whether dirty editor should be saved before this action is run.
	 * Default is <code>true</code>.
	 * 
	 * @return whether dirty editor should be saved before this action is run
	 */
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}
	
	/**
	 * Returns whether the user should be prompted to save dirty editors. The
	 * default is <code>true</code>.
	 * 
	 * @return whether the user should be prompted to save dirty editors
	 */
	protected boolean confirmSaveOfDirtyEditor() {
		return true;
	}
	
	protected ISynchronizationContext getSynchronizationContext() {
		return (ISynchronizationContext)getConfiguration().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
	}
	
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// Nothing to do
	}
	
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		Utils.syncExec(new Runnable() {
			public void run() {
				updateEnablement(getStructuredSelection());
			}
		}, (StructuredViewer)getConfiguration().getPage().getViewer());
		
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty() == ISynchronizePageConfiguration.P_MODE) {
			Utils.syncExec(new Runnable() {
				public void run() {
					updateEnablement(getStructuredSelection());
				}
			}, (StructuredViewer)getConfiguration().getPage().getViewer());
		}
	}

}
