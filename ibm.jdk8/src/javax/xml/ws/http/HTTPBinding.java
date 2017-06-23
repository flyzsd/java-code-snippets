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


import javax.xml.ws.Binding;

/** The <code>HTTPBinding</code> interface is an
 *  abstraction for the XML/HTTP binding.
 *
 *  @since JAX-WS 2.0
**/
public interface HTTPBinding extends Binding {

  /**
   * A constant representing the identity of the XML/HTTP binding.
   */
  public static final String HTTP_BINDING = "http://www.w3.org/2004/08/wsdl/http";
}
