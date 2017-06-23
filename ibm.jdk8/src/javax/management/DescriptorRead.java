/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2004, 2005. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2004, 2005, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;

/**
 * Interface to read the Descriptor of a management interface element
 * such as an MBeanInfo.
 * @since 1.6
 */
public interface DescriptorRead {
   /**
    * Returns a copy of Descriptor.
    *
    * @return Descriptor associated with the component implementing this interface.
    * The return value is never null, but the returned descriptor may be empty.
    */
    public Descriptor getDescriptor();
}
