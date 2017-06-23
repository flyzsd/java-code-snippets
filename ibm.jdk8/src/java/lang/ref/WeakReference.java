package java.lang.ref;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2016  All Rights Reserved
 */

/**
 * WeakReference objects are used to detect referents which
 * are no longer visible.
 *
 * @author		OTI
 * @version		initial
 * @since		1.2
 */
public class WeakReference<T> extends java.lang.ref.Reference<T> {

/**
 * Constructs a new instance of this class.
 *
 * @param		r	referent to track.
 * @param		q	queue to register to the reference object with.
 */
public WeakReference(T r, ReferenceQueue<? super T> q) {
	initReference(r, q);
}

/**
 * Constructs a new instance of this class.
 *
 * @param	r	referent to track.
 */
public WeakReference(T r) {
	initReference(r);
}
}
