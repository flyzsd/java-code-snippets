/*******************************************************************************
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 *
 *     Copyright IBM Corp. 2001, 2016 All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/

package java.lang;

/*
 * InternalAnonymousClassLoader cannot directly load classes.
 * This ClassLoader "owns" the native memory for classes that
 * have been loaded using sun.misc.Unsafe.defineAnonymousClass.
 */
final class InternalAnonymousClassLoader extends ClassLoader {

	InternalAnonymousClassLoader() {
		super();
	}

	 @Override
	 protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
	 		 throw new ClassNotFoundException();
	 }

	 @Override
	 protected Class<?> findClass(String name) throws ClassNotFoundException {
	 		 throw new ClassNotFoundException();
	 }
}
