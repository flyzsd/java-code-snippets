/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2000, 2001. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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


package javax.print.attribute;

/**
 * Interface PrintRequestAttribute is a tagging interface which a printing
 * attribute class implements to indicate the attribute denotes a
 * requested setting for a print job.
 * <p>
 * Attributes which are tagged with PrintRequestAttribute and are also tagged
 * as PrintJobAttribute, represent the subset of job attributes which
 * can be part of the specification of a job request.
 * <p>
 * If an attribute implements {@link DocAttribute  DocAttribute}
 * as well as PrintRequestAttribute, the client may include the
 * attribute in a <code>Doc</code>}'s attribute set to specify
 * a job setting which pertains just to that doc.
 * <P>
 *
 * @see DocAttributeSet
 * @see PrintRequestAttributeSet
 *
 * @author  Alan Kaminsky
 */

public interface PrintRequestAttribute extends Attribute {
}
