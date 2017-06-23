/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2006, 2006. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

package javax.tools;

/**
 * Interface for recognizing options.
 *
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public interface OptionChecker {

    /**
     * Determines if the given option is supported and if so, the
     * number of arguments the option takes.
     *
     * @param option an option
     * @return the number of arguments the given option takes or -1 if
     * the option is not supported
     */
    int isSupportedOption(String option);

}
