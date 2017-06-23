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


package java.io;

import java.security.AccessController;
import sun.security.action.GetPropertyAction;


/**
 * Package-private class for the canonical path.
 */
class CanonicalPath {

    // Note that orig_path and orig_dir are kept around only
    // until cpath is set.  Once the full canonical path has
    // been obtained it is assumed that the
    // original parameters are no longer needed.
    private String cpath = null;
    private String orig_path = null;
    private boolean orig_dir = false;

    // This flag is to support the use of hashCode by subclasses.
    //   We require that subclasses override the other methods or
    //   invoke a meaningful constructor here (otherwise cpath
    //   will be null and they'll be out of luck when they invoke
    //   a comparison method.
    private boolean cpath_set = false;

    // Meaningless, but needed to avoid compile error.
    public CanonicalPath() { }

    public CanonicalPath(String path, boolean directory) {
        // orig_path and orig_dir are not set since we're computing the
        //   full canonical path now.
        init_cpath(path,directory);
    }

    public CanonicalPath(String path, boolean directory,
      boolean set_cpath)
    {
        // Either get the full canonical path or remember the incoming
        //   parameters.
        if (set_cpath) {
            init_cpath(path,directory);
        }
        else {
            orig_path = path;
            orig_dir = directory;
        }
    }


    private void init_cpath(String path, boolean directory) {
        // To make compiler happy.
        final boolean temp_dir_flag = directory;


        // ibm@65348 - order of the next two lines reversed
        cpath = path;
        int len = cpath.length();

        if (len == 0) {
            cpath = (String) java.security.AccessController.doPrivileged(
                 new sun.security.action.GetPropertyAction("user.dir"));
        }

        // need a doPrivileged block as getCanonicalPath
        // might attempt to access user.dir to turn a relative
        // path into an absolute path.
        cpath = (String)
            AccessController.doPrivileged(
                new java.security.PrivilegedAction() {
            public Object run() {
                try {
                    File file = new File(cpath);
                    String canonical_path = file.getCanonicalPath();
                    int ln;
                    if (temp_dir_flag &&
                        ((ln=canonical_path.length()) == 0 ||
                        canonical_path.charAt(ln-1) != File.separatorChar)) {
                        return canonical_path + File.separator;
                    } else {
                        return canonical_path;
                    }
                } catch (IOException ioe) {
                    // ignore if we can't canonicalize path?
                }
                return cpath;
            }
        });
        cpath_set = true;
        orig_path = null;   // Don't need this any longer.
    }

    // The hashCode must be the hashCode of the String representing
    //  the full canonical path.  This is to support the de facto
    //  behavior of the original FilePermission code even though
    //  this definition of hashCode is not reflected in the
    //  FilePermission documentation as far as I can tell.
    public int hashCode() {
        if (!cpath_set) init_cpath(orig_path,orig_dir);
        return cpath.hashCode();
    }

    boolean equals(CanonicalPath that) {
        return (this.cpath.equals(that.cpath));
    }

    boolean startsWith(CanonicalPath that) {
        return (this.cpath.length() >= that.cpath.length()) &&
            this.cpath.startsWith(that.cpath);
    }

    boolean startsWithAndLonger(CanonicalPath that) {
        return (this.cpath.length() > that.cpath.length()) &&
            this.cpath.startsWith(that.cpath);
    }

    boolean hasBaseDir(CanonicalPath that) {
        int last = this.cpath.lastIndexOf(File.separatorChar);
        if (last == -1)
            return false;
        else {
            // Use regionMatches to avoid creating a new string

            return (that.cpath.length() == (last + 1)) &&
                that.cpath.regionMatches(0, this.cpath, 0, last+1);

        }
    }
}
