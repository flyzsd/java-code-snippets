package java.lang.reflect;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2016  All Rights Reserved
 */

/**
 * This class provides methods to dynamically create and access arrays.
 *
 * @author		OTI
 * @version		initial
 */
public final class Array {

/**
 * Prevent this class from being instantiated.
 */
private Array() {
}

/**
 * Return the element of the array at the specified index.
 * This reproduces the effect of <code>array[index]</code>
 * If the array component is a base type, the result is
 * automatically wrapped.
 *
 * @param	array	the array
 * @param	index	the index
 * @return	the requested element, possibly wrapped
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static Object get(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
	Class<?> arrayClass = array.getClass();
	if (arrayClass == int[].class) {
		return ((int[])array)[index];
	} else if (arrayClass == boolean[].class) {
		return ((boolean[])array)[index] ? Boolean.TRUE : Boolean.FALSE;
	} else if (arrayClass == float[].class) {
		return ((float[])array)[index];
	} else if (arrayClass == char[].class) {
		return ((char[])array)[index];
	} else if (arrayClass == double[].class) {
		return ((double[])array)[index];
	} else if (arrayClass == long[].class) {
		return ((long[])array)[index];
	} else if (arrayClass == short[].class) {
		return ((short[])array)[index];
	} else if (arrayClass == byte[].class) {
		/* Avoiding Byte cache yields 5x performance improvement. */
		return new Byte(((byte[])array)[index]);
	} else {
		try {
			return ((Object[])array)[index];
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}
}

/**
 * Return the element of the array at the specified index,
 * converted to a boolean if possible.
 * This reproduces the effect of <code>array[index]</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @return	the requested element
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the element cannot be converted to the requested type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static boolean getBoolean(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    try {
        return ((boolean[])array)[index];
    } catch (ClassCastException e) {
        throw new IllegalArgumentException(e.getMessage());
    }
}

/**
 * Return the element of the array at the specified index,
 * converted to a byte if possible.
 * This reproduces the effect of <code>array[index]</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @return	the requested element
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the element cannot be converted to the requested type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static byte getByte(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    try {
        return ((byte[])array)[index];
    } catch (ClassCastException e) {
        throw new IllegalArgumentException(e.getMessage());
    }
}

/**
 * Return the element of the array at the specified index,
 * converted to a char if possible.
 * This reproduces the effect of <code>array[index]</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @return	the requested element
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the element cannot be converted to the requested type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static char getChar(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    try {
        return ((char[])array)[index];
    } catch (ClassCastException e) {
        throw new IllegalArgumentException(e.getMessage());
    }
}

/**
 * Return the element of the array at the specified index,
 * converted to a double if possible.
 * This reproduces the effect of <code>array[index]</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @return	the requested element
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the element cannot be converted to the requested type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static double getDouble(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    if (array.getClass() == double[].class) {
        return ((double[])array)[index];
    } else {
    	return getFloat(array, index);
    }
}

/**
 * Return the element of the array at the specified index,
 * converted to a float if possible.
 * This reproduces the effect of <code>array[index]</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @return	the requested element
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the element cannot be converted to the requested type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static float getFloat(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    if (array.getClass() == float[].class) {
        return ((float[])array)[index];
    } else {
    	return getLong(array, index);
    }
}

/**
 * Return the element of the array at the specified index,
 * converted to an int if possible.
 * This reproduces the effect of <code>array[index]</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @return	the requested element
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the element cannot be converted to the requested type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static int getInt(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    Class<?> arrayClass = array.getClass();
	if (arrayClass == int[].class) {
        return ((int[])array)[index];
    } else if (arrayClass == char[].class) {
        return ((char[])array)[index];
    } else {
    	return getShort(array, index);
    }
}

/**
 * Return the length of the array.
 * This reproduces the effect of <code>array.length</code>
 *
 * @param	array	the array
 * @return	the length
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array
 */
public static int getLength(Object array) throws IllegalArgumentException {
	Class<?> arrayClass = array.getClass();
    if (arrayClass == int[].class) {
        return ((int[])array).length;
    } else if (arrayClass == boolean[].class) {
        return ((boolean[])array).length;
    } else if (arrayClass == float[].class) {
        return ((float[])array).length;
    } else if (arrayClass == char[].class) {
        return ((char[])array).length;
    } else if (arrayClass == double[].class) {
        return ((double[])array).length;
    } else if (arrayClass == long[].class) {
        return ((long[])array).length;
    } else if (arrayClass == short[].class) {
        return ((short[])array).length;
    } else if (arrayClass == byte[].class) {
        return ((byte[])array).length;
    } else {
	    try {
	        return ((Object[])array).length;
	    } catch (ClassCastException e) {
	    	throw new IllegalArgumentException(e.getMessage());
	    }
    }
}

/**
 * Return the element of the array at the specified index,
 * converted to a long if possible.
 * This reproduces the effect of <code>array[index]</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @return	the requested element
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the element cannot be converted to the requested type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static long getLong(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    if (array.getClass() == long[].class) {
        return ((long[])array)[index];
    } else {
    	return getInt(array, index);
    }
}

/**
 * Return the element of the array at the specified index,
 * converted to a short if possible.
 * This reproduces the effect of <code>array[index]</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @return	the requested element
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the element cannot be converted to the requested type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static short getShort(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    if (array.getClass() == short[].class) {
        return ((short[])array)[index];
    } else {
    	return getByte(array, index);
    }
}

private static native Object multiNewArrayImpl(Class componentType, int dimensions, int[] dimensionsArray);
private static native Object newArrayImpl(Class componentType, int dimension);

/**
 * Return a new multidimensional array of the specified component type and dimensions.
 * This reproduces the effect of <code>new componentType[d0][d1]...[dn]</code>
 * for a dimensions array of { d0, d1, ... , dn }
 *
 * @param	componentType	the component type of the new array
 * @param	dimensions		the dimensions of the new array
 * @return	the new array
 * @exception	java.lang.NullPointerException
 *					if the component type is null
 * @exception	java.lang.NegativeArraySizeException
 *					if any of the dimensions are negative
 * @exception	java.lang.IllegalArgumentException
 *					if componentType is Void.TYPE, or if the array of dimensions is of size zero, or exceeds the limit of
 *					the number of dimension for an array (currently 255)
 */
public static Object newInstance(Class<?> componentType, int... dimensions) throws NegativeArraySizeException, IllegalArgumentException
{
	int i, length;
	int[] reversed;

	if(componentType == null)
		throw new NullPointerException();
	if((length = dimensions.length) < 1)
		throw new IllegalArgumentException();

	// Native is stack-oriented. Reverse the dimensions.
	reversed = new int[length];

	for(i = 0; i < length; i++)
		if(dimensions[i] < 0)
			throw new NegativeArraySizeException();
		else
			reversed[length - i - 1] = dimensions[i];
	return multiNewArrayImpl(componentType, length, reversed);
}

/**
 * Return a new array of the specified component type and length.
 * This reproduces the effect of <code>new componentType[size]</code>
 *
 * @param	componentType	the component type of the new array
 * @param	size			the length of the new array
 * @return	the new array
 * @exception	java.lang.NullPointerException
 *					if the component type is null
 * @exception	java.lang.NegativeArraySizeException
 *					if the size if negative
 * @exception	java.lang.IllegalArgumentException
 *					if componentType is Void.TYPE, or the array dimension exceeds the limit of
 *					the number of dimension for an array (currently 255)
 */
public static Object newInstance(Class<?> componentType, int size) throws NegativeArraySizeException
{
	if(componentType == null)
		throw new NullPointerException();
	if(size < 0)
		throw new NegativeArraySizeException();
	return newArrayImpl(componentType, size);
}

/**
 * Set the element of the array at the specified index to the value.
 * This reproduces the effect of <code>array[index] = value</code>
 * If the array component is a base type, the value is automatically
 * unwrapped
 *
 * @param	array	the array
 * @param	index	the index
 * @param	value	the new value
 *
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the value cannot be converted to the array type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static void set(Object array, int index, Object value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
	final Class componentType = array.getClass().getComponentType();
	if (componentType != null && componentType.isPrimitive()) {
		if (value == null) {
			// trying to put null in a primitive array causes IllegalArgumentException in 1.5
			// but a NullPointerException in 1.4
			throw new IllegalArgumentException();
		}
		Class<?> valueClass = value.getClass();
		if (valueClass == Integer.class) {
            setInt(array, index, ((Integer)value).intValue());
        } else if (valueClass == Float.class) {
            setFloat(array, index, ((Float)value).floatValue());
        } else if (valueClass == Double.class) {
            setDouble(array, index, ((Double)value).doubleValue());
        } else if (valueClass == Long.class) {
            setLong(array, index, ((Long)value).longValue());
        } else if (valueClass == Short.class) {
            setShort(array, index, ((Short)value).shortValue());
        } else if (valueClass == Byte.class) {
            setByte(array, index, ((Byte)value).byteValue());
	    } else if (valueClass == Boolean.class) {
	        setBoolean(array, index, ((Boolean)value).booleanValue());
	    } else if (valueClass == Character.class) {
	        setChar(array, index, ((Character)value).charValue());
	    } else {
	    	/* value is not primitive type */
	    	throw new IllegalArgumentException();
	    }
	} else {
	    try {
	        ((Object[])array)[index] = value;
	    } catch (ClassCastException e) {
	        throw new IllegalArgumentException(e.getMessage());
	    } catch (ArrayStoreException e) {
	        throw new IllegalArgumentException(e.getMessage());
	    }
	}
}

/**
 * Set the element of the array at the specified index to the boolean
 * value. This reproduces the effect of <code>array[index] = value</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @param	value	the new value
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the value cannot be converted to the array type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static void setBoolean(Object array, int index, boolean value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    try {
        ((boolean[])array)[index] = value;
    } catch (ClassCastException e) {
        throw new IllegalArgumentException(e.getMessage());
    }
}

/**
 * Set the element of the array at the specified index to the byte
 * value. This reproduces the effect of <code>array[index] = value</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @param	value	the new value
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the value cannot be converted to the array type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static void setByte(Object array, int index, byte value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    if (array.getClass() == byte[].class) {
        ((byte[])array)[index] = value;
    } else {
    	setShort(array, index, value);
    }
}

/**
 * Set the element of the array at the specified index to the char
 * value. This reproduces the effect of <code>array[index] = value</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @param	value	the new value
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the value cannot be converted to the array type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static void setChar(Object array, int index, char value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    if (array.getClass() == char[].class) {
        ((char[])array)[index] = value;
    } else {
    	setInt(array, index, value);
    }
}

/**
 * Set the element of the array at the specified index to the double
 * value. This reproduces the effect of <code>array[index] = value</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @param	value	the new value
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the value cannot be converted to the array type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static void setDouble(Object array, int index, double value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    try {
        ((double[])array)[index] = value;
    } catch (ClassCastException e) {
        throw new IllegalArgumentException(e.getMessage());
    }
}

/**
 * Set the element of the array at the specified index to the float
 * value. This reproduces the effect of <code>array[index] = value</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @param	value	the new value
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the value cannot be converted to the array type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static void setFloat(Object array, int index, float value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    if (array.getClass() == float[].class) {
        ((float[])array)[index] = value;
    } else {
    	setDouble(array, index, value);
    }
}

/**
 * Set the element of the array at the specified index to the int
 * value. This reproduces the effect of <code>array[index] = value</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @param	value	the new value
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the value cannot be converted to the array type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static void setInt(Object array, int index, int value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    if (array.getClass() == int[].class) {
        ((int[])array)[index] = value;
    } else {
    	setLong(array, index, value);
    }
}

/**
 * Set the element of the array at the specified index to the long
 * value. This reproduces the effect of <code>array[index] = value</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @param	value	the new value
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the value cannot be converted to the array type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static void setLong(Object array, int index, long value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    if (array.getClass() == long[].class) {
        ((long[])array)[index] = value;
    } else {
    	setFloat(array, index, value);
    }
}

/**
 * Set the element of the array at the specified index to the short
 * value. This reproduces the effect of <code>array[index] = value</code>
 *
 * @param	array	the array
 * @param	index	the index
 * @param	value	the new value
 * @exception	java.lang.NullPointerException
 *					if the array is null
 * @exception	java.lang.IllegalArgumentException
 *					if the array is not an array or the value cannot be converted to the array type by a widening conversion
 * @exception	java.lang.ArrayIndexOutOfBoundsException
 *					if the index is out of bounds -- negative or greater than or equal to the array length
 */
public static void setShort(Object array, int index, short value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    if (array.getClass() == short[].class) {
        ((short[])array)[index] = value;
    } else {
    	setInt(array, index, value);
    }
}
}
