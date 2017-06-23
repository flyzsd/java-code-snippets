/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */
package java.lang.invoke;

/**
 * WrongMethodTypeException is thrown to indicate an attempt to invoke a MethodHandle with the wrong MethodType.
 * This exception can also be thrown when adapting a MethodHandle in a way that is incompatible with its MethodType.
 *
 * @author		OTI
 * @version		initial
 * @since		1.7
 */
@VMCONSTANTPOOL_CLASS
public class WrongMethodTypeException extends RuntimeException {

	/**
	 * Serialized version ID
	 */
	private static final long serialVersionUID = 292L;

	/**
	 * Construct a WrongMethodTypeException.
	 */
	public WrongMethodTypeException() {
		super();
	}

	/**
	 * Construct a WrongMethodTypeException with the supplied message.
	 */
	public WrongMethodTypeException(String message){
		super(message);
	}
}

