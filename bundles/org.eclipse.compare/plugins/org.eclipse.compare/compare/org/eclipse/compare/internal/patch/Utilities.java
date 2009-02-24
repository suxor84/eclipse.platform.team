/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.patch.ReaderCreator;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class Utilities {

	public static String getCharset(Object resource) {
		if (resource instanceof IEncodedStorage) {
			try {
				return ((IEncodedStorage) resource).getCharset();
			} catch (CoreException ex) {
				CompareUIPlugin.log(ex);
			}
		}
		return ResourcesPlugin.getEncoding();
	}

	public static IProject getProject(DiffProject diffProject) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(
				diffProject.getName());
	}
	
	public static ReaderCreator getReaderCreator(final IStorage storage){
		return new ReaderCreator(){
			public Reader createReader() throws CoreException {
				return Utilities.createReader(storage);
			}	
		};
	}
	
	public static BufferedReader createReader(IStorage storage)
			throws CoreException {
		String charset = null;
		if (storage instanceof IEncodedStorage) {
			IEncodedStorage es = (IEncodedStorage) storage;
			charset = es.getCharset();
		}
		InputStreamReader in = null;
		if (charset != null) {
			InputStream contents = storage.getContents();
			try {
				in = new InputStreamReader(contents, charset);
			} catch (UnsupportedEncodingException e) {
				CompareUIPlugin.log(e);
				try {
					contents.close();
				} catch (IOException e1) {
					// Ignore
				}
			}
		}
		if (in == null) {
			in = new InputStreamReader(storage.getContents());
		}
		return new BufferedReader(in);
	}
}
