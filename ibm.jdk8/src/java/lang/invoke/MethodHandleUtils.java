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
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2012  All Rights Reserved
 */
package java.lang.invoke;
    /**
     * Is this method a caller-sensitive method?
     * I.e., does it call Reflection.getCallerClass or a similer method
     * to ask about the identity of its caller?
     */

class MethodHandleUtils {
	
     static boolean isCallerSensitive(Class<?> clazz, String methodName) {

         return isCallerSensitive(null,clazz,methodName);


     }
	
         static boolean isCallerSensitive(MethodHandle MH, Class<?> clazz, String methodName) {
                  
        switch (methodName) {
        case "doPrivileged":
        case "doPrivilegedWithCombiner":
            return clazz == java.security.AccessController.class;
        case "checkMemberAccess":
                    return SecurityFrameInjector.virtualCallAllowed(MH, java.lang.SecurityManager.class);
      
        case "getUnsafe":
        	return clazz == sun.misc.Unsafe.class;
        case "lookup":
              return clazz == java.lang.invoke.MethodHandles.class;
        case "findStatic":
        case "findVirtual":
        case "findConstructor":
        case "findSpecial":
        case "findGetter":
        case "findSetter":
        case "findStaticGetter":
        case "findStaticSetter":
        case "bind":
        case "unreflect":
        case "unreflectSpecial":
        case "unreflectConstructor":
        case "unreflectGetter":
        case "unreflectSetter":
            return clazz == java.lang.invoke.MethodHandles.Lookup.class;
        case "invoke":
            return clazz == java.lang.reflect.Method.class;

        case "get":
        case "getBoolean":
        case "getByte":
        case "getChar":
        case "getShort":
        case "getInt":
        case "getLong":
        case "getFloat":
        case "getDouble":
        case "set":
        case "setBoolean":
        case "setByte":
        case "setChar":
        case "setShort":
        case "setInt":
        case "setLong":
        case "setFloat":
        case "setDouble":
            return clazz == java.lang.reflect.Field.class;

        case "newInstance":
        
            if (clazz == java.lang.reflect.Constructor.class)  return true;
            if (clazz == java.lang.Class.class)  return true;
            break;
        case "forName":
        case "getClassLoader":
        case "getClasses":
        case "getFields":
        case "getMethods":
        case "getConstructors":
        case "getDeclaredClasses":
        case "getDeclaredFields":
        case "getDeclaredMethods":
        case "getDeclaredConstructors":
        case "getField":
        case "getMethod":
        case "getConstructor":
        case "getDeclaredField":
        case "getDeclaredMethod":
        case "getDeclaredConstructor":
            return clazz == java.lang.Class.class;

        case "getConnection":
        case "getDriver":
        case "getDrivers":
        case "deregisterDriver":
            return clazz == java.sql.DriverManager.class;
        case "newUpdater":
        	if (clazz == java.util.concurrent.atomic.AtomicIntegerFieldUpdater.class)  return true;
            if (clazz == java.util.concurrent.atomic.AtomicLongFieldUpdater.class)  return true;
            if (clazz == java.util.concurrent.atomic.AtomicReferenceFieldUpdater.class)  return true;
            break;
        case "getContextClassLoader":
                    return SecurityFrameInjector.virtualCallAllowed(MH, java.lang.Thread.class);

        case "getPackage":
        case "getPackages":
            return clazz == java.lang.Package.class;

        case "getParent":
        case "getSystemClassLoader":
            return clazz == java.lang.ClassLoader.class;

        case "load":
        case "loadLibrary":

            if (clazz == java.lang.Runtime.class)  return true;
            if (clazz == java.lang.System.class)  return true;
            break;
        case "getCallerClass":
            if (clazz == sun.reflect.Reflection.class)  return true;
            if (clazz == java.lang.System.class)  return true;
            break;
        case "getCallerClassLoader":
            return clazz == java.lang.ClassLoader.class;
        case "registerAsParallelCapable":
                return SecurityFrameInjector.virtualCallAllowed(MH, java.lang.ClassLoader.class);

        case "getProxyClass":
        case "newProxyInstance":
            return clazz == java.lang.reflect.Proxy.class;
        case "asInterfaceInstance":
            return clazz == java.lang.invoke.MethodHandleProxies.class;
        case "getBundle":
        case "clearCache":
            return clazz == java.util.ResourceBundle.class;

        }
        return false;
    }

}
