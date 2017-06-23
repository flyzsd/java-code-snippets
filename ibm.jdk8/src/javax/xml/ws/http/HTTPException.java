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

package javax.xml.ws.http;


/** The <code>HTTPException</code> exception represents a
 *  XML/HTTP fault.
 *
 *  <p>Since there is no standard format for faults or exceptions
 *  in XML/HTTP messaging, only the HTTP status code is captured.
 *
 *  @since JAX-WS 2.0
**/
public class HTTPException extends javax.xml.ws.ProtocolException  {

  private int statusCode;

  /** Constructor for the HTTPException
   *  @param statusCode   <code>int</code> for the HTTP status code
  **/
  public HTTPException(int statusCode) {
    super();
    this.statusCode = statusCode;
  }

  /** Gets the HTTP status code.
   *
   *  @return HTTP status code
  **/
  public int getStatusCode() {
    return statusCode;
  }
}
