/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1996, 1998. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1996, 1998, Oracle and/or its affiliates. All rights reserved.
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
package java.rmi.dgc;

/**
 * A lease contains a unique VM identifier and a lease duration. A
 * Lease object is used to request and grant leases to remote object
 * references.
 */
public final class Lease implements java.io.Serializable {

    /**
     * @serial Virtual Machine ID with which this Lease is associated.
     * @see #getVMID
     */
    private VMID vmid;

    /**
     * @serial Duration of this lease.
     * @see #getValue
     */
    private long value;
    /** indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = -5713411624328831948L;

    /**
     * Constructs a lease with a specific VMID and lease duration. The
     * vmid may be null.
     * @param id VMID associated with this lease
     * @param duration lease duration
     */
    public Lease(VMID id, long duration)
    {
        vmid = id;
        value = duration;
    }

    /**
     * Returns the client VMID associated with the lease.
     * @return client VMID
     */
    public VMID getVMID()
    {
        return vmid;
    }

    /**
     * Returns the lease duration.
     * @return lease duration
     */
    public long getValue()
    {
        return value;
    }
}
