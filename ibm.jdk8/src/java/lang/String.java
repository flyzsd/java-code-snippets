
package java.lang;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2016  All Rights Reserved
 */

import java.io.Serializable;

import java.util.Locale;
import java.util.Comparator;
import java.io.UnsupportedEncodingException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.Formatter;
import java.util.StringJoiner;
import java.util.Iterator;

import java.nio.charset.Charset;

/**
 * Strings are objects which represent immutable arrays of
 * characters.
 *
 * @author		OTI
 * @version		initial
 *
 * @see			StringBuffer
 */

public final class String implements Serializable, Comparable<String>, CharSequence {
	// DO NOT CHANGE OR MOVE THIS LINE
	// IT MUST BE THE FIRST THING IN THE INITIALIZATION
	private static final boolean STRING_OPT_IN_HW = StrCheckHWAvailable();
	private static final long serialVersionUID = -6849794470754667710L;

	/**
	 * CaseInsensitiveComparator compares Strings ignoring the case of the
	 * characters.
	 */
	private static final class CaseInsensitiveComparator implements Comparator<String>, Serializable {
		static final long serialVersionUID = 8575799808933029326L;

		/**
		 * Compare the two objects to determine
		 * the relative ordering.
		 *
		 * @param		o1	an Object to compare
		 * @param		o2	an Object to compare
		 * @return		an int < 0 if object1 is less than object2,
		 *				0 if they are equal, and > 0 if object1 is greater
		 *
		 * @exception	ClassCastException when objects are not the correct type
		 */
		public int compare(String o1, String o2) {
			return o1.compareToIgnoreCase(o2);
		}
	};

	/**
	 * A Comparator which compares Strings ignoring the case of the
	 * characters.
	 */
	public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();
	private static final char[] ascii;
    private static String[] stringArray;
    private static final int stringArraySize = 10;
    private static class UnsafeHelpers {
        public final static long valueFieldOffset = getValueFieldOffset();
        static long getValueFieldOffset() {
        	try { return sun.misc.Unsafe.getUnsafe().objectFieldOffset(String.class.getDeclaredField("value")); }
            catch (NoSuchFieldException e) { throw new RuntimeException(e); }
        }
    }
	/**
	 *  This is a System property to enable copy in {@link #String(String)}}
	 */
	static boolean enableCopy;

	/**
    *  This is a System property to disable copying the String when offset is non-zero in {@link #substring(int)}} and {@link #substring(int, int)}}
    */
   static boolean disableCopyInSubstring;

	private final char [] value;
	private final int offset;
	private final int count;
	private int hashCode;

	static {
		stringArray = new String[stringArraySize];
		ascii = new char[128];
		for (int i=0; i<ascii.length; i++) {
			ascii[i] = (char)i;
		}
	}

/**
 * Answers an empty string.
 */
public String () {
	value = ascii;
	offset = 0;
	count = 0;
}

private String(String s, char c) {
	if (s == null) s = "null"; //$NON-NLS-1$
	offset = 0;
	value = new char[s.count + 1];
	count = s.count + 1;
	System.arraycopy(s.value, s.offset, value, 0, s.count);
	value[s.count] = c;
}

/**
 * Converts the byte array to a String using the default encoding
 * as specified by the file.encoding system property. If the system
 * property is not defined, the default encoding is ISO8859_1 (ISO-Latin-1).
 * If 8859-1 is not available, an ASCII encoding is used.
 *
 * @param		data	the byte array to convert to a String
 *
 * @throws		NullPointerException when data is null
 *
 * @see			#getBytes()
 * @see			#getBytes(int, int, byte[], int)
 * @see			#getBytes(String)
 * @see			#valueOf(boolean)
 * @see			#valueOf(char)
 * @see			#valueOf(char[])
 * @see			#valueOf(char[], int, int)
 * @see			#valueOf(double)
 * @see			#valueOf(float)
 * @see			#valueOf(int)
 * @see			#valueOf(long)
 * @see			#valueOf(Object)
 *
 */
public String (byte [] data) {
	this(data, 0, data.length);
}

/**
 * Converts the byte array to a String, setting the high byte
 * of every character to the specified value.
 *
 * @param		data	the byte array to convert to a String
 * @param		high	the high byte to use
 *
 * @throws		NullPointerException when data is null
 *
 * @deprecated	Use String(byte[]) or String(byte[], String) instead
 */
@Deprecated
public String (byte [] data, int high) {
	this(data, high, 0, data.length);
}

/**
 * Converts the byte array to a String using the default encoding
 * as specified by the file.encoding system property. If the system
 * property is not defined, the default encoding is ISO8859_1 (ISO-Latin-1).
 * If 8859-1 is not available, an ASCII encoding is used.
 *
 * @param		data	the byte array to convert to a String
 * @param		start	the starting offset in the byte array
 * @param		length	the number of bytes to convert
 *
 * @throws		IndexOutOfBoundsException when <code>length < 0, start < 0</code> or
 *				<code>start + length > data.length</code>
 * @throws		NullPointerException when data is null
 *
 * @see			#getBytes()
 * @see			#getBytes(int, int, byte[], int)
 * @see			#getBytes(String)
 * @see			#valueOf(boolean)
 * @see			#valueOf(char)
 * @see			#valueOf(char[])
 * @see			#valueOf(char[], int, int)
 * @see			#valueOf(double)
 * @see			#valueOf(float)
 * @see			#valueOf(int)
 * @see			#valueOf(long)
 * @see			#valueOf(Object)
 *
 */
public String (byte [] data, int start, int length) {
	// start + length could overflow, start/length maybe MaxInt
	if (start >= 0 && 0 <= length && length <= data.length - start) {
		offset = 0;
		value = StringCoding.decode(data, start, length);
		count = value.length;
	} else throw new StringIndexOutOfBoundsException();
}

/**
 * Converts the byte array to a String, setting the high byte
 * of every character to the specified value.
 *
 * @param		data	the byte array to convert to a String
 * @param		high	the high byte to use
 * @param		start	the starting offset in the byte array
 * @param		length	the number of bytes to convert
 *
 * @throws		IndexOutOfBoundsException when <code>length < 0, start < 0</code> or
 *				<code>start + length > data.length</code>
 * @throws		NullPointerException when data is null
 *
 * @deprecated	Use String(byte[], int, int) instead
 */
@Deprecated
public String (byte [] data, int high, int start, int length) {
	if (data != null) {
		// start + length could overflow, start/length maybe MaxInt
		if (start >= 0 && 0 <= length && length <= data.length - start) {
			offset = 0;
			value = new char[length];
			count = length;
			high <<= 8;
			for (int i = 0; i < count; i++)
				value[i] = (char)(high + (data[start++] & 0xff));
		} else throw new StringIndexOutOfBoundsException();
	} else throw new NullPointerException();
}

/**
 * Converts the byte array to a String using the specified encoding.
 *
 * @param		data	the byte array to convert to a String
 * @param		start	the starting offset in the byte array
 * @param		length	the number of bytes to convert
 * @param		encoding	the encoding
 *
 * @throws		IndexOutOfBoundsException when <code>length < 0, start < 0</code> or
 *				<code>start + length > data.length</code>
 * @throws		UnsupportedEncodingException when encoding is not supported
 * @throws		NullPointerException when data is null
 *
 * @see			#getBytes()
 * @see			#getBytes(int, int, byte[], int)
 * @see			#getBytes(String)
 * @see			#valueOf(boolean)
 * @see			#valueOf(char)
 * @see			#valueOf(char[])
 * @see			#valueOf(char[], int, int)
 * @see			#valueOf(double)
 * @see			#valueOf(float)
 * @see			#valueOf(int)
 * @see			#valueOf(long)
 * @see			#valueOf(Object)
 * @see			UnsupportedEncodingException
 */
public String (byte [] data, int start, int length, final String encoding) throws UnsupportedEncodingException {
	if (encoding == null) throw new NullPointerException();
	// start + length could overflow, start/length maybe MaxInt
	if (start >= 0 && 0 <= length && length <= data.length - start) {
		offset = 0;
		value = StringCoding.decode(encoding, data, start, length);
		count = value.length;
	} else throw new StringIndexOutOfBoundsException();
}

/**
 * Converts the byte array to a String using the specified encoding.
 *
 * @param		data	the byte array to convert to a String
 * @param		encoding	the encoding
 *
 * @throws		UnsupportedEncodingException when encoding is not supported
 * @throws		NullPointerException when data is null
 *
 * @see			#getBytes()
 * @see			#getBytes(int, int, byte[], int)
 * @see			#getBytes(String)
 * @see			#valueOf(boolean)
 * @see			#valueOf(char)
 * @see			#valueOf(char[])
 * @see			#valueOf(char[], int, int)
 * @see			#valueOf(double)
 * @see			#valueOf(float)
 * @see			#valueOf(int)
 * @see			#valueOf(long)
 * @see			#valueOf(Object)
 * @see			UnsupportedEncodingException
 */
public String (byte [] data, String encoding) throws UnsupportedEncodingException {
	this(data, 0, data.length, encoding);
}

/**
 * Initializes this String to contain the characters in the specified
 * character array. Modifying the character array after creating the
 * String has no effect on the String.
 *
 * @param 		data	the array of characters
 *
 * @throws		NullPointerException when data is null
 */
public String (char[] data) {
	this(data, 0, data.length);
}

/**
 * Initializes this String to use the specified character
 * array. The character array should not be modified after
 * the String is created.
 *
 * @param 		data	a non-null array of characters
 */
String (char[] data, boolean ignore) {
	offset = 0;
	value = data;
	count = data.length;
}

/**
 * Initializes this String to contain the specified characters in the
 * character array. Modifying the character array after creating the
 * String has no effect on the String.
 *
 * @param 		data	the array of characters
 * @param		start	the starting offset in the character array
 * @param		length	the number of characters to use
 *
 * @throws		IndexOutOfBoundsException when <code>length < 0, start < 0</code> or
 *				<code>start + length > data.length</code>
 * @throws		NullPointerException when data is null
 */
public String (char[] data, int start, int length) {
	// range check everything so a new char[] is not created
	// start + length could overflow, start/length maybe MaxInt
	if (start >= 0 && 0 <= length && length <= data.length - start) {
		offset = 0;
		value = new char[length];
		count = length;
		try {
			System.arraycopy(data, start, value, 0, count);
		} catch (IndexOutOfBoundsException e) {
			throw new StringIndexOutOfBoundsException();
		}
	} else throw new StringIndexOutOfBoundsException();
}

/*
 * Internal version of string constructor. Does not range check,
 * null check, or copy the character array.
 */
String (int start, int length, char[] data) {
	if (length == 0) {
		value = ascii;
		offset = 0;
		count = 0;
	} else if (length == 1 && data[start] < 128) {
		char theChar = data[start];
		value = ascii;
		offset = theChar;
		count = 1;
		hashCode = theChar;
	} else {
		value = data;
		offset = start;
		count = length;
	}
}

/*
 * Private version of String constructor that allocates a new array if start > 0
 * and length != 1 and copies the contents of data into the newly allocated array
 * for a length size of chars. Does not range check, null check, and skips zero
 * initialization in the JIT.
 */
private String(int start, int length, char[] data, boolean ignore) {
   if (length == 0) {
      value = ascii;
      offset = 0;
   } else if (length == 1 && data[start] < 128) {
      char theChar = data[start];
      value = ascii;
      offset = theChar;
      hashCode = theChar;
   } else {
      if (start == 0) {
         value = data;
      } else {
         value = new char[length];
         System.arraycopy(data, start, value, 0, length);
      }
      offset = 0;
   }

   count = length;
}

/**
 * Creates a string that is a copy of another string
 *
 * @param		string	the String to copy
 */
public String (String string) {
	int theCount = string.count;
	char[] data = string.value;
	if (enableCopy && theCount != data.length) {
		char theChar;
		if (theCount == 0) {
			offset = 0;
			value = ascii;
			count = 0;
		} else if (theCount == 1 && (theChar = data[string.offset]) < 128) {
			offset = theChar;
			value = ascii;
			count = 1;
			hashCode = theChar;
		} else {
			offset = 0;
			value = new char[theCount];
			System.arraycopy(data, string.offset, value, 0, theCount);
			count = theCount;
			hashCode = string.hashCode;
		}
	} else {
	    offset = string.offset;
	    value = data;
	    count = theCount;
	    hashCode = string.hashCode;
	}
}

/**
 * Creates a new string from the with specified length
 *
 * @param		numChars	length of new string
 */
private String(int numChars) {
	value = new char[numChars];
	offset = 0;
	count = 0;
}

/**
 * Creates a string from the contents of a StringBuffer.
 *
 * @param		stringbuffer	the StringBuffer
 */
public String (StringBuffer stringbuffer) {
	offset = 0;
	synchronized(stringbuffer) {
		value = stringbuffer.shareValue();
		count = stringbuffer.length();
	}
}

/*
 * Creates a string that is s1 + s2.
 */
private String(String s1, String s2) {
	if (s1 == null) s1 = "null"; //$NON-NLS-1$
	if (s2 == null) s2 = "null"; //$NON-NLS-1$
	int len = s1.count + s2.count;
	value = new char[len];
	offset = 0;
	System.arraycopy(s1.value, s1.offset, value, 0, s1.count);
	System.arraycopy(s2.value, s2.offset, value, s1.count, s2.count);
	count = len;
}

/*
 * Creates a string that is s1 + s2 + s3.
 */
private String(String s1, String s2, String s3) {
	if (s1 == null) s1 = "null"; //$NON-NLS-1$
	if (s2 == null) s2 = "null"; //$NON-NLS-1$
	if (s3 == null) s3 = "null"; //$NON-NLS-1$
	int len = s1.count + s2.count + s3.count;
	value = new char[len];
	offset = 0;
	System.arraycopy(s1.value, s1.offset, value, 0, s1.count);
	System.arraycopy(s2.value, s2.offset, value, s1.count, s2.count);
	System.arraycopy(s3.value, s3.offset, value, s1.count + s2.count, s3.count);
	count = len;
}

/*
 * Creates a string that is s1 + v1.
 */
private String(String s1, int v1) {
	if (s1 == null) s1 = "null"; //$NON-NLS-1$
	int len2 = 1;

	int quot;
	int i = v1;
	while ((i /= 10) != 0) len2++;
	if (v1 >= 0) {
		quot = -v1;
	} else {
		// leave room for '-'
		len2++;
		quot = v1;
	}
	int len = s1.count + len2;
	value = new char[len];
	int index = len - 1;
	do {
		int res = quot/10;
		int rem = quot - (res * 10);
		// write the digit into the correct position
		value[index] = (char)('0' - rem);
		index--;
		quot = res;
	} while(quot != 0);
	if (v1 < 0)
		value[index] = '-';
	offset = 0;
	System.arraycopy(s1.value, s1.offset, value, 0, s1.count);
	count = len;
}

/*
 * Creates a string that is v1 + s1 + v2 + s2 + s3.
 */
private String(int v1, String s1, int v2, String s2, String s3) {

	if (s1 == null) s1 = "null"; //$NON-NLS-1$
	if (s2 == null) s2 = "null"; //$NON-NLS-1$
	if (s3 == null) s3 = "null"; //$NON-NLS-1$

	// figure out num chars for v1

	int len2 = 1;

	int quot;
	int i = v1;
	while ((i /= 10) != 0) len2++;
	if (v1 >= 0) {
		quot = -v1;
	} else {
		// leave room for '-'
		len2++;
		quot = v1;
	}

	// figure out num chars for v2

	int len3 = 1;

	int quot2;
	int i2 = v2;
	while ((i2 /= 10) != 0) len3++;
	if (v2 >= 0) {
		quot2 = -v2;
	} else {
		// leave room for '-'
		len3++;
		quot2 = v2;
	}

	int len = s1.count + len2 + len3 + s2.count + s3.count;
	value = new char[len];

	// copy in s3 contents
	int start = len - s3.count;
	System.arraycopy(s3.value, s3.offset, value, start, s3.count);

	// copy in s2 contents
	start = start - s2.count;
	System.arraycopy(s2.value, s2.offset, value, start, s2.count);

	//copy in v2

	int index = start - 1;

	do {
		int res = quot2/10;
		int rem = quot2 - (res * 10);
		// write the digit into the correct position
		value[index] = (char)('0' - rem);
		index--;
		quot2 = res;
	} while(quot2 != 0);

	if (v2 < 0) {
		value[index--] = '-';
	}

	// copy in s1 contents
	start = index + 1 - s1.count;
	System.arraycopy(s1.value, s1.offset, value, start, s1.count);

	// copy in v1

	index = start - 1;

	do {
		int res = quot/10;
		int rem = quot - (res * 10);
		// write the digit into the correct position
		value[index] = (char)('0' - rem);
		index--;
		quot = res;
	} while(quot != 0);

	if (v1 < 0) {
		value[index] = '-';
	}

	offset = 0;
	count = len;
}

/*
 * Loads from the stringArray if concatenated result is found
 * else it creates a string that is s1 + s2 which is stored in
 * stringArray and then returned.
 */
static private String cachedConstantString(String s1, String s2, int index){
	if (index < stringArraySize){
		if (stringArray[index] == null){
			stringArray[index] = new String(s1,s2);
		}
	} else {
		return new String(s1,s2);
	}
	return stringArray[index];
}

/**
 * Answers the character at the specified offset in this String.
 *
 * @param 		index	the zero-based index in this string
 * @return		the character at the index
 *
 * @throws		IndexOutOfBoundsException when <code>index < 0</code> or
 * 				<code>index >= length()</code>
 */
public char charAt (int index) {
	if (0 <= index && index < count) return value[offset + index];
	throw new StringIndexOutOfBoundsException();
}

/**
 * Compares the specified String to this String using the Unicode values of the
 * characters. Answer 0 if the strings contain the same characters in the same
 * order. Answer a negative integer if the first non-equal character in this String
 * has a Unicode value which is less than the Unicode value of the character at
 * the same position in the specified string, or if this String is a prefix of the
 * specified string. Answer a positive integer if the first non-equal character in
 * this String has a Unicode value which is greater than the Unicode value of the
 * character at the same position in the specified string, or if the specified
 * String is a prefix of the this String.
 *
 * @param		string	the string to compare
 * @return		0 if the strings are equal, a negative integer if this String is before
 *				the specified String, or a positive integer if this String is after
 *				the specified String
 *
 * @throws		NullPointerException when string is null
 */
public int compareTo (String string) {
	// Code adapted from K&R, pg 101
	int o1 = offset, o2 = string.offset, result;
	int end = offset + (count < string.count ? count : string.count);
	char[] target = string.value;
	char[] source = value;
	target.getClass(); // implicit null check;
	source.getClass(); // implicit null check;
	while (o1 < end)
		if ((result = source[o1++] - target[o2++]) != 0) return result;
	return count - string.count;
}

private char compareValue(char ch) {
	if (ch < 128) {
		if ('A' <= ch && ch <= 'Z') return (char)(ch + ('a' - 'A'));
	}
	return Character.toLowerCase(Character.toUpperCase(ch));
}

/**
 * Compare the receiver to the specified String to determine
 * the relative ordering when the case of the characters is
 * ignored.
 *
 * @param		string	a String
 * @return		an int < 0 if this String is less than the specified String,
 *				0 if they are equal, and > 0 if this String is greater
 */
public int compareToIgnoreCase (String string) {
	int o1 = offset, o2 = string.offset, result;
	int end = offset + (count < string.count ? count : string.count);
	char c1, c2;
	char[] target = string.value;
	char[] source = value;
	target.getClass(); // implicit null check;
	source.getClass(); // implicit null check;
	while (o1 < end) {
		if ((c1 = source[o1++]) == (c2 = target[o2++])) continue;
		c1 = compareValue(c1);
		c2 = compareValue(c2);
		if ((result = c1 - c2) != 0) return result;
	}
	return count - string.count;
}

/**
 * Concatenates this String and the specified string.
 *
 * @param		string		the string to concatenate
 * @return		a String which is the concatenation of this String and the specified String
 *
 * @throws		NullPointerException 		if string is null
 */
public String concat (String string) {
	if (string.count > 0) {
		char[] buffer = new char[count + string.count];
		if (count > 0) {
			System.arraycopy (value, offset, buffer, 0, count);
		}
		System.arraycopy (string.value, string.offset, buffer, count, string.count);
		return new String (0, buffer.length, buffer);
	}
	return this;
}

/**
 * Creates a new String containing the characters in the specified
 * character array. Modifying the character array after creating the
 * String has no effect on the String.
 *
 * @param 		data		the array of characters
 * @return		the new String
 *
 * @throws		NullPointerException 		if data is null
 */
public static String copyValueOf(char[] data) {
	return new String(data, 0, data.length);
}

/**
 * Creates a new String containing the specified characters in the
 * character array. Modifying the character array after creating the
 * String has no effect on the String.
 *
 * @param 		data		the array of characters
 * @param		start		the starting offset in the character array
 * @param		length		the number of characters to use
 * @return		the new String
 *
 * @throws		IndexOutOfBoundsException	if <code>length < 0, start < 0</code> or <code>start + length > data.length</code>
 * @throws		NullPointerException 		if data is null
 */
public static String copyValueOf(char[] data, int start, int length) {
	return new String(data, start, length);
}

/**
 * Compares the specified string to this String to determine if the
 * specified string is a suffix.
 *
 * @param		suffix		the string to look for
 * @return		true when the specified string is a suffix of this String, false otherwise
 *
 * @throws		NullPointerException 		if suffix is null
 */
public boolean endsWith(String suffix) {
	return regionMatches(count - suffix.count, suffix, 0, suffix.count);
}

/**
 * Compares the specified object to this String and answer if they
 * are equal. The object must be an instance of String with the same
 * characters in the same order.
 *
 * @param		object		the object to compare
 * @return		true if the specified object is equal to this String, false otherwise
 *
 * @see			#hashCode()
 */
public boolean equals (Object object) {
	if (object == this) return true;
	if (object instanceof String) {
		String s = (String)object;
		if (count != s.count)
			return false;
		boolean offsetsEqual = offset == s.offset;
		if (offsetsEqual && value == s.value)
			return true;
		// There was a time hole between first read of s.hashCode and second read
		//	if another thread does hashcode computing for incoming string object
		int hash1 = hashCode;
		int hash2 = s.hashCode;
		if (hash1 != 0 && hash2 != 0 && hash1 != hash2)
			return false;

		if (com.ibm.oti.vm.VM.J9_JIT_STRING_DEDUP_POLICY == com.ibm.oti.vm.VM.J9_JIT_STRING_DEDUP_POLICY_DISABLED) {
			return regionMatches(0, s, 0, count);
		} else {
			if (!regionMatches(0, s, 0, count)) {
				return false;
			}
			com.ibm.jit.JITHelpers jh = J9VMInternals.jitHelpers;
			if (offsetsEqual && jh != null) {
                deduplicateStrings(s);
			}
			return true;
		}
	}
	return false;
}

private final void deduplicateStrings(String s) {
	com.ibm.jit.JITHelpers jh = J9VMInternals.jitHelpers;
	long voffset = UnsafeHelpers.valueFieldOffset;
	if (com.ibm.oti.vm.VM.J9_JIT_STRING_DEDUP_POLICY == com.ibm.oti.vm.VM.J9_JIT_STRING_DEDUP_POLICY_FAVOUR_LOWER) {
        if (com.ibm.oti.vm.VM.FJ9OBJECT_SIZE == 4) {
        	if (jh.getIntFromObject(this, voffset) < jh.getIntFromObject(s, voffset)) {
        		jh.putObjectInObject(s, voffset, value);
        	} else {
        		jh.putObjectInObject(this, voffset, s.value);
        	}
        } else {
        	if (jh.getLongFromObject(this, voffset) < jh.getLongFromObject(s, voffset)) {
        		jh.putObjectInObject(s, voffset, value);
        	} else {
        		jh.putObjectInObject(this, voffset, s.value);
        	}
        }
    } else {
    	if (com.ibm.oti.vm.VM.FJ9OBJECT_SIZE == 4) {
        	if (jh.getIntFromObject(this, voffset) > jh.getIntFromObject(s, voffset)) {
        		jh.putObjectInObject(s, voffset, value);
        	} else {
        		jh.putObjectInObject(this, voffset, s.value);
        	}
        } else {
        	if (jh.getLongFromObject(this, voffset) > jh.getLongFromObject(s, voffset)) {
        		jh.putObjectInObject(s, voffset, value);
        	} else {
        		jh.putObjectInObject(this, voffset, s.value);
        	}
        }
    }
}

/**
 * Compares the specified String to this String ignoring the case of the
 * characters and answer if they are equal.
 *
 * @param		string		the string to compare
 * @return		true if the specified string is equal to this String, false otherwise
 */
public boolean equalsIgnoreCase (String string) {
	if (string == this) return true;
	if (string == null || count != string.count) return false;

	int o1 = offset, o2 = string.offset;
	int end = offset + count;
	char c1, c2;
	char[] target = string.value;
	char[] source = value;
	while (o1 < end) {
		if ((c1 = source[o1++]) != (c2 = target[o2++])
			&& toUpperCase(c1) != toUpperCase(c2)
			// Required for unicode that we test both cases
			&& toLowerCase(c1) != toLowerCase(c2)
		)
			return false;
	}
	return true;
}

/**
 * Converts this String to a byte encoding using the default encoding
 * as specified by the file.encoding sytem property. If the system
 * property is not defined, the default encoding is ISO8859_1 (ISO-Latin-1).
 * If 8859-1 is not available, an ASCII encoding is used.
 *
 * @return		the byte array encoding of this String
 *
 * @see			String
 */
public byte[] getBytes() {
	return StringCoding.encode(value, offset, count);
}

/**
 * Converts this String to a byte array, ignoring the high order bits of
 * each character.
 *
 * @param		start		the starting offset of characters to copy
 * @param		end			the ending offset of characters to copy
 * @param		data		the destination byte array
 * @param		index		the starting offset in the byte array
 *
 * @throws		NullPointerException 		when data is null
 * @throws		IndexOutOfBoundsException 	when <code>start < 0, end > length(), index < 0, end - start > data.length - index</code>
 *
 * @deprecated	Use getBytes() or getBytes(String)
 */
@Deprecated
public void getBytes(int start, int end, byte[] data, int index) {
	if (0 <= start && start <= end && end <= count) {
		end += offset;
		try {
			for (int i = offset + start; i < end; i++)
				data[index++] = (byte)value[i];
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new StringIndexOutOfBoundsException();
		}
	} else throw new StringIndexOutOfBoundsException();
}

/**
 * Converts this String to a byte encoding using the specified encoding.
 *
 * @param		encoding	the encoding
 * @return		the byte array encoding of this String
 *
 * @throws		UnsupportedEncodingException when the encoding is not supported
 *
 * @see			String
 * @see			UnsupportedEncodingException
 */
public byte[] getBytes(String encoding) throws UnsupportedEncodingException {
	if (encoding == null) throw new NullPointerException();
	return StringCoding.encode(encoding, value, offset, count);
}

/**
 * Copies the specified characters in this String to the character array
 * starting at the specified offset in the character array.
 *
 * @param		start	the starting offset of characters to copy
 * @param		end		the ending offset of characters to copy
 * @param		buffer	the destination character array
 * @param		index	the starting offset in the character array
 *
 * @throws		IndexOutOfBoundsException when <code>start < 0, end > length(),
 *				start > end, index < 0, end - start > buffer.length - index</code>
 * @throws		NullPointerException when buffer is null
 */
public void getChars(int start, int end, char [] buffer, int index) {
	// NOTE last character not copied!
	// Fast range check.
	if (0 <= start && start <= end && end <= count)
		System.arraycopy (value, start + offset, buffer, index, end - start);
	else throw new StringIndexOutOfBoundsException();
}

/**
 * Copies the specified characters in this String to the character array
 * starting at the specified offset in the character array.
 *
 * @param		buffer	the destination character array
 * @param		index	the starting offset in the character array
 *
 * @throws		IndexOutOfBoundsException when <code>index < 0 ||
 * 					count > (buffer.length - index)</code>
 * @throws		NullPointerException when buffer is null
 */
void getChars(char [] buffer, int index) {
	System.arraycopy (value, offset, buffer, index, count);
}

/**
 * Answers an integer hash code for the receiver. Objects which are
 * equal answer the same value for this method.
 *
 * @return		the receiver's hash
 *
 * @see			#equals
 */
public int hashCode() {
	if (hashCode == 0) {
		hashCode = hashCodeImpl(value, offset, count);
	}
	return hashCode;
}

private static int hashCodeImpl(char[] value, int offset, int count) {
	int hash = 0, end = offset + count;
	for (int i = offset; i < end; i++) {
		hash = (hash << 5) - hash + value[i];
	}
	return hash;
}

/**
 * Searches in this String for the first index of the specified character. The
 * search for the character starts at the beginning and moves towards the
 * end of this String.
 *
 * @param		c	the character to find
 * @return		the index in this String of the specified character, -1 if the
 *				character isn't found
 *
 * @see			#lastIndexOf(int)
 * @see			#lastIndexOf(int, int)
 * @see			#lastIndexOf(String)
 * @see			#lastIndexOf(String, int)
 */
public int indexOf (int c) {
	return indexOf(c, 0);
}

/**
 * Searches in this String for the index of the specified character. The
 * search for the character starts at the specified offset and moves towards
 * the end of this String.
 *
 * @param		c		the character to find
 * @param		start	the starting offset
 * @return		the index in this String of the specified character, -1 if the
 *				character isn't found
 *
 * @see			#lastIndexOf(int)
 * @see			#lastIndexOf(int, int)
 * @see			#lastIndexOf(String)
 * @see			#lastIndexOf(String, int)
 */
public int indexOf(int c, int start) {
	if (start < count) {
		if (start < 0) start = 0;
		if (c >= 0 && c <= Character.MAX_VALUE) {
			char[] array = value;
			array.getClass(); // implicit null check
			int base = offset, end = base+count;
			for (int i=base+start; i<end; i++) {
				if (array[i] == c) return i-base;
			}
		} else if (c <= Character.MAX_CODE_POINT) {
			for (int i=start; i<count; i++) {
				int codePoint = codePointAt(i);
				if (codePoint == c) return i;
				if (codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT) i++;
			}
		}
	}
	return -1;
}

/**
 * Searches in this String for the first index of the specified string. The
 * search for the string starts at the beginning and moves towards the end
 * of this String.
 *
 * @param		string	the string to find
 * @return		the index in this String of the specified string, -1 if the
 *				string isn't found
 *
 * @throws		NullPointerException when string is null
 *
 * @see			#lastIndexOf(int)
 * @see			#lastIndexOf(int, int)
 * @see			#lastIndexOf(String)
 * @see			#lastIndexOf(String, int)
 *
 */
public int indexOf (String string) {
	return indexOf(string, 0);
}

/**
 * Searches in this String for the index of the specified string. The
 * search for the string starts at the specified offset and moves towards
 * the end of this String.
 *
 * @param		subString	the string to find
 * @param		start	the starting offset
 * @return		the index in this String of the specified string, -1 if the
 *				string isn't found
 *
 * @throws		NullPointerException when string is null
 *
 * @see			#lastIndexOf(int)
 * @see			#lastIndexOf(int, int)
 * @see			#lastIndexOf(String)
 * @see			#lastIndexOf(String, int)
 */
public int indexOf(String subString, int start) {
	if (start < 0) start = 0;
	int subCount = subString.count;
	if (subCount > 0) {
		if (subCount + start > count) return -1;
		char[] target = subString.value;
		char[] source = value;
		target.getClass(); // implicit null check
		source.getClass(); // implicit null check
		int subOffset = subString.offset;
		char firstChar = target[subOffset];
		int end = subOffset + subCount;
		while (true) {
			int i = indexOf(firstChar, start);
			if (i == -1 || subCount + i > count) return -1; // handles subCount > count || start >= count
			int o1 = offset + i, o2 = subOffset;
			while (++o2 < end && source[++o1] == target[o2]);
			if (o2 == end) return i;
			start = i + 1;
		}
	} else return start < count ? start : count;
}

/**
 * Searches an internal table of strings for a string equal to this String.
 * If the string is not in the table, it is added. Answers the string contained
 * in the table which is equal to this String. The same string object is always
 * answered for strings which are equal.
 *
 * @return		the interned string equal to this String
 */
public native String intern();

/**
 * Searches in this String for the last index of the specified character. The
 * search for the character starts at the end and moves towards the beginning
 * of this String.
 *
 * @param		c	the character to find
 * @return		the index in this String of the specified character, -1 if the
 *				character isn't found
 *
 * @see			#lastIndexOf(int)
 * @see			#lastIndexOf(int, int)
 * @see			#lastIndexOf(String)
 * @see			#lastIndexOf(String, int)
 */
public int lastIndexOf (int c) {
	return lastIndexOf(c, count - 1);
}

/**
 * Searches in this String for the index of the specified character. The
 * search for the character starts at the specified offset and moves towards
 * the beginning of this String.
 *
 * @param		c		the character to find
 * @param		start	the starting offset
 * @return		the index in this String of the specified character, -1 if the
 *				character isn't found
 *
 * @see			#lastIndexOf(int)
 * @see			#lastIndexOf(int, int)
 * @see			#lastIndexOf(String)
 * @see			#lastIndexOf(String, int)
 */
public int lastIndexOf(int c, int start) {
	if (start >= 0) {
		if (start >= count) start = count - 1;
		if (c >= 0 && c <= Character.MAX_VALUE) {
			for (int i=offset+start; i>=offset; --i) {
				if (value[i] == c) return i-offset;
			}
		} else if (c <= Character.MAX_CODE_POINT) {
			for (int i=start; i>=0; --i) {
				int codePoint = codePointAt(i);
				if (codePoint == c) return i;
				if (codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT) --i;
			}
		}
	}
	return -1;
}

/**
 * Searches in this String for the last index of the specified string. The
 * search for the string starts at the end and moves towards the beginning
 * of this String.
 *
 * @param		string	the string to find
 * @return		the index in this String of the specified string, -1 if the
 *				string isn't found
 *
 * @throws		NullPointerException when string is null
 *
 * @see			#lastIndexOf(int)
 * @see			#lastIndexOf(int, int)
 * @see			#lastIndexOf(String)
 * @see			#lastIndexOf(String, int)
 */
public int lastIndexOf (String string) {
	// Use count instead of count - 1 so lastIndexOf("") answers count
	return lastIndexOf(string, count);
}

/**
 * Searches in this String for the index of the specified string. The
 * search for the string starts at the specified offset and moves towards
 * the beginning of this String.
 *
 * @param		subString	the string to find
 * @param		start	the starting offset
 * @return		the index in this String of the specified string, -1 if the
 *				string isn't found
 *
 * @throws		NullPointerException when string is null
 *
 * @see			#lastIndexOf(int)
 * @see			#lastIndexOf(int, int)
 * @see			#lastIndexOf(String)
 * @see			#lastIndexOf(String, int)
 */
public int lastIndexOf(String subString, int start) {
	int subCount = subString.count;
	if (subCount <= count && start >= 0) {
		if (subCount > 0) {
			if (start > count - subCount)
				start = count - subCount; // count and subCount are both >= 1
			char[] target = subString.value;
			char[] source = value;
			target.getClass(); // implicit null check
			source.getClass(); // implicit null check
			int subOffset = subString.offset;
			char firstChar = target[subOffset];
			int end = subOffset + subCount;
			while (true) {
				int i = lastIndexOf(firstChar, start);
				if (i == -1) return -1;
				int o1 = offset + i, o2 = subOffset;
				while (++o2 < end && source[++o1] == target[o2]);
				if (o2 == end) return i;
				start = i - 1;
			}
		} else return start < count ? start : count;
	} else return -1;
}

/**
 * Answers the size of this String.
 *
 * @return		the number of characters in this String
 */
public int length() {
	return count;
}

/**
 * Compares the specified string to this String and compares the specified
 * range of characters to determine if they are the same.
 *
 * @param		thisStart	the starting offset in this String
 * @param		string		the string to compare
 * @param		start		the starting offset in string
 * @param		length		the number of characters to compare
 * @return		true if the ranges of characters is equal, false otherwise
 *
 * @throws		NullPointerException when string is null
 */
public boolean regionMatches(int thisStart, String string, int start, int length) {
	string.getClass(); // implict null check
	if (start < 0 || string.count - start < length) return false;
	if (thisStart < 0 || count - thisStart < length) return false;
	if (length <= 0) return true;
	int o1 = offset + thisStart, o2 = string.offset + start;
	int end = length - 1;
	char[] source = value;
	char[] target = string.value;
	target.getClass(); // implicit null check
	source.getClass(); // implicit null check
	// fast path check - strings are much more likely to be different at the end
	if (source[o1 + end] != target[o2 + end]) return false;
	for (int i = 0; i < end; ++i) {
		if (source[o1 + i] != target[o2 + i])
			return false;
	}
	return true;
}

/**
 * Compares the specified string to this String and compares the specified
 * range of characters to determine if they are the same. When ignoreCase
 * is true, the case of the characters is ignored during the comparison.
 *
 * @param		ignoreCase	specifies if case should be ignored
 * @param		thisStart	the starting offset in this String
 * @param		string		the string to compare
 * @param		start		the starting offset in string
 * @param		length		the number of characters to compare
 * @return		true if the ranges of characters is equal, false otherwise
 *
 * @throws		NullPointerException when string is null
 */
public boolean regionMatches(boolean ignoreCase, int thisStart, String string, int start, int length) {
	if (!ignoreCase) return regionMatches(thisStart, string, start, length);

	string.getClass(); // implicit null check
	if (thisStart < 0 || length > count - thisStart) return false;
	if (start < 0 || length > string.count - start) return false;

	thisStart += offset;
	start += string.offset;
	int end = thisStart + length;
	char c1, c2;
	char[] target = string.value;
	char[] source = value;
	target.getClass(); // implicit null check
	source.getClass(); // implicit null check
	while (thisStart < end) {
		if ((c1 = source[thisStart++]) != (c2 = target[start++])
			&& toUpperCase(c1) != toUpperCase(c2)
			// Required for unicode that we test both cases
			&& toLowerCase(c1) != toLowerCase(c2)
		)
			return false;
	}
	return true;
}

/**
 * Replaces occurrences of the specified character
 * with another character.
 *
 * @param		oldChar	the character to replace
 * @param		newChar	the replacement character
 * @return		a String with occurrences of oldChar replaced by newChar
 */
public String replace(char oldChar, char newChar) {
	int index = indexOf(oldChar, 0);
	if (index == -1) return this;

	char[] buffer = new char[count];
	System.arraycopy(value, offset, buffer, 0, count);
	do {
		buffer[index++] = newChar;
	} while ((index = indexOf(oldChar, index)) != -1);
	return new String(0, count, buffer);
}

/**
 * Compares the specified string to this String to determine if the
 * specified string is a prefix.
 *
 * @param		prefix	the string to look for
 * @return		true when the specified string is a prefix of this String, false otherwise
 *
 * @throws		NullPointerException when prefix is null
 */
public boolean startsWith(String prefix) {
	return startsWith(prefix, 0);
}

/**
 * Compares the specified string to this String, starting at the specified
 * offset, to determine if the specified string is a prefix.
 *
 * @param		prefix	the string to look for
 * @param		start	the starting offset
 * @return		true when the specified string occurs in this String at the
 *				specified offset, false otherwise
 *
 * @throws		NullPointerException when prefix is null
 */
public boolean startsWith(String prefix, int start) {
	return regionMatches(start, prefix, 0, prefix.count);
}

/**
 * Copies a range of characters into a new String.
 *
 * @param		start	the offset of the first character
 * @return		a new String containing the characters from start to the end
 *				of the string
 *
 * @throws		IndexOutOfBoundsException when <code>start < 0</code> or
 *				<code>start > length()</code>
 */
public String substring (int start) {
	if (start == 0)
		return this;
	if (0 <= start && start <= count) {
	   if (!disableCopyInSubstring) {
	      return new String(offset + start, count - start, value, false);
	   } else {
	      return new String(offset + start, count - start, value);
	   }
	}
	throw new StringIndexOutOfBoundsException(start);
}

/**
 * Copies a range of characters.
 *
 * @param		start	the offset of the first character
 * @param		end		the offset one past the last character
 * @return		a String containing the characters from start to end - 1
 *
 * @throws		IndexOutOfBoundsException when <code>start < 0, start > end</code> or
 *				<code>end > length()</code>
 */
public String substring (int start, int end) {
	if (start == 0 && end == count)
		return this;
	// NOTE last character not copied!
	// Fast range check.
	if (0 <= start && start <= end && end <= count) {
      if (!disableCopyInSubstring) {
         return new String (offset + start, end - start, value, false);
      } else {
         return new String (offset + start, end - start, value);
      }
	}
	throw new StringIndexOutOfBoundsException();
}

/**
 * Copies the characters in this String to a character array.
 *
 * @return		a character array containing the characters of this String
 */
public char[] toCharArray() {
	char[] buffer = new char[count];
	System.arraycopy(value, offset, buffer, 0, count);
	return buffer;
}

/**
 * Converts the characters in this String to lowercase, using the default
 * Locale. To convert to lower case independent of any locale, use toLowerCase(Locale.ROOT).
 *
 * @return		a new String containing the lowercase characters equivalent to the
 *				characters in this String
 */
public String toLowerCase() {
	return toLowerCase(Locale.getDefault());
}

private int toLowerCase(int codePoint) {
	if (codePoint < 128) {
		if ('A' <= codePoint && codePoint <= 'Z') return codePoint + ('a' - 'A');
		else return codePoint;
	} else {
		return Character.toLowerCase(codePoint);
	}
}

private int toUpperCase(int codePoint) {
	if (codePoint < 128) {
		if ('a' <= codePoint && codePoint <= 'z') return codePoint - ('a' - 'A');
		else return codePoint;
	} else {
		return Character.toUpperCase(codePoint);
	}
}

// Some of the data below originated from the Unicode Character Database file www.unicode.org/Public/4.0-Update/SpecialCasing-4.0.0.txt. Data from this file was extracted, used in the code and/or converted to an array representation for performance and size.

/**
 * Converts the characters in this String to lowercase, using the specified
 * Locale.
 *
 * @param		locale the Locale
 * @return		a String containing the lowercase characters equivalent to the
 *				characters in this String
 */
public String toLowerCase(Locale locale) {
	// check locale for null
	String language = locale.getLanguage();

	if (isEmpty())
		return this;

	if (StrHWAvailable() && "en" == language) { //$NON-NLS-1$
		String output = toLowerHWOptimized(new String(count));
		if (output != null)
			return output;
	}

	return toLowerCaseCore(language);
}

/**
 * The core of lower case conversion. This is the old, not-as-fast path.
 *
 * @param language	a string representing the Locale
 * @return				a new string object
 */
private String toLowerCaseCore(String language) {
	boolean turkishAzeri = false;
	boolean lithuanian = false;
	StringBuilder builder = null;
	int base = offset;
	char[] array = value;
	array.getClass();
	for (int o = 0; o < count; o++) {
		int codePoint = array[base + o];
		if (codePoint >= Character.MIN_HIGH_SURROGATE && codePoint <= Character.MAX_HIGH_SURROGATE)
			codePoint = codePointAt(o);
		int lowerCase = toLowerCase(codePoint);
		if (codePoint != lowerCase) {
			// if we have yet to create the buffer do so now since we need it
			if (builder == null) {
				turkishAzeri = "tr" == language || "az" == language; //$NON-NLS-1$ //$NON-NLS-2$
				lithuanian = "lt" == language; //$NON-NLS-1$
				builder = new StringBuilder(count);
				builder.append(array, base, o);
			}

			if (codePoint == 0x3a3) {
				builder.append(convertSigma(o));
				continue;
			}
			if (!turkishAzeri && (0x0130 == codePoint)) {
				builder.append("i\u0307"); //$NON-NLS-1$
				continue;
			}

			if (turkishAzeri) {
				if (codePoint == 0x49) {
					// special case mappings. I becomes dotless i, unless follwed by DOT ABOVE
					boolean dotAbove = (o + 1) < count && charAt(o + 1) == '\u0307';
					builder.append(dotAbove ? 'i' : '\u0131');
					if (dotAbove) {
						++o;
					}
				} else {
					builder.appendCodePoint(lowerCase);
				}
			} else if (lithuanian) {
				if (codePoint == 0x49 || codePoint == 0x4a || codePoint == 0x12e) { // I, J, I WITH OGONEK
					builder.append(codePoint == 0x12e ? '\u012f' : (char)(codePoint + 0x20));
					if ((o + 1) < count) {
						int nextPoint = codePointAt(o + 1);
						if (isCombiningAbove(nextPoint)) {
							builder.append('\u0307');
						}
					}
				} else if (codePoint == 0xcc) { // I WITH GRAVE
					builder.append("i\u0307\u0300"); //$NON-NLS-1$
				} else if (codePoint == 0xcd) { // I WITH ACUTE
					builder.append("i\u0307\u0301"); //$NON-NLS-1$
				} else if (codePoint == 0x128) { // I WITH TILDE
					builder.append("i\u0307\u0303"); //$NON-NLS-1$
				} else {
					builder.appendCodePoint(lowerCase);
				}
			} else {
				builder.appendCodePoint(lowerCase);

			}
		} else if (builder != null) {
			builder.appendCodePoint(codePoint);
		}

		if (codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
			++o;
		}
	}
	if (builder == null) {
		return this;
	}
	builder.trimToSize();
	return builder.toString();
}

private static int binarySearchRange(char[] data, char c) {
	char value = 0;
	int low = 0, mid = -1, high = data.length - 1;
	while (low <= high) {
		mid = (low + high) >> 1;
		value = data[mid];
		if (c > value) low = mid + 1;
		else if (c == value) return mid;
		else high = mid - 1;
	}
	return mid - (c < value ? 1 : 0);
}

	private static char[] startCombiningAbove = "\u0300\u033d\u0346\u034a\u0350\u0357\u0363\u0483\u0592\u0597\u059c\u05a8\u05ab\u05af\u05c4\u0610\u0653\u0657\u06d6\u06df\u06e4\u06e7\u06eb\u0730\u0732\u0735\u073a\u073d\u073f\u0743\u0745\u0747\u0749\u0951\u0953\u0f82\u0f86\u17dd\u193a\u20d0\u20d4\u20db\u20e1\u20e7\u20e9\ufe20".value; //$NON-NLS-1$
	private static char[] endCombiningAbove = "\u0314\u0344\u0346\u034c\u0352\u0357\u036f\u0486\u0595\u0599\u05a1\u05a9\u05ac\u05af\u05c4\u0615\u0654\u0658\u06dc\u06e2\u06e4\u06e8\u06ec\u0730\u0733\u0736\u073a\u073d\u0741\u0743\u0745\u0747\u074a\u0951\u0954\u0f83\u0f87\u17dd\u193a\u20d1\u20d7\u20dc\u20e1\u20e7\u20e9\ufe23".value; //$NON-NLS-1$

private static boolean isCombiningAbove(int codePoint) {
	if (codePoint < 0xffff) {
		int index = binarySearchRange(startCombiningAbove, (char)codePoint);
		return index >= 0 && endCombiningAbove[index] >= codePoint;
	} else if ((codePoint >= 0x1d185 && codePoint <= 0x1d189) ||
			codePoint >= 0x1d1aa && codePoint <= 0x1d1ad)
	{
		return true;
	}
	return false;
}

private boolean isWordPart(int codePoint) {
	return codePoint == 0x345 || isWordStart(codePoint);
}

private boolean isWordStart(int codePoint) {
	int type = Character.getType(codePoint);
	return (type >= Character.UPPERCASE_LETTER && type <= Character.TITLECASE_LETTER) ||
		(codePoint >= 0x2b0 && codePoint <= 0x2b8) ||
		(codePoint >= 0x2c0 && codePoint <= 0x2c1) ||
		(codePoint >= 0x2e0 && codePoint <= 0x2e4) ||
		codePoint == 0x37a ||
		(codePoint >= 0x2160 && codePoint <= 0x217f) ||
		(codePoint >= 0x1d2c && codePoint <= 0x1d61);
}

private char convertSigma(int pos) {
	if (pos == 0 || !isWordStart(codePointBefore(pos)) ||
		((pos + 1) < count && isWordPart(codePointAt(pos + 1))))
	{
		return '\u03c3';
	}
	return '\u03c2';
}

/**
 * Answers a string containing a concise, human-readable
 * description of the receiver.
 *
 * @return		this String
 */
public String toString() {
	return this;
}

/**
 * Converts the characters in this String to uppercase, using the default
 * Locale. To convert to upper case independent of any locale, use toUpperCase(Locale.ROOT).
 *
 * @return		a String containing the uppercase characters equivalent to the
 *				characters in this String
 */
public String toUpperCase() {
	return toUpperCase(Locale.getDefault());
}

private static final char[] upperValues = "SS\u0000\u02bcN\u0000J\u030c\u0000\u0399\u0308\u0301\u03a5\u0308\u0301\u0535\u0552\u0000H\u0331\u0000T\u0308\u0000W\u030a\u0000Y\u030a\u0000A\u02be\u0000\u03a5\u0313\u0000\u03a5\u0313\u0300\u03a5\u0313\u0301\u03a5\u0313\u0342\u1f08\u0399\u0000\u1f09\u0399\u0000\u1f0a\u0399\u0000\u1f0b\u0399\u0000\u1f0c\u0399\u0000\u1f0d\u0399\u0000\u1f0e\u0399\u0000\u1f0f\u0399\u0000\u1f08\u0399\u0000\u1f09\u0399\u0000\u1f0a\u0399\u0000\u1f0b\u0399\u0000\u1f0c\u0399\u0000\u1f0d\u0399\u0000\u1f0e\u0399\u0000\u1f0f\u0399\u0000\u1f28\u0399\u0000\u1f29\u0399\u0000\u1f2a\u0399\u0000\u1f2b\u0399\u0000\u1f2c\u0399\u0000\u1f2d\u0399\u0000\u1f2e\u0399\u0000\u1f2f\u0399\u0000\u1f28\u0399\u0000\u1f29\u0399\u0000\u1f2a\u0399\u0000\u1f2b\u0399\u0000\u1f2c\u0399\u0000\u1f2d\u0399\u0000\u1f2e\u0399\u0000\u1f2f\u0399\u0000\u1f68\u0399\u0000\u1f69\u0399\u0000\u1f6a\u0399\u0000\u1f6b\u0399\u0000\u1f6c\u0399\u0000\u1f6d\u0399\u0000\u1f6e\u0399\u0000\u1f6f\u0399\u0000\u1f68\u0399\u0000\u1f69\u0399\u0000\u1f6a\u0399\u0000\u1f6b\u0399\u0000\u1f6c\u0399\u0000\u1f6d\u0399\u0000\u1f6e\u0399\u0000\u1f6f\u0399\u0000\u1fba\u0399\u0000\u0391\u0399\u0000\u0386\u0399\u0000\u0391\u0342\u0000\u0391\u0342\u0399\u0391\u0399\u0000\u1fca\u0399\u0000\u0397\u0399\u0000\u0389\u0399\u0000\u0397\u0342\u0000\u0397\u0342\u0399\u0397\u0399\u0000\u0399\u0308\u0300\u0399\u0308\u0301\u0399\u0342\u0000\u0399\u0308\u0342\u03a5\u0308\u0300\u03a5\u0308\u0301\u03a1\u0313\u0000\u03a5\u0342\u0000\u03a5\u0308\u0342\u1ffa\u0399\u0000\u03a9\u0399\u0000\u038f\u0399\u0000\u03a9\u0342\u0000\u03a9\u0342\u0399\u03a9\u0399\u0000FF\u0000FI\u0000FL\u0000FFIFFLST\u0000ST\u0000\u0544\u0546\u0000\u0544\u0535\u0000\u0544\u053b\u0000\u054e\u0546\u0000\u0544\u053d\u0000".value; //$NON-NLS-1$

/**
 * Return the index of the specified character into the upperValues table.
 * The upperValues table contains three entries at each position. These
 * three characters are the upper case conversion. If only two characters
 * are used, the third character in the table is \u0000.
 *
 * @param ch the char being converted to upper case
 *
 * @return the index into the upperValues table, or -1
 */
private int upperIndex(int ch) {
	int index = -1;
	if (ch <= 0x587) {
		if (ch == 0xdf) index = 0;
		else if (ch <= 0x149) {if (ch == 0x149) index = 1;}
		else if (ch <= 0x1f0) {if (ch == 0x1f0) index = 2;}
		else if (ch <= 0x390) {if (ch == 0x390) index = 3;}
		else if (ch <= 0x3b0) {if (ch == 0x3b0) index = 4;}
		else if (ch <= 0x587) {if (ch == 0x587) index = 5;}
	} else if (ch >= 0x1e96) {
		if (ch <= 0x1e9a)
			index = 6 + ch - 0x1e96;
		else if (ch >= 0x1f50 && ch <= 0x1ffc) {
			index = "\u000b\u0000\f\u0000\r\u0000\u000e\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u000f\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f !\"#$%&'()*+,-./0123456789:;<=>\u0000\u0000?@A\u0000BC\u0000\u0000\u0000\u0000D\u0000\u0000\u0000\u0000\u0000EFG\u0000HI\u0000\u0000\u0000\u0000J\u0000\u0000\u0000\u0000\u0000KL\u0000\u0000MN\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000OPQ\u0000RS\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000TUV\u0000WX\u0000\u0000\u0000\u0000Y".value[ch - 0x1f50]; //$NON-NLS-1$
			if (index == 0) index = -1;
		} else if (ch >= 0xfb00) {
			if (ch <= 0xfb06)
				index = 90 + ch - 0xfb00;
			else if (ch >= 0xfb13 && ch <= 0xfb17)
				index = 97 + ch - 0xfb13;
		}
	}
	return index;
}

/**
 * Converts the characters in this String to uppercase, using the specified
 * Locale.
 *
 * @param		locale the Locale
 * @return		a String containing the uppercase characters equivalent to the
 *				characters in this String
 */
public String toUpperCase(Locale locale) {
	String language = locale.getLanguage();

	if (isEmpty())
		return this;

	if (StrHWAvailable() && "en" == language) { //$NON-NLS-1$
		String output = toUpperHWOptimized(new String(count));
		if (output != null)
			return output;
	}

	return toUpperCaseCore(language);
}

/**
 * The core of upper case conversion. This is the old, not-as-fast path.
 *
 * @param		language the string representing the locale
 * @return		the upper case string
 */
private String toUpperCaseCore(String language) {
	boolean turkishAzeri = "tr" == language || "az" == language; //$NON-NLS-1$ //$NON-NLS-2$
	boolean lithuanian = "lt" == language; //$NON-NLS-1$
	StringBuilder builder = null;
	int base = offset;
	char[] array = value;
	array.getClass();
	for (int o = 0; o < count; o++) {
		int codePoint = array[base + o];
		if (codePoint >= Character.MIN_HIGH_SURROGATE && codePoint <= Character.MAX_HIGH_SURROGATE)
			codePoint = codePointAt(o);
		int index = -1;
		if (codePoint >= 0xdf && codePoint <= 0xfb17)
			index = upperIndex(codePoint);
		if (index == -1) {
			int upper = (!turkishAzeri || codePoint != 0x69) ? toUpperCase(codePoint) : 0x130;
			if (codePoint != upper) {
				if (builder == null) {
					builder = new StringBuilder(count);
					builder.append(array, base, o);
				}
				builder.appendCodePoint(upper);
			} else if (builder != null) builder.appendCodePoint(codePoint);
			if (lithuanian && codePoint <= 0x1ecb && (o + 1) < count && charAt(o+1) == '\u0307' &&
				"ij\u012f\u0268\u0456\u0458\u1e2d\u1ecb".indexOf(codePoint, 0) != -1) //$NON-NLS-1$
			{
				o++;
			}
		} else {
			if (builder == null) {
				builder = new StringBuilder(count + (count / 6) + 2);
				builder.append(array, base, o);
			}
			int target = index * 3;
			char val = upperValues[target];
			builder.append(val);
			val = upperValues[target+1];
			builder.append(val);
			val = upperValues[target+2];
			if (val != 0) {
				builder.append(val);
			}
		}
		if (codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
			o++;
		}
	}
	if (builder == null) {
		return this;
	}
	builder.trimToSize();
	return builder.toString();
}

/**
 * Removes white space characters from the beginning
 * and end of the string.
 *
 * @return		a String with characters <code><= \\u0020</code> removed from the beginning
 *				and the end
 */
public String trim () {
	int start = offset, last = offset + count - 1;
	int end = last;
	char[] array = value;
	array.getClass(); // implicit null check
	while ((start <= end) && (array [start] <= ' ')) start++;
	while ((end >= start) && (array [end] <= ' ')) end--;
	if (start == offset && end == last) return this;
	return new String (start, end - start + 1, array);
}

/**
 * Returns a String containing the characters in the specified
 * character array. Modifying the character array after creating the
 * String has no effect on the String.
 *
 * @param 		data		the array of characters
 * @return		the String
 *
 * @throws		NullPointerException 	when data is null
 */
public static String valueOf(char[] data) {
	return new String(data, 0, data.length);
}

/**
 * Returns a String containing the specified characters in the
 * character array. Modifying the character array after creating the
 * String has no effect on the String.
 *
 * @param 		data		the array of characters
 * @param		start		the starting offset in the character array
 * @param		length		the number of characters to use
 * @return		the String
 *
 * @throws		IndexOutOfBoundsException 	when <code>length < 0, start < 0</code> or <code>start + length > data.length</code>
 * @throws		NullPointerException 		when data is null
 */
public static String valueOf(char[] data, int start, int length) {
	return new String(data, start, length);
}

/**
 * Converts the specified character to its string representation.
 *
 * @param 		value		the character
 * @return		the character converted to a string
 */
public static String valueOf (char value) {
	String s;
	if (value < 128)
		s = new String((int)value, 1, ascii);
	else
		s = new String(0, 1, new char[] {value});
	s.hashCode = value;
	return s;
}

/**
 * Converts the specified double to its string representation.
 *
 * @param 		value		the double
 * @return		the double converted to a string
 */
public static String valueOf (double value) {
	return Double.toString (value);
}

/**
 * Converts the specified float to its string representation.
 *
 * @param 		value		the float
 * @return		the float converted to a string
 */
public static String valueOf (float value) {
	return Float.toString (value);
}

/**
 * Converts the specified integer to its string representation.
 *
 * @param 		value		the integer
 * @return		the integer converted to a string
 */
public static String valueOf (int value) {
	return Integer.toString (value);
}

/**
 * Converts the specified long to its string representation.
 *
 * @param 		value		the long
 * @return		the long converted to a string
 */
public static String valueOf (long value) {
	return Long.toString (value);
}

/**
 * Converts the specified object to its string representation. If the
 * object is null answer the string <code>"null"</code>, otherwise use <code>toString()</code>
 * to get the string representation.
 *
 * @param 		value		the object
 * @return		the object converted to a string
 */
public static String valueOf (Object value) {
	return value != null ? value.toString() : "null"; //$NON-NLS-1$
}

/**
 * Converts the specified boolean to its string representation. When
 * the boolean is true answer <code>"true"</code>, otherwise answer <code>"false"</code>.
 *
 * @param 		value		the boolean
 * @return		the boolean converted to a string
 */
public static String valueOf (boolean value) {
	return value ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
}

/**
 * Answers whether the characters in the StringBuffer strbuf are the same as
 * those in this String.
 *
 * @param		strbuf		the StringBuffer to compare this String to
 * @return		true when the characters in strbuf are identical to  those in this String. If they are not, false will be returned.
 *
 * @throws	 	NullPointerException when strbuf is null
 *
 * @since 1.4
 */
public boolean contentEquals(StringBuffer strbuf) {
	synchronized(strbuf) {
		int size = strbuf.length();
		if (count != size) return false;
		return regionMatches(0, new String(0, size, strbuf.getValue()), 0, size);
	}
}

/**
 * Determines whether a this String matches a given regular expression.
 *
 * @param 		expr		the regular expression to be matched
 * @return 		true if the expression matches, otherwise false
 *
 * @throws	 	PatternSyntaxException	if the syntax of the supplied  regular expression is not valid
 * @throws	 	NullPointerException	if expr is null
 *
 * @since 1.4
 */
public boolean matches(String expr) {
	return Pattern.matches(expr, this);
}

/**
 * Replace any substrings within this String that match the supplied regular expression
 * expr, with the String substitute.
 *
 * @param 		expr		the regular expression to match
 * @param 		substitute	the string to replace the matching substring with
 * @return 		the new string
 *
 * @throws		NullPointerException	if expr is null
 *
 * @since 1.4
 */
public String replaceAll(String expr, String substitute) {
	return	Pattern.compile(expr).matcher(this).replaceAll(substitute);
}

/**
 * Replace any substrings within this String that match the supplied regular expression
 * expr, with the String substitute.
 *
 * @param 		expr		the regular expression to match
 * @param 		substitute	the string to replace the matching substring with
 * @return 		the new string
 *
 * @throws		NullPointerException	if expr is null
 *
 * @since 1.4
 */
public String replaceFirst(String expr, String substitute) {
	return	Pattern.compile(expr).matcher(this).replaceFirst(substitute);
}

/**
 * Splits this string around matches of the given regular expression.
 * Calling this method is same as calling split(regex,0).
 * Therefore, empty string(s) at the end of the returned array will be discarded.
 *
 *
 * @param 		regex	Regular expression that is used as a delimiter
 * @return 		The array of strings which are splitted around the regex
 *
 * @throws	 	PatternSyntaxException if the syntex of regex is invalid
 *
 * @since 1.4
 */
public String[] split(String regex) {
	return	split(regex, 0);
}

private static final char[] regexMetaChars = new char[]
	{ '.', '$', '|', '(', ')', '[', ']', '{', '}', '^', '?', '*', '+', '\\' };

private static final boolean hasMetaChars(String s) {
	for (int i = s.offset, e = s.offset + s.count; i < e; ++i) {
		char ch = s.value[i];

		// Note the surrogate ranges are HIGH: \uD800-\uDBFF; LOW: \uDC00-\uDFFF
		// this check is, therefore, equivalent to returning true if the character
		// falls anywhere in this range including the range between MAX_LOW_SURROGATE
		// and MIN_HIGH_SURROGATE which happen to be adjacent
		if (ch >= Character.MIN_HIGH_SURROGATE
				&& ch <= Character.MAX_LOW_SURROGATE) { return true; }

		for (int j = 0; j < regexMetaChars.length; ++j) {
			if (s.value[i] == regexMetaChars[j]) { return true; }
		}
	}
	return false;
}

/**
 * Splits this String using the given regular expression.
 *
 * max controls the number of times the regex is applied to this string.
 * If max is positive, then regex can be applied to this String max-1 times.
 * The returned array size can not be bigger than max, and the last element of
 * the returned array contains all input after the last match of the regex.
 * If max is negative or zero, then regex can be applied to this string as many times as
 * possible and there is no size limit in the returned array.
 * If max is 0, all the empty string(s) at the end of the returned array will be discarded.
 *
 *
 *
 * @param 		regex	Regular expression that is used as a delimiter
 * @param 		max		The threshold of the returned array
 * @return 		The array of strings which are splitted around the regex
 *
 *	@throws	 	PatternSyntaxException if the syntex of regex is invalid
 *
 * @since 1.4
 */
public String[] split(String regex, int max) {
	// it is faster to handle simple splits inline (i.e. no fancy regex matching)
	// so we test for a suitable string and handle this here if we can
	if (regex != null && regex.count > 0 && !hasMetaChars(regex)) {
		if (max == 1) {
			return new String[] { this };
		}
		// clamp max to 100 - an experimental determined value to maximize the
		// benefit of presizing while avoiding a premature OOM. The default case
		// is 10 since this is the default size of an ArrayList and making the
		// allocation conditional complicates program analysis
		java.util.ArrayList<String> parts = new java.util.ArrayList<String>(max > 0 && max < 100 ? max : 10);
		char[] chars = this.value;
		chars.getClass();
		int start = offset, current = offset, end = offset + count;
		int rOffset = regex.offset;
		if (regex.count == 1) {
			char splitChar = regex.value[rOffset];
			while (current < end) {
				if (chars[current] == splitChar) {
					parts.add(new String(start, current - start, chars, false));
					start = current + 1;
					if (max > 0 && parts.size() == max - 1) {
						parts.add(new String(start, end - start, chars, false));
						break;
					}
				}
				current = current + 1;
			}
		} else {
			int rEnd = regex.offset + regex.count, rCount = regex.count;
			char[] splitChars = regex.value;
			splitChars.getClass();
			char firstChar = regex.value[rOffset];
			while (current < end) {
				if (chars[current] == firstChar) {
					int idx = current + 1;
					int matchIdx = rOffset + 1;
					while (matchIdx < rEnd && idx < end) {
						if (chars[idx] != splitChars[matchIdx]) {
							break;
						}
						matchIdx++;
						idx++;
					}
					if (matchIdx == rEnd) {
						parts.add(new String(start, current - start, chars, false));
						start = current + rCount;
						if (max > 0 && parts.size() == max - 1) {
							parts.add(new String(start, end - start, chars, false));
							break;
						}
						current = current + rCount;
						continue;
					}
				}
				current = current + 1;
			}
		}
		if (parts.size() == 0) {
			return new String[] { this };
		} else if (start <= current && parts.size() != max) {
			parts.add(new String(start, current - start, chars, false));
		}
		if (max == 0) {
			end = parts.size();
			while (end > 0 && parts.get(end - 1).count == 0) {
				end -= 1;
				parts.remove(end);
			}
		}
		return parts.toArray(new String[parts.size()]);
	}
	return Pattern.compile(regex).split(this, max);
}

/**
 * Has the same result as the substring function,
 * but is present so that String may implement the CharSequence interface.
 *
 * @param		start	the offset the first character
 * @param		end	the offset of one past the last character to include
 *
 * @return		the subsequence requested
 *
 * @throws		IndexOutOfBoundsException when start or end is less than zero, start is greater than end, or end is greater than the length of the String.
 *
 * @see 		java.lang.CharSequence#subSequence(int, int)
 *
 * @since 1.4
 */
public CharSequence subSequence(int start, int end) {
	return substring(start, end);
}

/*
 * An implementation of a String.indexOf that is supposed to perform substantially
 * better than the default algorithm if the the "needle" (the subString being searched
 * for) is a constant string.
 *
 * In the jit, if we encounter a call to String.indexOf(String), where the needle
 * is a constant string, we compute the values cache, md2 and lastChar, and change
 * the call to the following method.  This code can be enabled by setting TR_FastIndexOf=1.
 * It searches for the availablility of the following signature before doing the
 * optimization.
 */
private static int indexOf(String haystackString,
                           String needleString,
                           int cache,
                           int md2,
                           char lastChar) {
    char[] haystack        = haystackString.value;
    haystack.getClass(); // implict null check
    int haystackOffset     = haystackString.offset;
    int haystackLength     = haystackString.count;
    char[] needle          = needleString.value;
    needle.getClass(); // implicit null check
    int needleOffset       = needleString.offset;
    int needleLength       = needleString.count;
	int needleLengthMinus1 = needleLength - 1;
	int haystackEnd        = haystackOffset + haystackLength;
	outer_loop :
	for (int i = haystackOffset + needleLengthMinus1; i < haystackEnd;) {
		if (lastChar == haystack[i]) {
			for (int j = 0; j < needleLengthMinus1; ++j) {
				if (needle[j + needleOffset]
					!= haystack[i+j-needleLengthMinus1]) {
					int skip = 1;
					int result = 0;
					if ((cache & (1 << haystack[i])) == 0) {
						result = 0;
					} else {
						result = 1;
					}
					result = result - 1;
					skip += (result & j);
					i += Math.max(md2, skip);
					continue outer_loop;
				}

			}
			return i - needleLengthMinus1 - haystackOffset;
		}

		int result = 0;
		if ((cache & (1 << haystack[i])) == 0) {
			result = 0;
		} else {
			result = 1;
		}
		result = result - 1;
		i += (result & needleLengthMinus1);
		i++;
	}

	return -1;
}

/**
 * @param		data	the byte array to convert to a String
 * @param		start	the starting offset in the byte array
 * @param		length	the number of bytes to convert
 *
 * @since 1.5
 */
public String(int[] data, int start, int length) {
	// range check everything so a new char[] is not created
	// start + length could overflow, start/length maybe MaxInt
	if (start >= 0 && 0 <= length && length <= data.length - start) {
		int size = 0;
		for (int i=start; i<start+length; i++) {
			int codePoint = data[i];
			if (codePoint < Character.MIN_CODE_POINT) throw new IllegalArgumentException();
			else if (codePoint < Character.MIN_SUPPLEMENTARY_CODE_POINT) size++;
			else if (codePoint <= Character.MAX_CODE_POINT) size += 2;
			else throw new IllegalArgumentException();
		}
		offset = 0;
		value = new char[size];
		count = size;
		int j = 0;
		for (int i=start; i<start+length; i++) {
			int codePoint = data[i];
			if (codePoint < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
				value[j++] = (char)codePoint;
			} else {
				codePoint -= Character.MIN_SUPPLEMENTARY_CODE_POINT;
				value[j++] = (char)(Character.MIN_HIGH_SURROGATE + (codePoint >> 10));
				value[j++] = (char)(Character.MIN_LOW_SURROGATE + (codePoint & 0x3ff));
			}
		}
	} else throw new StringIndexOutOfBoundsException();
}

/**
 * Creates a string from the contents of a StringBuilder.
 *
 * @param		sBuilder	the StringBuilder
 *
 * @since 1.5
 */
public String(StringBuilder sBuilder) {
	offset = 0;
	value = sBuilder.shareValue();
	count = sBuilder.length();
}

/**
 * Returns the Unicode character at the given point.
 *
 * @param 		index		the character index
 * @return		the Unicode character value at the index
 *
 * @since		1.5
 */
public int codePointAt(int index) {
	if (index >= 0 && index < count) {
		int high = value[offset + index];
		if ((index + 1) < count && high >= Character.MIN_HIGH_SURROGATE && high <= Character.MAX_HIGH_SURROGATE) {
			int low = value[offset + index + 1];
			if (low >= Character.MIN_LOW_SURROGATE && low <= Character.MAX_LOW_SURROGATE)
				return Character.MIN_SUPPLEMENTARY_CODE_POINT +
					((high - Character.MIN_HIGH_SURROGATE) << 10) +
					(low - Character.MIN_LOW_SURROGATE);
		}
		return high;
	} else throw new StringIndexOutOfBoundsException(index);
}

/**
 * Returns the Unicode character before the given point.
 *
 * @param 		index		the character index
 * @return		the Unicode character value before the index
 *
 * @since		1.5
 */
public int codePointBefore(int index) {
	if (index > 0 && index <= count) {
		int low = value[offset + index - 1];
		if (index > 1 && low >= Character.MIN_LOW_SURROGATE && low <= Character.MAX_LOW_SURROGATE) {
			int high = value[offset + index - 2];
			if (high >= Character.MIN_HIGH_SURROGATE && high <= Character.MAX_HIGH_SURROGATE)
				return Character.MIN_SUPPLEMENTARY_CODE_POINT +
					((high - Character.MIN_HIGH_SURROGATE) << 10) +
					(low - Character.MIN_LOW_SURROGATE);
		}
		return low;
	} else throw new StringIndexOutOfBoundsException(index);
}

/**
 * Returns the total Unicode values in the specified range.
 *
 * @param 		start		first index
 * @param		end			last index
 * @return		the total Unicode values
 *
 * @since		1.5
 */
public int codePointCount(int start, int end) {
	if (start >= 0 && start <= end && end <= count) {
		int count = 0, last = offset + end;
		for (int i=offset+start; i<last; i++) {
			int high = value[i];
			if (i + 1 < last && high >= Character.MIN_HIGH_SURROGATE && high <= Character.MAX_HIGH_SURROGATE) {
				int low = value[i+1];
				if (low >= Character.MIN_LOW_SURROGATE && low <= Character.MAX_LOW_SURROGATE) i++;
			}
			count++;
		}
		return count;
	} else throw new IndexOutOfBoundsException();
}

/**
 * Returns the index of the code point that was offset by <code>codePointCount</code>.
 *
 * @param 		start			the position to offset
 * @param		codePointCount	the code point count
 * @return		the offset index
 *
 * @since		1.5
 */
public int offsetByCodePoints(int start, int codePointCount) {
	if (start >= 0 && start <= count) {
		int index = start;
		if (codePointCount == 0) return start;
		else if (codePointCount > 0) {
			for (int i=0; i<codePointCount; i++) {
				if (index == count) throw new IndexOutOfBoundsException();
				int high = value[offset + index];
				if ((index + 1) < count && high >= Character.MIN_HIGH_SURROGATE && high <= Character.MAX_HIGH_SURROGATE) {
					int low = value[offset + index + 1];
					if (low >= Character.MIN_LOW_SURROGATE && low <= Character.MAX_LOW_SURROGATE) index++;
				}
				index++;
			}
		} else {
			for (int i=codePointCount; i<0; i++) {
				if (index < 1) throw new IndexOutOfBoundsException();
				int low = value[offset + index - 1];
				if (index > 1 && low >= Character.MIN_LOW_SURROGATE && low <= Character.MAX_LOW_SURROGATE) {
					int high = value[offset + index - 2];
					if (high >= Character.MIN_HIGH_SURROGATE && high <= Character.MAX_HIGH_SURROGATE) index--;
				}
				index--;
			}
		}
		return index;
	} else throw new IndexOutOfBoundsException();
}

/**
 * Compares the content of the character sequence to this String
 *
 * @param 		buffer		the character sequence
 * @return		<code>true</code> if the content of this String is equal to the character sequence, <code>false</code> otherswise.
 *
 * @since		1.5
 */
public boolean contentEquals(CharSequence buffer) {
	if (count != buffer.length()) return false;
	for (int i=0; i<count; i++) {
		if (value[offset+i] != buffer.charAt(i)) return false;
	}
	return true;
}

/**
 * @param 		sequence	the sequence to compare to
 * @return		<code>true</code> if this String contains the sequence, <code>false</code> otherswise.
 *
 * @since		1.5
 */
public boolean contains(CharSequence sequence) {
	int subCount;
	if ((subCount = sequence.length()) > count) return false;
	int start = 0;
	if (subCount > 0) {
		if (subCount + start > count) return false;
		int subOffset = 0;
		char firstChar = sequence.charAt(subOffset);
		while (true) {
			int i = indexOf(firstChar, start);
			if (i == -1 || subCount + i > count) return false; // handles subCount > count || start >= count
			int o1 = offset + i, o2 = subOffset;
			while (++o2 < subCount && value[++o1] == sequence.charAt(o2));
			if (o2 == subCount) return true;
			start = i + 1;
		}
	} else return true;
}

/**
 * @param 		sequence1	the old character sequence
 * @param 		sequence2	the new character sequence
 * @return		the new String
 *
 * @since		1.5
 */
public String replace(CharSequence sequence1, CharSequence sequence2) {
	if (sequence2 == null)
		throw new NullPointerException();
	int sequence1Length = sequence1.length();
	if (sequence1Length == 0) {
		StringBuilder result = new StringBuilder((count+1) * sequence2.length());
		result.append(sequence2);
		for (int i=0; i<count; i++) {
			result.append(charAt(i));
			result.append(sequence2);
		}
		return result.toString();
	}
	StringBuilder result = new StringBuilder();
	char first = sequence1.charAt(0);
	int start = 0, copyStart = 0, firstIndex;
	while (start < count) {
		if ((firstIndex = indexOf(first, start)) == -1)
			break;
		boolean found = true;
		if (sequence1.length() > 1) {
			if (firstIndex + sequence1Length > count)
				break;
			for (int i=1; i<sequence1Length; i++) {
				if (charAt(firstIndex + i) != sequence1.charAt(i)) {
					found = false;
					break;
				}
			}
		}
		if (found) {
			result.append(substring(copyStart, firstIndex));
			result.append(sequence2);
			copyStart = start = firstIndex + sequence1Length;
		} else {
			start = firstIndex + 1;
		}
	}
	if (result.length() == 0 && copyStart == 0)
		return this;
	result.append(substring(copyStart));
	return result.toString();
}

/**
 * Format the receiver using the specified format and args.
 *
 * @param format the format to use
 * @param args the format arguments to use
 *
 * @return the formatted result
 *
 * @see java.util.Formatter#format(String, Object...)
 */
public static String format(String format, Object... args) {
	Formatter formatter = new Formatter();
	formatter.format(format, args);
	return formatter.toString();
}

/**
 * Format the receiver using the specified local, format and args.
 *
 * @param locale the locale used to create the Formatter, may be null
 * @param format the format to use
 * @param args the format arguments to use
 *
 * @return the formatted result
 *
 * @see java.util.Formatter#format(String, Object...)
 */
public static String format(Locale locale, String format, Object... args) {
	Formatter formatter = new Formatter(locale);
	formatter.format(format, args);
	return formatter.toString();
}

private static final java.io.ObjectStreamField[] serialPersistentFields = {
};

/**
 * Answers if this String has no characters, a length of zero.
 *
 * @return		true if this String has no characters, false otherwise
 *
 * @since		1.6
 *
 * @see			#length
 */
public boolean isEmpty() {
	return count == 0;
}

/**
 * Converts the byte array to a String using the specified Charset.
 *
 * @param		data	the byte array to convert to a String
 * @param		aCharset	the Charset to use
 *
 * @throws		NullPointerException when data is null
 *
 * @since		1.6
 *
 * @see			#String(byte[], int, int, Charset)
 * @see			#getBytes(Charset)
 */
public String (byte [] data, Charset aCharset) {
	this(data, 0, data.length, aCharset);
}

/**
 * Converts the byte array to a String using the specified Charset.
 *
 * @param		data	the byte array to convert to a String
 * @param		start	the starting offset in the byte array
 * @param		length	the number of bytes to convert
 * @param		aCharset	the Charset to use
 *
 * @throws		IndexOutOfBoundsException when <code>length < 0, start < 0</code> or
 *				<code>start + length > data.length</code>
 * @throws		NullPointerException when data is null
 *
 * @since		1.6
 *
 * @see			#String(byte[], Charset)
 * @see			#getBytes(Charset)
 */
public String (byte [] data, int start, int length, Charset aCharset) {
	if (aCharset == null) throw new NullPointerException();
	// start + length could overflow, start/length maybe MaxInt
	if (start >= 0 && 0 <= length && length <= data.length - start) {
		offset = 0;
		value = StringCoding.decode(aCharset, data, start, length);
		count = value.length;
	} else throw new StringIndexOutOfBoundsException();
}

/**
 * Converts this String to a byte encoding using the specified Charset.
 *
 * @param		aCharset	the Charset to use
 * @return		the byte array encoding of this String
 *
 * @since		1.6
 */
public byte[] getBytes(Charset aCharset) {
	return StringCoding.encode(aCharset, value, offset, count);
}

/**
 * Creates a new String by putting each element together joined by the delimiter.
 * If an element is null, then "null" is used as string to join.
 *
 * @param delimiter Used as joiner to put elements together
 * @param elements	Elements to be joined
 * @return string of joined elements by delimiter
 * @throws NullPointerException if one of the arguments is null
 *
 */
public static String join(CharSequence delimiter, CharSequence... elements)
{
	StringJoiner stringJoiner = new StringJoiner(delimiter);
	for (CharSequence element : elements) {
		stringJoiner.add(element);
	}
	return stringJoiner.toString();
}

/**
 * Creates a new String by putting each element together joined by the delimiter.
 * If an element is null, then "null" is used as string to join.
 *
 * @param delimiter Used as joiner to put elements together
 * @param elements	Elements to be joined
 * @return string of joined elements by delimiter
 * @throws NullPointerException if one of the arguments is null
 *
 */
public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements)
{
	StringJoiner stringJoiner = new StringJoiner(delimiter);
	Iterator<? extends CharSequence> elementsIterator = elements.iterator();
	while (elementsIterator.hasNext()) {
		stringJoiner.add(elementsIterator.next());
	}
	return stringJoiner.toString();
}

// DO NOT MODIFY THIS METHOD
/* The method is only called once to setup the flag DFP_HW_AVAILABLE
 *	Return value
 *		true - when JIT compiled this method, replaces it with loadconst 1
 *		if -Xjit:disableHWAcceleratedStringCaseConv hasn't been supplied
 *		false - if still interpreting this method or disabled by VM option
 */
private final static boolean StrCheckHWAvailable() {
	return false;
}

// DO NOT MODIFY THIS METHOD
/*
 *	Return value
 *		true - when JIT compiled this method, replaces it with loadconst 1
 *		if -Xjit:disableHWAcceleratedStringCaseConv hasn't been supplied
 *		false - if still interpreting this method or disabled by VM option
 */
private final static boolean StrHWAvailable(){
	return STRING_OPT_IN_HW;
}

//DO NOT CHANGE CONTENTS OF THESE METHODS
private final String toUpperHWOptimized(String input) { input = toUpperCaseCore("en"); return input; } //$NON-NLS-1$
private final String toLowerHWOptimized(String input) { input = toLowerCaseCore("en"); return input; } //$NON-NLS-1$

}
