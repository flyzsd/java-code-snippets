package java.lang.ref;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2016  All Rights Reserved
 */

/**
 * PhantomReference objects are used to detect referents which
 * are no longer visible and are eligible to have their storage
 * reclaimed.
 *
 * @author		OTI
 * @version		initial
 * @since		JDK1.2
 */
public class PhantomReference<T> extends java.lang.ref.Reference<T> {

/**
 * Return the referent of the reference object.  Phantom reference
 * objects referents are inaccessible, and so null is returned.
 *
 * @return		Object
 *					Returns null.
 */
public T get() {
	return null;
}

/**
 * Constructs a new instance of this class.
 *
 * @param		r
 *					referent to track.
 * @param		q
 *					queue to register to the reference object with.
 */
public PhantomReference(T r, ReferenceQueue<? super T> q) {
	initReference(r, q);
}
}
