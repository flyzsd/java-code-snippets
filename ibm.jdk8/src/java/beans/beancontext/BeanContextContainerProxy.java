/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1998, 2002. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1998, 2002, Oracle and/or its affiliates. All rights reserved.
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

package java.beans.beancontext;

import java.awt.Container;

/**
 * <p>
 * This interface is implemented by BeanContexts' that have an AWT Container
 * associated with them.
 * </p>
 *
 * @author Laurence P. G. Cable
 * @since 1.2
 *
 * @see java.beans.beancontext.BeanContext
 * @see java.beans.beancontext.BeanContextSupport
 */

public interface BeanContextContainerProxy {

    /**
     * Gets the <code>java.awt.Container</code> associated
     * with this <code>BeanContext</code>.
     * @return the <code>java.awt.Container</code> associated
     * with this <code>BeanContext</code>.
     */
    Container getContainer();
}
