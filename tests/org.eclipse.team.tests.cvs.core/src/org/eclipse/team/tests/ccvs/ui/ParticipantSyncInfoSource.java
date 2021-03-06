/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui;

import junit.framework.AssertionFailedError;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SynchronizeView;
import org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPage;

public class ParticipantSyncInfoSource extends SyncInfoSource {

    public static ISynchronizePage getSyncViewPage(ISynchronizeParticipant participant) throws PartInitException {
		IWorkbenchPage activePage = TeamUIPlugin.getActivePage();
		ISynchronizeView view = (ISynchronizeView)activePage.showView(ISynchronizeView.VIEW_ID);
		IPage page = ((SynchronizeView)view).getPage(participant);
		return (ISynchronizePage)page;
	}
    
	public ParticipantSyncInfoSource() {
		IWorkbenchPage activePage = TeamUIPlugin.getActivePage();
		try {
			activePage.showView(ISynchronizeView.VIEW_ID);
		} catch (PartInitException e) {
			throw new AssertionFailedError("Cannot show sync view in active page");
		}
	}
	
	protected SyncInfoToDiffConverter getConverter(Subscriber subscriber) {
		SyncInfoToDiffConverter converter = (SyncInfoToDiffConverter)Utils.getAdapter(subscriber, SyncInfoToDiffConverter.class);
		if (converter == null)
			converter = SyncInfoToDiffConverter.getDefault();
		return converter;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource#tearDown()
	 */
	public void tearDown() {
		ISynchronizeParticipantReference[] participants = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (int i = 0; i < participants.length; i++) {
			try {
				ISynchronizeParticipantReference ref = participants[i];
				if(ref.getParticipant().getId().equals(CVSMergeSubscriber.ID)) {
					TeamUI.getSynchronizeManager().removeSynchronizeParticipants(new ISynchronizeParticipant[] {ref.getParticipant()});
				}
			} catch (TeamException e) {
				return;
			}
		}
		// Process all async events that may have been generated above
		while (Display.getCurrent().readAndDispatch()) {};
	}
	
	protected void showParticipant(ISynchronizeParticipant participant) throws AssertionFailedError {
		ISynchronizeManager synchronizeManager = TeamUI.getSynchronizeManager();
		synchronizeManager.addSynchronizeParticipants(
				new ISynchronizeParticipant[] {participant});	
		IWorkbenchPage activePage = TeamUIPlugin.getActivePage();
		try {
			ISynchronizeView view = (ISynchronizeView)activePage.showView(ISynchronizeView.VIEW_ID);
			view.display(participant);
		} catch (PartInitException e) {
			throw new AssertionFailedError("Cannot show sync view in active page");
		}
	}
	
	/**
	 * Assert that the model for the subscriber matches what is being displayed.
	 * Default is to do nothing. Subclasses may override
	 * @param subscriber the subscriber
	 */
	public void assertViewMatchesModel(Subscriber subscriber) {
	    // Default is to do nothing. Subclasses may override
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource#refresh(org.eclipse.team.core.subscribers.Subscriber, org.eclipse.core.resources.IResource[])
     */
    public void refresh(Subscriber subscriber, IResource[] resources)
            throws TeamException {
        super.refresh(subscriber, resources);
		assertViewMatchesModel(subscriber);
    }

}
