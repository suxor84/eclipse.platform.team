/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.examples.xml;

import java.text.MessageFormat;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.eclipse.compare.examples.xml.ui.StatusDialog;
import org.eclipse.compare.examples.xml.ui.StatusInfo;

/**
 * This class is used to add or edit an ID Mapping Scheme
 */
public class XMLCompareAddIdMapDialog extends StatusDialog {
	
	private IdMap fIdMap;
	private HashMap fIdMaps;
	private HashMap fIdMapsInternal;
	private HashMap fIdExtensionToName;
	private boolean fEdit;
	
	private Text fIdMapText;
	private Text fIdMapExtText;

	public XMLCompareAddIdMapDialog(Shell parent, IdMap idmap, HashMap idmaps, HashMap idmapsInternal, HashMap idextensiontoname, boolean edit) {
		super(parent);
	
		fEdit= edit;
		if (fEdit)
			setTitle(XMLCompareMessages.XMLCompareAddIdMapDialog_editTitle); 
		else
			setTitle(XMLCompareMessages.XMLCompareAddIdMapDialog_newTitle); 

		fIdMap= idmap;
		fIdMaps= idmaps;
		fIdMapsInternal= idmapsInternal;
		fIdExtensionToName= idextensiontoname;
	}
	
	/**
	 * Creates and returns the contents of the upper part 
	 * of the dialog (above the button bar).
	 *
	 * Subclasses should override.
	 *
	 * @param ancestor the parent composite to contain the dialog area
	 * @return the dialog area control
	 */		
	protected Control createDialogArea(Composite ancestor) {
		Composite composite= (Composite) super.createDialogArea(ancestor);
		
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label= new Label(inner, SWT.NULL);
		label.setText(XMLCompareMessages.XMLCompareAddIdMapDialog_label); 
		label.setLayoutData(new GridData());

		fIdMapText= new Text(inner, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertWidthInCharsToPixels(30);
		fIdMapText.setLayoutData(data);
		fIdMapText.setText(fIdMap.getName());
		fIdMapText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e){
				doValidation();
			}
		});
	
		label= new Label(inner, SWT.NULL);
		label.setText(XMLCompareMessages.XMLCompareAddIdMapDialog_extlabel); 
		label.setLayoutData(new GridData());

		fIdMapExtText= new Text(inner, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertWidthInCharsToPixels(30);
		fIdMapExtText.setLayoutData(data);
		fIdMapExtText.setText(fIdMap.getExtension());
		fIdMapExtText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e){
				doValidation();
			}
		});

		
		fIdMapText.setFocus();

		return composite;
	}
	
	/**
	 * Validate user input
	 */		
	private void doValidation() {
		StatusInfo status= new StatusInfo();
		String newText= fIdMapText.getText();
		if (newText.length() == 0)
			status.setError(XMLCompareMessages.XMLCompareAddIdMapDialog_error_noname); 
		else if (XMLComparePreferencePage.containsInvalidCharacters(newText))
			status.setError(XMLCompareMessages.XMLCompareAddIdMapDialog_error_invalidname); 
		else if ( (!fEdit && (fIdMaps.containsKey(newText) || fIdMapsInternal.containsKey(newText)) )
					|| (fEdit && !newText.equals(fIdMap.getName()) && (fIdMaps.containsKey(newText) || fIdMapsInternal.containsKey(newText)) )
				 )
			status.setError(XMLCompareMessages.XMLCompareAddIdMapDialog_error_idmapExists); 
		newText= fIdMapExtText.getText().toLowerCase();
		if (newText.length() > 0) {
			if (newText.indexOf(".") > -1) //$NON-NLS-1$
				status.setError(XMLCompareMessages.XMLCompareAddIdMapDialog_error_extfullstop); 
			else if (fIdExtensionToName.containsKey(newText) && !fIdExtensionToName.get(newText).equals(fIdMap.getName()))
				status.setError(MessageFormat.format("{0} {1}",new String[] {XMLCompareMessages.XMLCompareAddIdMapDialog_error_extExists,(String)fIdExtensionToName.get(newText)})); 
		}
		updateStatus(status);
	}
	
	/**
	 * Notifies that the ok button of this dialog has been pressed.
	 */	
	protected void okPressed() {
		fIdMap.setName(fIdMapText.getText());
		fIdMap.setExtension(fIdMapExtText.getText().toLowerCase());
		super.okPressed();
	}
}