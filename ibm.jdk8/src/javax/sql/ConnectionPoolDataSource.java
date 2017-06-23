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

package javax.sql;

import java.sql.SQLException;


/**
 * A factory for <code>PooledConnection</code>
 * objects.  An object that implements this interface will typically be
 * registered with a naming service that is based on the
 * Java&trade; Naming and Directory Interface
 * (JNDI).
 *
 * @since 1.4
 */

public interface ConnectionPoolDataSource  extends CommonDataSource {

  /**
   * Attempts to establish a physical database connection that can
   * be used as a pooled connection.
   *
   * @return  a <code>PooledConnection</code> object that is a physical
   *         connection to the database that this
   *         <code>ConnectionPoolDataSource</code> object represents
   * @exception SQLException if a database access error occurs
   * @exception java.sql.SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  PooledConnection getPooledConnection() throws SQLException;

  /**
   * Attempts to establish a physical database connection that can
   * be used as a pooled connection.
   *
   * @param user the database user on whose behalf the connection is being made
   * @param password the user's password
   * @return  a <code>PooledConnection</code> object that is a physical
   *         connection to the database that this
   *         <code>ConnectionPoolDataSource</code> object represents
   * @exception SQLException if a database access error occurs
   * @exception java.sql.SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  PooledConnection getPooledConnection(String user, String password)
    throws SQLException;
 }
