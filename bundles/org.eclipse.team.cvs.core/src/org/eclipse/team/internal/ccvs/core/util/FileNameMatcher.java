package org.eclipse.team.internal.ccvs.core.util;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.team.internal.ccvs.core.CVSException;

/**
 * A FileNameMatcher associates a String with a String pattern.
 */
public class FileNameMatcher {
	
	private List matchers = new ArrayList();
	private List results = new ArrayList();
	private static final String TRUE = "true"; //$NON-NLS-1$
	
	public FileNameMatcher() {
	}
	
	public FileNameMatcher(String[] patterns) {
		register(patterns);
	}
		
	void register(String[] patterns) {
		for (int i = 0; i < patterns.length; i++) {
			register(patterns[i],TRUE);
		}
	}
	
	public void register(String pattern, String result) {
		
		Assert.isTrue(matchers.size() == results.size());
		
		pattern = pattern.trim();
		
		// The empty pattern matches everything, but we want to match
		// nothing with it, so we just do not register anything
		if (pattern.length() == 0) {
			return;
		}
	
		matchers.add(new StringMatcher(pattern,false,false));
		results.add(result);
		
	}
	
	public String getMatch(String name) {
		StringMatcher stringMatcher;
		
		for (int i = 0; i < matchers.size(); i++) {
			stringMatcher = (StringMatcher) matchers.get(i);
			if (stringMatcher.match(name)) {
				return (String)results.get(i);
			}
		}
		
		return null;
	}
	
	public boolean match(String name) {
		return getMatch(name) != null;
	}
}