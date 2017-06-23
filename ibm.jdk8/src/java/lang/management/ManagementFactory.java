/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2005, 2016  All Rights Reserved.
 */

package java.lang.management;

import java.io.IOException;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerPermission;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;

import com.ibm.lang.management.ManagementUtils;
import com.ibm.lang.management.OpenTypeMappingIHandler;
import com.ibm.lang.management.PlatformMbeanListProvider;

/**
 * TODO Type description
 *
 */
public class ManagementFactory {

    /**
     * Comment for <code>platformServer</code>
     */
    private static MBeanServer platformServer;

    /**
     * The unique <code>ObjectName</code> string identifier for the virtual
     * machine's singleton {@link ClassLoadingMXBean}
     */
    public static final String CLASS_LOADING_MXBEAN_NAME = "java.lang:type=ClassLoading"; //$NON-NLS-1$

    /**
     * The unique <code>ObjectName</code> string identifier for the virtual
     * machine's singleton {@link CompilationMXBean}
     */
    public static final String COMPILATION_MXBEAN_NAME = "java.lang:type=Compilation"; //$NON-NLS-1$

    /**
     * The prefix for all <code>ObjectName</code> strings which represent a
     * {@link GarbageCollectorMXBean}. The unique <code>ObjectName</code> for
     * a <code>GarbageCollectorMXBean</code> can be formed by adding
     * &quot;,name= <i>collector name </i>&quot; to this constant.
     */
    public static final String GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE = "java.lang:type=GarbageCollector"; //$NON-NLS-1$

    /**
     * The prefix for all <code>ObjectName</code> strings which represent a
     * {@link GarbageCollectorMXBean}. The unique <code>ObjectName</code> for
     * a <code>GarbageCollectorMXBean</code> can be formed by adding
     * &quot;,name= <i>collector name </i>&quot; to this constant.
     */
    //CMVC181275 This field should not be public static final according to Java7 spec b145.
    private static final String BUFFERPOOL_MXBEAN_DOMAIN_TYPE = "java.nio:type=BufferPool"; //$NON-NLS-1$

    //VM team adds a new bean
//    private static final String  HYPERFISOR_MXBEAN_NAME = "com.ibm.virtualization.management:type=Hypervisor";

    /**
     * The prefix for all <code>ObjectName</code> strings which represent a
     * {@link MemoryManagerMXBean}. The unique <code>ObjectName</code> for a
     * <code>MemoryManagerMXBean</code> can be formed by adding &quot;,name=
     * <i>manager name </i>&quot; to this constant.
     */
    public static final String MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE = "java.lang:type=MemoryManager"; //$NON-NLS-1$

    /**
     * The unique <code>ObjectName</code> string identifier for the virtual
     * machine's singleton {@link MemoryMXBean}
     */
    public static final String MEMORY_MXBEAN_NAME = "java.lang:type=Memory"; //$NON-NLS-1$

    /**
     * The prefix for all <code>ObjectName</code> strings which represent a
     * {@link MemoryPoolMXBean}. The unique <code>ObjectName</code> for a
     * <code>MemoryPoolMXBean</code> can be formed by adding &quot;,name=
     * <i>memory pool name </i>&quot; to this constant.
     */
    public static final String MEMORY_POOL_MXBEAN_DOMAIN_TYPE = "java.lang:type=MemoryPool"; //$NON-NLS-1$

    /**
     * The unique <code>ObjectName</code> string identifier for the virtual
     * machine's singleton {@link OperatingSystemMXBean}
     */
    public static final String OPERATING_SYSTEM_MXBEAN_NAME = "java.lang:type=OperatingSystem"; //$NON-NLS-1$

    /**
     * The unique <code>ObjectName</code> string identifier for the virtual
     * machine's singleton {@link RuntimeMXBean}
     */
    public static final String RUNTIME_MXBEAN_NAME = "java.lang:type=Runtime"; //$NON-NLS-1$

    /**
     * The unique <code>ObjectName</code> string identifier for the virtual
     * machine's singleton {@link ThreadMXBean}
     */
    public static final String THREAD_MXBEAN_NAME = "java.lang:type=Threading"; //$NON-NLS-1$

    private static Map<String, String[]> interfaceNameLookupTable;

    private static Set<String> multiInstanceBeanNames;

    static {
        interfaceNameLookupTable = new HashMap<String, String[]>();
        // Public API types
        interfaceNameLookupTable.put("java.lang.management.ClassLoadingMXBean", //$NON-NLS-1$
                new String[] { CLASS_LOADING_MXBEAN_NAME });
        interfaceNameLookupTable.put("java.lang.management.MemoryMXBean", //$NON-NLS-1$
                new String[] { MEMORY_MXBEAN_NAME });
        interfaceNameLookupTable.put("java.lang.management.ThreadMXBean", //$NON-NLS-1$
                new String[] { THREAD_MXBEAN_NAME });
        interfaceNameLookupTable.put("java.lang.management.RuntimeMXBean", //$NON-NLS-1$
                new String[] { RUNTIME_MXBEAN_NAME });
        interfaceNameLookupTable.put("com.ibm.lang.management.RuntimeMXBean", //$NON-NLS-1$
                new String[] { RUNTIME_MXBEAN_NAME });
        interfaceNameLookupTable.put(
                "java.lang.management.OperatingSystemMXBean", //$NON-NLS-1$
                new String[] { OPERATING_SYSTEM_MXBEAN_NAME });
        interfaceNameLookupTable.put("java.lang.management.CompilationMXBean", //$NON-NLS-1$
                new String[] { COMPILATION_MXBEAN_NAME });
        interfaceNameLookupTable.put(
                "java.lang.management.GarbageCollectorMXBean", //$NON-NLS-1$
                new String[] { GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE });
        interfaceNameLookupTable.put(
                "java.lang.management.MemoryManagerMXBean", //$NON-NLS-1$
                // above interface is implemented by MemoryManagerMXBeans and GarbageCollectorMXBeans
                new String[] { MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE, GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE });
        interfaceNameLookupTable.put("java.lang.management.MemoryPoolMXBean", //$NON-NLS-1$
                new String[] { MEMORY_POOL_MXBEAN_DOMAIN_TYPE });

        // Logging MXBean type
        interfaceNameLookupTable.put("java.lang.management.PlatformLoggingMXBean", //$NON-NLS-1$
                new String[] { LogManager.LOGGING_MXBEAN_NAME });
        interfaceNameLookupTable.put("java.util.logging.LoggingMXBean", //$NON-NLS-1$
                new String[] { LogManager.LOGGING_MXBEAN_NAME });

        // Proprietary types
        interfaceNameLookupTable.put(
                "com.ibm.lang.management.GarbageCollectorMXBean", //$NON-NLS-1$
                new String[] { GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE });
        interfaceNameLookupTable.put(
                "com.ibm.lang.management.MemoryMXBean", //$NON-NLS-1$
                new String[] { MEMORY_MXBEAN_NAME });
        interfaceNameLookupTable.put(
                "com.ibm.lang.management.MemoryPoolMXBean", //$NON-NLS-1$
                new String[] { MEMORY_POOL_MXBEAN_DOMAIN_TYPE });
        interfaceNameLookupTable.put(
                "com.ibm.lang.management.OperatingSystemMXBean", //$NON-NLS-1$
                new String[] { OPERATING_SYSTEM_MXBEAN_NAME });

        // since 1.7 buffer pool type
        interfaceNameLookupTable.put("java.lang.management.BufferPoolMXBean", //$NON-NLS-1$
                new String[] { BUFFERPOOL_MXBEAN_DOMAIN_TYPE });

		if (ManagementUtils.isRunningOnUnix()) {
	        interfaceNameLookupTable.put(
	                "com.ibm.lang.management.UnixOperatingSystemMXBean", //$NON-NLS-1$
	                new String[] { OPERATING_SYSTEM_MXBEAN_NAME });
		}

        // Below is for the new added beans in vm side
        List<Class> interfacesFromVMUtils = null;
        //Get a list of all available MXBeans objects using VmManagementUtils
        try{
            //get com.ibm.lang.management.VmManagementUtils from vm.jar
            Class object  =  Class.forName("com.ibm.lang.management.VmManagementUtils"); //$NON-NLS-1$
            PlatformMbeanListProvider pmp = (PlatformMbeanListProvider)object.newInstance();
            interfacesFromVMUtils = pmp.getAllAvailableMBeanInterfaces();
        }catch (ClassNotFoundException e) {
        } catch (IllegalAccessException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
        } catch (InstantiationException e) {
        	if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
        }

        List<PlatformManagedObject> beansFromVMUtils = null;
    	//Get a list of all available MXBeans objects using VmManagementUtils
    	try{
    		//get com.ibm.lang.management.VmManagementUtils from vm.jar
            Class object  =  Class.forName("com.ibm.lang.management.VmManagementUtils"); //$NON-NLS-1$
            PlatformMbeanListProvider pmp = (PlatformMbeanListProvider)object.newInstance();
            beansFromVMUtils = pmp.getAllAvailableMBeans();
    	}catch (ClassNotFoundException e) {
        	//if com.ibm.lang.management.VmManagementUtils is not found, no registration is done
    	} catch (IllegalAccessException e) {
    		if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
		} catch (InstantiationException e) {
			if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
		}

        String interfaceName = null;
        String objectName = null;
        if(interfacesFromVMUtils != null){
            for(int i = 0; i < interfacesFromVMUtils.size(); i++){
            	interfaceName = interfacesFromVMUtils.get(i).getName();
            	objectName = beansFromVMUtils.get(i).getObjectName().toString();
            	interfaceNameLookupTable.put(interfaceName, new String[] {objectName});
            }
        }
//        interfaceNameLookupTable.put("com.ibm.virtualization.management.HypervisorMXBean",
//                new String[] { HYPERFISOR_MXBEAN_NAME });

        multiInstanceBeanNames = new HashSet<String>();
        multiInstanceBeanNames.add("java.lang.management.GarbageCollectorMXBean"); //$NON-NLS-1$
        multiInstanceBeanNames.add("com.ibm.lang.management.GarbageCollectorMXBean"); //$NON-NLS-1$
        multiInstanceBeanNames.add("java.lang.management.MemoryManagerMXBean"); //$NON-NLS-1$
        multiInstanceBeanNames.add("java.lang.management.MemoryPoolMXBean"); //$NON-NLS-1$
        multiInstanceBeanNames.add("com.ibm.lang.management.MemoryPoolMXBean"); //$NON-NLS-1$
    }

    /**
     * Private constructor ensures that this class cannot be instantiated by
     * users.
     */
    private ManagementFactory() {
        // NO OP
    }

    /**
     * Returns the singleton <code>MXBean</code> for the virtual machine's
     * class loading system.
     *
     * @return the virtual machine's {@link ClassLoadingMXBean}
     */
    public static ClassLoadingMXBean getClassLoadingMXBean() {
        return ManagementUtils.getClassLoadingBean();
    }

    /**
     * Returns the singleton <code>MXBean</code> for the virtual machine's
     * compilation system <i>if and only if the virtual machine has a
     * compilation system enabled </i>. If no compilation exists for this
     * virtual machine, a <code>null</code> is returned.
     *
     * @return the virtual machine's {@link CompilationMXBean}or
     *         <code>null</code> if there is no compilation system for this
     *         virtual machine.
     */
    public static CompilationMXBean getCompilationMXBean() {
        return ManagementUtils.getCompilationBean();
    }

    /**
     * Returns a list of all of the instances of {@link GarbageCollectorMXBean}
     * in this virtual machine. Owing to the dynamic nature of this kind of
     * <code>MXBean</code>, it is possible that instances may be created or
     * destroyed between the invocation and return of this method.
     *
     * @return a list of all known <code>GarbageCollectorMXBean</code> s in
     *         this virtual machine.
     */
    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return ManagementUtils.getGarbageCollectorMXBeans();
    }

    /**
     * Returns a list of all of the instances of {@link MemoryManagerMXBean}in
     * this virtual machine. Owing to the dynamic nature of this kind of
     * <code>MXBean</code>, it is possible that instances may be created or
     * destroyed between the invocation and return of this method.
     * <p>
     * Note that the list of <code>MemoryManagerMXBean</code> instances will
     * include instances of <code>MemoryManagerMXBean</code> sub-types such as
     * <code>GarbageCollectorMXBean</code>.
     * </p>
     *
     * @return a list of all known <code>MemoryManagerMXBean</code> s in this
     *         virtual machine.
     */
    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return ManagementUtils.getMemoryManagerMXBeans();
    }

    /**
     * Returns the singleton <code>MXBean</code> for the virtual machine's
     * memory system.
     *
     * @return the virtual machine's {@link MemoryMXBean}
     */
    public static MemoryMXBean getMemoryMXBean() {
        return ManagementUtils.getMemoryBean();
    }

    /**
     * Returns a list of all of the instances of {@link MemoryPoolMXBean}in
     * this virtual machine. Owing to the dynamic nature of this kind of
     * <code>MXBean</code>, it is possible that instances may be created or
     * destroyed between the invocation and return of this method.
     *
     * @return a list of all known <code>MemoryPoolMXBean</code> s in this
     *         virtual machine.
     */
    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return ManagementUtils.getMemoryPoolMXBeans();
    }

    /**
     * Returns the singleton <code>MXBean</code> for the operating system
     * which the virtual machine runs on.
     *
     * @return the virtual machine's {@link OperatingSystemMXBean}
     */
    public static OperatingSystemMXBean getOperatingSystemMXBean() {
        return (OperatingSystemMXBean)ManagementUtils.getOperatingSystemBean();
    }

    /**
     * Returns a reference to the virtual machine's platform
     * <code>MBeanServer</code>. This <code>MBeanServer</code> will have
     * all of the platform <code>MXBean</code> s registered with it including
     * any dynamic <code>MXBean</code> s (e.g. instances of
     * {@link GarbageCollectorMXBean}that may be unregistered and destroyed at
     * a later time.
     * <p>
     * In order to simplify the process of distribution and discovery of managed
     * beans it is good practice to register all managed beans (in addition to
     * the platform <code>MXBean</code>s) with this server.
     * </p>
     * <p>
     * A custom <code>MBeanServer</code> can be created by this method if the
     * System property <code>javax.management.builder.initial</code> has been
     * set with the fully qualified name of a subclass of
     * {@link javax.management.MBeanServerBuilder}.
     * </p>
     *
     * @return the platform <code>MBeanServer</code>.
     * @throws SecurityException
     *             if there is a Java security manager in operation and the
     *             caller of this method does not have
     *             &quot;createMBeanServer&quot;
     *             <code>MBeanServerPermission</code>.
     * @see MBeanServer
     * @see javax.management.MBeanServerPermission
     */
    public static MBeanServer getPlatformMBeanServer() {
        // CMVC 93006
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new MBeanServerPermission("createMBeanServer")); //$NON-NLS-1$
        }

        synchronized (ManagementFactory.class) {
            if (platformServer == null) {
                platformServer = MBeanServerFactory.createMBeanServer();

                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        registerPlatformBeans(platformServer);
                        return null;
                    }// end method run
                });
            }
        }// end synchronized
        return platformServer;
    }

    /**
     * Returns the singleton <code>MXBean</code> for the virtual machine's
     * runtime system.
     *
     * @return the virtual machine's {@link RuntimeMXBean}
     */
    public static RuntimeMXBean getRuntimeMXBean() {
        return ManagementUtils.getRuntimeBean();
    }

    /**
     * Returns the singleton <code>MXBean</code> for the virtual machine's
     * threading system.
     *
     * @return the virtual machine's {@link ThreadMXBean}
     */
    public static ThreadMXBean getThreadMXBean() {
        return ManagementUtils.getThreadBean();
    }

    /**
     * @param <T>
     * @param connection
     * @param mxbeanName
     * @param mxbeanInterface
     * @return a new proxy object representing the named <code>MXBean</code>.
     *         All subsequent method invocations on the proxy will be routed
     *         through the supplied {@link MBeanServerConnection} object.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static <T> T newPlatformMXBeanProxy(
            MBeanServerConnection connection, String mxbeanName,
            Class<T> mxbeanInterface) throws IOException {
        // Check that the named object implements the specified interface
        verifyNamedMXBean(mxbeanName, mxbeanInterface);

        T result = null;
        Class[] interfaces = null;
        if (ManagementUtils.isANotificationEmitter(mxbeanInterface)) {
            // Proxies of the MemoryMXBean and OperatingSystemMXBean interfaces
            // must also implement the NotificationEmitter interface.
            interfaces = new Class[] { mxbeanInterface,
                    NotificationEmitter.class };
        } else {
            interfaces = new Class[] { mxbeanInterface };
        }

        result = (T) Proxy.newProxyInstance(interfaces[0].getClassLoader(),
                interfaces, new OpenTypeMappingIHandler(connection,
                        mxbeanInterface.getName(), mxbeanName));
        return result;
    }

    /**
     * Returns the platform MXBean implementing
     * the given {@code mxbeanInterface} which is specified
     * to have one single instance in the Java virtual machine.
     * This method may return {@code null} if the management interface
     * is not implemented in the Java virtual machine (for example,
     * a Java virtual machine with no compilation system does not
     * implement {@link CompilationMXBean});
     * otherwise, this method is equivalent to calling:
     * <pre>
     *    {@link #getPlatformMXBeans(Class)
     *      getPlatformMXBeans(mxbeanInterface)}.get(0);
     * </pre>
     *
     * @param mxbeanInterface a management interface for a platform
     *     MXBean with one single instance in the Java virtual machine
     *     if implemented.
     *
     * @return the platform MXBean that implements
     * {@code mxbeanInterface}, or {@code null} if not exist.
     *
     * @throws IllegalArgumentException if {@code mxbeanInterface}
     * is not a platform management interface or
     * not a singleton platform MXBean.
     *
     * @since 1.7
     */
    public static <T extends PlatformManagedObject>
            T getPlatformMXBean(Class<T> mxbeanInterface) {
        if (!getPlatformManagementInterfaces().contains(mxbeanInterface)) {
            // K0601 = {0} is not a valid MXBean interface.
            throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0601", mxbeanInterface.getName())); //$NON-NLS-1$
        }

        if (multiInstanceBeanNames.contains(mxbeanInterface)) {
            // K0602 = {0} can have zero or more than one instances
            throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0602", mxbeanInterface.getName())); //$NON-NLS-1$
        }

        //work around, need time to refactor the code to support this new API.
        for (PlatformManagedObject bean : ManagementUtils.getAllAvailableMXBeans()) {
            if (mxbeanInterface.isAssignableFrom(bean.getClass())) {
                return (T) bean;
            }
        }
        return null;
    }

    /**
     *
     * @param <T>
     * @param mxbeanInterface
     * @return list of MXBean objects implementing the <code>mxbeanInterface</code>.
     * @throws IllegalArgumentException
     *
     * @since 1.7
     */
    public static <T extends PlatformManagedObject> List<T> getPlatformMXBeans(Class<T> mxbeanInterface)
            throws IllegalArgumentException {

        if (!getPlatformManagementInterfaces().contains(mxbeanInterface)) {
            // K0601 = {0} is not a valid MXBean interface.
            throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0601", mxbeanInterface.getName())); //$NON-NLS-1$
        }

        List<T> matchedBeans = new LinkedList<T>();

        for (PlatformManagedObject bean : ManagementUtils.getAllAvailableMXBeans()) {
            if (mxbeanInterface.isAssignableFrom(bean.getClass())) {
                matchedBeans.add((T) bean);
            }
        }

        return matchedBeans;
    }

    /**
     * Returns the platform MXBean proxy for
     * {@code mxbeanInterface} which is specified to have one single
     * instance in a Java virtual machine and the proxy will
     * forward the method calls through the given {@code MBeanServerConnection}.
     * This method may return {@code null} if the management interface
     * is not implemented in the Java virtual machine being monitored
     * (for example, a Java virtual machine with no compilation system
     * does not implement {@link CompilationMXBean});
     * otherwise, this method is equivalent to calling:
     * <pre>
     *     {@link #getPlatformMXBeans(MBeanServerConnection, Class)
     *        getPlatformMXBeans(connection, mxbeanInterface)}.get(0);
     * </pre>
     *
     * @param connection the {@code MBeanServerConnection} to forward to.
     * @param mxbeanInterface a management interface for a platform
     *     MXBean with one single instance in the Java virtual machine
     *     being monitored, if implemented.
     *
     * @return the platform MXBean proxy for
     * forwarding the method calls of the {@code mxbeanInterface}
     * through the given {@code MBeanServerConnection},
     * or {@code null} if not exist.
     *
     * @throws IllegalArgumentException if {@code mxbeanInterface}
     * is not a platform management interface or
     * not a singleton platform MXBean.
     * @throws java.io.IOException if a communication problem
     * occurred when accessing the {@code MBeanServerConnection}.
     *
     * @see #newPlatformMXBeanProxy
     * @since 1.7
     */
    public static <T extends PlatformManagedObject>
            T getPlatformMXBean(MBeanServerConnection connection,
                                Class<T> mxbeanInterface)
        throws java.io.IOException
    {
        if (!getPlatformManagementInterfaces().contains(mxbeanInterface)) {
            // K0601 = {0} is not a valid MXBean interface.
            throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0601", mxbeanInterface.getName())); //$NON-NLS-1$
        }

        if (multiInstanceBeanNames.contains(mxbeanInterface)) {
            // K0602 = {0} can have zero or more than one instances
            throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0602", mxbeanInterface.getName())); //$NON-NLS-1$
        }

        //work around, need time to refactor the code to support this new API.
        Set<ObjectName> beanNames = connection.queryNames(null, null);

        for (ObjectName objectName : beanNames) {
            boolean matches = false;

            try {
                matches = connection.isInstanceOf(objectName, mxbeanInterface.getName());
            } catch (InstanceNotFoundException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }
            }

            if (matches) {
                return newPlatformMXBeanProxy(connection, objectName.toString(), mxbeanInterface);
            }
        }

        return null;
    }

    /**
     *
     * @param <T>
     * @param connection
     * @param mxbeanInterface
     * @return list of MXBean proxies that can proxy the <code>mxbeanInterface</code>
     *         using the specified <code>MBeanServerConnection</code>.
     * @throws IllegalArgumentException
     * @throws IOException
     *
     * @since 1.7
     */
    public static <T extends PlatformManagedObject> List<T> getPlatformMXBeans(MBeanServerConnection connection,
            Class<T> mxbeanInterface) throws IllegalArgumentException, IOException {

        if (!getPlatformManagementInterfaces().contains(mxbeanInterface)) {
            // K0601 = {0} is not a valid MXBean interface.
            throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0601", mxbeanInterface.getName())); //$NON-NLS-1$
        }

        List<T> matchedBeans = new LinkedList<T>();

        Set<ObjectName> beanNames = connection.queryNames(null, null);

        for (ObjectName objectName : beanNames) {
            boolean matches = false;

            try {
                matches = connection.isInstanceOf(objectName, mxbeanInterface.getName());
            } catch (InstanceNotFoundException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }
            }

            if (matches) {
                T beanProxy = newPlatformMXBeanProxy(connection, objectName.toString(), mxbeanInterface);
                matchedBeans.add(beanProxy);
            }
        }

        return matchedBeans;
    }

    /**
     *
     * @return Set of all platform <code>MXBean</code> interface classes.
     *
     * @since 1.7
     */
    public static Set<Class<? extends PlatformManagedObject>> getPlatformManagementInterfaces() {
        Set<Class<? extends PlatformManagedObject>> interfaces =
            new HashSet<Class<? extends PlatformManagedObject>>();

        interfaces.add(java.lang.management.ClassLoadingMXBean.class);
        interfaces.add(java.lang.management.CompilationMXBean.class);
        interfaces.add(java.lang.management.GarbageCollectorMXBean.class);
        interfaces.add(java.lang.management.MemoryMXBean.class);
        interfaces.add(java.lang.management.MemoryManagerMXBean.class);
        interfaces.add(java.lang.management.MemoryPoolMXBean.class);
        interfaces.add(java.lang.management.OperatingSystemMXBean.class);
        interfaces.add(java.lang.management.RuntimeMXBean.class);
        interfaces.add(java.lang.management.ThreadMXBean.class);

        interfaces.add(java.lang.management.BufferPoolMXBean.class);

        interfaces.add(java.lang.management.PlatformLoggingMXBean.class);

        interfaces.add(com.ibm.lang.management.GarbageCollectorMXBean.class);
        interfaces.add(com.ibm.lang.management.MemoryMXBean.class);
        interfaces.add(com.ibm.lang.management.MemoryPoolMXBean.class);
        interfaces.add(com.ibm.lang.management.OperatingSystemMXBean.class);
		if (ManagementUtils.isRunningOnUnix()) {
	        interfaces.add(com.ibm.lang.management.UnixOperatingSystemMXBean.class);
		}

        List<Class> interfacesFromVMUtils = null;
        //Get a list of all available MXBeans objects using VmManagementUtils
        try{
            //get com.ibm.lang.management.VmManagementUtils from vm.jar
            Class object  =  Class.forName("com.ibm.lang.management.VmManagementUtils"); //$NON-NLS-1$
            PlatformMbeanListProvider pmp = (PlatformMbeanListProvider)object.newInstance();
            interfacesFromVMUtils = pmp.getAllAvailableMBeanInterfaces();
        }catch (ClassNotFoundException e) {
            //if com.ibm.lang.management.VmManagementUtils is not found, no registration is done
        } catch (IllegalAccessException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
        } catch (InstantiationException e) {
        	if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
        }
        if(interfacesFromVMUtils != null){
            for(int i = 0; i < interfacesFromVMUtils.size(); i++){
                interfaces.add(interfacesFromVMUtils.get(i));
            }
        }

        return interfaces;
    }

    /**
     * @param mxbeanName
     * @param mxbeanInterface
     */
    private static void verifyNamedMXBean(String mxbeanName, Class<?> mxbeanInterface) {
        if (mxbeanName == null) {
	        // K0603 = name cannot be null
	        throw new NullPointerException(com.ibm.oti.util.Msg.getString("K0603")); //$NON-NLS-1$
        }

        ObjectName mxbeanObjectName;
        try {
            mxbeanObjectName = new ObjectName(mxbeanName);
        } catch (MalformedObjectNameException e) {
            // K0604 = {0} is not a valid ObjectName format.
            throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0604", mxbeanName), e); //$NON-NLS-1$
        }

        String mxbeanInterfaceName = mxbeanInterface.getName();
        String validObjectNames[] = interfaceNameLookupTable.get(mxbeanInterfaceName);

        if (validObjectNames != null) {
            if (multiInstanceBeanNames.contains(mxbeanInterfaceName)) {
                for (String expectedObjectName : validObjectNames) {
                    // partial match is good enough
                    if (mxbeanName.startsWith(expectedObjectName)) {
                        return;
                    }
                }
                // No partial match, try comparing types.
                for (String expectedObjectName : validObjectNames) {
                    try {
                        ObjectName on1 = new ObjectName(expectedObjectName);
                        String keyProperty1 = on1.getKeyProperty("type"); //$NON-NLS-1$
                        String keyProperty2 = mxbeanObjectName.getKeyProperty("type"); //$NON-NLS-1$
                        boolean isKeySame = (null == keyProperty1)? null == keyProperty2 : keyProperty1.equals(keyProperty2);
                        if ((on1.getDomain().equals(mxbeanObjectName.getDomain())) && isKeySame) {
                            return;
                        }
                    } catch (MalformedObjectNameException e) {
                        // do nothing, throw exception below if there is no match
                    } catch (NullPointerException e) {
                        // do nothing, throw exception below if there is no match
                    }
                }
            } else {
                // exact match required
                for (String expectedObjectName : validObjectNames) {
                    if (expectedObjectName.equals(mxbeanName)) {
                        return;
                    }
                }
            }
        }

        // K0605 = {0} is not an instance of interface {1}
        throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0605", mxbeanName, mxbeanInterfaceName)); //$NON-NLS-1$
    }

    /**
     * Register the singleton platform MXBeans :
     * <ul>
     * <li>ClassLoadingMXBean
     * <li>MemoryMXBean
     * <li>ThreadMXBean
     * <li>RuntimeMXBean
     * <li>OperatingSystemMXBean
     * <li>CompilationMXBean ( <i>only if the VM has a compilation system </i>)
     * </ul>
     * <p>
     * This method will be called only once in the lifetime of the virtual
     * machine, at the point where the singleton platform MBean server has been
     * created.
     *
     * @param platformServer
     *            the platform <code>MBeanServer</code> for this virtual
     *            machine.
     */
    private static void registerPlatformBeans(MBeanServer platformServer) {
    	List<PlatformManagedObject> beansFromVMUtils = null;
    	//Get a list of all available MXBeans objects using VmManagementUtils
    	try{
    		//get com.ibm.lang.management.VmManagementUtils from vm.jar
            Class object  =  Class.forName("com.ibm.lang.management.VmManagementUtils"); //$NON-NLS-1$
            PlatformMbeanListProvider pmp = (PlatformMbeanListProvider)object.newInstance();
            beansFromVMUtils = pmp.getAllAvailableMBeans();
    	}catch (ClassNotFoundException e) {
        	//if com.ibm.lang.management.VmManagementUtils is not found, no registration is done
    	} catch (IllegalAccessException e) {
    		if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
		} catch (InstantiationException e) {
			if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
		}
    	//Get a list of all available MXBeans objects using ManagementUtils
        try {
            List<PlatformManagedObject> beans = ManagementUtils.getAllAvailableMXBeans();
            //Add the bean list returned from VmManagementUtils into the bean list returned from ManagementUtils
            if(beansFromVMUtils != null){
            	beans.addAll(beansFromVMUtils);
            }
            for (PlatformManagedObject bean : beans) {
                ObjectName objectName = bean.getObjectName();
                if (!platformServer.isRegistered(objectName)) {
                    platformServer.registerMBean(bean, objectName);
                }
            }
        } catch (InstanceAlreadyExistsException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        } catch (MBeanRegistrationException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        } catch (NotCompliantMBeanException e) {
        	e.printStackTrace();
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        } catch (NullPointerException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
    }
}
