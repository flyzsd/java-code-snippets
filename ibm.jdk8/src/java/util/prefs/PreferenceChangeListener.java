/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2000, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.util.prefs;

/**
 * A listener for receiving preference change events.
 *
 * @author  Josh Bloch
 * @see Preferences
 * @see PreferenceChangeEvent
 * @see NodeChangeListener
 * @since   1.4
 */
@FunctionalInterface
public interface PreferenceChangeListener extends java.util.EventListener {
    /**
     * This method gets called when a preference is added, removed or when
     * its value is changed.
     * <p>
     * @param evt A PreferenceChangeEvent object describing the event source
     *          and the preference that has changed.
     */
    void preferenceChange(PreferenceChangeEvent evt);
}
