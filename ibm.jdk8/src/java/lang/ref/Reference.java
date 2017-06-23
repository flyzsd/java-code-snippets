package java.lang.ref;

import com.ibm.oti.vm.VM;
/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2016  All Rights Reserved
 */

/**
 * Abstract class which describes behavior common to all reference objects.
 *
 * @author		OTI
 * @version		initial
 * @since		1.2
 */
public abstract class Reference<T> extends Object {
	private static final int STATE_INITIAL = 0;
	private static final int STATE_CLEARED = 1;
	private static final int STATE_ENQUEUED = 2;

	private T referent;
	private ReferenceQueue queue;
	private int state;

/**
 * Make the referent null.  This does not force the reference object to be enqueued.
 */
public void clear() {
	synchronized(this) {
		referent = null;
		/* change the state to cleared if it's not already cleared or enqueued */
		if (STATE_INITIAL == state) {
			state = STATE_CLEARED;
		}
	}
}

/**
 * Force the reference object to be enqueued if it has been associated with a queue.
 *
 * @return	true if Reference is enqueued, false otherwise.
 */
public boolean enqueue() {
	return enqueueImpl();
}

/**
 * Return the referent of the reference object.
 *
 * @return	the referent to which reference refers,
 *			or null if object has been cleared.
 */
public T get() {
	if (VM.J9_GC_POLICY != VM.J9_GC_POLICY_METRONOME) {
		return referent;
	}
	return getImpl();
}

private native T getImpl();

/**
 * Return whether the reference object has been enqueued.
 *
 * @return	true if Reference has been enqueued, false otherwise.
 */
public boolean isEnqueued () {
	synchronized(this) {
		return state == STATE_ENQUEUED;
	}
}

/**
 * Enqueue the reference object on the associated queue.
 *
 * @return	true if the Reference was successfully
 *			enqueued, false otherwise.
 */
boolean enqueueImpl() {
	final ReferenceQueue tempQueue;
	boolean result;
	T tempReferent = referent;
	synchronized(this) {
		/* Static order for the following code (DO NOT CHANGE) */
		tempQueue = queue;
		queue = null;
		if (state == STATE_ENQUEUED || tempQueue == null) {
			return false;
		}
		result = tempQueue.enqueue(this);
		if (result) {
			state = STATE_ENQUEUED;
			if (null != tempReferent) {
				reprocess();
			}
		}
		return result;
	}
}

private native void reprocess();

/**
 * Constructs a new instance of this class.
 */
Reference() {
}

/**
 * Initialize a newly created reference object. Associate the
 * reference object with the referent.
 *
 * @param r the referent
 */
void initReference (T r) {
	state = STATE_INITIAL;
	referent = r;
}

/**
 * Initialize a newly created reference object.  Associate the
 * reference object with the referent, and the specified ReferenceQueue.
 *
 * @param r the referent
 * @param q the ReferenceQueue
 */
void initReference (T r, ReferenceQueue q) {
	queue = q;
	state = STATE_INITIAL;
	referent = r;
}

/**
 * Called when a Reference has been removed from its ReferenceQueue.
 * Set the enqueued field to false.
 */
void dequeue() {
	synchronized(this) {
		state = STATE_CLEARED;
	}
}
}
