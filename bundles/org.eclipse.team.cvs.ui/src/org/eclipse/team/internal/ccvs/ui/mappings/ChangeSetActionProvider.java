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
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.ccvs.core.mapping.ChangeSetModelProvider;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.core.subscribers.*;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentService;

public class ChangeSetActionProvider extends SynchronizationActionProvider {

	/**
     * Menu group that can be added to the context menu
     */
    public final static String CHANGE_SET_GROUP = "changeSetActions"; //$NON-NLS-1$
    
	private CreateChangeSetAction createChangeSet;
	private MenuManager addToChangeSet;
    private EditChangeSetAction editChangeSet;
    private RemoveChangeSetAction removeChangeSet;
    private MakeDefaultChangeSetAction makeDefault;
    
	private class CreateChangeSetAction extends ModelParticipantAction {
	    
        public CreateChangeSetAction(ISynchronizePageConfiguration configuration) {
            super(TeamUIMessages.ChangeLogModelProvider_0, configuration); 
        }

        public void run() {
        	final IDiff[] diffs = getLocalChanges(getStructuredSelection());
        	syncExec(new Runnable() {
				public void run() {
					createChangeSet(diffs);
				}
			});
        }
        
		private void createChangeSet(IDiff[] diffs) {
            ActiveChangeSet set =  getChangeSetCapability().createChangeSet(getConfiguration(), diffs);
            if (set != null) {
                getActiveChangeSetManager().add(set);
            }
        }
        
		protected boolean isEnabledForSelection(IStructuredSelection selection) {
			return isContentProviderEnabled() && containsLocalChanges(selection);
		}
	}
	
	private class AddToChangeSetAction extends ModelParticipantAction {
		 
        private final ActiveChangeSet set;
	    
        public AddToChangeSetAction(ISynchronizePageConfiguration configuration, ActiveChangeSet set, ISelection selection) {
            super(set == null ? TeamUIMessages.ChangeSetActionGroup_2 : set.getTitle(), configuration); 
            this.set = set;
            selectionChanged(selection);
        }
        
        public void run() {
        	IDiff[] diffArray = getLocalChanges(getStructuredSelection());
            if (set != null) {
				set.add(diffArray);
            } else {
                ChangeSet[] sets = getActiveChangeSetManager().getSets();
                IResource[] resources = getResources(diffArray);
                for (int i = 0; i < sets.length; i++) {
                    ActiveChangeSet activeSet = (ActiveChangeSet)sets[i];
					activeSet.remove(resources);
                }
            }
        }

		private IResource[] getResources(IDiff[] diffArray) {
			List result = new ArrayList();
			for (int i = 0; i < diffArray.length; i++) {
				IDiff diff = diffArray[i];
				IResource resource = ResourceDiffTree.getResourceFor(diff);
				if (resource != null) {
					result.add(resource);
				}
			}
			return (IResource[]) result.toArray(new IResource[result.size()]);
		}

		protected boolean isEnabledForSelection(IStructuredSelection selection) {
			return isContentProviderEnabled() && containsLocalChanges(selection);
		}
	}

	private abstract class ChangeSetAction extends BaseSelectionListenerAction {

        public ChangeSetAction(String title, ISynchronizePageConfiguration configuration) {
            super(title);
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.ui.actions.BaseSelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
         */
        protected boolean updateSelection(IStructuredSelection selection) {
            return getSelectedSet() != null;
        }

        protected ActiveChangeSet getSelectedSet() {
            IStructuredSelection selection = getStructuredSelection();
            if (selection.size() == 1) {
                Object first = selection.getFirstElement();
                if (first instanceof ActiveChangeSet) {
					return (ActiveChangeSet) first;
				}
            }
            return null;
        }
	}
	
	private class EditChangeSetAction extends ChangeSetAction {

        public EditChangeSetAction(ISynchronizePageConfiguration configuration) {
            super(TeamUIMessages.ChangeLogModelProvider_6, configuration); 
        }
        
        public void run() {
            ActiveChangeSet set = getSelectedSet();
            if (set == null) return;
            getChangeSetCapability().editChangeSet(getSynchronizePageConfiguration(), set);
        }
	}
	
	private class RemoveChangeSetAction extends ChangeSetAction {

        public RemoveChangeSetAction(ISynchronizePageConfiguration configuration) {
            super("Remove Change Set", configuration); //$NON-NLS-1$
        }
        
        public void run() {
            ActiveChangeSet set = getSelectedSet();
            if (set == null) return;
            if (MessageDialog.openConfirm(getSynchronizePageConfiguration().getSite().getShell(), TeamUIMessages.ChangeSetActionGroup_0, NLS.bind(TeamUIMessages.ChangeSetActionGroup_1, new String[] { set.getTitle() }))) { // 
                getActiveChangeSetManager().remove(set);
            }
        }
	}
	
	private class MakeDefaultChangeSetAction extends ChangeSetAction {
        public MakeDefaultChangeSetAction(ISynchronizePageConfiguration configuration) {
            super(TeamUIMessages.ChangeLogModelProvider_9, configuration); 
        }
        
        public void run() {
            ActiveChangeSet set = getSelectedSet();
            if (set == null) return;
    		getActiveChangeSetManager().makeDefault(set);
        }
	}
	
	public ChangeSetActionProvider() {
		super();
	}

	protected void initialize() {
		super.initialize();
		if (getChangeSetCapability().supportsActiveChangeSets()) {
			addToChangeSet = new MenuManager(TeamUIMessages.ChangeLogModelProvider_12); 
			addToChangeSet.setRemoveAllWhenShown(true);
			addToChangeSet.addMenuListener(new IMenuListener() {
	            public void menuAboutToShow(IMenuManager manager) {
	                addChangeSets(manager);
	            }
	        });
			createChangeSet = new CreateChangeSetAction(getSynchronizePageConfiguration());
			addToChangeSet.add(createChangeSet);
			addToChangeSet.add(new Separator());
			editChangeSet = new EditChangeSetAction(getSynchronizePageConfiguration());
			makeDefault = new MakeDefaultChangeSetAction(getSynchronizePageConfiguration());
			removeChangeSet = new RemoveChangeSetAction(getSynchronizePageConfiguration());
		}
	}
	
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
        if (getChangeSetCapability().enableActiveChangeSetsFor(getSynchronizePageConfiguration())) {
			appendToGroup(
					menu, 
					CHANGE_SET_GROUP, 
					addToChangeSet);
			appendToGroup(
					menu, 
					CHANGE_SET_GROUP, 
					editChangeSet);
			appendToGroup(
					menu, 
					CHANGE_SET_GROUP, 
					removeChangeSet);
			appendToGroup(
					menu, 
					CHANGE_SET_GROUP, 
					makeDefault);
        }
	}
	
	public void dispose() {
	    if (addToChangeSet != null) {
			addToChangeSet.dispose();
			addToChangeSet.removeAll();
	    }
		super.dispose();
	}
	
    protected void addChangeSets(IMenuManager manager) {
        ChangeSet[] sets = getActiveChangeSetManager().getSets();
        ISelection selection = getContext().getSelection();
        createChangeSet.selectionChanged(selection);
		addToChangeSet.add(createChangeSet);
		addToChangeSet.add(new Separator());
        for (int i = 0; i < sets.length; i++) {
            ActiveChangeSet set = (ActiveChangeSet)sets[i];
            AddToChangeSetAction action = new AddToChangeSetAction(getSynchronizePageConfiguration(), set, selection);
            manager.add(action);
        }
        addToChangeSet.add(new Separator());
        // Action that removes change set resources
        addToChangeSet.add(new AddToChangeSetAction(getSynchronizePageConfiguration(), null, selection));
    }
	
	private boolean appendToGroup(IContributionManager manager, String groupId, IContributionItem item) {
		if (manager == null || item == null) return false;
		IContributionItem group = manager.find(groupId);
		if (group != null) {
			manager.appendToGroup(group.getId(), item);
			return true;
		}
		return false;
	}
	
	private boolean appendToGroup(IContributionManager manager, String groupId, IAction action) {
		if (manager == null || action == null) return false;
		IContributionItem group = manager.find(groupId);
		if (group != null) {
			manager.appendToGroup(group.getId(), action);
			//registerActionWithWorkbench(action);
			return true;
		}
		return false;
	}
	
    public ChangeSetCapability getChangeSetCapability() {
        ISynchronizeParticipant participant = getSynchronizePageConfiguration().getParticipant();
        if (participant instanceof IChangeSetProvider) {
            IChangeSetProvider provider = (IChangeSetProvider) participant;
            return provider.getChangeSetCapability();
        }
        return null;
    }
    
    private void syncExec(final Runnable runnable) {
		final Control ctrl = getSynchronizePageConfiguration().getPage().getViewer().getControl();
		Utils.syncExec(runnable, ctrl);
    }

	private SubscriberChangeSetCollector getActiveChangeSetManager() {
		return CVSUIPlugin.getPlugin().getChangeSetManager();
	}

	
    public IDiff[] getLocalChanges(IStructuredSelection selection) {
		if (selection instanceof ITreeSelection) {
			ITreeSelection ts = (ITreeSelection) selection;
			TreePath[] paths = ts.getPaths();
			List result = new ArrayList();
			for (int i = 0; i < paths.length; i++) {
				TreePath path = paths[i];
				IDiff[] diffs = getLocalChanges(path);
				for (int j = 0; j < diffs.length; j++) {
					IDiff diff = diffs[j];
					result.add(diff);
				}
			}
			return (IDiff[]) result.toArray(new IDiff[result.size()]);
		}
		return new IDiff[0];
	}

	private IDiff[] getLocalChanges(TreePath path) {
		IResourceDiffTree tree = getDiffTree(path);
		if (path.getSegmentCount() == 1) {
			return ((ResourceDiffTree)tree).getDiffs();
		}
		ResourceTraversal[] traversals = getTraversals(path.getLastSegment());
		return tree.getDiffs(traversals);
	}

	private IResourceDiffTree getDiffTree(TreePath path) {
		return getContentProvider().getDiffTree(path);
	}

	public boolean containsLocalChanges(IStructuredSelection selection) {
		if (selection instanceof ITreeSelection) {
			ITreeSelection ts = (ITreeSelection) selection;
			TreePath[] paths = ts.getPaths();
			for (int i = 0; i < paths.length; i++) {
				TreePath path = paths[i];
				if (containsLocalChanges(path)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean containsLocalChanges(TreePath path) {
		IResourceDiffTree tree = getDiffTree(path);
		ResourceTraversal[] traversals = getTraversals(path.getLastSegment());
		return tree.hasMatchingDiffs(traversals, getVisibleLocalChangesFilter());
	}

	private ResourceTraversal[] getTraversals(Object element) {
		if (element instanceof ChangeSet) {
			ChangeSet set = (ChangeSet) element;
			return new ResourceTraversal[] { new ResourceTraversal(set.getResources(), IResource.DEPTH_ZERO, IResource.NONE) };
		}
		if (element instanceof IProject) {
			IProject project = (IProject) element;
			return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { project }, IResource.DEPTH_INFINITE, IResource.NONE) };
		}
		if (element instanceof IFile) {
			IFile file = (IFile) element;
			return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { file }, IResource.DEPTH_ZERO, IResource.NONE) };
		}
		if (element instanceof IFolder) {
			IFolder folder = (IFolder) element;
			if (getLayout().equals(IPreferenceIds.COMPRESSED_LAYOUT)) {
				return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { folder }, IResource.DEPTH_ONE, IResource.NONE) };
			} else if (getLayout().equals(IPreferenceIds.TREE_LAYOUT)) {
				return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { folder }, IResource.DEPTH_INFINITE, IResource.NONE) };
			} else if (getLayout().equals(IPreferenceIds.FLAT_LAYOUT)) {
				return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { folder }, IResource.DEPTH_ZERO, IResource.NONE) };
			}
		}
		return null;
	}
	
	private FastDiffFilter getVisibleLocalChangesFilter() {
	    return new FastDiffFilter() {
			public boolean select(IDiff diff) {
				if (diff instanceof IThreeWayDiff && isVisible(diff)) {
					IThreeWayDiff twd = (IThreeWayDiff) diff;
					if (twd.getDirection() == IThreeWayDiff.OUTGOING || twd.getDirection() == IThreeWayDiff.CONFLICTING) {
						return true;
					}
				}
				return false;
			}
		};
	}

	private boolean isVisible(IDiff diff) {
		ChangeSetContentProvider provider = getContentProvider();
		if (provider == null)
			return false;
		return provider.isVisible(diff);
	}
	
	private ChangeSetContentProvider getContentProvider() {
		INavigatorContentService service = getActionSite().getContentService();
		Set set = service.findContentExtensionsByTriggerPoint(getModelProvider());
		for (Iterator iter = set.iterator(); iter.hasNext();) {
			INavigatorContentExtension extension = (INavigatorContentExtension) iter.next();
			ITreeContentProvider provider = extension.getContentProvider();
			if (provider instanceof ChangeSetContentProvider) {
				return (ChangeSetContentProvider) provider;
			}
		}
		return null;
	}

	private Object getModelProvider() {
		return ChangeSetModelProvider.getProvider();
	}

	private String getLayout() {
		return TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCVIEW_DEFAULT_LAYOUT);
	}
	
	public void setContext(ActionContext context) {
		super.setContext(context);
		if (context != null) {
	        if (editChangeSet != null)
		        editChangeSet.selectionChanged((IStructuredSelection)getContext().getSelection());
	        if (removeChangeSet != null)
	            removeChangeSet.selectionChanged((IStructuredSelection)getContext().getSelection());
	        if (makeDefault != null)
		        makeDefault.selectionChanged((IStructuredSelection)getContext().getSelection());
		}
	}

	protected boolean isContentProviderEnabled() {
		return getContentProvider() != null;
	}
}
