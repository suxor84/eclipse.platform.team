/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.filesystem.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.*;
import org.eclipse.team.examples.filesystem.*;

/**
 * This is an old-style (pre-3.0) project set serializer used to test backwards compatibility
 */
public class ProjectSetSerializer implements IProjectSetSerializer {

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.IProjectSetSerializer#asReference(org.eclipse.core.resources.IProject[], java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public String[] asReference(IProject[] providerProjects, Object context, IProgressMonitor monitor) {
		Assert.isTrue(context instanceof Shell);
		List refs = new ArrayList();
		for (int i = 0; i < providerProjects.length; i++) {
			IProject project = providerProjects[i];
			FileSystemProvider provider = (FileSystemProvider)RepositoryProvider.getProvider(project, FileSystemPlugin.PROVIDER_ID);
			if (provider != null) {
				refs.add(asReference(provider));
			}
		}
		return (String[]) refs.toArray(new String[refs.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.IProjectSetSerializer#addToWorkspace(java.lang.String[], java.lang.String, java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IProject[] addToWorkspace(String[] referenceStrings, String filename, Object context, IProgressMonitor monitor) {
		Assert.isTrue(context instanceof Shell);
		List projects = new ArrayList();
		for (int i = 0; i < referenceStrings.length; i++) {
			String string = referenceStrings[i];
			String projectName = getProjectName(string);
			String path = getPath(string);
			if (projectName != null && path != null) {
				try {
					IProject project = createProject(projectName, monitor);
					RepositoryProvider.map(project, FileSystemPlugin.PROVIDER_ID);
					FileSystemProvider provider = (FileSystemProvider) RepositoryProvider.getProvider(project);
					provider.setTargetLocation(path);
					projects.add(project);
				} catch (CoreException e) {
					ErrorDialog.openError(
						(Shell)context,
						Policy.bind("ConfigurationWizard.errorMapping"), //$NON-NLS-1$
						Policy.bind("ConfigurationWizard.error"), //$NON-NLS-1$
						e.getStatus());
				}
			}
		}
		return (IProject[]) projects.toArray(new IProject[projects.size()]);
	}

	/**
	 * @param provider
	 * @return
	 */
	private String asReference(FileSystemProvider provider) {
		return provider.getProject().getName() + "," + provider.getRoot().toString(); //$NON-NLS-1$
	}
	
	/**
	 * @param string
	 * @return
	 */
	private String getProjectName(String string) {
		int i = string.indexOf(',');
		if (i == -1) return null;
		return string.substring(0, i);
	}
	
	/**
	 * @param string
	 * @return
	 */
	private String getPath(String string) {
		int i = string.indexOf(',');
		if (i == -1) return null;
		return string.substring(i + 1);
	}
	
	/**
	 * @param projectName
	 * @return
	 * @throws CoreException
	 */
	private IProject createProject(String projectName, IProgressMonitor monitor) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (!project.exists()) {
			project.create(monitor);
		}
		if (!project.isOpen()) {
			project.open(monitor);
		}
		return project;
	}
}
