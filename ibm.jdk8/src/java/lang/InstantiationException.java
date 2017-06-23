
package java.lang;

/*******************************************************************************
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 *
 *     Copyright IBM Corp. 1998, 2016 All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/

/**
 * This exception is thrown when a program attempts
 * to access a constructor which is not accessible
 * from the location where the reference is made.
 *
 * @author		OTI
 * @version		initial
 */
public class InstantiationException extends ReflectiveOperationException {
	private static final long serialVersionUID = -8441929162975509110L;

/**
 * Constructs a new instance of this class with its
 * walkback filled in.
 *
 * @author		OTI
 * @version		initial
 */
public InstantiationException () {
	super();
}
/**
 * Constructs a new instance of this class with its
 * walkback and message filled in.
 *
 * @author		OTI
 * @version		initial
 *
 * @param		detailMessage String
 *				The detail message for the exception.
 */
public InstantiationException (String detailMessage) {
	super(detailMessage);
}

/**
 * Constructs a new instance of this class with its
 * walkback and message filled in.
 *
 * @author		OTI
 * @version		initial
 *
 * @param		clazz Class
 *				The class which cannot be instantiated.
 */
InstantiationException (Class clazz) {
	super(clazz.getName());
}
}
