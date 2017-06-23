/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2010, 2011. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package javax.lang.model.type;

import java.util.List;

/**
 * Represents a union type.
 *
 * As of the {@link javax.lang.model.SourceVersion#RELEASE_7
 * RELEASE_7} source version, union types can appear as the type
 * of a multi-catch exception parameter.
 *
 * @since 1.7
 */
public interface UnionType extends TypeMirror {

    /**
     * Return the alternatives comprising this union type.
     *
     * @return the alternatives comprising this union type.
     */
    List<? extends TypeMirror> getAlternatives();
}
