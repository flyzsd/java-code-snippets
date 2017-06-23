/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2004, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.bind.annotation.adapters;

import javax.xml.bind.DatatypeConverter;

/**
 * {@link XmlAdapter} for <tt>xs:hexBinary</tt>.
 *
 * <p>
 * This {@link XmlAdapter} binds <tt>byte[]</tt> to the hexBinary representation in XML.
 *
 * @author Kohsuke Kawaguchi
 * @since JAXB 2.0
 */
public final class HexBinaryAdapter extends XmlAdapter<String,byte[]> {
    public byte[] unmarshal(String s) {
        if(s==null)     return null;
        return DatatypeConverter.parseHexBinary(s);
    }

    public String marshal(byte[] bytes) {
        if(bytes==null)     return null;
        return DatatypeConverter.printHexBinary(bytes);
    }
}
