/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.ui.TeamOperation;

public class EclipseRunnable implements Runnable {
	private TeamOperation op;
	private Exception ex;
	private IProgressMonitor monitor;
	
	public EclipseRunnable(TeamOperation op, IProgressMonitor monitor) {
		this.monitor = monitor;
		this.op = op;
	}
	
	public void run() {
		try {
			op.run(monitor);
		} catch (InvocationTargetException e) {
			ex = e;
		} catch (InterruptedException e) {
			ex = e;
		}
	}
	
	public Exception getException(){
		return ex;
	}
}