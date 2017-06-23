/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2014, 2014. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * ===========================================================================
 (C) Copyright Sun Microsystems Inc, 1992, 2004. All rights reserved.
 * ===========================================================================
 */


/*
 * ===========================================================================
 * Change activity:
 *
 * Reason  Date    Origin   Description
 * ------ ------   -------- ----------------------------------------------------
 * 087365 18052005 mpartrid Module created. 
 * J-2295 13072007 mbluemel Ported to Java 6
 *
 * ===========================================================================
 * Module Information:
 *
 * DESCRIPTION: Class containing Socket utility methods.
 * ===========================================================================
 */
 
package java.net;

class SocketUtil
{
    public SocketUtil()
    {
    }

    // Dummy SocketUtil methods, for Windows implementation.

    static private long getThreadTag()
    {
        return 0;
    }

    static private boolean isBlocked(long tag)
    {
        return false;
    }

    static private boolean interrupt(long tag)
    {
        return false;
    }

}
