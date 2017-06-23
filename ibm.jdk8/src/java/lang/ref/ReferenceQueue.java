package java.lang.ref;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2016  All Rights Reserved
 */

/**
 * ReferenceQueue is the container on which reference objects
 * are enqueued when their reachability type is detected for
 * the referent.
 *
 * @author		OTI
 * @version		initial
 * @since		1.2
 */

public class ReferenceQueue<T> extends Object {
	private Reference[] references;
	private int head, tail;
	private boolean empty;

	static private final int DEFAULT_QUEUE_SIZE = 128;

	private static final Class reflectRefClass;

	private static final Class classNameLockRefClass;

	static {
		// cause sun.misc.Cleaner to be loaded
		Class cl = sun.misc.Cleaner.class;
		Class tmpClass = null;
		try {
			tmpClass = Class.forName("java.lang.Class$ReflectRef"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {}
		reflectRefClass = tmpClass;

		Class tmpClass2 = null;
		try {
			tmpClass2 = Class.forName("java.lang.ClassLoader$ClassNameLockRef"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {}
		classNameLockRefClass = tmpClass2;
	}

/**
 * Returns the next available reference from the queue
 * if one is enqueued, null otherwise.  Does not wait
 * for a reference to become available.
 *
 * @return		Reference
 *					next available Reference or NULL.
 */
public Reference<? extends T> poll () {
	Reference ref;

	/* Optimization to return immediately and not synchronize if there is nothing in the queue */
	if(empty) {
		return null;
	}
	synchronized(this) {
		if(empty) {
			return null;
		}
		ref = references[head];
		references[head++] = null;
		ref.dequeue();
		if(head == references.length) {
			head = 0;
		}
		if(head == tail) {
			empty = true;
		}
	}
	return ref;
}

/**
 * Return the next available enqueued reference on the queue, blocking
 * indefinitely until one is available.
 *
 * @author		OTI
 * @version		initial
 *
 * @return		Reference
 *					a Reference object if one is available,
 *                  null otherwise.
 * @exception	InterruptedException
 *					to interrupt the wait.
 */
public Reference<? extends T> remove() throws InterruptedException {
	return remove(0L);
}

/**
 * Return the next available enqueued reference on the queue, blocking
 * up to the time given until one is available.  Return null if no
 * reference became available.
 *
 * @author		OTI
 * @version		initial
 *
 * @param		timeout
 *					maximum time spent waiting for a reference object
 *					to become available.
 * @return		Reference
 *					a Reference object if one is available,
 *                  null otherwise.
 * @exception	IllegalArgumentException
 *					if the wait period is negative.
 * @exception	InterruptedException
 *					to interrupt the wait.
 */
public Reference<? extends T> remove(long timeout) throws IllegalArgumentException, InterruptedException {
	if (timeout < 0) throw new IllegalArgumentException();

	Reference ref;
	synchronized(this) {
		if(empty) {
			wait(timeout);
			if(empty) return null;
		}
		ref = references[head];
		references[head++] = null;
		ref.dequeue();
		if(head == references.length) {
			head = 0;
		}
		if(head == tail) {
			empty = true;
		} else {
			notifyAll();
		}
	}
	return ref;
}

/**
 * Enqueue the reference object on the receiver.
 *
 * @param		reference
 *					reference object to be enqueued.
 * @return		boolean
 *					true if reference is enqueued.
 *					false if reference failed to enqueue.
 */
boolean enqueue (Reference reference) {
	if (reference instanceof sun.misc.Cleaner) {
		reference.dequeue();
		((sun.misc.Cleaner)reference).clean();
		return true;
	}
	Class refClass = reference.getClass();
	if (refClass == reflectRefClass
			||	refClass == classNameLockRefClass
	) {
		reference.dequeue();
		((Runnable)reference).run();
		return true;
	}
	synchronized(this) {
		if ( references == null) {
			references =  new Reference[DEFAULT_QUEUE_SIZE];
		} else if(!empty && head == tail) {
			/* Queue is full - grow */
			int newQueueSize = (int)(references.length * 1.10);
			Reference newQueue[] = new Reference[newQueueSize];
			System.arraycopy(references, head, newQueue, 0, references.length - head);
			if(tail > 0) {
				System.arraycopy(references, 0, newQueue, references.length - head, tail);
			}
			head = 0;
			tail = references.length;
			references = newQueue;
		}
		references[tail++] = reference;
		if(tail == references.length) {
			tail = 0;
		}
		empty = false;
		notifyAll();
	}
	return true;
}

/**
 * Constructs a new instance of this class.
 */
public ReferenceQueue() {
	head = 0;
	tail = 0;
	empty = true;
}
}
