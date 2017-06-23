package java.lang;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2016  All Rights Reserved
 */

import java.util.Map;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 *	A Thread is a unit of concurrent execution in Java. It has its own call stack
 * for methods being called and their parameters. Threads in the same VM interact and
 * synchronize by the use of shared Objects and monitors associated with these objects.
 * Synchronized methods and part of the API in Object also allow Threads to cooperate.
 *
 *	When a Java program starts executing there is an implicit Thread (called "main")
 * which is automatically created by the VM. This Thread belongs to a ThreadGroup
 * (also called "main") which is automatically created by the bootstrap sequence by
 * the VM as well.
 *
 * @see			java.lang.Object
 * @see			java.lang.ThreadGroup
 */
public class Thread implements Runnable {

/* Maintain thread shape in all configs */

	/**
	 * The maximum priority value for a Thread.
	 */
	public final static int MAX_PRIORITY = 10;		// Maximum allowed priority for a thread
	/**
	 * The minimum priority value for a Thread.
	 */
	public final static int MIN_PRIORITY = 1;			// Minimum allowed priority for a thread
	/**
	 * The default priority value for a Thread.
	 */
	public final static int NORM_PRIORITY = 5;		// Normal priority for a thread
	private static int createCount = -1;					// Used internally to compute Thread names that comply with the Java specification
	private static final class UniqueIdLock {}
	private static Object uniqueIdLock = new UniqueIdLock();
	private static long uniqueIdCount = 1;
	private static final int NANOS_MAX = 999999;		// Max value for nanoseconds parameter to sleep and join
	private static final int INITIAL_LOCAL_STORAGE_CAPACITY = 5;	// Initial number of local storages when the Thread is created
	static final long NO_REF = 0;				// Symbolic constant, no threadRef assigned or already cleaned up

	// Instance variables
	private long threadRef;									// Used by the VM
	long stackSize = 0;
	private volatile boolean started;				// If !isAlive(), tells if Thread died already or hasn't even started
	private String name;						// The Thread's name
	private int priority = NORM_PRIORITY;			// The Thread's current priority
	private boolean isDaemon;				// Tells if the Thread is a daemon thread or not.

	ThreadGroup group;			// A Thread belongs to exactly one ThreadGroup
	private Runnable runnable;				// Target (optional) runnable object
	private boolean stopCalled = false;			// Used by the VM
	private ClassLoader contextClassLoader;	// Used to find classes and resources in this Thread
	ThreadLocal.ThreadLocalMap threadLocals;
	private java.security.AccessControlContext inheritedAccessControlContext;

	private static final class ThreadLock {}
	private Object lock = new ThreadLock();

	ThreadLocal.ThreadLocalMap inheritableThreadLocals;
	private volatile sun.nio.ch.Interruptible blockOn;

	int threadLocalsIndex;
	int inheritableThreadLocalsIndex;

	private volatile UncaughtExceptionHandler exceptionHandler;
	private long uniqueId;

	volatile Object parkBlocker;

	private static ThreadGroup systemThreadGroup;		// Assigned by the vm
	private static ThreadGroup mainGroup;				// ThreadGroup where the "main" Thread starts

	private volatile static UncaughtExceptionHandler defaultExceptionHandler;

	long threadLocalRandomSeed;
	int threadLocalRandomProbe;
	int threadLocalRandomSecondarySeed;

/**
 * 	Constructs a new Thread with no runnable object and a newly generated name.
 * The new Thread will belong to the same ThreadGroup as the Thread calling
 * this constructor.
 *
 * @see			java.lang.ThreadGroup
 */
public Thread() {
	this(null, null, newName(), null);
}

/**
 *
 * Private constructor to be used by the VM for the threads attached through JNI.
 * They already have a running thread with no associated Java Thread, so this is
 * where the binding is done.
 *
 * @param		vmName			Name for the Thread being created (or null to auto-generate a name)
 * @param		vmThreadGroup	ThreadGroup for the Thread being created (or null for main threadGroup)
 * @param		vmPriority		Priority for the Thread being created
 * @param		vmIsDaemon		Indicates whether or not the Thread being created is a daemon thread
 *
 * @see			java.lang.ThreadGroup
 */
private Thread(String vmName, Object vmThreadGroup, int vmPriority, boolean vmIsDaemon) {
	super();

	String threadName = (vmName == null) ? newName() : vmName;
	setNameImpl(threadRef, threadName);
	name = threadName;

	isDaemon = vmIsDaemon;
	priority = vmPriority;	// If we called setPriority(), it would have to be after setting the ThreadGroup (further down),
							// because of the checkAccess() call (which requires the ThreadGroup set). However, for the main
							// Thread or JNI-C attached Threads we just trust the value the VM is passing us, and just assign.

	ThreadGroup threadGroup = null;
	boolean booting = false;
	if (mainGroup == null) { // only occurs during bootstrap
		booting = true;
		mainGroup = new ThreadGroup(systemThreadGroup);
	}
	threadGroup = vmThreadGroup == null ? mainGroup : (ThreadGroup)vmThreadGroup;

	initialize(booting, threadGroup, null, null);	// no parent Thread
 	this.group.add(this);

	if (booting) {
		System.completeInitialization();
	}
}

/*
 * Called after everything else is initialized.
 */
void completeInitialization() {
	// Get the java.system.class.loader
	contextClassLoader = ClassLoader.getSystemClassLoader();
}

/**
 * 	Constructs a new Thread with a runnable object and a newly generated name.
 * The new Thread will belong to the same ThreadGroup as the Thread calling
 * this constructor.
 *
 * @param		runnable		a java.lang.Runnable whose method <code>run</code> will be executed by the new Thread
 *
 * @see			java.lang.ThreadGroup
 * @see			java.lang.Runnable
 */
public Thread(Runnable runnable) {
	this(null, runnable, newName(), null);
}

/**
 * Constructs a new Thread with a runnable object and a newly generated name,
 * setting the specified AccessControlContext.
 * The new Thread will belong to the same ThreadGroup as the Thread calling
 * this constructor.
 *
 * @param		runnable		a java.lang.Runnable whose method <code>run</code> will be executed by the new Thread
 * @param		acc				the AccessControlContext to use for the Thread
 *
 * @see			java.lang.ThreadGroup
 * @see			java.lang.Runnable
 */
Thread(Runnable runnable, AccessControlContext acc) {
	this(null, runnable, newName(), acc);
}

/**
 * Constructs a new Thread with a runnable object and name provided.
 * The new Thread will belong to the same ThreadGroup as the Thread calling
 * this constructor.
 *
 * @param		runnable		a java.lang.Runnable whose method <code>run</code> will be executed by the new Thread
 * @param		threadName		Name for the Thread being created
 *
 * @see			java.lang.ThreadGroup
 * @see			java.lang.Runnable
 */
public Thread(Runnable runnable, String threadName) {
	this(null, runnable, threadName, null);
}

/**
 * 	Constructs a new Thread with no runnable object and the name provided.
 * The new Thread will belong to the same ThreadGroup as the Thread calling
 * this constructor.
 *
 * @param		threadName		Name for the Thread being created
 *
 * @see			java.lang.ThreadGroup
 * @see			java.lang.Runnable
 */
public Thread(String threadName) {
	this(null, null, threadName, null);
}

/**
 * 	Constructs a new Thread with a runnable object and a newly generated name.
 * The new Thread will belong to the ThreadGroup passed as parameter.
 *
 * @param		group			ThreadGroup to which the new Thread will belong
 * @param		runnable		a java.lang.Runnable whose method <code>run</code> will be executed by the new Thread
 *
 * @exception	SecurityException
 *					if <code>group.checkAccess()</code> fails with a SecurityException
 * @exception	IllegalThreadStateException
 *					if <code>group.destroy()</code> has already been done
 *
 * @see			java.lang.ThreadGroup
 * @see			java.lang.Runnable
 * @see			java.lang.SecurityException
 * @see			java.lang.SecurityManager
 */
public Thread(ThreadGroup group, Runnable runnable) {
	this(group, runnable, newName(), null);
}

/**
 * 	Constructs a new Thread with a runnable object, the given name and
 * belonging to the ThreadGroup passed as parameter.
 *
 * @param		group			ThreadGroup to which the new Thread will belong
 * @param		runnable		a java.lang.Runnable whose method <code>run</code> will be executed by the new Thread
 * @param		threadName		Name for the Thread being created
 * @param		stack			Platform dependent stack size
 *
 * @exception	SecurityException
 *					if <code>group.checkAccess()</code> fails with a SecurityException
 * @exception	IllegalThreadStateException
 *					if <code>group.destroy()</code> has already been done
 *
 * @since 1.4
 *
 * @see			java.lang.ThreadGroup
 * @see			java.lang.Runnable
 * @see			java.lang.SecurityException
 * @see			java.lang.SecurityManager
 */
public Thread(ThreadGroup group, Runnable runnable, String threadName, long stack) {
	this(group, runnable, threadName, null);
	this.stackSize = stack;
}

/**
 * 	Constructs a new Thread with a runnable object, the given name and
 * belonging to the ThreadGroup passed as parameter.
 *
 * @param		group			ThreadGroup to which the new Thread will belong
 * @param		runnable		a java.lang.Runnable whose method <code>run</code> will be executed by the new Thread
 * @param		threadName		Name for the Thread being created
 *
 * @exception	SecurityException
 *					if <code>group.checkAccess()</code> fails with a SecurityException
 * @exception	IllegalThreadStateException
 *					if <code>group.destroy()</code> has already been done
 *
 * @see			java.lang.ThreadGroup
 * @see			java.lang.Runnable
 * @see			java.lang.SecurityException
 * @see			java.lang.SecurityManager
 */
public Thread(ThreadGroup group, Runnable runnable, String threadName) {
	this(group, runnable, threadName, null);
}

private Thread(ThreadGroup group, Runnable runnable, String threadName, AccessControlContext acc) {
	super();
	if (threadName==null) throw new NullPointerException();
	this.name = threadName;		// We avoid the public API 'setName', since it does redundant work (checkAccess)
	this.runnable = runnable;	// No API available here, so just direct access to inst. var.
	Thread currentThread  = currentThread();

	this.isDaemon = currentThread.isDaemon(); // We avoid the public API 'setDaemon', since it does redundant work (checkAccess)

	if (group == null) {
		SecurityManager currentManager = System.getSecurityManager();
		 // if there is a security manager...
		if (currentManager != null)
			// Ask SecurityManager for ThreadGroup
			group = currentManager.getThreadGroup();
	}
	if (group == null)
		// Same group as Thread that created us
		group = currentThread.getThreadGroup();

	initialize(false, group, currentThread, acc);

	setPriority(currentThread.getPriority());	// In this case we can call the public API according to the spec - 20.20.10
}

/**
 * 	Initialize the thread according to its parent Thread and the ThreadGroup
 * where it should be added.
 *
 * @param		booting					Indicates if the JVM is booting up, i.e. if the main thread is being attached
 * @param		threadGroup	ThreadGroup		The ThreadGroup to which the receiver is being added.
 * @param		parentThread	Thread	The creator Thread from which to inherit some values like local storage, etc.
 *										If null, the receiver is either the main Thread or a JNI-C attached Thread.
 * @param		acc						The AccessControlContext. If null, use the current context
 */
private void initialize(boolean booting, ThreadGroup threadGroup, Thread parentThread, AccessControlContext acc) {
	synchronized(uniqueIdLock) {
		uniqueId = uniqueIdCount++;
	}

	this.group = threadGroup;

	if (booting) {
		System.afterClinitInitialization();
	}

	// initialize the thread local storage before making other calls
	if (parentThread != null) { // Non-main thread
		if (parentThread.inheritableThreadLocals != null) {
			inheritableThreadLocals = ThreadLocal.createInheritedMap(parentThread.inheritableThreadLocals);
		}

		final SecurityManager sm = System.getSecurityManager();
		final Class implClass = getClass();
		final Class thisClass = Thread.class;
		if (sm != null && implClass != thisClass) {
			boolean override = ((Boolean)AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					try {
						java.lang.reflect.Method method = implClass.getMethod("getContextClassLoader", new Class[0]); //$NON-NLS-1$
						if (method.getDeclaringClass() != thisClass) {
							return Boolean.TRUE;
						}
					} catch (NoSuchMethodException e) {
					}
					try {
						java.lang.reflect.Method method = implClass.getDeclaredMethod("setContextClassLoader", new Class[]{ClassLoader.class}); //$NON-NLS-1$
						if (method.getDeclaringClass() != thisClass) {
							return Boolean.TRUE;
						}
					} catch (NoSuchMethodException e) {
					}
					return Boolean.FALSE;
				}
			})).booleanValue();
			if (override) {
				sm.checkPermission(new RuntimePermission("enableContextClassLoaderOverride")); //$NON-NLS-1$
			}
		}
		// By default a Thread "inherits" the context ClassLoader from its creator
		contextClassLoader = parentThread.getContextClassLoader();
	} else { // no parent: main thread, or one attached through JNI-C
		if (booting) {
			// Preload and initialize the JITHelpers class
			try {
				Class.forName("com.ibm.jit.JITHelpers"); //$NON-NLS-1$
			} catch(ClassNotFoundException e) {
				// Continue silently if the class can't be loaded and initialized for some reason,
				// The JIT will tolerate this.
			}

			// Explicitly initialize ClassLoaders, so ClassLoader methods (such as
			// ClassLoader.callerClassLoader) can be used before System is initialized
			ClassLoader.initializeClassLoaders();
		}
		// Just set the context class loader
		contextClassLoader = ClassLoader.getSystemClassLoader();
	}

	threadGroup.checkAccess();
	threadGroup.checkNewThread(this);

	inheritedAccessControlContext = acc == null ? AccessController.getContext() : acc;
}

/**
 * 	Constructs a new Thread with no runnable object, the given name and
 * belonging to the ThreadGroup passed as parameter.
 *
 * @param		group			ThreadGroup to which the new Thread will belong
 * @param		threadName		Name for the Thread being created
 *
 * @exception	SecurityException
 *					if <code>group.checkAccess()</code> fails with a SecurityException
 * @exception	IllegalThreadStateException
 *					if <code>group.destroy()</code> has already been done
 *
 * @see			java.lang.ThreadGroup
 * @see			java.lang.SecurityException
 * @see			java.lang.SecurityManager
 */
public Thread(ThreadGroup group, String threadName) {
	this(group, null, threadName, null);
}

/**
 * Returns how many threads are active in the <code>ThreadGroup</code>
 * which the current thread belongs to.
 *
 * @return Number of Threads
 */
public static int activeCount(){
	return currentThread().getThreadGroup().activeCount();
}

/**
 * This method is used for operations that require approval from
 * a SecurityManager. If there's none installed, this method is a no-op.
 * If there's a SecurityManager installed , <code>checkAccess(Ljava.lang.Thread;)</code>
 * is called for that SecurityManager.
 *
 * @see			java.lang.SecurityException
 * @see			java.lang.SecurityManager
 */
public final void checkAccess() {
	SecurityManager currentManager = System.getSecurityManager();
	if (currentManager != null) currentManager.checkAccess(this);
}

/**
 * 	Returns the number of stack frames in this thread.
 *
 * @return		Number of stack frames
 *
 * @deprecated	The results of this call were never well defined.
 *				To make things worse, it would depend if the Thread was
 * 				suspended or not, and suspend was deprecated too.
 */
@Deprecated
public int countStackFrames() {
	return 0;
}

/**
 * Answers the instance of Thread that corresponds to the running Thread
 * which calls this method.
 *
 * @return		a java.lang.Thread corresponding to the code that called <code>currentThread()</code>
 */
public static native Thread currentThread();

/**
 * 	Destroys the receiver without any monitor cleanup. Not implemented.
 *
 * @deprecated May cause deadlocks.
 */
@Deprecated
public void destroy() {
	throw new NoSuchMethodError();
}

/**
 * 	Prints a text representation of the stack for this Thread.
 */
public static void dumpStack() {
	new Throwable().printStackTrace();
}

/**
 * 	Copies an array with all Threads which are in the same ThreadGroup as
 * the receiver - and subgroups - into the array <code>threads</code>
 * passed as parameter. If the array passed as parameter is too small no
 * exception is thrown - the extra elements are simply not copied.
 *
 * @param		threads	array into which the Threads will be copied
 *
 * @return		How many Threads were copied over
 *
 * @exception	SecurityException
 *					if the installed SecurityManager fails <code>checkAccess(Ljava.lang.Thread;)</code>
 *
 * @see			java.lang.SecurityException
 * @see			java.lang.SecurityManager
 */
public static int enumerate(Thread[] threads) {
	return currentThread().getThreadGroup().enumerate(threads, true);
}

/**
 * 	Returns the context ClassLoader for the receiver.
 *
 * @return		ClassLoader		The context ClassLoader
 *
 * @see			java.lang.ClassLoader
 * @see			#getContextClassLoader()
 */
@sun.reflect.CallerSensitive
public ClassLoader getContextClassLoader() {
	SecurityManager currentManager = System.getSecurityManager();
	 // if there is a security manager...
	if (currentManager != null) {
		ClassLoader callerClassLoader = ClassLoader.callerClassLoader();
		if (ClassLoader.needsClassLoaderPermissionCheck(callerClassLoader, contextClassLoader)) {
			currentManager.checkPermission(RuntimePermission.permissionToGetClassLoader);
		}
	}
	return contextClassLoader;
}

/**
 * Answers the name of the receiver.
 *
 * @return		the receiver's name (a java.lang.String)
 */
public final String getName() {
	return String.valueOf(name);
}

/**
 * Answers the priority of the receiver.
 *
 * @return		the receiver's priority (an <code>int</code>)
 *
 * @see			Thread#setPriority
 */
public final int getPriority() {
	return priority;
}

/**
 * Answers the ThreadGroup to which the receiver belongs
 *
 * @return		the receiver's ThreadGroup
 */
public final ThreadGroup getThreadGroup() {
	return group;
}

/**
 * Posts an interrupt request to the receiver
 *
 * @exception	SecurityException
 *					if <code>group.checkAccess()</code> fails with a SecurityException
 *
 * @see			java.lang.SecurityException
 * @see			java.lang.SecurityManager
 * @see			Thread#interrupted
 * @see			Thread#isInterrupted
 */
public void interrupt() {
	SecurityManager currentManager = System.getSecurityManager();

	if (currentManager != null) {
		if (currentThread() != this) {
			currentManager.checkAccess(this);
		}
	}

	synchronized(lock) {
		interruptImpl();
		sun.nio.ch.Interruptible localBlockOn = blockOn;
		if (localBlockOn != null) {
			localBlockOn.interrupt(this);
		}
	}
}

/**
 * Answers a <code>boolean</code> indicating whether the current Thread
 * (<code>currentThread()</code>) has a pending interrupt request
 * (<code>true</code>) or not (<code>false</code>). It also has the
 * side-effect of clearing the flag.
 *
 * @return		a <code>boolean</code>
 *
 * @see			Thread#currentThread
 * @see			Thread#interrupt
 * @see			Thread#isInterrupted
 */
public static native boolean interrupted();

/**
 * Posts an interrupt request to the receiver
 *
 * @see			Thread#interrupted
 * @see			Thread#isInterrupted
 */
private native void interruptImpl();

/**
 * Answers <code>true</code> if the receiver has
 * already been started and still runs code (hasn't died yet).
 * Answers <code>false</code> either if the receiver hasn't been
 * started yet or if it has already started and run to completion and died.
 *
 * @return		a <code>boolean</code>
 *
 * @see			Thread#start
 */
public final boolean isAlive() {
	synchronized(lock) {
		return threadRef != NO_REF;
	}
}

/**
 * Answers <code>true</code> if the receiver has
 * already died and been removed from the ThreadGroup
 * where it belonged.
 *
 * @return		a <code>boolean</code>
 *
 * @see			Thread#start
 * @see			Thread#isAlive
 */
private boolean isDead() {
	// Has already started, is not alive anymore, and has been removed from the ThreadGroup
	synchronized(lock) {
		return started && threadRef == NO_REF;
	}
}

/**
 * Answers a <code>boolean</code> indicating whether the receiver
 * is a daemon Thread (<code>true</code>) or not (<code>false</code>)
 *	A daemon Thread only runs as long as there are non-daemon Threads
 * running. When the last non-daemon Thread ends, the whole program ends
 * no matter if it had daemon Threads still running or not.
 *
 * @return		a <code>boolean</code>
 *
 * @see			Thread#setDaemon
 */
public final boolean isDaemon() {
	return this.isDaemon;
}

/**
 * Answers a <code>boolean</code> indicating whether the receiver
 * has a pending interrupt request (<code>true</code>) or not (<code>false</code>)
 *
 * @return		a <code>boolean</code>
 *
 * @see			Thread#interrupt
 * @see			Thread#interrupted
 */
public boolean isInterrupted() {
	synchronized(lock) {
		return isInterruptedImpl();
	}
}

private native boolean isInterruptedImpl();

/**
 * Blocks the current Thread (<code>Thread.currentThread()</code>) until the
 * receiver finishes its execution and dies.
 *
 * @exception	InterruptedException
 *					if <code>interrupt()</code> was called for the receiver while
 *					it was in the <code>join()</code> call
 *
 * @see			Object#notifyAll
 * @see			java.lang.ThreadDeath
 */
public final synchronized void join() throws InterruptedException {
	if (started)
		while (!isDead())
			wait(0);
}

/**
 * Blocks the current Thread (<code>Thread.currentThread()</code>) until the
 * receiver finishes its execution and dies or the specified timeout expires, whatever
 * happens first.
 *
 * @param		timeoutInMilliseconds		The maximum time to wait (in milliseconds).
 *
 * @exception	InterruptedException
 *					if <code>interrupt()</code> was called for the receiver while
 *					it was in the <code>join()</code> call
 *
 * @see			Object#notifyAll
 * @see			java.lang.ThreadDeath
 */
public final void join(long timeoutInMilliseconds) throws InterruptedException {
	join(timeoutInMilliseconds, 0);
}

/**
 * Blocks the current Thread (<code>Thread.currentThread()</code>) until the
 * receiver finishes its execution and dies or the specified timeout expires, whatever
 * happens first.
 *
 * @param		timeoutInMilliseconds	The maximum time to wait (in milliseconds).
 * @param		nanos					Extra nanosecond precision
 *
 * @exception	InterruptedException
 *					if <code>interrupt()</code> was called for the receiver while
 *					it was in the <code>join()</code> call
 *
 * @see			Object#notifyAll
 * @see			java.lang.ThreadDeath
 */
public final synchronized void join(long timeoutInMilliseconds, int nanos) throws InterruptedException {
	if (timeoutInMilliseconds < 0 || nanos < 0 || nanos > NANOS_MAX)
		throw new IllegalArgumentException();

	if (!started || isDead()) return;

	// No nanosecond precision for now, we would need something like 'currentTimenanos'

	long totalWaited = 0;
	long toWait = timeoutInMilliseconds;
	boolean timedOut = false;

	if (timeoutInMilliseconds == 0 & nanos > 0) {
		// We either round up (1 millisecond) or down (no need to wait, just return)
		if (nanos < 500000)
			timedOut = true;
		else
			toWait = 1;
	}
	while (!timedOut && !isDead()) {
		long start = System.currentTimeMillis();
		wait(toWait);
		long waited = System.currentTimeMillis() - start;
		totalWaited+= waited;
		toWait -= waited;
		// Anyone could do a synchronized/notify on this thread, so if we wait
		// less than the timeout, we must check if the thread really died
		timedOut = (totalWaited >= timeoutInMilliseconds);
	}

}

/**
 * Private method that generates Thread names that comply with the Java specification
 *
 * @version		initial
 *
 * @return		a java.lang.String representing a name for the next Thread being generated
 *
 * @see			Thread#createCount
 */
private synchronized static String newName() {
	if (createCount == -1) {
		createCount++;
		return "main"; //$NON-NLS-1$
	} else {
		return "Thread-" + createCount++; //$NON-NLS-1$
	}
}

/**
 * This is a no-op if the receiver was never suspended, or suspended and already
 * resumed. If the receiver is suspended, however, makes it resume to the point
 * where it was when it was suspended.
 *
 * @exception	SecurityException
 *					if <code>checkAccess()</code> fails with a SecurityException
 *
 * @see			Thread#suspend()
 *
 * @deprecated	Used with deprecated method Thread.suspend().
 */
@Deprecated
public final void resume() {
	checkAccess();
	synchronized(lock) {
		resumeImpl();
	}
}

/**
 * Private method for the VM to do the actual work of resuming the Thread
 *
 */
private native void resumeImpl();

/**
 * Calls the <code>run()</code> method of the Runnable object the receiver holds.
 * If no Runnable is set, does nothing.
 *
 * @see			Thread#start
 */
public void run() {
	if (runnable != null) {
		runnable.run();
	}
}

/**
 * 	Set the context ClassLoader for the receiver.
 *
 * @param		cl		The context ClassLoader
 *
 * @see			java.lang.ClassLoader
 * @see			#getContextClassLoader()
 */
public void setContextClassLoader(ClassLoader cl) {
	SecurityManager currentManager = System.getSecurityManager();
	 // if there is a security manager...
	if (currentManager != null) {
		// then check permission
		currentManager.checkPermission(RuntimePermission.permissionToSetContextClassLoader);
	}
	contextClassLoader = cl;
}

/**
 * Set if the receiver is a daemon Thread or not. This can only be done
 * before the Thread starts running.
 *
 * @param		isDaemon		A boolean indicating if the Thread should be daemon or not
 *
 * @exception	SecurityException
 *					if <code>checkAccess()</code> fails with a SecurityException
 *
 * @see			Thread#isDaemon
 */
public final void setDaemon(boolean isDaemon) {
	checkAccess();
	synchronized(lock) {
		if (!this.started) {
			this.isDaemon = isDaemon;
		} else {
			if (isAlive()) {
				throw new IllegalThreadStateException();
			}
		}
	}
}

/**
 * Sets the name of the receiver.
 *
 * @param		threadName		new name for the Thread
 *
 * @exception	SecurityException
 *					if <code>checkAccess()</code> fails with a SecurityException
 *
 * @see			Thread#getName
 */
public final void setName(String threadName) {
	checkAccess();
	if (threadName == null) {
		throw new NullPointerException();
	}
	synchronized(lock) {
		if (started && threadRef != NO_REF) {
			setNameImpl(threadRef, threadName);
		}
		name = threadName;
	}
}

private native void setNameImpl(long threadRef, String threadName);

/**
 * Sets the priority of the receiver. Note that the final priority set may not be the
 * parameter that was passed - it will depend on the receiver's ThreadGroup. The priority
 * cannot be set to be higher than the receiver's ThreadGroup's maxPriority().
 *
 * @param		priority		new priority for the Thread
 *
 * @exception	SecurityException
 *					if <code>checkAccess()</code> fails with a SecurityException
 * @exception	IllegalArgumentException
 *					if the new priority is greater than Thread.MAX_PRIORITY or less than
 *					Thread.MIN_PRIORITY
 *
 * @see			Thread#getPriority
 */
public final void setPriority(int priority){
	checkAccess();
	if (MIN_PRIORITY <= priority && priority <= MAX_PRIORITY) {
		int finalPriority = priority;
		int threadGroupMaxPriority = getThreadGroup().getMaxPriority();
		if (threadGroupMaxPriority < priority) finalPriority = threadGroupMaxPriority;
		this.priority = finalPriority;
		synchronized(lock) {
			if (started && threadRef != NO_REF) {
				setPriorityNoVMAccessImpl(threadRef, finalPriority);
			}
		}
	} else throw new IllegalArgumentException();
}

/**
 * Private method to tell the VM that the priority for the receiver is changing.
 *
 * @param		priority		new priority for the Thread
 *
 * @see			Thread#setPriority
 */
private native void setPriorityNoVMAccessImpl(long threadRef, int priority);

/**
 * Causes the thread which sent this message to sleep an interval
 * of time (given in milliseconds). The precision is not guaranteed -
 * the Thread may sleep more or less than requested.
 *
 * @param		time		The time to sleep in milliseconds.
 *
 * @exception	InterruptedException
 *					if <code>interrupt()</code> was called for this Thread while it was sleeping
 *
 * @see			Thread#interrupt()
 */

public static void sleep(long time) throws InterruptedException {
	sleep(time, 0);
}

/**
 * Causes the thread which sent this message to sleep an interval
 * of time (given in milliseconds). The precision is not guaranteed -
 * the Thread may sleep more or less than requested.
 *
 * @param		time		The time to sleep in milliseconds.
 * @param		nanos		Extra nanosecond precision
 *
 * @exception	InterruptedException
 *					if <code>interrupt()</code> was called for this Thread while it was sleeping
 *
 * @see			Thread#interrupt()
 */
public static native void sleep(long time, int nanos) throws InterruptedException;

/**
 * Starts the new Thread of execution. The <code>run()</code> method of the receiver
 * will be called by the receiver Thread itself (and not the Thread calling <code>start()</code>).
 *
 * @exception	IllegalThreadStateException
 *					Unspecified in the Java language specification
 *
 * @see			Thread#run
 */
public synchronized void start() {
	boolean success = false;

	try {
		synchronized(lock) {
			if (started) {
				// K0341 = Thread is already started
				throw new IllegalThreadStateException(com.ibm.oti.util.Msg.getString("K0341")); //$NON-NLS-1$
			}

			group.add(this);

			startImpl();

			success = true;
		}
 	} finally {
 		if (!success && !started) {
 	 		group.remove(this);
 		}
 	}
}

private native void startImpl();

/**
 * Requests the receiver Thread to stop and throw ThreadDeath.
 * The Thread is resumed if it was suspended and awakened if it was
 * sleeping, so that it can proceed to throw ThreadDeath.
 *
 * @exception	SecurityException
 *					if <code>checkAccess()</code> fails with a SecurityException
 *
 * @deprecated
 */
@Deprecated
public final void stop() {
	/* the only case we don't want to do the check is if the thread has been started but is now dead */
	if (!isDead()){
		stopWithThrowable(new ThreadDeath());
	}
}

/**
 * Throws UnsupportedOperationException.
 *
 * @param		throwable		Throwable object to be thrown by the Thread
 *
 * @deprecated
 */
@Deprecated
public final void stop(Throwable throwable) {
	throw new UnsupportedOperationException();
 }

private final synchronized void stopWithThrowable(Throwable throwable) {
	checkAccess();
	if (currentThread() != this || !(throwable instanceof ThreadDeath)) {
		SecurityManager currentManager = System.getSecurityManager();
		if (currentManager != null)
			currentManager.checkPermission(RuntimePermission.permissionToStopThread);
	}

	synchronized(lock) {
		if (throwable != null){
			if (!started){
				/* [PR CMVC 179978] Java7:JCK:java_lang.Thread fails in all plat*/
				/*
				 * if the thread has not yet been simply store the fact that stop has been called
				 * The JVM uses this to determine if stop has been called before start
				 */
				stopCalled = true;
			} else {
				/* thread was started so do the full stop */
				stopImpl(throwable);
			}
		}
		else throw new NullPointerException();
	}
}

/**
 * Private method for the VM to do the actual work of stopping the Thread
 *
 * @param		throwable		Throwable object to be thrown by the Thread
 */
private native void stopImpl(Throwable throwable);

/**
 * This is a no-op if the receiver is suspended. If the receiver <code>isAlive()</code>
 * however, suspended it until <code>resume()</code> is sent to it. Suspend requests
 * are not queued, which means that N requests are equivalent to just one - only one
 * resume request is needed in this case.
 *
 * @exception	SecurityException
 *					if <code>checkAccess()</code> fails with a SecurityException
 *
 * @see			Thread#resume()
 *
 * @deprecated May cause deadlocks.
 */
@Deprecated
public final void suspend() {
	checkAccess();
	if (currentThread() == this) suspendImpl();
	else {
		synchronized( lock ) {
			suspendImpl();
		}
	}
}

/**
 * Private method for the VM to do the actual work of suspending the Thread
 *
 * Conceptually this method can't be synchronized or a Thread that suspends itself will
 * do so with the Thread's lock and this will cause deadlock to valid Java programs.
 */
private native void suspendImpl();

/**
 * Answers a string containing a concise, human-readable
 * description of the receiver.
 *
 * @return		a printable representation for the receiver.
 */
public String toString() {
	ThreadGroup localGroup = getThreadGroup();

	return "Thread[" + this.getName() + "," + this.getPriority() + "," + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		(null == localGroup ? "" : localGroup.getName()) + "]" ; //$NON-NLS-1$ //$NON-NLS-2$
}

/**
 * Causes the thread which sent this message to yield execution to another Thread
 * that is ready to run. The actual scheduling is implementation-dependent.
 *
 * @version		initial
 */
public static native void yield();

/**
 * Returns whether the current thread has a monitor lock on the specified object.
 *
 * @param object the object to test for the monitor lock
 * @return true when the current thread has a monitor lock on the specified object
 *
 * @since 1.4
 */
public static native boolean holdsLock(Object object);

void blockedOn(sun.nio.ch.Interruptible interruptible) {
	synchronized(lock) {
		blockOn = interruptible;
	}
}

private native Throwable getStackTraceImpl();

/**
 * Returns an array of StackTraceElement, where each element of the array represents a frame
 * on the Java stack.
 *
 * @return an array of StackTraceElement
 *
 * @throws SecurityException if the RuntimePermission "getStackTrace" is not allowed
 *
 * @see java.lang.StackTraceElement
 */
public StackTraceElement[] getStackTrace() {
	if (Thread.currentThread() != this) {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkPermission(new RuntimePermission("getStackTrace")); //$NON-NLS-1$
	}
	Throwable t;

	synchronized(lock) {
		if (!isAlive()) {
			return new StackTraceElement[0];
		}
		t = getStackTraceImpl();
	}
	return J9VMInternals.getStackTrace(t, false);
}

/**
 * Returns a Map containing Thread keys, and values which are arrays of StackTraceElement. The Map contains
 * all Threads which were alive at the time this method was called.
 *
 * @return an array of StackTraceElement
 *
 * @throws SecurityException if the RuntimePermission "getStackTrace" is not allowed, or the
 * 		RuntimePermission "modifyThreadGroup" is not allowed
 *
 * @see #getStackTrace()
 */
public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		security.checkPermission(new RuntimePermission("getStackTrace")); //$NON-NLS-1$
		security.checkPermission(RuntimePermission.permissionToModifyThreadGroup);
	}
	// Allow room for more Threads to be created before calling enumerate()
	int count = systemThreadGroup.activeCount() + 20;
	Thread[] threads = new Thread[count];
	count = systemThreadGroup.enumerate(threads);
	java.util.Map result = new java.util.HashMap(count*4/3);
	for (int i=0; i<count; i++) {
		result.put(threads[i], threads[i].getStackTrace());
	}
	return result;
}

/**
 * Return a unique id for this Thread.
 *
 * @return a positive unique id for this Thread.
 */
public long getId() {
	return uniqueId;
}

/**
 * A handler which is invoked when an uncaught exception occurs in a Thread.
 */
@FunctionalInterface
public static interface UncaughtExceptionHandler {
	/**
	 * The method invoked when an uncaught exception occurs in a Thread.
	 *
	 * @param thread the Thread where the uncaught exception occurred
	 * @param throwable the uncaught exception
	 */
	public void uncaughtException(Thread thread, Throwable throwable) ;
}

/**
 * Return the UncaughtExceptionHandler for this Thread.
 *
 * @return the UncaughtExceptionHandler for this Thread
 *
 * @see UncaughtExceptionHandler
 */
public UncaughtExceptionHandler getUncaughtExceptionHandler() {
	if (exceptionHandler == null)
		return getThreadGroup();
	return exceptionHandler;
}

/**
 * Set the UncaughtExceptionHandler for this Thread.
 *
 * @param handler the UncaughtExceptionHandler to set
 *
 * @see UncaughtExceptionHandler
 */
public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
	exceptionHandler = handler;
}

/**
 * Return the default UncaughtExceptionHandler used for new Threads.
 *
 * @return the default UncaughtExceptionHandler for new Threads
 *
 * @see UncaughtExceptionHandler
 */
public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
	return defaultExceptionHandler;
}

/**
 * Set the UncaughtExceptionHandler used for new  Threads.
 *
 * @param handler the UncaughtExceptionHandler to set
 *
 * @see UncaughtExceptionHandler
 */
public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
	SecurityManager security = System.getSecurityManager();
	if (security != null)
		security.checkPermission(new RuntimePermission("setDefaultUncaughtExceptionHandler")); //$NON-NLS-1$
	defaultExceptionHandler = handler;
}

/**
 * The possible Thread states.
 */
// The order of the States is known by the getStateImpl() native
public static enum State {
	/**
	 * A Thread which has not yet started.
	 */
	NEW,
	/**
	 * A Thread which is running or suspended.
	 */
	RUNNABLE,
	/**
	 * A Thread which is blocked on a monitor.
	 */
	BLOCKED,
	/**
	 * A Thread which is waiting with no timeout.
	 */
	WAITING,
	/**
	 * A Thread which is waiting with a timeout.
	 */
	TIMED_WAITING,
	/**
	 * A thread which is no longer alive.
	 */
	TERMINATED }

/**
 * Returns the current Thread state.
 *
 * @return the current Thread state constant.
 *
 * @see State
 */
public State getState() {
	synchronized(lock) {
		if (threadRef == NO_REF) {
			if (isDead()) {
				return State.TERMINATED;
			}
			return State.NEW;
		}
		return State.values()[getStateImpl(threadRef)];
	}
}

private native int getStateImpl(long threadRef);

/**
 * Any uncaught exception in any Thread has to be forwarded (by the VM) to the Thread's ThreadGroup
 * by sending this message (uncaughtException). This allows users to define custom ThreadGroup classes
 * and custom behavior for when a Thread has an uncaughtException or when it does (ThreadDeath).
 *
 * @version		initial
 *
 * @param		e		The uncaught exception itself
 *
 * @see			Thread#stop()
 * @see			Thread#stop(Throwable)
 * @see			ThreadDeath
 */
void uncaughtException(Throwable e) {
	UncaughtExceptionHandler handler = getUncaughtExceptionHandler();
	if (handler != null)
		handler.uncaughtException(this, e);
}

/**
 * Called by J9VMInternals.threadCleanup() so the Thread can release resources
 * and change its state.
 *
 * @see J9VMInternals#threadCleanup()
 */
void cleanup() {
	group = null;

	runnable = null;
	inheritedAccessControlContext = null;

	threadLocals = null;
	inheritableThreadLocals = null;

	synchronized(lock) {
		threadRef = Thread.NO_REF;				// So that isAlive() can work
	}
}

// prevent subclasses from becoming Cloneable
protected Object clone() throws CloneNotSupportedException {
	throw new CloneNotSupportedException();
}

}