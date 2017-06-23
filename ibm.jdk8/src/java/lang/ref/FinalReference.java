package java.lang.ref;

/*******************************************************************************
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 *
 *     Copyright IBM Corp. 2003, 2016 All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/

final class FinalReference<T> extends Reference<T> {

	public FinalReference(T referent, ReferenceQueue<? super T> q) {
		initReference(referent, q);
	}

}
