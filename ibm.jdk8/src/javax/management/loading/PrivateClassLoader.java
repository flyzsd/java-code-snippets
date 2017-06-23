/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2002, 2007. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.loading;

/**
 * Marker interface indicating that a ClassLoader should not be added
 * to the {@link ClassLoaderRepository}.  When a ClassLoader is
 * registered as an MBean in the MBean server, it is added to the
 * MBean server's ClassLoaderRepository unless it implements this
 * interface.
 *
 * @since 1.5
 */
public interface PrivateClassLoader {}
