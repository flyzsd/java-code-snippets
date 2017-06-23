
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
 * This runtime exception is thrown when a program attempts to cast a an object
 * to a type which it is not compatable with.
 *
 * @author OTI
 * @version initial
 */
public class ClassCastException extends RuntimeException {
	private static final long serialVersionUID = -9223365651070458532L;

	/**
	 * Constructs a new instance of this class with its walkback filled in.
	 *
	 * @author OTI
	 * @version initial
	 */
	public ClassCastException() {
		super();
	}

	/**
	 * Constructs a new instance of this class with its walkback and message
	 * filled in.
	 *
	 * @author OTI
	 * @version initial
	 *
	 * @param detailMessage
	 *            String The detail message for the exception.
	 */
	public ClassCastException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * Constructs a new instance of this class with its walkback and message
	 * filled in.
	 *
	 * @author OTI
	 * @version initial
	 *
	 * @param instanceClass
	 *            Class The class being cast from.
	 *
	 * @param castClass
	 *            Class The class being cast to.
	 */
	ClassCastException(Class instanceClass, Class castClass) {
		// K0340 = {0} incompatible with {1}
		super(com.ibm.oti.util.Msg.getString("K0340", instanceClass.getName(), castClass.getName())); //$NON-NLS-1$
	}

}
