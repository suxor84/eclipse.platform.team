/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests.performance;

import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.internal.DocLineComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.test.performance.PerformanceTestCase;


public class RangeDifferencerTest extends PerformanceTestCase {

	public RangeDifferencerTest(String name) {
        super(name);
    }

	/*
	 * Creates document with 5000 lines.
	 * Parameter code determines where additional lines are added.
	 */
	private IDocument createDocument(int code) {
		StringBuffer sb= new StringBuffer();
		for (int i= 0; i < 5000; i++) {
			sb.append("line "); //$NON-NLS-1$
			sb.append(Integer.toString(i));
			sb.append('\n');
			
			int mod= i % 10;
			switch (code) {
			case 1:
				if (mod == 1)
					sb.append("outgoing\n"); //$NON-NLS-1$
				if (mod == 4)
					sb.append("conflict1\n");				 //$NON-NLS-1$
				break;
			case 2:
				if (mod == 7)
					sb.append("incoming\n"); //$NON-NLS-1$
				if (mod == 4)
					sb.append("conflict2\n");				 //$NON-NLS-1$
				break;
			}
		}
		return new Document(sb.toString());
	}
	
	public void testLargeDocument() {
			    
		ITokenComparator ancestor= new DocLineComparator(createDocument(0), null, false);
		ITokenComparator left= new DocLineComparator(createDocument(1), null, false);
		ITokenComparator right= new DocLineComparator(createDocument(2), null, false);

		RangeDifference[] diffs= null;
		
		// a warm up run
		diffs= RangeDifferencer.findRanges(new NullProgressMonitor(), ancestor, left, right);
		
		// assert that result correct
		for (int i= 0; i < diffs.length-6; i+= 6) {
			assertEquals(diffs[i+0].kind(), RangeDifference.NOCHANGE);
			assertEquals(diffs[i+1].kind(), RangeDifference.LEFT);
			assertEquals(diffs[i+2].kind(), RangeDifference.NOCHANGE);
			assertEquals(diffs[i+3].kind(), RangeDifference.CONFLICT);
			assertEquals(diffs[i+4].kind(), RangeDifference.NOCHANGE);
			assertEquals(diffs[i+5].kind(), RangeDifference.RIGHT);
		}
		
		// now do 3 performance runs
		for (int count= 0; count < 3; count++) {
			startMeasuring();
			RangeDifferencer.findRanges(new NullProgressMonitor(), ancestor, left, right);
			stopMeasuring();
		}
		
		commitMeasurements();
		assertPerformance();
	}
}