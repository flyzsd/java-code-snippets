/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1997, 2015. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.io.Closeable;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;                                                         //IBM-shared_classes_misc
import java.util.WeakHashMap;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;                                                  //IBM-shared_classes_misc
import java.util.regex.Matcher;                                                  //IBM-shared_classes_misc
import java.util.regex.PatternSyntaxException;                                  //IBM-shared_classes_misc
import java.util.StringTokenizer;
import sun.misc.Resource;
import sun.misc.URLClassPath;
import sun.net.www.ParseUtil;
import sun.security.util.SecurityConstants;
import com.ibm.jvm.ClassLoaderDiagnosticsHelper;                                //IBM-shared_classes_misc
import com.ibm.oti.shared.Shared;                                               //IBM-shared_classes_misc
import com.ibm.oti.shared.SharedClassHelperFactory;                             //IBM-shared_classes_misc
import com.ibm.oti.shared.SharedClassURLClasspathHelper;                        //IBM-shared_classes_misc
import com.ibm.oti.shared.HelperAlreadyDefinedException;                        //IBM-shared_classes_misc
import com.ibm.jvm.MemorySafetyService;                                         //IBM-shared_classes_misc

/**
 * This class loader is used to load classes and resources from a search
 * path of URLs referring to both JAR files and directories. Any URL that
 * ends with a '/' is assumed to refer to a directory. Otherwise, the URL
 * is assumed to refer to a JAR file which will be opened as needed.
 * <p>
 * The AccessControlContext of the thread that created the instance of
 * URLClassLoader will be used when subsequently loading classes and
 * resources.
 * <p>
 * The classes that are loaded are by default granted permission only to
 * access the URLs specified when the URLClassLoader was created.
 *
 * @author  David Connelly
 * @since   1.2
 */
public class URLClassLoader extends SecureClassLoader implements Closeable {
    /* The search path for classes and resources */
    private final URLClassPath ucp;

    /* The context to be used when loading classes and resources */
    private final AccessControlContext acc;
    /* Private member fields used for Shared classes*/                           //IBM-shared_classes_misc
    private SharedClassURLClasspathHelper sharedClassURLClasspathHelper;         //IBM-shared_classes_misc
    private SharedClassMetaDataCache sharedClassMetaDataCache;                   //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
    /*                                                                           //IBM-shared_classes_misc
     * Wrapper class for maintaining the index of where the metadata (codesource and manifest)  //IBM-shared_classes_misc
     * is found - used only in Shared classes context.                           //IBM-shared_classes_misc
     */                                                                          //IBM-shared_classes_misc
    private class SharedClassIndexHolder implements SharedClassURLClasspathHelper.IndexHolder {  //IBM-shared_classes_misc
        int index;                                                               //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
        public void setIndex(int index) {                                        //IBM-shared_classes_misc
            this.index = index;                                                  //IBM-shared_classes_misc
        }                                                                        //IBM-shared_classes_misc
    }                                                                            //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
    /*                                                                           //IBM-shared_classes_misc
     * Wrapper class for internal storage of metadata (codesource and manifest) associated with   //IBM-shared_classes_misc
     * shared class - used only in Shared classes context.                       //IBM-shared_classes_misc
     */                                                                          //IBM-shared_classes_misc
    private class SharedClassMetaData {                                          //IBM-shared_classes_misc
        private CodeSource codeSource;                                           //IBM-shared_classes_misc
        private Manifest manifest;                                               //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
        SharedClassMetaData(CodeSource codeSource, Manifest manifest) {          //IBM-shared_classes_misc
            this.codeSource = codeSource;                                        //IBM-shared_classes_misc
            this.manifest = manifest;                                            //IBM-shared_classes_misc
        }                                                                        //IBM-shared_classes_misc
        public CodeSource getCodeSource() { return codeSource; }                 //IBM-shared_classes_misc
        public Manifest getManifest() { return manifest; }                       //IBM-shared_classes_misc
    }                                                                            //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
    /*                                                                           //IBM-shared_classes_misc
     * Represents a collection of SharedClassMetaData objects retrievable by     //IBM-shared_classes_misc
     * index.                                                                    //IBM-shared_classes_misc
     */                                                                          //IBM-shared_classes_misc
    private class SharedClassMetaDataCache {                                     //IBM-shared_classes_misc
        private final static int BLOCKSIZE = 10;                                 //IBM-shared_classes_misc
        private SharedClassMetaData[] store;                                     //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
        public SharedClassMetaDataCache(int initialSize) {                       //IBM-shared_classes_misc
            /* Allocate space for an initial amount of metadata entries */       //IBM-shared_classes_misc
            store = new SharedClassMetaData[initialSize];                        //IBM-shared_classes_misc
        }                                                                        //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
        /**                                                                      //IBM-shared_classes_misc
         * Retrieves the SharedClassMetaData stored at the given index, or null  //IBM-shared_classes_misc
         * if no SharedClassMetaData was previously stored at the given index    //IBM-shared_classes_misc
         * or the index is out of range.                                         //IBM-shared_classes_misc
         */                                                                      //IBM-shared_classes_misc
        public synchronized SharedClassMetaData getSharedClassMetaData(int index) {  //IBM-shared_classes_misc
            if (index < 0 || store.length < (index+1)) {                         //IBM-shared_classes_misc
                return null;                                                     //IBM-shared_classes_misc
            }                                                                    //IBM-shared_classes_misc
            return store[index];                                                 //IBM-shared_classes_misc
        }                                                                        //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
        /**                                                                      //IBM-shared_classes_misc
         * Stores the supplied SharedClassMetaData at the given index in the     //IBM-shared_classes_misc
         * store. The store will be grown to contain the index if necessary.     //IBM-shared_classes_misc
         */                                                                      //IBM-shared_classes_misc
        public synchronized void setSharedClassMetaData(int index,               //IBM-shared_classes_misc
                                                     SharedClassMetaData data) {  //IBM-shared_classes_misc
            ensureSize(index);                                                   //IBM-shared_classes_misc
            store[index] = data;                                                 //IBM-shared_classes_misc
        }                                                                        //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
        /* Ensure that the store can hold at least index number of entries */    //IBM-shared_classes_misc
        private synchronized void ensureSize(int index) {                        //IBM-shared_classes_misc
            if (store.length < (index+1)) {                                      //IBM-shared_classes_misc
                int newSize = (index+BLOCKSIZE);                                 //IBM-shared_classes_misc
                SharedClassMetaData[] newSCMDS = new SharedClassMetaData[newSize];  //IBM-shared_classes_misc
                System.arraycopy(store, 0, newSCMDS, 0, store.length);           //IBM-shared_classes_misc
                store = newSCMDS;                                                //IBM-shared_classes_misc
            }                                                                    //IBM-shared_classes_misc
        }                                                                        //IBM-shared_classes_misc
    }                                                                            //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
    /*                                                                           //IBM-shared_classes_misc
     * Return true if shared classes support is active, otherwise false.         //IBM-shared_classes_misc
     */                                                                          //IBM-shared_classes_misc
    private boolean usingSharedClasses() {                                       //IBM-shared_classes_misc
        return (sharedClassURLClasspathHelper != null);                          //IBM-shared_classes_misc
    }                                                                            //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
    /*                                                                           //IBM-shared_classes_misc
     * Initialize support for shared classes.                                    //IBM-shared_classes_misc
     */                                                                          //IBM-shared_classes_misc
    private void initializeSharedClassesSupport(URL[] initialClassPath) {        //IBM-shared_classes_misc
        /* get the Shared class helper and initialize the metadata store if we are sharing */  //IBM-shared_classes_misc
        SharedClassHelperFactory sharedClassHelperFactory = Shared.getSharedClassHelperFactory();  //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
        if (sharedClassHelperFactory != null) {                                  //IBM-shared_classes_misc
            try {                                                                //IBM-shared_classes_misc
                this.sharedClassURLClasspathHelper = sharedClassHelperFactory.getURLClasspathHelper(this, initialClassPath);  //IBM-shared_classes_misc
            } catch (HelperAlreadyDefinedException ex) { // thrown if we get 2 types of helper for the same classloader  //IBM-shared_classes_misc
                ex.printStackTrace();                                            //IBM-shared_classes_misc
            }                                                                    //IBM-shared_classes_misc
            /* Only need to create a meta data cache if using shared classes */  //IBM-shared_classes_misc
            if (usingSharedClasses()) {                                          //IBM-shared_classes_misc
                /* Create a metadata cache */                                    //IBM-shared_classes_misc
                this.sharedClassMetaDataCache = new SharedClassMetaDataCache(initialClassPath.length);  //IBM-shared_classes_misc
            }                                                                    //IBM-shared_classes_misc
        }                                                                        //IBM-shared_classes_misc
    }                                                                            //IBM-shared_classes_misc

    /**
     * Constructs a new URLClassLoader for the given URLs. The URLs will be
     * searched in the order specified for classes and resources after first
     * searching in the specified parent class loader. Any URL that ends with
     * a '/' is assumed to refer to a directory. Otherwise, the URL is assumed
     * to refer to a JAR file which will be downloaded and opened as needed.
     *
     * <p>If there is a security manager, this method first
     * calls the security manager's {@code checkCreateClassLoader} method
     * to ensure creation of a class loader is allowed.
     *
     * @param urls the URLs from which to load classes and resources
     * @param parent the parent class loader for delegation
     * @exception  SecurityException  if a security manager exists and its
     *             {@code checkCreateClassLoader} method doesn't allow
     *             creation of a class loader.
     * @exception  NullPointerException if {@code urls} is {@code null}.
     * @see SecurityManager#checkCreateClassLoader
     */
    public URLClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
        // this is to make the stack depth consistent with 1.1
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        initializeSharedClassesSupport(urls);                                    //IBM-shared_classes_misc
	ucp = new URLClassPath(urls, null, sharedClassURLClasspathHelper);       //IBM-shared_classes_misc
        this.acc = AccessController.getContext();
    }

    URLClassLoader(URL[] urls, ClassLoader parent,
                   AccessControlContext acc) {
        super(parent);
        // this is to make the stack depth consistent with 1.1
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        ucp = new URLClassPath(urls);
        this.acc = acc;
    }

    /**
     * Constructs a new URLClassLoader for the specified URLs using the
     * default delegation parent {@code ClassLoader}. The URLs will
     * be searched in the order specified for classes and resources after
     * first searching in the parent class loader. Any URL that ends with
     * a '/' is assumed to refer to a directory. Otherwise, the URL is
     * assumed to refer to a JAR file which will be downloaded and opened
     * as needed.
     *
     * <p>If there is a security manager, this method first
     * calls the security manager's {@code checkCreateClassLoader} method
     * to ensure creation of a class loader is allowed.
     *
     * @param urls the URLs from which to load classes and resources
     *
     * @exception  SecurityException  if a security manager exists and its
     *             {@code checkCreateClassLoader} method doesn't allow
     *             creation of a class loader.
     * @exception  NullPointerException if {@code urls} is {@code null}.
     * @see SecurityManager#checkCreateClassLoader
     */
    public URLClassLoader(URL[] urls) {
        super();
        // this is to make the stack depth consistent with 1.1
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        initializeSharedClassesSupport(urls);                                    //IBM-shared_classes_misc
	ucp = new URLClassPath(urls, null, sharedClassURLClasspathHelper);       //IBM-shared_classes_misc
        this.acc = AccessController.getContext();
    }

    URLClassLoader(URL[] urls, AccessControlContext acc) {
        super();
        // this is to make the stack depth consistent with 1.1
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        ucp = new URLClassPath(urls);
        this.acc = acc;
    }

    /**
     * Constructs a new URLClassLoader for the specified URLs, parent
     * class loader, and URLStreamHandlerFactory. The parent argument
     * will be used as the parent class loader for delegation. The
     * factory argument will be used as the stream handler factory to
     * obtain protocol handlers when creating new jar URLs.
     *
     * <p>If there is a security manager, this method first
     * calls the security manager's {@code checkCreateClassLoader} method
     * to ensure creation of a class loader is allowed.
     *
     * @param urls the URLs from which to load classes and resources
     * @param parent the parent class loader for delegation
     * @param factory the URLStreamHandlerFactory to use when creating URLs
     *
     * @exception  SecurityException  if a security manager exists and its
     *             {@code checkCreateClassLoader} method doesn't allow
     *             creation of a class loader.
     * @exception  NullPointerException if {@code urls} is {@code null}.
     * @see SecurityManager#checkCreateClassLoader
     */
    public URLClassLoader(URL[] urls, ClassLoader parent,
                          URLStreamHandlerFactory factory) {
        super(parent);
        // this is to make the stack depth consistent with 1.1
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        initializeSharedClassesSupport(urls);                                    //IBM-shared_classes_misc
	ucp = new URLClassPath(urls, factory, sharedClassURLClasspathHelper);    //IBM-shared_classes_misc
        acc = AccessController.getContext();
    }

    /* A map (used as a set) to keep track of closeable local resources
     * (either JarFiles or FileInputStreams). We don't care about
     * Http resources since they don't need to be closed.
     *
     * If the resource is coming from a jar file
     * we keep a (weak) reference to the JarFile object which can
     * be closed if URLClassLoader.close() called. Due to jar file
     * caching there will typically be only one JarFile object
     * per underlying jar file.
     *
     * For file resources, which is probably a less common situation
     * we have to keep a weak reference to each stream.
     */

    private WeakHashMap<Closeable,Void>
        closeables = new WeakHashMap<>();

    /**
     * Returns an input stream for reading the specified resource.
     * If this loader is closed, then any resources opened by this method
     * will be closed.
     *
     * <p> The search order is described in the documentation for {@link
     * #getResource(String)}.  </p>
     *
     * @param  name
     *         The resource name
     *
     * @return  An input stream for reading the resource, or {@code null}
     *          if the resource could not be found
     *
     * @since  1.7
     */
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        try {
            if (url == null) {
                return null;
            }
            URLConnection urlc = url.openConnection();
            InputStream is = urlc.getInputStream();
            if (urlc instanceof JarURLConnection) {
                JarURLConnection juc = (JarURLConnection)urlc;
                JarFile jar = juc.getJarFile();
                synchronized (closeables) {
                    if (!closeables.containsKey(jar)) {
                        closeables.put(jar, null);
                    }
                }
            } else if (urlc instanceof sun.net.www.protocol.file.FileURLConnection) {
                synchronized (closeables) {
                    closeables.put(is, null);
                }
            }
            return is;
        } catch (IOException e) {
            return null;
        }
    }

   /**
    * Closes this URLClassLoader, so that it can no longer be used to load
    * new classes or resources that are defined by this loader.
    * Classes and resources defined by any of this loader's parents in the
    * delegation hierarchy are still accessible. Also, any classes or resources
    * that are already loaded, are still accessible.
    * <p>
    * In the case of jar: and file: URLs, it also closes any files
    * that were opened by it. If another thread is loading a
    * class when the {@code close} method is invoked, then the result of
    * that load is undefined.
    * <p>
    * The method makes a best effort attempt to close all opened files,
    * by catching {@link IOException}s internally. Unchecked exceptions
    * and errors are not caught. Calling close on an already closed
    * loader has no effect.
    * <p>
    * @exception IOException if closing any file opened by this class loader
    * resulted in an IOException. Any such exceptions are caught internally.
    * If only one is caught, then it is re-thrown. If more than one exception
    * is caught, then the second and following exceptions are added
    * as suppressed exceptions of the first one caught, which is then re-thrown.
    *
    * @exception SecurityException if a security manager is set, and it denies
    *   {@link RuntimePermission}{@code ("closeClassLoader")}
    *
    * @since 1.7
    */
    public void close() throws IOException {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new RuntimePermission("closeClassLoader"));
        }
        List<IOException> errors = ucp.closeLoaders();

        // now close any remaining streams.

        synchronized (closeables) {
            Set<Closeable> keys = closeables.keySet();
            for (Closeable c : keys) {
                try {
                    c.close();
                } catch (IOException ioex) {
                    errors.add(ioex);
                }
            }
            closeables.clear();
        }

        if (errors.isEmpty()) {
            return;
        }

        IOException firstex = errors.remove(0);

        // Suppress any remaining exceptions

        for (IOException error: errors) {
            firstex.addSuppressed(error);
        }
        throw firstex;
    }

    /**
     * Appends the specified URL to the list of URLs to search for
     * classes and resources.
     * <p>
     * If the URL specified is {@code null} or is already in the
     * list of URLs, or if this loader is closed, then invoking this
     * method has no effect.
     *
     * @param url the URL to be added to the search path of URLs
     */
    protected void addURL(URL url) {
        ucp.addURL(url);
    }

    /**
     * Returns the search path of URLs for loading classes and resources.
     * This includes the original list of URLs specified to the constructor,
     * along with any URLs subsequently appended by the addURL() method.
     * @return the search path of URLs for loading classes and resources.
     */
    public URL[] getURLs() {
        return ucp.getURLs();
    }

    /* Stores a list of classes for which is show detailed class loading */      //IBM-shared_classes_misc
    private static Vector showClassLoadingFor = null;                            //IBM-shared_classes_misc
    /* Caches whether the vector showClassLoadingFor is empty */                 //IBM-shared_classes_misc
    private static boolean showLoadingMessages = false;                          //IBM-shared_classes_misc
    /* Property to use to setup the detailed classloading output */              //IBM-shared_classes_misc
    private final static String showClassLoadingProperty = "ibm.cl.verbose";     //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
    /*                                                                           //IBM-shared_classes_misc
     * Initializes the showClassLoadingFor vector. All expressions are precompiled  //IBM-shared_classes_misc
     */                                                                          //IBM-shared_classes_misc
    static                                                                       //IBM-shared_classes_misc
    {                                                                            //IBM-shared_classes_misc
        Vector showClassLoadingFor = new Vector();                               //IBM-shared_classes_misc
        String classes = System.getProperty(showClassLoadingProperty);           //IBM-shared_classes_misc
        /* If the property exists then process the supplied file expressions */  //IBM-shared_classes_misc
        if (classes != null) {                                                   //IBM-shared_classes_misc
            StringTokenizer classMatchers = new StringTokenizer(classes, ",");   //IBM-shared_classes_misc
            while (classMatchers.hasMoreTokens()) {                              //IBM-shared_classes_misc
                String classMatcher = classMatchers.nextToken();                 //IBM-shared_classes_misc
                /* Do the replacements to allow more readable expressions to be supplied */  //IBM-shared_classes_misc
                String classMatcherExp = classMatcher.replaceAll("\\.", "\\.");  //IBM-shared_classes_misc
                classMatcherExp = classMatcherExp.replaceAll("\\*", ".*");       //IBM-shared_classes_misc
                Pattern pattern;                                                 //IBM-shared_classes_misc
                /* Add the compiled pattern to the vector */                     //IBM-shared_classes_misc
                try {                                                            //IBM-shared_classes_misc
                    pattern = Pattern.compile(classMatcherExp);                  //IBM-shared_classes_misc
                    showClassLoadingFor.addElement(pattern);                     //IBM-shared_classes_misc
                } catch (PatternSyntaxException e) {                             //IBM-shared_classes_misc
                    /*                                                           //IBM-shared_classes_misc
                     * The user has supplied something which has is not          //IBM-shared_classes_misc
                     * a legal regular expression (or isn't now that it has been  //IBM-shared_classes_misc
                     * transformed!) Warn the user and ignore this expression    //IBM-shared_classes_misc
                     */                                                          //IBM-shared_classes_misc
                    System.err.println("Illegal class matching expression \"" + classMatcher +  //IBM-shared_classes_misc
                        "\" supplied by property " + showClassLoadingProperty);  //IBM-shared_classes_misc
                }                                                                //IBM-shared_classes_misc
            }                                                                    //IBM-shared_classes_misc
        }                                                                        //IBM-shared_classes_misc
        /*                                                                       //IBM-shared_classes_misc
         * Cache whether a check should be made to see whether to show loading messages for  //IBM-shared_classes_misc
         * a particular class                                                    //IBM-shared_classes_misc
         */                                                                      //IBM-shared_classes_misc
        if (!showClassLoadingFor.isEmpty()) {                                    //IBM-shared_classes_misc
            showLoadingMessages = true;                                          //IBM-shared_classes_misc
        }                                                                        //IBM-shared_classes_misc
        URLClassLoader.showClassLoadingFor = showClassLoadingFor;                //IBM-shared_classes_misc
    }                                                                            //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
    /*                                                                           //IBM-shared_classes_misc
     * Returns whether the class loading should be explicitly shown for a        //IBM-shared_classes_misc
     * particular class. This is determined by the system property ibm.cl.verbose  //IBM-shared_classes_misc
     * which contains a comma separated list of file expressions.                //IBM-shared_classes_misc
     * A file expression being a regular expression but with .* substituted for *  //IBM-shared_classes_misc
     * and \. substituted for . to allow a more readable form to be used         //IBM-shared_classes_misc
     * If no property exists or contains no expressions then showClassLoadingFor  //IBM-shared_classes_misc
     * will be an empty vector and this will be the only test each time this function  //IBM-shared_classes_misc
     * is called. Otherwise name will be matched against each expression in the vector  //IBM-shared_classes_misc
     * and if a match is found true is returned, otherwise false                 //IBM-shared_classes_misc
     */                                                                          //IBM-shared_classes_misc
    private boolean showClassLoading(String name)                                //IBM-shared_classes_misc
    {                                                                            //IBM-shared_classes_misc
        /* If there are supplied expressions try and match this class name against them */  //IBM-shared_classes_misc
        if (URLClassLoader.showLoadingMessages) {                                //IBM-shared_classes_misc
            Enumeration patterns = URLClassLoader.showClassLoadingFor.elements();  //IBM-shared_classes_misc
            while (patterns.hasMoreElements()) {                                 //IBM-shared_classes_misc
                Pattern pattern = (Pattern)patterns.nextElement();               //IBM-shared_classes_misc
                Matcher matcher = pattern.matcher(name);                         //IBM-shared_classes_misc
                if (matcher.matches()) {                                         //IBM-shared_classes_misc
                    return true;                                                 //IBM-shared_classes_misc
                }                                                                //IBM-shared_classes_misc
            }                                                                    //IBM-shared_classes_misc
        }                                                                        //IBM-shared_classes_misc
        /* Either no expressions or no matches */                                //IBM-shared_classes_misc
        return false;                                                            //IBM-shared_classes_misc
    }                                                                            //IBM-shared_classes_misc
                                                                                //IBM-shared_classes_misc
                                                                                //IBM-shared_classes_misc
    /**
     * Finds and loads the class with the specified name from the URL search
     * path. Any URLs referring to JAR files are loaded and opened as needed
     * until the class is found.
     *
     * @param name the name of the class
     * @return the resulting class
     * @exception ClassNotFoundException if the class could not be found,
     *            or if the loader is closed.
     * @exception NullPointerException if {@code name} is {@code null}.
     */
    protected Class<?> findClass(final String name)
        throws ClassNotFoundException
    {
        final Class<?> result;
        try {
            boolean scl = showClassLoading(name);                                //IBM-shared_classes_misc
            if (scl) {                                                            //IBM-shared_classes_misc
                ClassLoaderDiagnosticsHelper.attemptingToLoadClass(this, name);  //IBM-shared_classes_misc
            }                                                                     //IBM-shared_classes_misc
            /* Try to find the class from the shared cache using the class name.  If we found the class  //IBM-shared_classes_misc
             * and if we have its corresponding metadata (codesource and manifest entry) already cached,  //IBM-shared_classes_misc
             * then we define the class passing in these parameters.  If however, we do not have the  //IBM-shared_classes_misc
             * metadata cached, then we define the class as normal.  Also, if we do not find the class //IBM-shared_classes_misc
             * from the shared class cache, we define the class as normal.      //IBM-shared_classes_misc
             */                                                                 //IBM-shared_classes_misc
            if (usingSharedClasses()) {                                         //IBM-shared_classes_misc
                SharedClassIndexHolder sharedClassIndexHolder = new SharedClassIndexHolder(); /*ibm@94142*/ //IBM-shared_classes_misc
                byte[] sharedClazz = sharedClassURLClasspathHelper.findSharedClass(name, sharedClassIndexHolder); //IBM-shared_classes_misc
                                                                                //IBM-shared_classes_misc
                if (sharedClazz != null) {                                      //IBM-shared_classes_misc
                    int indexFoundData = sharedClassIndexHolder.index;          //IBM-shared_classes_misc
                    SharedClassMetaData metadata = sharedClassMetaDataCache.getSharedClassMetaData(indexFoundData);  //IBM-shared_classes_misc
                    if (metadata != null) {                                     //IBM-shared_classes_misc
                        try {                                                   //IBM-shared_classes_misc
                            Class clazz = defineClass(name,sharedClazz,         //IBM-shared_classes_misc
                                               metadata.getCodeSource(),        //IBM-shared_classes_misc
                                               metadata.getManifest());         //IBM-shared_classes_misc
                            if (scl) {                                           //IBM-shared_classes_misc
                                ClassLoaderDiagnosticsHelper.loadedClass(this, name);  //IBM-shared_classes_misc
                            }                                                   //IBM-shared_classes_misc
                            return clazz;                                       //IBM-shared_classes_misc
                        } catch (IOException e) {                               //IBM-shared_classes_misc
                            e.printStackTrace();                                //IBM-shared_classes_misc
                        }                                                      //IBM-shared_classes_misc
                   }                                                          //IBM-shared_classes_misc
               }                                                               //IBM-shared_classes_misc
           }                                                           //IBM-shared_classes_misc
            ClassFinder loader = new ClassFinder(name, this);    /*ibm@80916.1*/ //IBM-shared_classes_misc
            Class clazz = (Class)AccessController.doPrivileged(loader, acc);    //IBM-shared_classes_misc
            if (clazz == null) {                                     /*ibm@802*/ //IBM-shared_classes_misc
              if (scl) {                                                        //IBM-shared_classes_misc
                    ClassLoaderDiagnosticsHelper.failedToLoadClass(this, name);   //IBM-shared_classes_misc
                }                                                               //IBM-shared_classes_misc
                       throw new ClassNotFoundException(name);              /*ibm@802*/ //IBM-shared_classes_misc
            }                                                                    //IBM-shared_classes_misc
            if (scl) {                                                            //IBM-shared_classes_misc
                ClassLoaderDiagnosticsHelper.loadedClass(this, name);           //IBM-shared_classes_misc
            }                                                                   //IBM-shared_classes_misc
            return clazz;                                            /*ibm@802*/ //IBM-shared_classes_misc

        } catch (java.security.PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        }
    }

    /*
     * Retrieve the package using the specified package name.
     * If non-null, verify the package using the specified code
     * source and manifest.
     */
    private Package getAndVerifyPackage(String pkgname,
                                        Manifest man, URL url) {
        Package pkg = getPackage(pkgname);
        if (pkg != null) {
            // Package found, so check package sealing.
            if (pkg.isSealed()) {
                // Verify that code source URL is the same.
                if (!pkg.isSealed(url)) {
                    throw new SecurityException(
                        "sealing violation: package " + pkgname + " is sealed");
                }
            } else {
                // Make sure we are not attempting to seal the package
                // at this code source URL.
                if ((man != null) && isSealed(pkgname, man)) {
                    throw new SecurityException(
                        "sealing violation: can't seal package " + pkgname +
                        ": already loaded");
                }
            }
        }
        return pkg;
    }

    // Also called by VM to define Package for classes loaded from the CDS
    // archive
    private void definePackageInternal(String pkgname, Manifest man, URL url)
    {
        if (getAndVerifyPackage(pkgname, man, url) == null) {
            try {
                if (man != null) {
                    definePackage(pkgname, man, url);
                } else {
                    definePackage(pkgname, null, null, null, null, null, null, null);
                }
            } catch (IllegalArgumentException iae) {
                // parallel-capable class loaders: re-verify in case of a
                // race condition
                if (getAndVerifyPackage(pkgname, man, url) == null) {
                    // Should never happen
                    throw new AssertionError("Cannot find package " +
                                             pkgname);
                }
            }
        }
    }

    /*
     * Defines a Class using the class bytes obtained from the specified
     * Resource. The resulting Class must be resolved before it can be
     * used.
     */
    private Class<?> defineClass(String name, Resource res) throws IOException {
         Class clazz = null;                                                    //IBM-shared_classes_misc
         CodeSource cs = null;                                                  //IBM-shared_classes_misc
         Manifest man = null;                                                   //IBM-shared_classes_misc
         long t0 = System.nanoTime();
        int i = name.lastIndexOf('.');
        URL url = res.getCodeSourceURL();
        if (i != -1) {
            String pkgname = name.substring(0, i);
            // Check if package already loaded.
             man = res.getManifest();                                           //IBM-shared_classes_misc            
	definePackageInternal(pkgname, man, url);
        }
        // Now read the class bytes and define the class
        java.nio.ByteBuffer bb = res.getByteBuffer();
       if (bb != null)                                                          //IBM-T6_wrt_bringup
       {                                                                        //IBM-T6_wrt_bringup
			// Use (direct) ByteBuffer:                                          //IBM-T6_wrt_bringup
			CodeSigner[] signers = res.getCodeSigners();                         //IBM-T6_wrt_bringup
			                                                        //IBM-T6_wrt_bringup
			long oldMemory = -1;                                    //IBM-T6_wrt_bringup
			try                                                     //IBM-T6_wrt_bringup
			{                                                       //IBM-T6_wrt_bringup
				oldMemory = MemorySafetyService.enterSafeMemoryArea(); //IBM-T6_wrt_bringup
				cs = new CodeSource(url, signers);              //IBM-T6_wrt_bringup
			}                                                       //IBM-T6_wrt_bringup
			finally                                                 //IBM-T6_wrt_bringup
			{                                                       //IBM-T6_wrt_bringup
				MemorySafetyService.exitLastMemoryArea(oldMemory); //IBM-T6_wrt_bringup
			}                                                       //IBM-T6_wrt_bringup
			                                                        //IBM-T6_wrt_bringup
			clazz = defineClass(name, bb, cs);                                   //IBM-T6_wrt_bringup
		                                                                //IBM-T6_wrt_bringup
	   }                                                                    //IBM-T6_wrt_bringup
	   else                                                                 //IBM-T6_wrt_bringup
	   {                                                                    //IBM-T6_wrt_bringup
             byte[] b = res.getBytes();                                         //IBM-shared_classes_misc
		    // must read certificates AFTER reading bytes.                       //IBM-T6_wrt_bringup
			CodeSigner[] signers = res.getCodeSigners();                         //IBM-T6_wrt_bringup
			                                                        //IBM-T6_wrt_bringup
			long oldMemory = -1;                                    //IBM-T6_wrt_bringup
			try                                                     //IBM-T6_wrt_bringup
			{                                                       //IBM-T6_wrt_bringup
				oldMemory = MemorySafetyService.enterSafeMemoryArea(); //IBM-T6_wrt_bringup
				cs = new CodeSource(url, signers);              //IBM-T6_wrt_bringup
			}                                                       //IBM-T6_wrt_bringup
			finally                                                 //IBM-T6_wrt_bringup
			{                                                       //IBM-T6_wrt_bringup
				MemorySafetyService.exitLastMemoryArea(oldMemory); //IBM-T6_wrt_bringup
			}                                                       //IBM-T6_wrt_bringup
			                                                        //IBM-T6_wrt_bringup
			clazz = defineClass(name, b, 0, b.length, cs);                       //IBM-T6_wrt_bringup
		}                                                                        //IBM-T6_wrt_bringup
                                                                                //IBM-shared_classes_misc
        /*                                                                      //IBM-shared_classes_misc
         * Since we have already stored the class path index (of where this resource came from), we can retrieve //IBM-shared_classes_misc
         * it here.  The storing is done in getResource() in URLClassPath.java.  The index is the specified //IBM-shared_classes_misc
         * position in the URL search path (see getLoader()).  The storeSharedClass() call below, stores the //IBM-shared_classes_misc
         * class in the shared class cache for future use.                      //IBM-shared_classes_misc
         */                                                                     //IBM-shared_classes_misc
        if (usingSharedClasses()) {                                             //IBM-shared_classes_misc
                                                                                //IBM-shared_classes_misc
            /* Determine the index into the search path for this class */       //IBM-shared_classes_misc
            int index = res.getClasspathLoadIndex();                            //IBM-shared_classes_misc
            /* Check to see if we have already cached metadata for this index */ //IBM-shared_classes_misc
            SharedClassMetaData metadata = sharedClassMetaDataCache.getSharedClassMetaData(index); //IBM-shared_classes_misc
            /* If we have not already cached the metadata for this index... */  //IBM-shared_classes_misc
            if (metadata == null) {                                             //IBM-shared_classes_misc
                /* ... create a new metadata entry */                           //IBM-shared_classes_misc
                metadata = new SharedClassMetaData(cs, man);                    //IBM-T6_wrt_bringup
                long oldMemory = -1;                                            //IBM-T6_wrt_bringup
				try                                             //IBM-T6_wrt_bringup
				{                                               //IBM-T6_wrt_bringup
					oldMemory = MemorySafetyService.enterSafeMemoryArea(); //IBM-T6_wrt_bringup
					metadata = new SharedClassMetaData(cs, man); //IBM-T6_wrt_bringup
				}                                               //IBM-T6_wrt_bringup
				finally                                         //IBM-T6_wrt_bringup
				{                                               //IBM-T6_wrt_bringup
					MemorySafetyService.exitLastMemoryArea(oldMemory); //IBM-T6_wrt_bringup
				}                                               //IBM-T6_wrt_bringup
                                                                                //IBM-T6_wrt_bringup
                /* Cache the metadata for this index for future use */          //IBM-shared_classes_misc
                sharedClassMetaDataCache.setSharedClassMetaData(index, metadata); //IBM-shared_classes_misc
                                                                                //IBM-shared_classes_misc
            }                                                                   //IBM-shared_classes_misc
            boolean storeSuccessful = false;                                    //IBM-shared_classes_misc
            try {                                                               //IBM-shared_classes_misc
               /* Store class in shared class cache for future use */           //IBM-shared_classes_misc
                storeSuccessful =                                               //IBM-shared_classes_misc
                  sharedClassURLClasspathHelper.storeSharedClass(clazz, index); //IBM-shared_classes_misc
            } catch (Exception e) {                                             //IBM-shared_classes_misc
                e.printStackTrace();                                            //IBM-shared_classes_misc
            }                                                                   //IBM-shared_classes_misc
        }
                                                                                //IBM-shared_classes_misc
        return clazz;                                                           //IBM-shared_classes_misc
    }
    /*                                                                          //IBM-shared_classes_misc
     * Defines a class using the class bytes, codesource and manifest           //IBM-shared_classes_misc
     * obtained from the specified shared class cache. The resulting            //IBM-shared_classes_misc
     * class must be resolved before it can be used.  This method is            //IBM-shared_classes_misc
     * used only in a Shared classes context.                                   //IBM-shared_classes_misc
     */                                                                         //IBM-shared_classes_misc
    private Class defineClass(String name, byte[] sharedClazz, CodeSource codesource, Manifest man) throws IOException { //IBM-shared_classes_misc
	int i = name.lastIndexOf('.');                                          //IBM-shared_classes_misc
	URL url = codesource.getLocation();                                     //IBM-shared_classes_misc
	if (i != -1) {                                                          //IBM-shared_classes_misc
	    String pkgname = name.substring(0, i);                              //IBM-shared_classes_misc
	    // Check if package already loaded.                                 //IBM-shared_classes_misc
	    Package pkg = getPackage(pkgname);                                  //IBM-shared_classes_misc
            if (pkg != null) {                                                  //IBM-shared_classes_misc
		// Package found, so check package sealing.                     //IBM-shared_classes_misc
		if (pkg.isSealed()) {                                           //IBM-shared_classes_misc
		    // Verify that code source URL is the same.                 //IBM-shared_classes_misc
		    if (!pkg.isSealed(url)) {                                   //IBM-shared_classes_misc
			throw new SecurityException(                            //IBM-shared_classes_misc
			    "sealing violation: package " + pkgname + " is sealed"); //IBM-shared_classes_misc
		    }                                                           //IBM-shared_classes_misc
		} else {                                                        //IBM-shared_classes_misc
		    // Make sure we are not attempting to seal the package      //IBM-shared_classes_misc
		    // at this code source URL.                                 //IBM-shared_classes_misc
		    if ((man != null) && isSealed(pkgname, man)) {              //IBM-shared_classes_misc
			throw new SecurityException(                            //IBM-shared_classes_misc
			    "sealing violation: can't seal package " + pkgname +  //IBM-shared_classes_misc
			    ": already loaded");                                //IBM-shared_classes_misc
		    }                                                           //IBM-shared_classes_misc
		}                                                               //IBM-shared_classes_misc
	    } else {                                                            //IBM-shared_classes_misc
		if (man != null) {                                              //IBM-shared_classes_misc
		    definePackage(pkgname, man, url);                           //IBM-shared_classes_misc
		} else {                                                        //IBM-shared_classes_misc
                    definePackage(pkgname, null, null, null, null, null, null, null); //IBM-shared_classes_misc
                }                                                               //IBM-shared_classes_misc
	    }                                                                   //IBM-shared_classes_misc
 	}                                                                      //IBM-shared_classes_misc
	/*                                                                      //IBM-shared_classes_misc
         * Now read the class bytes and define the class.  We don't need to call  //IBM-shared_classes_misc
         * storeSharedClass(), since its already in our shared class cache.     //IBM-shared_classes_misc
         */                                                                     //IBM-shared_classes_misc
        return defineClass(name, sharedClazz, 0, sharedClazz.length, codesource); //IBM-shared_classes_misc
     }                                                                          //IBM-shared_classes_misc
                                                                                //IBM-shared_classes_misc

    /**
     * Defines a new package by name in this ClassLoader. The attributes
     * contained in the specified Manifest will be used to obtain package
     * version and sealing information. For sealed packages, the additional
     * URL specifies the code source URL from which the package was loaded.
     *
     * @param name  the package name
     * @param man   the Manifest containing package version and sealing
     *              information
     * @param url   the code source url for the package, or null if none
     * @exception   IllegalArgumentException if the package name duplicates
     *              an existing package either in this class loader or one
     *              of its ancestors
     * @return the newly defined Package object
     */
    protected Package definePackage(String name, Manifest man, URL url)
        throws IllegalArgumentException
    {
        String path = name.replace('.', '/').concat("/");
        String specTitle = null, specVersion = null, specVendor = null;
        String implTitle = null, implVersion = null, implVendor = null;
        String sealed = null;
        URL sealBase = null;

        Attributes attr = man.getAttributes(path);
        if (attr != null) {
            specTitle   = attr.getValue(Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            specVendor  = attr.getValue(Name.SPECIFICATION_VENDOR);
            implTitle   = attr.getValue(Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            implVendor  = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            sealed      = attr.getValue(Name.SEALED);
        }
        attr = man.getMainAttributes();
        if (attr != null) {
            if (specTitle == null) {
                specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            }
            if (specVersion == null) {
                specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            }
            if (specVendor == null) {
                specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            }
            if (implTitle == null) {
                implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            }
            if (implVersion == null) {
                implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            }
            if (implVendor == null) {
                implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            }
            if (sealed == null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        if ("true".equalsIgnoreCase(sealed)) {
            sealBase = url;
        }
        return definePackage(name, specTitle, specVersion, specVendor,
                             implTitle, implVersion, implVendor, sealBase);
    }

    /*
     * Returns true if the specified package name is sealed according to the
     * given manifest.
     */
    private boolean isSealed(String name, Manifest man) {
        String path = name.replace('.', '/').concat("/");
        Attributes attr = man.getAttributes(path);
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Name.SEALED);
        }
        if (sealed == null) {
            if ((attr = man.getMainAttributes()) != null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }

    /**
     * Finds the resource with the specified name on the URL search path.
     *
     * @param name the name of the resource
     * @return a {@code URL} for the resource, or {@code null}
     * if the resource could not be found, or if the loader is closed.
     */
    public URL findResource(final String name) {
        /*
         * The same restriction to finding classes applies to resources
         */
        URL url = AccessController.doPrivileged(
            new PrivilegedAction<URL>() {
                public URL run() {
                    return ucp.findResource(name, true);
                }
            }, acc);

        return url != null ? ucp.checkURL(url) : null;
    }

    /**
     * Returns an Enumeration of URLs representing all of the resources
     * on the URL search path having the specified name.
     *
     * @param name the resource name
     * @exception IOException if an I/O exception occurs
     * @return an {@code Enumeration} of {@code URL}s
     *         If the loader is closed, the Enumeration will be empty.
     */
    public Enumeration<URL> findResources(final String name)
        throws IOException
    {
        final Enumeration<URL> e = ucp.findResources(name, true);

        return new Enumeration<URL>() {
            private URL url = null;

            private boolean next() {
                if (url != null) {
                    return true;
                }
                do {
                    URL u = AccessController.doPrivileged(
                        new PrivilegedAction<URL>() {
                            public URL run() {
                                if (!e.hasMoreElements())
                                    return null;
                                return e.nextElement();
                            }
                        }, acc);
                    if (u == null)
                        break;
                    url = ucp.checkURL(u);
                } while (url == null);
                return url != null;
            }

            public URL nextElement() {
                if (!next()) {
                    throw new NoSuchElementException();
                }
                URL u = url;
                url = null;
                return u;
            }

            public boolean hasMoreElements() {
                return next();
            }
        };
    }

    /**
     * Returns the permissions for the given codesource object.
     * The implementation of this method first calls super.getPermissions
     * and then adds permissions based on the URL of the codesource.
     * <p>
     * If the protocol of this URL is "jar", then the permission granted
     * is based on the permission that is required by the URL of the Jar
     * file.
     * <p>
     * If the protocol is "file" and there is an authority component, then
     * permission to connect to and accept connections from that authority
     * may be granted. If the protocol is "file"
     * and the path specifies a file, then permission to read that
     * file is granted. If protocol is "file" and the path is
     * a directory, permission is granted to read all files
     * and (recursively) all files and subdirectories contained in
     * that directory.
     * <p>
     * If the protocol is not "file", then permission
     * to connect to and accept connections from the URL's host is granted.
     * @param codesource the codesource
     * @exception NullPointerException if {@code codesource} is {@code null}.
     * @return the permissions granted to the codesource
     */
    protected PermissionCollection getPermissions(CodeSource codesource)
    {
        PermissionCollection perms = super.getPermissions(codesource);

        URL url = codesource.getLocation();

        Permission p;
        URLConnection urlConnection;

        try {
            urlConnection = url.openConnection();
            p = urlConnection.getPermission();
        } catch (java.io.IOException ioe) {
            p = null;
            urlConnection = null;
        }

        if (p instanceof FilePermission) {
            // if the permission has a separator char on the end,
            // it means the codebase is a directory, and we need
            // to add an additional permission to read recursively
            String path = p.getName();
            if (path.endsWith(File.separator)) {
                path += "-";
                //p = new FilePermission(path, SecurityConstants.FILE_READ_ACTION); //IBM-T6_wrt_bringup
                                                                                //IBM-T6_wrt_bringup
                long oldMemory = -1;                                            //IBM-T6_wrt_bringup
				try                                             //IBM-T6_wrt_bringup
				{                                               //IBM-T6_wrt_bringup
					oldMemory = MemorySafetyService.enterSafeMemoryArea(); //IBM-T6_wrt_bringup
					p = new FilePermission(path, SecurityConstants.FILE_READ_ACTION); //IBM-T6_wrt_bringup
				}                                               //IBM-T6_wrt_bringup
				finally                                         //IBM-T6_wrt_bringup
				{                                               //IBM-T6_wrt_bringup
					MemorySafetyService.exitLastMemoryArea(oldMemory); //IBM-T6_wrt_bringup
				}                                               //IBM-T6_wrt_bringup
		    }                                                           //IBM-T6_wrt_bringup
        } else if ((p == null) && (url.getProtocol().equals("file"))) {
            String path = url.getFile().replace('/', File.separatorChar);
            path = ParseUtil.decode(path);
            if (path.endsWith(File.separator))
                path += "-";
                                                                                //IBM-T6_wrt_bringup
            //p =  new FilePermission(path, SecurityConstants.FILE_READ_ACTION); //IBM-T6_wrt_bringup
            long oldMemory = -1;                                                //IBM-T6_wrt_bringup
			try                                                     //IBM-T6_wrt_bringup
			{                                                       //IBM-T6_wrt_bringup
				oldMemory = MemorySafetyService.enterSafeMemoryArea(); //IBM-T6_wrt_bringup
				p = new FilePermission(path, SecurityConstants.FILE_READ_ACTION); //IBM-T6_wrt_bringup
			}                                                       //IBM-T6_wrt_bringup
			finally                                                 //IBM-T6_wrt_bringup
			{                                                       //IBM-T6_wrt_bringup
				MemorySafetyService.exitLastMemoryArea(oldMemory); //IBM-T6_wrt_bringup
			}                                                       //IBM-T6_wrt_bringup
			                                                        //IBM-T6_wrt_bringup
        } else {
            /**
             * Not loading from a 'file:' URL so we want to give the class
             * permission to connect to and accept from the remote host
             * after we've made sure the host is the correct one and is valid.
             */
            URL locUrl = url;
            if (urlConnection instanceof JarURLConnection) {
                locUrl = ((JarURLConnection)urlConnection).getJarFileURL();
            }
            String host = locUrl.getHost();
            if (host != null && (host.length() > 0))
            {                                                                   //IBM-T6_wrt_bringup
                long oldMemory = -1;                                            //IBM-T6_wrt_bringup
				try                                             //IBM-T6_wrt_bringup
				{                                               //IBM-T6_wrt_bringup
					oldMemory = MemorySafetyService.enterObjectsMemoryArea(this); //IBM-T6_wrt_bringup
					p = new SocketPermission(host, SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION); //IBM-T6_wrt_bringup
				}                                               //IBM-T6_wrt_bringup
				finally                                         //IBM-T6_wrt_bringup
				{                                               //IBM-T6_wrt_bringup
					MemorySafetyService.exitLastMemoryArea(oldMemory); //IBM-T6_wrt_bringup
				}                                               //IBM-T6_wrt_bringup
			}	                                               //IBM-T6_wrt_bringup
        }

        // make sure the person that created this class loader
        // would have this permission

        if (p != null) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                final Permission fp = p;
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() throws SecurityException {
                        sm.checkPermission(fp);
                        return null;
                    }
                }, acc);
            }
            perms.add(p);
        }
        return perms;
    }

    /**
     * Creates a new instance of URLClassLoader for the specified
     * URLs and parent class loader. If a security manager is
     * installed, the {@code loadClass} method of the URLClassLoader
     * returned by this method will invoke the
     * {@code SecurityManager.checkPackageAccess} method before
     * loading the class.
     *
     * @param urls the URLs to search for classes and resources
     * @param parent the parent class loader for delegation
     * @exception  NullPointerException if {@code urls} is {@code null}.
     * @return the resulting class loader
     */
    public static URLClassLoader newInstance(final URL[] urls,
                                             final ClassLoader parent) {
        // Save the caller's context
        final AccessControlContext acc = AccessController.getContext();
        // Need a privileged block to create the class loader
        URLClassLoader ucl = AccessController.doPrivileged(
            new PrivilegedAction<URLClassLoader>() {
                public URLClassLoader run() {
                    return new FactoryURLClassLoader(urls, parent, acc);
                }
            });
        return ucl;
    }

    /**
     * Creates a new instance of URLClassLoader for the specified
     * URLs and default parent class loader. If a security manager is
     * installed, the {@code loadClass} method of the URLClassLoader
     * returned by this method will invoke the
     * {@code SecurityManager.checkPackageAccess} before
     * loading the class.
     *
     * @param urls the URLs to search for classes and resources
     * @exception  NullPointerException if {@code urls} is {@code null}.
     * @return the resulting class loader
     */
    public static URLClassLoader newInstance(final URL[] urls) {
        // Save the caller's context
        final AccessControlContext acc = AccessController.getContext();
        // Need a privileged block to create the class loader
        URLClassLoader ucl = AccessController.doPrivileged(
            new PrivilegedAction<URLClassLoader>() {
                public URLClassLoader run() {
                    return new FactoryURLClassLoader(urls, acc);
                }
            });
        return ucl;
    }

    static {
        sun.misc.SharedSecrets.setJavaNetAccess (
            new sun.misc.JavaNetAccess() {
                public URLClassPath getURLClassPath (URLClassLoader u) {
                    return u.ucp;
                }

                public String getOriginalHostName(InetAddress ia) {
                    return ia.holder.getOriginalHostName();
                }
            }
        );
        ClassLoader.registerAsParallelCapable();
    }
                                                                                //IBM-shared_classes_misc
final class ClassFinder implements PrivilegedExceptionAction                    //IBM-shared_classes_misc
  {                                                                              //IBM-shared_classes_misc
     private String name;                                                        //IBM-shared_classes_misc
     private ClassLoader classloader;                                            //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
     public ClassFinder(String name, ClassLoader loader) {                       //IBM-shared_classes_misc
        this.name = name;                                                        //IBM-shared_classes_misc
        this.classloader = loader;                                               //IBM-shared_classes_misc
     }                                                                           //IBM-shared_classes_misc
                                                                                 //IBM-shared_classes_misc
     public Object run() throws ClassNotFoundException {                         //IBM-shared_classes_misc
	String path = name.replace('.', '/').concat(".class");                   //IBM-shared_classes_misc
        try {                                                                    //IBM-shared_classes_misc
            Resource res = ucp.getResource(path, false, classloader, showClassLoading(name)); //IBM-shared_classes_misc
            if (res != null)                                                     //IBM-shared_classes_misc
                return defineClass(name, res);                                   //IBM-shared_classes_misc
        } catch (IOException e) {                                                //IBM-shared_classes_misc
                throw new ClassNotFoundException(name, e);                       //IBM-shared_classes_misc
        }                                                                        //IBM-shared_classes_misc
        return null;                                                             //IBM-shared_classes_misc
     }                                                                           //IBM-shared_classes_misc
  }                                                                              //IBM-shared_classes_misc
}

                                                                                //IBM-shared_classes_misc
                                                                                //IBM-shared_classes_misc
final class FactoryURLClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    FactoryURLClassLoader(URL[] urls, ClassLoader parent,
                          AccessControlContext acc) {
        super(urls, parent, acc);
    }

    FactoryURLClassLoader(URL[] urls, AccessControlContext acc) {
        super(urls, acc);
    }

    public final Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        // First check if we have permission to access the package. This
        // should go away once we've added support for exported packages.
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            int i = name.lastIndexOf('.');
            if (i != -1) {
                sm.checkPackageAccess(name.substring(0, i));
            }
        }
        return super.loadClass(name, resolve);
    }
}
//IBM-shared_classes_misc
//IBM-T6_wrt_bringup
