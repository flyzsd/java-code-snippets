
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
 * This error is thrown when the VM notices that a
 * an attempt is being made to create a new instance
 * of a class which has no visible constructors from
 * the location where new is invoked.
 * <p>
 * Note that this can only occur when inconsistant
 * class files are being loaded.
 *
 * @author		OTI
 * @version		initial
 */
public class InstantiationError extends IncompatibleClassChangeError {
	private static final long serialVersionUID = -4885810657349421204L;

/**
 * Constructs a new instance of this class with its
 * walkback filled in.
 *
 * @author		OTI
 * @version		initial
 */
public InstantiationError () {
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
public InstantiationError (String detailMessage) {
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
InstantiationError (Class clazz) {
	super(clazz.getName());
}

}
