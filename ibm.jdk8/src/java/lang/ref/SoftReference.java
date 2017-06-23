package java.lang.ref;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2016  All Rights Reserved
 */

/**
 * SoftReference objects are used to detect referents which
 * are no longer visible and who's memory is to be reclaimed.
 *
 * @author		OTI
 * @version		initial
 * @since		1.2
 */
public class SoftReference<T> extends java.lang.ref.Reference<T> {
	private volatile int age;

/**
 * Constructs a new instance of this class.
 *
 * @param		r	referent to track.
 * @param		q	queue to register to the reference object with.
 */
public SoftReference(T r, ReferenceQueue<? super T> q) {
	initReference(r, q);
}

/**
 * Constructs a new instance of this class.
*
 * @param		r	referent to track.
 */
public SoftReference(T r) {
	initReference(r);
}

/**
 * Return the referent of the reference object.
 *
 * @return	Referent to which reference refers,
 *			or null if object has been cleared.
 */
public T get () {
	age = 0;
	return super.get();
}
}
