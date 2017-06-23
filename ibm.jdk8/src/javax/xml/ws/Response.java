/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2005, 2010. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.ws;

import java.util.Map;
import java.util.concurrent.Future;

/** The <code>Response</code> interface provides methods used to obtain the
 *  payload and context of a message sent in response to an operation
 *  invocation.
 *
 *  <p>For asynchronous operation invocations it provides additional methods
 *  to check the status of the request. The <code>get(...)</code> methods may
 *  throw the standard
 *  set of exceptions and their cause may be a <code>RemoteException</code> or a
 *  {@link WebServiceException} that represents the error that occured during the
 *  asynchronous method invocation.</p>
 *
 *  @since JAX-WS 2.0
**/
public interface Response<T> extends Future<T> {
    /** Gets the contained response context.
     *
     * @return The contained response context. May be <code>null</code> if a
     * response is not yet available.
     *
    **/
    Map<String,Object> getContext();
}
