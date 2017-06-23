
package java.security;

/*******************************************************************************
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 *
 *     Copyright IBM Corp. 1998, 2016 All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * An AccessControlContext encapsulates the information which is needed
 * by class AccessController to detect if a Permission would be granted
 * at a particular point in a programs execution.
 *
 * @author		OTI
 * @version		initial
 */
public final class AccessControlContext {

	static final int STATE_NOT_AUTHORIZED = 0; //	It has been confirmed that the ACC is NOT authorized
	static final int STATE_AUTHORIZED = 1; // It has been confirmed that the ACC is authorized
	static final int STATE_UNKNOWN = 2; // The ACC state is unknown yet.

	private static int debugSetting = -1;
	private static ArrayList<String> debugPermClassArray;
	private static ArrayList<String> debugPermNameArray;
	private static ArrayList<String> debugPermActionsArray;
	static ArrayList<URL> debugCodeBaseArray;

	/* Constants used to set the value of the debugHasCodebase field */
	private static final int DEBUG_UNINITIALIZED_HASCODEBASE = 0;
	private static final int DEBUG_NO_CODEBASE = 1;
	private static final int DEBUG_HAS_CODEBASE = 2;

	static final int DEBUG_DISABLED = 0;
	static final int DEBUG_ACCESS_DENIED = 1;	// debug is enabled for access denied, and failure
	static final int DEBUG_ENABLED = 2;			// debug is enabled for access allowed, stacks, domains, and threads

	DomainCombiner domainCombiner;
	ProtectionDomain[] context;
	int authorizeState = STATE_UNKNOWN;
	// This flag is to determine if current ACC contains privileged PDs such that
	// createAccessControlContext permission need to be checked before invoking ProtectionDomain.implies()
	private boolean containPrivilegedContext = false;
	// This is the ProtectionDomain of the creator of this ACC
	private ProtectionDomain	callerPD;
	AccessControlContext doPrivilegedAcc; // AccessControlContext usually coming from a doPrivileged call
	boolean	isLimitedContext = false;	// flag to indicate if there are limited permissions
	Permission[] limitedPerms;	// the limited permissions when isLimitedContext is true
	AccessControlContext	nextStackAcc;	// AccessControlContext in next call stack when isLimitedContext is true
	private int debugHasCodebase;	// Set to the value of DEBUG_UNINITIALIZED_HASCODEBASE be default. Cache the result of hasDebugCodeBase()

	private static final SecurityPermission createAccessControlContext =
		new SecurityPermission("createAccessControlContext");	//$NON-NLS-1$
	private static final SecurityPermission getDomainCombiner =
		new SecurityPermission("getDomainCombiner");	//$NON-NLS-1$

	static final int DEBUG_ACCESS = 1;
	static final int DEBUG_ACCESS_STACK = 2;
	static final int DEBUG_ACCESS_DOMAIN = 4;
	static final int DEBUG_ACCESS_FAILURE = 8;
	static final int DEBUG_ACCESS_THREAD = 0x10;
	static final int DEBUG_ALL = 0xff;
	static final int CACHE_ARRAY_SIZE = 3;
	static final int CACHE_INDEX_PDS_IMPLIED = 0;
	static final int CACHE_INDEX_PERMS_IMPLIED = 1;
	static final int CACHE_INDEX_PERMS_NOT_IMPLIED = 2;

static int debugSetting() {
	if (debugSetting != -1) return debugSetting;
	debugSetting = 0;
	String value = (String)AccessController.doPrivileged(new PrivilegedAction() {
		public Object run() {
			return System.getProperty("java.security.debug");	//$NON-NLS-1$
		}});
	if (value == null) return debugSetting;
	StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(value));
	tokenizer.resetSyntax();
	tokenizer.wordChars(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT);
	tokenizer.quoteChar('"');
	tokenizer.whitespaceChars(',', ',');

	try {
		while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
			String keyword = tokenizer.sval;
			if (keyword.equals("all")) {	//$NON-NLS-1$
				debugSetting  = DEBUG_ALL;
				return debugSetting;
			}
			if (keyword.startsWith("access:")) {	//$NON-NLS-1$
				debugSetting |= DEBUG_ACCESS;
				keyword = keyword.substring(7);
			}

			if (keyword.equals("access")) {	//$NON-NLS-1$
				debugSetting |= DEBUG_ACCESS;
			} else if (keyword.equals("stack")) {	//$NON-NLS-1$
				debugSetting |= DEBUG_ACCESS_STACK;
			} else if (keyword.equals("domain")) {	//$NON-NLS-1$
				debugSetting |= DEBUG_ACCESS_DOMAIN;
			} else if (keyword.equals("failure")) {	//$NON-NLS-1$
				debugSetting |= DEBUG_ACCESS_FAILURE;
			} else if (keyword.equals("thread")) {	//$NON-NLS-1$
				debugSetting |= DEBUG_ACCESS_THREAD;
			} else if (keyword.startsWith("permission=")) {	//$NON-NLS-1$
				String debugPermClass = keyword.substring(11);
				if (debugPermClass.isEmpty() && tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
					debugPermClass = tokenizer.sval;
				}
				if (null == debugPermClassArray) {
					debugPermClassArray = new ArrayList<String>();
				}
				debugPermClassArray.add(debugPermClass);
			} else if (keyword.startsWith("codebase=")) {	//$NON-NLS-1$
				String codebase = keyword.substring(9);
				if (codebase.isEmpty() && tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
					codebase = tokenizer.sval;
				}
				URL debugCodeBase = null;
				try {
					debugCodeBase = new URL(codebase);
				} catch (MalformedURLException e) {
					System.err.println("Error setting -Djava.security.debug=access:codebase - " + e);	//$NON-NLS-1$
				}
				if (null != debugCodeBase) {
					if (null == debugCodeBaseArray) {
						debugCodeBaseArray = new ArrayList<URL>();
					}
					debugCodeBaseArray.add(debugCodeBase);
				}
			} else if (keyword.startsWith("permname=")) {	//$NON-NLS-1$
				String debugPermName = keyword.substring(9);
				if (debugPermName.isEmpty() && tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
					debugPermName = tokenizer.sval;
				}
				if (null == debugPermNameArray) {
					debugPermNameArray = new ArrayList<String>();
				}
				debugPermNameArray.add(debugPermName);
			} else if (keyword.startsWith("permactions=")) {	//$NON-NLS-1$
				String debugPermActions = keyword.substring(12);
				if (debugPermActions.isEmpty() && tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
					debugPermActions = tokenizer.sval;
				}
				if (null == debugPermActionsArray) {
					debugPermActionsArray = new ArrayList<String>();
				}
				debugPermActionsArray.add(debugPermActions);
			}
		}
	} catch (IOException e) {
	}
	if (0 == (debugSetting & DEBUG_ACCESS)) {
		// If the access keyword is not specified, none of the other keywords have any affect
		debugSetting = 0;
	}
	return debugSetting;
}

/**
 * Return true if the specified Permission is enabled for debug.
 * @param perm a Permission instance
 * @return Return true if the specified Permission is enabled for debug
 */
static boolean debugPermission(Permission perm) {
	boolean result = true;
	if (debugPermClassArray != null) {
		result = false;
		String permClassName = perm.getClass().getName();
		for (String debugPermClass : debugPermClassArray) {
			if (debugPermClass.equals(permClassName)) {
				return true;
			}
		}
	}
	if (debugPermNameArray != null) {
		result = false;
		String permName = perm.getName();
		for (String debugPermName : debugPermNameArray) {
			if (debugPermName.equals(permName)) {
				return true;
			}
		}
	}
	if (debugPermActionsArray != null) {
		result = false;
		String permActions = perm.getActions();
		for (String debugPermActions : debugPermActionsArray) {
			if (debugPermActions.equals(permActions)) {
				return true;
			}
		}
	}
	return result;
}

/**
 * Check if the receiver contains a ProtectionDomain using the debugCodeBase, which
 * was parsed from the java.security.debug system property.
 *
 * @return true if the AccessControlContext contains a ProtectionDomain which
 * matches the debug codebase.
 */
boolean hasDebugCodeBase() {
	if (debugHasCodebase != DEBUG_UNINITIALIZED_HASCODEBASE) {
		return debugHasCodebase == DEBUG_HAS_CODEBASE ? true : false;
	}
	ProtectionDomain[] pds = this.context;
	if (pds != null) {
		for (int i = 0; i < pds.length; i++) {
			ProtectionDomain pd = this.context[i];
			CodeSource cs = null == pd ? null : pd.getCodeSource();
			if ((cs != null) && debugCodeBase(cs.getLocation()))  {
				debugHasCodebase = DEBUG_HAS_CODEBASE;
				return true;
			}
		}
		if ((this.doPrivilegedAcc != null) && this.doPrivilegedAcc.hasDebugCodeBase()) {
			debugHasCodebase = DEBUG_HAS_CODEBASE;
			return true;
		}
		if ((this.nextStackAcc != null) && this.nextStackAcc.hasDebugCodeBase()) {
			debugHasCodebase = DEBUG_HAS_CODEBASE;
			return true;
		}
	}
	debugHasCodebase = DEBUG_NO_CODEBASE;
	return false;
}

/**
 * Return true if the specified codebase location is enabled for debug.
 * @param location a codebase URL
 * @return Return true if the specified codebase location is enabled for debug
 */
static boolean debugCodeBase(URL location) {
	if (location != null) {
		for (URL debugCodeBase : debugCodeBaseArray) {
			if (debugCodeBase.equals(location)) {
				return true;
			}
		}
	}

	return false;
}

static void debugPrintAccess() {
	System.err.print("access: ");	//$NON-NLS-1$
	if ((debugSetting() & DEBUG_ACCESS_THREAD) == DEBUG_ACCESS_THREAD) {
		System.err.print("(" + Thread.currentThread() + ")");	//$NON-NLS-1$ //$NON-NLS-2$
	}
}

/**
 * Constructs a new instance of this class given an array of
 * protection domains.
 *
 * @param fromContext the array of ProtectionDomain
 *
 * @exception	NullPointerException if fromContext is null
 */
public AccessControlContext(ProtectionDomain[] fromContext) {
	int length = fromContext.length;
	if (length == 0) {
		context = null;
	} else {
		int domainIndex = 0;
		context = new ProtectionDomain[length];
		next : for (int i = 0; i < length; i++) {
			ProtectionDomain current = fromContext[i];
			if (current == null) continue;
			for (int j = 0; j < i; j++)
				if (current == context[j]) continue next;
			context[domainIndex++] = current;
		}
		if (domainIndex == 0) {
			context = null;
		} else if (domainIndex != length) {
			ProtectionDomain[] copy = new ProtectionDomain[domainIndex];
			System.arraycopy(context, 0, copy, 0, domainIndex);
			context = copy;
		}
	}
	// this.containPrivilegedContext is set to false by default
	// this.authorizeState is STATE_UNKNOWN by default
}

AccessControlContext(ProtectionDomain[] context, int authorizeState) {
	super();
	switch (authorizeState) {
	default:
		// authorizeState can't be STATE_UNKNOWN, callerPD always is NULL
		throw new IllegalArgumentException();
	case STATE_AUTHORIZED:
	case STATE_NOT_AUTHORIZED:
		break;
	}
	this.context = context;
	this.authorizeState = authorizeState;
	this.containPrivilegedContext = true;
}

AccessControlContext(AccessControlContext acc, ProtectionDomain[] context, int authorizeState) {
	super();
	switch (authorizeState) {
	default:
		// authorizeState can't be STATE_UNKNOWN, callerPD always is NULL
		throw new IllegalArgumentException();
	case STATE_AUTHORIZED:
		if (null != acc) {
			// inherit the domain combiner when authorized
			this.domainCombiner = acc.domainCombiner;
		}
		break;
	case STATE_NOT_AUTHORIZED:
		break;
	}
	this.doPrivilegedAcc = acc;
	this.context = context;
	this.authorizeState = authorizeState;
	this.containPrivilegedContext = true;
}

/**
 * Constructs a new instance of this class given a context
 * and a DomainCombiner
 *
 * @param acc the AccessControlContext
 * @param combiner the DomainCombiner
 *
 * @exception	java.security.AccessControlException thrown
 * 					when the caller doesn't have the  "createAccessControlContext" SecurityPermission
 * @exception	NullPointerException if the provided context is null.
 */
public AccessControlContext(AccessControlContext acc, DomainCombiner combiner) {
	this(acc, combiner, false);
}

/**
 * Constructs a new instance of this class given a context and a DomainCombiner
 * Skip the Permission "createAccessControlContext" check if preauthorized is true
 *
 * @param acc the AccessControlContext
 * @param combiner the DomainCombiner
 * @param preauthorized the flag to indicate if the permission check can be skipped
 *
 * @exception	java.security.AccessControlException thrown
 * 					when the caller doesn't have the  "createAccessControlContext" SecurityPermission
 * @exception	NullPointerException if the provided context is null.
 */
AccessControlContext(AccessControlContext acc, DomainCombiner combiner, boolean preauthorized) {
	if (!preauthorized) {
		SecurityManager security = System.getSecurityManager();
		if (null != security)	{
			security.checkPermission(createAccessControlContext);
		}
	}
	this.authorizeState = STATE_AUTHORIZED;
	this.context = acc.context;
	this.domainCombiner = combiner;
	this.containPrivilegedContext = acc.containPrivilegedContext;
	this.isLimitedContext = acc.isLimitedContext;
	this.limitedPerms = acc.limitedPerms;
	this.nextStackAcc = acc.nextStackAcc;
	this.doPrivilegedAcc = acc.doPrivilegedAcc;
}

/**
 * Combine checked and toBeCombined
 * Assuming:	there are no null & dup elements in checked
 * 				there might be null & dups in toBeCombined
 *
 * @param objPDs the flag to indicate if it is a ProtectionDomain or Permission object element
 * @param checked the array of objects already checked
 * @param toBeCombined the array of objects to be combined
 * @param start the start position in the toBeCombined array
 * @param len the number of element to be copied
 * @param justCombine the flag to indicate if null/dup check is needed
 *
 * @return the combination of these two array
 */
private static Object[] combineObjs(boolean objPDs, Object[] checked, Object[] toBeCombined, int start, int len, boolean justCombine) {
	if (null == toBeCombined) {
		return	checked;
	}
	int lenChecked = (null == checked) ? 0 : checked.length;
	int lenTobeCombined = ( len > (toBeCombined.length - start)) ? (toBeCombined.length - start) : len;
	Object[] answer = null;
	if (objPDs) {
		answer = new ProtectionDomain[lenChecked + lenTobeCombined];
	} else {
		answer = new Permission[lenChecked + lenTobeCombined];
	}
	if (null != checked) {
		System.arraycopy(checked, 0, answer, 0, lenChecked);
	}
	if (justCombine) {	// no null/dup check
		System.arraycopy(toBeCombined, start, answer, lenChecked, lenTobeCombined);
	} else {	// remove the null & dups
		int counter = 0;
		for (int i = 0; i < lenTobeCombined; i++) {
			if (null != toBeCombined[start + i]) {	// remove null
				boolean found = false;
				for (int j = (lenChecked + counter - 1); j >= 0; j--) {	// check starts from newly added elements
//				for (int j = 0; j < (lenChecked + counter); j++) {
					if (toBeCombined[start + i] == answer[j]) {
						found = true;	// find dup
						break;
					}
				}
				if (!found) {
					answer[lenChecked + counter++] = toBeCombined[start + i];
				}
			}
		}

		Object[] result = null;
		if (objPDs) {
			result = new ProtectionDomain[lenChecked + counter];
		} else {
			result = new Permission[lenChecked + counter];
		}
		System.arraycopy(answer, 0, result, 0, lenChecked + counter);
		answer = result;
	}
	return	answer;
}

/**
 * Combine checked and toBeCombined ProtectionDomain objects
 * Assuming:	there are no null & dup elements in checked
 * 				there might be null & dups in toBeCombined
 *
 * @param checked the array of objects already checked
 * @param toBeCombined the array of objects to be combined
 *
 * @return the combination of these two array
 */
static ProtectionDomain[] combinePDObjs(ProtectionDomain[] checked, Object[] toBeCombined) {
	return (ProtectionDomain[])combineObjs(true, checked, toBeCombined, 0, (null != toBeCombined) ? toBeCombined.length : 0, false);
}

/**
 * Combine checked and toBeCombined Permission objects
 * Assuming:	there are no null & dup elements in checked
 * 				there might be null & dups in toBeCombined
 *
 * @param checked the array of objects already checked
 * @param toBeCombined the array of objects to be combined
 * @param start the start position in the toBeCombined array
 * @param len the number of element to be copied
 * @param justCombine the flag to indicate if null/dup check is needed
 *
 * @return the combination of these two array
 */
static Permission[] combinePermObjs(Permission[] checked, Permission[] toBeCombined, int start, int len, boolean justCombine) {
	return (Permission[])combineObjs(false, checked, toBeCombined, start, len, justCombine);
}

/**
 * Perform ProtectionDomain.implies(permission) with known ProtectionDomain objects already implied
 *
 * @param perm the permission to be checked
 * @param toCheck the ProtectionDomain to be checked
 * @param cacheChecked the cached check result which is an array with following three elements:
 * 	ProtectionDomain[] pdsImplied, Permission[] permsImplied, Permission[] permsNotImplied
 *
 * @return -1 if toCheck is null, among pdsImplied or each ProtectionDomain within toCheck implies perm,
 * 			otherwise the index of ProtectionDomain objects not implied
 */
static int checkPermWithCachedPDsImplied(Permission perm, Object[] toCheck, Object[] cacheChecked) {
	if (null == toCheck) {
		return -1; // nothing to check, implied
	}

	ProtectionDomain[] pdsImplied = null;
	if (null != cacheChecked) {
		pdsImplied = (ProtectionDomain[])cacheChecked[CACHE_INDEX_PDS_IMPLIED];
	} else {
		cacheChecked = new Object[CACHE_ARRAY_SIZE];
	}
	// in reverse order as per Oracle behavior
	for (int i = (toCheck.length - 1); i >= 0; i--) {
		if (null != toCheck[i]) {
			if (null != pdsImplied) {
				boolean found = false;
				for (int j = 0; j < pdsImplied.length; j++) {
					found = (toCheck[i] == pdsImplied[j]);
					if (found) {
						break;
					}
				}
				if (found) {
					continue;	// already implied
				}
			}
			if (!((ProtectionDomain)toCheck[i]).implies(perm)) {
				return i;	// NOT implied
			}
		}
	}
	cacheChecked[CACHE_INDEX_PDS_IMPLIED] = combinePDObjs(pdsImplied, toCheck);
	return	-1;	// All implied
}

/**
 * Perform Permission.implies(permission) with known Permission objects already implied & NOT implied
 *
 * @param perm the permission to be checked
 * @param permsLimited the limited Permission to be checked
 * @param cacheChecked the cached check result which is an array with following three elements:
 * 	ProtectionDomain[] pdsImplied, Permission[] permsImplied, Permission[] permsNotImplied
 *
 * @return true if there is a limited permission implied perm, otherwise false
 */
static boolean checkPermWithCachedPermImplied(Permission perm, Permission[] permsLimited, Object[] cacheChecked) {
	if (null == permsLimited) {
		return false;
	}
	Permission[] permsImplied = null;
	Permission[] permsNotImplied = null;
	if (null != cacheChecked) {
		permsImplied = (Permission[])cacheChecked[CACHE_INDEX_PERMS_IMPLIED];
		permsNotImplied = (Permission[])cacheChecked[CACHE_INDEX_PERMS_NOT_IMPLIED];
	} else {
		cacheChecked = new Object[CACHE_ARRAY_SIZE];
	}
	boolean	success = false;
	int lenNotImplied = permsLimited.length;
	for (int j = 0; j < permsLimited.length; j++) {
		if (null != permsLimited[j]) { // go through each non-null limited permission
			if (null != permsImplied)	{
				for (int k = 0; k < permsImplied.length; k++) {
					if (permsLimited[j] == permsImplied[k]) {
						success = true;	//	already implied before
						break;
					}
				}
				if (success) {	//	already implied
					lenNotImplied = j;
					break;
				}
			}
			boolean	notImplied = false;
			if (null != permsNotImplied) {
				for (int k = 0; k < permsNotImplied.length; k++) {
					if (permsLimited[j] == permsNotImplied[k]) {
						notImplied = true;	//	already NOT implied before
						lenNotImplied = j;
						break;
					}
				}
			}
			if (!notImplied && permsLimited[j].implies(perm)) {
				success = true;	//	just implied
				cacheChecked[CACHE_INDEX_PERMS_IMPLIED] = combinePermObjs(permsImplied, permsLimited, j, 1, true);
				lenNotImplied = j;
				break;
			}
		}
	}
	if (0 < lenNotImplied) {
		cacheChecked[CACHE_INDEX_PERMS_NOT_IMPLIED] = combinePermObjs(permsNotImplied, permsLimited, 0, lenNotImplied, false);
	}
	return success;
}

/**
 * Checks if the permission perm is allowed as per incoming
 * 	AccessControlContext/ProtectionDomain[]/isLimited/Permission[]
 * while taking advantage cached
 * 	ProtectionDomain[] pdsImplied, Permission[] permsImplied, Permission[] permsNotImplied
 *
 * @param perm the permission to be checked
 * @param accCurrent the current AccessControlContext to be checked
 * @param debug debug flags
 * @param pdsContext the current context to be checked
 * @param isLimited the flag to indicate if there are limited permission(s)
 * @param permsLimited the limited permission(s) to be checked
 * @param accNext the next AccessControlContext to be checked
 * @param cacheChecked the cached check result which is an array with following three elements:
 * 	ProtectionDomain[] pdsImplied, Permission[] permsImplied, Permission[] permsNotImplied
 *
 * @return true if the access is granted by a limited permission, otherwise an exception is thrown or
 * 			false is returned to indicate the access was NOT granted by any limited permission.
 *
 */
static boolean checkPermissionWithCache(
		Permission perm,
		Object[] pdsContext,
		int debug
		,
		AccessControlContext accCurrent,
		boolean isLimited,
		Permission[] permsLimited,
		AccessControlContext accNext,
		Object[] cacheChecked
) throws AccessControlException {
	if (((debug & DEBUG_ENABLED) != 0) && ((debugSetting() & DEBUG_ACCESS_DOMAIN) != 0)) {
		debugPrintAccess();
		if (pdsContext == null || pdsContext.length == 0) {
			System.err.println("domain (context is null)");	//$NON-NLS-1$
		} else {
			for (int i=0; i<pdsContext.length; i++) {
				System.err.println("domain " + i + " " + pdsContext[i]);	//$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	int i = (pdsContext == null) ? 0 : pdsContext.length;
	i = checkPermWithCachedPDsImplied(perm, pdsContext, cacheChecked);
	if (0 <= i) {
		// debug for access denied is not optional
		if (((debug & DEBUG_ACCESS_DENIED) != 0) && (debugSetting() & DEBUG_ACCESS) != 0) {
			debugPrintAccess();
			System.err.println("access denied " + perm);	//$NON-NLS-1$
		}
		if (((debug & DEBUG_ACCESS_DENIED) != 0) && (debugSetting() & DEBUG_ACCESS_FAILURE) != 0) {
			new Exception("Stack trace").printStackTrace();	//$NON-NLS-1$
			System.err.println("domain that failed " + pdsContext[i]);	//$NON-NLS-1$
		}
		// K002c = Access denied {0}
		throw new AccessControlException(com.ibm.oti.util.Msg.getString("K002c", perm), perm);	//$NON-NLS-1$
	}
	if (null != accCurrent
		&& (null != accCurrent.context || null != accCurrent.doPrivilegedAcc || null != accCurrent.limitedPerms || null != accCurrent.nextStackAcc)
	) {
		// accCurrent check either throwing a security exception (denied) or continue checking (the return value doesn't matter)
		checkPermissionWithCache(perm, accCurrent.context, debug, accCurrent.doPrivilegedAcc, accCurrent.isLimitedContext, accCurrent.limitedPerms, accCurrent.nextStackAcc, cacheChecked);
	}
	if (isLimited && null != permsLimited) {
		if (checkPermWithCachedPermImplied(perm, permsLimited, cacheChecked)) {
			return true;	// implied by a limited permission
		}
		if (null != accNext) {
			checkPermissionWithCache(perm, accNext.context, debug, accNext.doPrivilegedAcc, accNext.isLimitedContext, accNext.limitedPerms, accNext.nextStackAcc, cacheChecked);
		}
		return false;	// NOT implied by any limited permission
	}
	if ((debug & DEBUG_ENABLED) != 0) {
		debugPrintAccess();
		System.err.println("access allowed " + perm);	//$NON-NLS-1$
	}
	return	true;
}

/**
 * Helper to print debug information for checkPermission().
 *
 * @param perm the permission to check
 * @return if debugging is enabled
 */
private boolean debugHelper(Permission perm) {
	boolean debug = true;
	if (debugCodeBaseArray != null) {
		debug = hasDebugCodeBase();
	}
	if (debug) {
		debug = debugPermission(perm);
	}

	if (debug && ((debugSetting() & DEBUG_ACCESS_STACK) != 0)) {
		new Exception("Stack trace for " + perm).printStackTrace();	//$NON-NLS-1$
	}
	return debug;
}

/**
 * Checks if the permission <code>perm</code> is allowed in this context.
 * All ProtectionDomains must grant the permission for it to be granted.
 *
 * @param		perm java.security.Permission
 *					the permission to check
 * @exception	java.security.AccessControlException
 *					thrown when perm is not granted.
 *				NullPointerException if perm is null
 */
public void checkPermission(Permission perm) throws AccessControlException {
	if (perm == null) throw new NullPointerException();
	if (null != context && (STATE_AUTHORIZED != authorizeState) && containPrivilegedContext && null != System.getSecurityManager()) {
		// only check SecurityPermission "createAccessControlContext" when context is not null, not authorized and containPrivilegedContext.
		if (STATE_UNKNOWN == authorizeState) {
			if (null == callerPD || callerPD.implies(createAccessControlContext)) {
				authorizeState = STATE_AUTHORIZED;
			} else {
				authorizeState = STATE_NOT_AUTHORIZED;
			}
			callerPD = null;
		}
		if (STATE_NOT_AUTHORIZED == authorizeState) {
			// K002d = Access denied {0} due to untrusted AccessControlContext since {1} is denied
			throw new AccessControlException(com.ibm.oti.util.Msg.getString("K002d", perm, createAccessControlContext), perm);	//$NON-NLS-1$
		}
	}

	boolean debug = (debugSetting() & DEBUG_ACCESS) != 0;
	if (debug) {
		debug = debugHelper(perm);
	}
	checkPermissionWithCache(perm,  this.context, debug ? DEBUG_ENABLED | DEBUG_ACCESS_DENIED : DEBUG_DISABLED, this.doPrivilegedAcc,this.isLimitedContext, this.limitedPerms, this.nextStackAcc, new Object[CACHE_ARRAY_SIZE]);
}

/**
 * Compares the argument to the receiver, and answers true
 * if they represent the <em>same</em> object using a class
 * specific comparison. In this case, they must both be
 * AccessControlContexts and contain the same protection domains.
 *
 * @param		o		the object to compare with this object
 * @return		<code>true</code>
 *					if the object is the same as this object
 *				<code>false</code>
 *					if it is different from this object
 * @see			#hashCode
 */
public boolean equals(Object o) {
	if (this == o) return true;
	if (o == null || this.getClass() != o.getClass()) return false;
	AccessControlContext otherContext = (AccessControlContext) o;
	// match RI behaviors, i.e., ignore isAuthorized when performing equals
//	if ((this.isAuthorized != otherContext.isAuthorized)
	if (((null == this.domainCombiner) && (null != otherContext.domainCombiner))
		|| ((null != this.domainCombiner) && !this.domainCombiner.equals(otherContext.domainCombiner))
	) {
		return false;
	}
	if (isLimitedContext != otherContext.isLimitedContext) {
		return	false;
	}
	ProtectionDomain[] otherDomains = otherContext.context;
	int length = context == null ? 0 : context.length;
	int olength = otherDomains == null ? 0 : otherDomains.length;
	if (length != olength) return false;

	next : for (int i = 0; i < length; i++) {
		ProtectionDomain current = context[i];
		for (int j = 0; j < length; j++) {
			if ((null == current && null == otherDomains[j])
				|| (null != current && current.equals(otherDomains[j]))
			) {
				continue next;
			}
		}
		return false;
	}
	if (null != doPrivilegedAcc && !doPrivilegedAcc.equals(otherContext.doPrivilegedAcc)) {
		return false;
	}
	if (isLimitedContext) {
		Permission[] otherLimitedPerms = otherContext.limitedPerms;
		int permsLen = (null == limitedPerms) ? 0 : limitedPerms.length;
		int opermsLen = (null == otherLimitedPerms) ? 0 : otherLimitedPerms.length;
		if (permsLen != opermsLen) return false;

		nextPermsCheck : for (int i = 0; i < permsLen; i++) {
			Permission current = limitedPerms[i];
			for (int j = 0; j < opermsLen; j++) {
				if ((null == current && null == otherLimitedPerms[j])
					|| (null != current && current.equals(otherLimitedPerms[j]))
				) {
					continue nextPermsCheck;
				}
			}
			return false;
		}
		if (null != nextStackAcc) {
			return	nextStackAcc.equals(otherContext.nextStackAcc);
		}
	}
	return true;
}

/**
 * Answers an integer hash code for the receiver. Any two
 * objects which answer <code>true</code> when passed to
 * <code>equals</code> must answer the same value for this
 * method.
 *
 * @return		the receiver's hash
 *
 * @see			#equals
 */
public int hashCode() {
	int result=0;
	int i = context == null ? 0 : context.length;
	while (--i>=0)
		result ^= context[i].hashCode();
/*
	// JCK test doesn't include following two fields during hashcode calculation
	if (null != this.domainCombiner) {
		result ^= this.domainCombiner.hashCode();
	}

	result = result + (this.isAuthorized ? 1231 : 1237);
*/
	// RI equals not impacted by limited context,
	// JCK still passes with following cause the AccessControlContext in question doesn't have limited context
	// J9 might fail JCK test if JCK hashcode test changes

	if (null != doPrivilegedAcc) {
		result ^= doPrivilegedAcc.hashCode();
	}
//	result = result + (this.isLimitedContext ? 1231 : 1237);
	if (this.isLimitedContext) {
		i = (limitedPerms == null) ? 0 : limitedPerms.length;
		while (--i >= 0) {
			if (null != limitedPerms[i]) {
				result ^= limitedPerms[i].hashCode();
			}
		}
		if (null != nextStackAcc) {
			result ^= nextStackAcc.hashCode();
		}
	}
	return result;
}

/**
 * Answers the DomainCombiner for the receiver.
 *
 * @return the DomainCombiner or null
 *
 * @exception	java.security.AccessControlException thrown
 * 					when the caller doesn't have the  "getDomainCombiner" SecurityPermission
 */
public DomainCombiner getDomainCombiner() {
	SecurityManager security = System.getSecurityManager();
	if (security != null)
		security.checkPermission(getDomainCombiner);
	return domainCombiner;
}

/**
 * Answers the DomainCombiner for the receiver.
 * This is for internal use without checking "getDomainCombiner" SecurityPermission.
 *
 * @return the DomainCombiner or null
 *
 */
DomainCombiner getCombiner() {
	return domainCombiner;
}

/*
 * Added to resolve: S6907662, CVE-2010-4465: System clipboard should ensure access restrictions
 * Used internally:
 *  	java.awt.AWTEvent
 *		java.awt.Component
 *		java.awt.EventQueue
 *		java.awt.MenuComponent
 *		java.awt.TrayIcon
 *		java.security.ProtectionDomain
 *		javax.swing.Timer
 *		javax.swing.TransferHandler
 */
ProtectionDomain[] getContext() {
	return context;
}

/*
 * Added to resolve: S6907662, CVE-2010-4465: System clipboard should ensure access restrictions
 * Basically a copy of AccessController.toArrayOfProtectionDomains().
 * Called internally from java.security.ProtectionDomain
 */
AccessControlContext(ProtectionDomain[] domains, AccessControlContext acc) {
	int len = 0, size = domains == null ? 0 : domains.length;
	int extra = 0;
	if (acc != null && acc.context != null) {
		extra = acc.context.length;
	}
	ProtectionDomain[] answer = new ProtectionDomain[size + extra];
	for (int i = 0; i < size; i++) {
		boolean found = false;
		if ((answer[len] = (ProtectionDomain)domains[i]) == null)
			break;
		if (acc != null && acc.context != null) {
			for (int j=0; j<acc.context.length; j++) {
				if (answer[len] == acc.context[j]) {
					found = true;
					break;
				}
			}
		}
		if (!found) len++;
	}
	if (len == 0 && acc != null) context = acc.context;
	else
	if (len + extra == 0) {
		context = null;
	} else {
		if (len < size) {
			ProtectionDomain[] copy = new ProtectionDomain[len + extra];
			System.arraycopy(answer, 0, copy, 0, len);
			answer = copy;
		}
		if (acc != null && acc.context != null)
			System.arraycopy(acc.context, 0, answer, len, acc.context.length);
		context = answer;
	}
	this.authorizeState = STATE_AUTHORIZED;
	this.containPrivilegedContext = true;
	if ((null != acc) && (STATE_AUTHORIZED == acc.authorizeState)) {
		// inherit the domain combiner when authorized
		this.domainCombiner = acc.domainCombiner;
	}
}

/*
 * Added to resolve: S6907662, CVE-2010-4465: System clipboard should ensure access restrictions
 * Called internally from java.security.ProtectionDomain
 */
AccessControlContext optimize() {
	return this;
}

}
