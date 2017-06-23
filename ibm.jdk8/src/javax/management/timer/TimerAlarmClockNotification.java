/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2002, 2006. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2002, 2006, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.timer;

/**
 * <p>Definitions of the notifications sent by TimerAlarmClock
 * MBeans.</p>
 */
class TimerAlarmClockNotification
    extends javax.management.Notification {

    /* Serial version */
    private static final long serialVersionUID = -4841061275673620641L;

    /*
     * ------------------------------------------
     *  CONSTRUCTORS
     * ------------------------------------------
     */

    /**
     * Constructor.
     *
     * @param source the source.
     */
    public TimerAlarmClockNotification(TimerAlarmClock source) {
        super("", source, 0);
    }
}
