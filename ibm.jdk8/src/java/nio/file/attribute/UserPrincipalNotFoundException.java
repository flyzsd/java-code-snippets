/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2007, 2009. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

import java.io.IOException;

/**
 * Checked exception thrown when a lookup of {@link UserPrincipal} fails because
 * the principal does not exist.
 *
 * @since 1.7
 */

public class UserPrincipalNotFoundException
    extends IOException
{
    static final long serialVersionUID = -5369283889045833024L;

    private final String name;

    /**
     * Constructs an instance of this class.
     *
     * @param   name
     *          the principal name; may be {@code null}
     */
    public UserPrincipalNotFoundException(String name) {
        super();
        this.name = name;
    }

    /**
     * Returns the user principal name if this exception was created with the
     * user principal name that was not found, otherwise <tt>null</tt>.
     *
     * @return  the user principal name or {@code null}
     */
    public String getName() {
        return name;
    }
}
