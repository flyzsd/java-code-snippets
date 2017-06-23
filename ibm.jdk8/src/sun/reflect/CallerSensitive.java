/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2013, 2016  All Rights Reserved
 */
package sun.reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@SuppressWarnings("javadoc")
public @interface CallerSensitive {

}
