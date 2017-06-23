/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2000, 2000. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
package java.beans;

/**
 * An ExceptionListener is notified of internal exceptions.
 *
 * @since 1.4
 *
 * @author Philip Milne
 */
public interface ExceptionListener {
    /**
     * This method is called when a recoverable exception has
     * been caught.
     *
     * @param e The exception that was caught.
     *
     */
    public void exceptionThrown(Exception e);
}
