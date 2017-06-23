/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1997, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.security.spec;

/**
 * A (transparent) specification of the key material
 * that constitutes a cryptographic key.
 *
 * <p>If the key is stored on a hardware device, its
 * specification may contain information that helps identify the key on the
 * device.
 *
 * <P> A key may be specified in an algorithm-specific way, or in an
 * algorithm-independent encoding format (such as ASN.1).
 * For example, a DSA private key may be specified by its components
 * {@code x}, {@code p}, {@code q}, and {@code g}
 * (see {@link DSAPrivateKeySpec}), or it may be
 * specified using its DER encoding
 * (see {@link PKCS8EncodedKeySpec}).
 *
 * <P> This interface contains no methods or constants. Its only purpose
 * is to group (and provide type safety for) all key specifications.
 * All key specifications must implement this interface.
 *
 * @author Jan Luehe
 *
 *
 * @see java.security.Key
 * @see java.security.KeyFactory
 * @see EncodedKeySpec
 * @see X509EncodedKeySpec
 * @see PKCS8EncodedKeySpec
 * @see DSAPrivateKeySpec
 * @see DSAPublicKeySpec
 *
 * @since 1.2
 */

public interface KeySpec { }
