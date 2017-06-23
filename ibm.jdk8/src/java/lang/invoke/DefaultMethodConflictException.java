/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2014, 2016  All Rights Reserved
 */
package java.lang.invoke;

/**
 * DefaultMethodConflictException is thrown when method resolution
 * found conflicting definitions of interface default methods.
 */
@VMCONSTANTPOOL_CLASS
final class DefaultMethodConflictException extends RuntimeException {
	/**
	 * Serialized version ID
	 */
	private static final long serialVersionUID = 292L;

	/**
	 * Construct a DefaultMethodConflictException with the supplied message.
	 * @param message Describes the method conflicts
	 */
	DefaultMethodConflictException(String message){
		super(message);
	}
}

