/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1999, 1999. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */


package javax.naming.directory;

import javax.naming.NamingException;

/**
  * This exception is thrown when the specification of
  * the SearchControls for a search operation is invalid. For example, if the scope is
  * set to a value other than OBJECT_SCOPE, ONELEVEL_SCOPE, SUBTREE_SCOPE,
  * this exception is thrown.
  * <p>
  * Synchronization and serialization issues that apply to NamingException
  * apply directly here.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @since 1.3
  */
public class InvalidSearchControlsException extends NamingException {
    /**
     * Constructs a new instance of InvalidSearchControlsException.
     * All fields are set to null.
     */
    public InvalidSearchControlsException() {
        super();
    }

    /**
     * Constructs a new instance of InvalidSearchControlsException
     * with an explanation. All other fields set to null.
     * @param msg Detail about this exception. Can be null.
     * @see java.lang.Throwable#getMessage
     */
    public InvalidSearchControlsException(String msg) {
        super(msg);
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = -5124108943352665777L;
}
