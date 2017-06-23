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

package java.io;

import java.security.*;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Collections;
import sun.security.util.SecurityConstants;
import com.ibm.jvm.MemorySafetyService;                                         //IBM-T6_wrt_bringup

/**
 * This class represents access to a file or directory.  A FilePermission consists
 * of a pathname and a set of actions valid for that pathname.
 * <P>
 * Pathname is the pathname of the file or directory granted the specified
 * actions. A pathname that ends in "/*" (where "/" is
 * the file separator character, <code>File.separatorChar</code>) indicates
 * all the files and directories contained in that directory. A pathname
 * that ends with "/-" indicates (recursively) all files
 * and subdirectories contained in that directory. A pathname consisting of
 * the special token "&lt;&lt;ALL FILES&gt;&gt;" matches <b>any</b> file.
 * <P>
 * Note: A pathname consisting of a single "*" indicates all the files
 * in the current directory, while a pathname consisting of a single "-"
 * indicates all the files in the current directory and
 * (recursively) all files and subdirectories contained in the current
 * directory.
 * <P>
 * The actions to be granted are passed to the constructor in a string containing
 * a list of one or more comma-separated keywords. The possible keywords are
 * "read", "write", "execute", "delete", and "readlink". Their meaning is
 * defined as follows:
 *
 * <DL>
 *    <DT> read <DD> read permission
 *    <DT> write <DD> write permission
 *    <DT> execute
 *    <DD> execute permission. Allows <code>Runtime.exec</code> to
 *         be called. Corresponds to <code>SecurityManager.checkExec</code>.
 *    <DT> delete
 *    <DD> delete permission. Allows <code>File.delete</code> to
 *         be called. Corresponds to <code>SecurityManager.checkDelete</code>.
 *    <DT> readlink
 *    <DD> read link permission. Allows the target of a
 *         <a href="../nio/file/package-summary.html#links">symbolic link</a>
 *         to be read by invoking the {@link java.nio.file.Files#readSymbolicLink
 *         readSymbolicLink } method.
 * </DL>
 * <P>
 * The actions string is converted to lowercase before processing.
 * <P>
 * Be careful when granting FilePermissions. Think about the implications
 * of granting read and especially write access to various files and
 * directories. The "&lt;&lt;ALL FILES&gt;&gt;" permission with write action is
 * especially dangerous. This grants permission to write to the entire
 * file system. One thing this effectively allows is replacement of the
 * system binary, including the JVM runtime environment.
 *
 * <p>Please note: Code can always read a file from the same
 * directory it's in (or a subdirectory of that directory); it does not
 * need explicit permission to do so.
 *
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 *
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 * @since 1.2
 *
 * @serial exclude
 */

public final class FilePermission extends Permission implements Serializable {

    /**
     * Execute action.
     */
    private final static int EXECUTE = 0x1;
    /**
     * Write action.
     */
    private final static int WRITE   = 0x2;
    /**
     * Read action.
     */
    private final static int READ    = 0x4;
    /**
     * Delete action.
     */
    private final static int DELETE  = 0x8;
    /**
     * Read link action.
     */
    private final static int READLINK    = 0x10;

    /**
     * All actions (read,write,execute,delete,readlink)
     */
    private final static int ALL     = READ|WRITE|EXECUTE|DELETE|READLINK;
    /**
     * No actions.
     */
    private final static int NONE    = 0x0;

    // the actions mask
    private transient int mask;

    // does path indicate a directory? (wildcard or recursive)
    private transient boolean directory;

    // is it a recursive directory specification?
    private transient boolean recursive;

    /**
     * the actions string.
     *
     * @serial
     */
    private String actions; // Left null as long as possible, then
                            // created and re-used in the getAction function.

    // canonicalized dir path. In the case of
    // directories, it is the name "/blah/*" or "/blah/-" without
    // the last character (the "*" or "-").


    // static Strings used by init(int mask)
    private static final char RECURSIVE_CHAR = '-';
    private static final char WILD_CHAR = '*';

/*
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("***\n");
        sb.append("cpath = "+cpath+"\n");
        sb.append("mask = "+mask+"\n");
        sb.append("actions = "+getActions()+"\n");
        sb.append("directory = "+directory+"\n");
        sb.append("recursive = "+recursive+"\n");
        sb.append("***\n");
        return sb.toString();
    }
*/
/* JSE-1216 starts */                                                           //IBM-net_perf
    //  Flags to assist in lazy setting.                                        //IBM-net_perf
    // Are directory and recursive set?                                         //IBM-net_perf
    private transient boolean dir_rec_set = false;                              //IBM-net_perf

    // Hierarchical cache version of canonical path.                            //IBM-net_perf
    private transient CanonicalPath cpath = null;                               //IBM-net_perf
                                                                                //IBM-net_perf
    // Partially processed path name;                                           //IBM-net_perf
    private transient String path_string;                                       //IBM-net_perf
                                                                                //IBM-net_perf
    private final static FileSystem fs = DefaultFileSystem.getFileSystem();            //IBM-net_perf
/*JSE-1216 ends*/                                                               //IBM-net_perf
                                                                                //IBM-net_perf
    private static final long serialVersionUID = 7930732926638008763L;

    /**
     * initialize a FilePermission object. Common to all constructors.
     * Also called during de-serialization.
     *
     * @param mask the actions mask to use.
     *
     */
    private void init(int mask) {
        if ((mask & ALL) != mask)
                throw new IllegalArgumentException("invalid actions mask");

        if (mask == NONE)
                throw new IllegalArgumentException("invalid actions mask");

        if (getName() == null)                 /*JSE-1216*/                     //IBM-net_perf
                throw new NullPointerException("name can't be null");

        this.mask = mask;

/*JSE-1216 starts - Moving path canonicalization to happen later when required*/ //IBM-net_perf
    cpath = null;                                                               //IBM-net_perf
    dir_rec_set = false;                                                        //IBM-net_perf
    path_string = null;                                                         //IBM-net_perf
/*JSE-1216 ends*/                                                               //IBM-net_perf
                                                                                //IBM-net_perf
    }                                                                           //IBM-net_perf
                                                                                //IBM-net_perf
/* Sets the directory and recursive flag, and does partial processing           //IBM-net_perf
 * of the path name.                                                            //IBM-net_perf
 */                                                                             //IBM-net_perf
    private void get_dir_rec() {                                                //IBM-net_perf
                                                                                //IBM-net_perf
        path_string = getName();                                                //IBM-net_perf
                                                                                //IBM-net_perf
        if (path_string.equals("<<ALL FILES>>")) {                              //IBM-net_perf
            directory = true;
            recursive = true;
            path_string = "";                                                   //IBM-net_perf
            dir_rec_set = true;                                                 //IBM-net_perf
            return;
        }

 		path_string = AccessController.doPrivileged(new PrivilegedAction<String>() { //IBM-net_perf
            public String run() {                                               //IBM-net_perf
                try {                                                           //IBM-net_perf
                    String path = path_string;                                  //IBM-net_perf
                    if (path_string.endsWith("*")) {                            //IBM-net_perf
                        // call getCanonicalPath with a path with wildcard character //IBM-net_perf
                        // replaced to avoid calling it with paths that are     //IBM-net_perf
                        // intended to match all entries in a directory         //IBM-net_perf
                        path = path.substring(0, path.length()-1) + "-";        //IBM-net_perf
                        path = new File(path).getCanonicalPath();               //IBM-net_perf
                        return path.substring(0, path.length()-1) + "*";        //IBM-net_perf
                    } else {                                                    //IBM-net_perf
                        return new File(path).getCanonicalPath();               //IBM-net_perf
                    }                                                           //IBM-net_perf
                } catch (IOException ioe) {                                     //IBM-net_perf
                    return path_string;                                         //IBM-net_perf
                }                                                               //IBM-net_perf
            }                                                                   //IBM-net_perf
        });                                                                     //IBM-net_perf
                                                                                //IBM-net_perf
        int len = path_string.length();                                         //IBM-net_perf
        char last = ((len > 0) ? path_string.charAt(len - 1) : 0);              //IBM-net_perf

        if (last == RECURSIVE_CHAR &&
            (len == 1 || path_string.charAt(len - 2) == File.separatorChar)) {  //IBM-net_perf
            directory = true;
            recursive = true;
            path_string = path_string.substring(0, --len);                      //IBM-net_perf
        } else if (last == WILD_CHAR &&
            (len == 1 || path_string.charAt(len - 2) == File.separatorChar)) {  //IBM-net_perf
            directory = true;
            //recursive = false;
            path_string = path_string.substring(0, --len);                      //IBM-net_perf
        }                                                                       //IBM-net_perf
        dir_rec_set = true;                                                     //IBM-net_perf
    }                                                                           //IBM-net_perf

                                                                                //IBM-net_perf
    //  Initialize the hierarchical cached canonical path object.               //IBM-net_perf
    private void init_cpath()                                                   //IBM-T6_wrt_bringup
    {                                                                           //IBM-T6_wrt_bringup
		long oldMemory = -1;                                            //IBM-T6_wrt_bringup
		try                                                             //IBM-T6_wrt_bringup
		{                                                               //IBM-T6_wrt_bringup
			oldMemory = MemorySafetyService.enterObjectsMemoryArea(this); //IBM-T6_wrt_bringup
	        cpath = fs.getCanonicalPath(path_string,directory);                      //IBM-T6_wrt_bringup
		}                                                               //IBM-T6_wrt_bringup
		finally                                                         //IBM-T6_wrt_bringup
		{                                                               //IBM-T6_wrt_bringup
			MemorySafetyService.exitLastMemoryArea(oldMemory);      //IBM-T6_wrt_bringup
		}                                                               //IBM-T6_wrt_bringup
    }
                                                                                //IBM-T6_wrt_bringup
/*JSE-1216 ends*/                                                               //IBM-net_perf

    /**
     * Creates a new FilePermission object with the specified actions.
     * <i>path</i> is the pathname of a file or directory, and <i>actions</i>
     * contains a comma-separated list of the desired actions granted on the
     * file or directory. Possible actions are
     * "read", "write", "execute", "delete", and "readlink".
     *
     * <p>A pathname that ends in "/*" (where "/" is
     * the file separator character, <code>File.separatorChar</code>)
     * indicates all the files and directories contained in that directory.
     * A pathname that ends with "/-" indicates (recursively) all files and
     * subdirectories contained in that directory. The special pathname
     * "&lt;&lt;ALL FILES&gt;&gt;" matches any file.
     *
     * <p>A pathname consisting of a single "*" indicates all the files
     * in the current directory, while a pathname consisting of a single "-"
     * indicates all the files in the current directory and
     * (recursively) all files and subdirectories contained in the current
     * directory.
     *
     * <p>A pathname containing an empty string represents an empty path.
     *
     * @param path the pathname of the file/directory.
     * @param actions the action string.
     *
     * @throws IllegalArgumentException
     *          If actions is <code>null</code>, empty or contains an action
     *          other than the specified possible actions.
     */
    public FilePermission(String path, String actions) {
        super(path);
        init(getMask(actions));
    }

    /**
     * Creates a new FilePermission object using an action mask.
     * More efficient than the FilePermission(String, String) constructor.
     * Can be used from within
     * code that needs to create a FilePermission object to pass into the
     * <code>implies</code> method.
     *
     * @param path the pathname of the file/directory.
     * @param mask the action mask to use.
     */

    // package private for use by the FilePermissionCollection add method
    FilePermission(String path, int mask) {
        super(path);
        init(mask);
    }

    /**
     * Checks if this FilePermission object "implies" the specified permission.
     * <P>
     * More specifically, this method returns true if:
     * <ul>
     * <li> <i>p</i> is an instanceof FilePermission,
     * <li> <i>p</i>'s actions are a proper subset of this
     * object's actions, and
     * <li> <i>p</i>'s pathname is implied by this object's
     *      pathname. For example, "/tmp/*" implies "/tmp/foo", since
     *      "/tmp/*" encompasses all files in the "/tmp" directory,
     *      including the one named "foo".
     * </ul>
     *
     * @param p the permission to check against.
     *
     * @return <code>true</code> if the specified permission is not
     *                  <code>null</code> and is implied by this object,
     *                  <code>false</code> otherwise.
     */
    public boolean implies(Permission p) {
        if (!(p instanceof FilePermission))
            return false;

        FilePermission that = (FilePermission) p;

        // we get the effective mask. i.e., the "and" of this and that.
        // They must be equal to that.mask for implies to return true.

        return ((this.mask & that.mask) == that.mask) && impliesIgnoreMask(that);
    }

    /**
     * Checks if the Permission's actions are a proper subset of the
     * this object's actions. Returns the effective mask iff the
     * this FilePermission's path also implies that FilePermission's path.
     *
     * @param that the FilePermission to check against.
     * @return the effective mask
     */
    boolean impliesIgnoreMask(FilePermission that) {
        if (this.getName().equals("<<ALL FILES>>"))              /*JSE-1216 starts*/ //IBM-net_perf
                return true;                                                    //IBM-net_perf
        if (!this.dir_rec_set) this.get_dir_rec();                              //IBM-net_perf
        if (!that.dir_rec_set) that.get_dir_rec();               /*JSE-1216 ends*/ //IBM-net_perf
        if (this.directory) {
            if (this.recursive) {
                // make sure that.path is longer then path so
                // something like /foo/- does not imply /foo
               if (that.cpath == null) that.init_cpath();  	/*JSE-1216 starts*/  //IBM-net_perf
               if (this.cpath == null) this.init_cpath();                       //IBM-net_perf
                                                                                //IBM-net_perf
               if (that.directory) {                                            //IBM-net_perf
                   return that.cpath.startsWith(this.cpath);                    //IBM-net_perf
               }                                                                //IBM-net_perf
               else {                                                           //IBM-net_perf
                   return that.cpath.startsWithAndLonger(this.cpath);           //IBM-net_perf
               }                           /*JSE-1216 ends*/                    //IBM-net_perf
            } else {
                if (that.directory) {
                    // if the permission passed in is a directory
                    // specification, make sure that a non-recursive
                    // permission (i.e., this object) can't imply a recursive
                    // permission.
                    if (that.recursive)
                        return false;
                    else {
                        if (that.cpath == null) that.init_cpath();              //IBM-net_perf
                        if (this.cpath == null) this.init_cpath();              //IBM-net_perf
                        return (this.cpath.equals(that.cpath));                 //IBM-net_perf
                    }
                } else {                                                        //IBM-net_perf
                    if (that.cpath == null) that.init_cpath();      /*JSE-1216 starts*/ //IBM-net_perf
                    if (this.cpath == null) this.init_cpath();                  //IBM-net_perf
                    return (that.cpath.hasBaseDir(this.cpath));                 //IBM-net_perf
                }
            }
        } else {
            if(that.directory || that.recursive )                               //IBM-net_perf
               return false;                                                    //IBM-net_perf
            if (that.cpath == null) that.init_cpath();                          //IBM-net_perf
            if (this.cpath == null) this.init_cpath();           /*JSE-1216 ends*/  //IBM-net_perf
            return (this.cpath.equals(that.cpath));
        }
    }

    /**
     * Checks two FilePermission objects for equality. Checks that <i>obj</i> is
     * a FilePermission, and has the same pathname and actions as this object.
     *
     * @param obj the object we are testing for equality with this object.
     * @return <code>true</code> if obj is a FilePermission, and has the same
     *          pathname and actions as this FilePermission object,
     *          <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (! (obj instanceof FilePermission))
            return false;

        FilePermission that = (FilePermission) obj;

/*JSE-1216 starts*/                                                             //IBM-net_perf
        if (!this.dir_rec_set) this.get_dir_rec();                              //IBM-net_perf
        if (!that.dir_rec_set) that.get_dir_rec();                              //IBM-net_perf
        if ((this.mask == that.mask) &&                                         //IBM-net_perf
            (this.directory == that.directory) &&
            (this.recursive == that.recursive)) {                               //IBM-net_perf
            if (that.cpath == null) that.init_cpath();                          //IBM-net_perf
            if (this.cpath == null) this.init_cpath();                          //IBM-net_perf
            return this.cpath.equals(that.cpath);                               //IBM-net_perf
        }                                                                       //IBM-net_perf
        else {                                                                  //IBM-net_perf
            return false;                                                       //IBM-net_perf
        }                                                                       //IBM-net_perf
/*JSE-1216 ends*/                                                               //IBM-net_perf
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        return 0;
    }

    /**
     * Converts an actions String to an actions mask.
     *
     * @param actions the action string.
     * @return the actions mask.
     */
    private static int getMask(String actions) {
        int mask = NONE;

        // Null action valid?
        if (actions == null) {
            return mask;
        }

        // Use object identity comparison against known-interned strings for
        // performance benefit (these values are used heavily within the JDK).
        if (actions == SecurityConstants.FILE_READ_ACTION) {
            return READ;
        } else if (actions == SecurityConstants.FILE_WRITE_ACTION) {
            return WRITE;
        } else if (actions == SecurityConstants.FILE_EXECUTE_ACTION) {
            return EXECUTE;
        } else if (actions == SecurityConstants.FILE_DELETE_ACTION) {
            return DELETE;
        } else if (actions == SecurityConstants.FILE_READLINK_ACTION) {
            return READLINK;
        }

        char[] a = actions.toCharArray();

        int i = a.length - 1;
        if (i < 0)
            return mask;

        while (i != -1) {
            char c;

            // skip whitespace
            while ((i!=-1) && ((c = a[i]) == ' ' ||
                               c == '\r' ||
                               c == '\n' ||
                               c == '\f' ||
                               c == '\t'))
                i--;

            // check for the known strings
            int matchlen;

            if (i >= 3 && (a[i-3] == 'r' || a[i-3] == 'R') &&
                          (a[i-2] == 'e' || a[i-2] == 'E') &&
                          (a[i-1] == 'a' || a[i-1] == 'A') &&
                          (a[i] == 'd' || a[i] == 'D'))
            {
                matchlen = 4;
                mask |= READ;

            } else if (i >= 4 && (a[i-4] == 'w' || a[i-4] == 'W') &&
                                 (a[i-3] == 'r' || a[i-3] == 'R') &&
                                 (a[i-2] == 'i' || a[i-2] == 'I') &&
                                 (a[i-1] == 't' || a[i-1] == 'T') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 5;
                mask |= WRITE;

            } else if (i >= 6 && (a[i-6] == 'e' || a[i-6] == 'E') &&
                                 (a[i-5] == 'x' || a[i-5] == 'X') &&
                                 (a[i-4] == 'e' || a[i-4] == 'E') &&
                                 (a[i-3] == 'c' || a[i-3] == 'C') &&
                                 (a[i-2] == 'u' || a[i-2] == 'U') &&
                                 (a[i-1] == 't' || a[i-1] == 'T') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 7;
                mask |= EXECUTE;

            } else if (i >= 5 && (a[i-5] == 'd' || a[i-5] == 'D') &&
                                 (a[i-4] == 'e' || a[i-4] == 'E') &&
                                 (a[i-3] == 'l' || a[i-3] == 'L') &&
                                 (a[i-2] == 'e' || a[i-2] == 'E') &&
                                 (a[i-1] == 't' || a[i-1] == 'T') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 6;
                mask |= DELETE;

            } else if (i >= 7 && (a[i-7] == 'r' || a[i-7] == 'R') &&
                                 (a[i-6] == 'e' || a[i-6] == 'E') &&
                                 (a[i-5] == 'a' || a[i-5] == 'A') &&
                                 (a[i-4] == 'd' || a[i-4] == 'D') &&
                                 (a[i-3] == 'l' || a[i-3] == 'L') &&
                                 (a[i-2] == 'i' || a[i-2] == 'I') &&
                                 (a[i-1] == 'n' || a[i-1] == 'N') &&
                                 (a[i] == 'k' || a[i] == 'K'))
            {
                matchlen = 8;
                mask |= READLINK;

            } else {
                // parse error
                throw new IllegalArgumentException(
                        "invalid permission: " + actions);
            }

            // make sure we didn't just match the tail of a word
            // like "ackbarfaccept".  Also, skip to the comma.
            boolean seencomma = false;
            while (i >= matchlen && !seencomma) {
                switch(a[i-matchlen]) {
                case ',':
                    seencomma = true;
                    break;
                case ' ': case '\r': case '\n':
                case '\f': case '\t':
                    break;
                default:
                    throw new IllegalArgumentException(
                            "invalid permission: " + actions);
                }
                i--;
            }

            // point i at the location of the comma minus one (or -1).
            i -= matchlen;
        }

        return mask;
    }

    /**
     * Return the current action mask. Used by the FilePermissionCollection.
     *
     * @return the actions mask.
     */
    int getMask() {
        return mask;
    }

    /**
     * Return the canonical string representation of the actions.
     * Always returns present actions in the following order:
     * read, write, execute, delete, readlink.
     *
     * @return the canonical string representation of the actions.
     */
    private static String getActions(int mask) {
        StringBuilder sb = new StringBuilder();
        boolean comma = false;

        if ((mask & READ) == READ) {
            comma = true;
            sb.append("read");
        }

        if ((mask & WRITE) == WRITE) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("write");
        }

        if ((mask & EXECUTE) == EXECUTE) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("execute");
        }

        if ((mask & DELETE) == DELETE) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("delete");
        }

        if ((mask & READLINK) == READLINK) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("readlink");
        }

        return sb.toString();
    }

    /**
     * Returns the "canonical string representation" of the actions.
     * That is, this method always returns present actions in the following order:
     * read, write, execute, delete, readlink. For example, if this FilePermission
     * object allows both write and read actions, a call to <code>getActions</code>
     * will return the string "read,write".
     *
     * @return the canonical string representation of the actions.
     */
    public String getActions() {
        if (actions == null)
            actions = getActions(this.mask);

        return actions;
    }

    /**
     * Returns a new PermissionCollection object for storing FilePermission
     * objects.
     * <p>
     * FilePermission objects must be stored in a manner that allows them
     * to be inserted into the collection in any order, but that also enables the
     * PermissionCollection <code>implies</code>
     * method to be implemented in an efficient (and consistent) manner.
     *
     * <p>For example, if you have two FilePermissions:
     * <OL>
     * <LI>  <code>"/tmp/-", "read"</code>
     * <LI>  <code>"/tmp/scratch/foo", "write"</code>
     * </OL>
     *
     * <p>and you are calling the <code>implies</code> method with the FilePermission:
     *
     * <pre>
     *   "/tmp/scratch/foo", "read,write",
     * </pre>
     *
     * then the <code>implies</code> function must
     * take into account both the "/tmp/-" and "/tmp/scratch/foo"
     * permissions, so the effective permission is "read,write",
     * and <code>implies</code> returns true. The "implies" semantics for
     * FilePermissions are handled properly by the PermissionCollection object
     * returned by this <code>newPermissionCollection</code> method.
     *
     * @return a new PermissionCollection object suitable for storing
     * FilePermissions.
     */
    public PermissionCollection newPermissionCollection()                       //IBM-T6_wrt_bringup
    {                                                                           //IBM-T6_wrt_bringup
       long oldMemory = -1;                                            //IBM-T6_wrt_bringup
       try                                                             //IBM-T6_wrt_bringup
       {                                                               //IBM-T6_wrt_bringup
           oldMemory = MemorySafetyService.enterSafeMemoryArea();  //IBM-T6_wrt_bringup
           return new FilePermissionCollection();                  //IBM-T6_wrt_bringup
       }                                                               //IBM-T6_wrt_bringup
       finally                                                         //IBM-T6_wrt_bringup
       {                                                               //IBM-T6_wrt_bringup
           MemorySafetyService.exitLastMemoryArea(oldMemory);      //IBM-T6_wrt_bringup
       }                                                               //IBM-T6_wrt_bringup
                                                                                //IBM-T6_wrt_bringup
    }

    /**
     * WriteObject is called to save the state of the FilePermission
     * to a stream. The actions are serialized, and the superclass
     * takes care of the name.
     */
    private void writeObject(ObjectOutputStream s)
        throws IOException
    {
        // Write out the actions. The superclass takes care of the name
        // call getActions to make sure actions field is initialized
        if (actions == null)
            getActions();
        s.defaultWriteObject();
    }

    /**
     * readObject is called to restore the state of the FilePermission from
     * a stream.
     */
    private void readObject(ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
        // Read in the actions, then restore everything else by calling init.
        s.defaultReadObject();
        init(getMask(actions));
    }
}

/**
 * A FilePermissionCollection stores a set of FilePermission permissions.
 * FilePermission objects
 * must be stored in a manner that allows them to be inserted in any
 * order, but enable the implies function to evaluate the implies
 * method.
 * For example, if you have two FilePermissions:
 * <OL>
 * <LI> "/tmp/-", "read"
 * <LI> "/tmp/scratch/foo", "write"
 * </OL>
 * And you are calling the implies function with the FilePermission:
 * "/tmp/scratch/foo", "read,write", then the implies function must
 * take into account both the /tmp/- and /tmp/scratch/foo
 * permissions, so the effective permission is "read,write".
 *
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 *
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 *
 * @serial include
 *
 */

final class FilePermissionCollection extends PermissionCollection
    implements Serializable
{
    // Not serialized; see serialization section at end of class
    private transient List<Permission> perms;

    /**
     * Create an empty FilePermissionCollection object.
     */
    public FilePermissionCollection() {
        perms = new ArrayList<>();
    }

    /**
     * Adds a permission to the FilePermissionCollection. The key for the hash is
     * permission.path.
     *
     * @param permission the Permission object to add.
     *
     * @exception IllegalArgumentException - if the permission is not a
     *                                       FilePermission
     *
     * @exception SecurityException - if this FilePermissionCollection object
     *                                has been marked readonly
     */
    public void add(Permission permission) {
        if (! (permission instanceof FilePermission))
            throw new IllegalArgumentException("invalid permission: "+
                                               permission);
        if (isReadOnly())
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");

        synchronized (this) {
            perms.add(permission);
        }
    }

    /**
     * Check and see if this set of permissions implies the permissions
     * expressed in "permission".
     *
     * @param permission the Permission object to compare
     *
     * @return true if "permission" is a proper subset of a permission in
     * the set, false if not.
     */
    public boolean implies(Permission permission) {
        if (! (permission instanceof FilePermission))
            return false;

        FilePermission fp = (FilePermission) permission;

        int desired = fp.getMask();
        int effective = 0;
        int needed = desired;

        synchronized (this) {
            int len = perms.size();
            for (int i = 0; i < len; i++) {
                FilePermission x = (FilePermission) perms.get(i);
                if (((needed & x.getMask()) != 0) && x.impliesIgnoreMask(fp)) {
                    effective |=  x.getMask();
                    if ((effective & desired) == desired)
                        return true;
                    needed = (desired ^ effective);
                }
            }
        }
        return false;
    }

    /**
     * Returns an enumeration of all the FilePermission objects in the
     * container.
     *
     * @return an enumeration of all the FilePermission objects.
     */
    public Enumeration<Permission> elements() {
        // Convert Iterator into Enumeration
        synchronized (this) {
            return Collections.enumeration(perms);
        }
    }

    private static final long serialVersionUID = 2202956749081564585L;

    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:
    //    private Vector permissions;

    /**
     * @serialField permissions java.util.Vector
     *     A list of FilePermission objects.
     */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("permissions", Vector.class),
    };

    /**
     * @serialData "permissions" field (a Vector containing the FilePermissions).
     */
    /*
     * Writes the contents of the perms field out as a Vector for
     * serialization compatibility with earlier releases.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()

        // Write out Vector
        Vector<Permission> permissions = new Vector<>(perms.size());
        synchronized (this) {
            permissions.addAll(perms);
        }

        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("permissions", permissions);
        out.writeFields();
    }

    /*
     * Reads in a Vector of FilePermissions and saves them in the perms field.
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        // Don't call defaultReadObject()

        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();

        // Get the one we want
        @SuppressWarnings("unchecked")
        Vector<Permission> permissions = (Vector<Permission>)gfields.get("permissions", null);
        perms = new ArrayList<>(permissions.size());
        perms.addAll(permissions);
    }
}
//IBM-net_perf
//IBM-T6_wrt_bringup
