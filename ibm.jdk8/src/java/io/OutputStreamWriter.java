/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1996, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import sun.nio.cs.StreamEncoder;


/**
 * An OutputStreamWriter is a bridge from character streams to byte streams:
 * Characters written to it are encoded into bytes using a specified {@link
 * java.nio.charset.Charset charset}.  The charset that it uses
 * may be specified by name or may be given explicitly, or the platform's
 * default charset may be accepted.
 *
 * <p> Each invocation of a write() method causes the encoding converter to be
 * invoked on the given character(s).  The resulting bytes are accumulated in a
 * buffer before being written to the underlying output stream.  The size of
 * this buffer may be specified, but by default it is large enough for most
 * purposes.  Note that the characters passed to the write() methods are not
 * buffered.
 *
 * <p> For top efficiency, consider wrapping an OutputStreamWriter within a
 * BufferedWriter so as to avoid frequent converter invocations.  For example:
 *
 * <pre>
 * Writer out
 *   = new BufferedWriter(new OutputStreamWriter(System.out));
 * </pre>
 *
 * <p> A <i>surrogate pair</i> is a character represented by a sequence of two
 * <tt>char</tt> values: A <i>high</i> surrogate in the range '&#92;uD800' to
 * '&#92;uDBFF' followed by a <i>low</i> surrogate in the range '&#92;uDC00' to
 * '&#92;uDFFF'.
 *
 * <p> A <i>malformed surrogate element</i> is a high surrogate that is not
 * followed by a low surrogate or a low surrogate that is not preceded by a
 * high surrogate.
 *
 * <p> This class always replaces malformed surrogate elements and unmappable
 * character sequences with the charset's default <i>substitution sequence</i>.
 * The {@linkplain java.nio.charset.CharsetEncoder} class should be used when more
 * control over the encoding process is required.
 *
 * @see BufferedWriter
 * @see OutputStream
 * @see java.nio.charset.Charset
 *
 * @author      Mark Reinhold
 * @since       JDK1.1
 */

public class OutputStreamWriter extends Writer {

     // New variables for caching writes                                        //IBM-perf_ShortString
	private static final int BUFFER_SIZE = 512;                              //IBM-perf_ShortString
	private OutputStreamWriter osw = null;		//output stream writer to wrap  //IBM-perf_ShortString
	private char[] buffer = new char[BUFFER_SIZE];                           //IBM-perf_ShortString
	private volatile int index = -1;                                         //IBM-perf_ShortString
	private volatile boolean streamClosed = false;                           //IBM-perf_ShortString
	private boolean bufferFlushed = true;                                    //IBM-perf_ShortString
        private final StreamEncoder se;                                         //IBM-perf_ShortString

    /**
     * Creates an OutputStreamWriter that uses the named charset.
     *
     * @param  out
     *         An OutputStream
     *
     * @param  charsetName
     *         The name of a supported
     *         {@link java.nio.charset.Charset charset}
     *
     * @exception  UnsupportedEncodingException
     *             If the named encoding is not supported
     */
    public OutputStreamWriter(OutputStream out, String charsetName)
        throws UnsupportedEncodingException
    {
        super(out);
        if (charsetName == null)
            throw new NullPointerException("charsetName");
        se = StreamEncoder.forOutputStreamWriter(out, out, charsetName);
    }

    /**
     * Creates an OutputStreamWriter that uses the default character encoding.
     *
     * @param  out  An OutputStream
     */
    public OutputStreamWriter(OutputStream out) {
        super(out);
        try {
            se = StreamEncoder.forOutputStreamWriter(out, out, (String)null);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    /**
     * Creates an OutputStreamWriter that uses the given charset.
     *
     * @param  out
     *         An OutputStream
     *
     * @param  cs
     *         A charset
     *
     * @since 1.4
     * @spec JSR-51
     */
    public OutputStreamWriter(OutputStream out, Charset cs) {
        super(out);
        if (cs == null)
            throw new NullPointerException("charset");
        se = StreamEncoder.forOutputStreamWriter(out, out, cs);
    }

    /**
     * Creates an OutputStreamWriter that uses the given charset encoder.
     *
     * @param  out
     *         An OutputStream
     *
     * @param  enc
     *         A charset encoder
     *
     * @since 1.4
     * @spec JSR-51
     */
    public OutputStreamWriter(OutputStream out, CharsetEncoder enc) {
        super(out);
        if (enc == null)
            throw new NullPointerException("charset encoder");
        se = StreamEncoder.forOutputStreamWriter(out, out, enc);
    }

    /**
     * Returns the name of the character encoding being used by this stream.
     *
     * <p> If the encoding has an historical name then that name is returned;
     * otherwise the encoding's canonical name is returned.
     *
     * <p> If this instance was created with the {@link
     * #OutputStreamWriter(OutputStream, String)} constructor then the returned
     * name, being unique for the encoding, may differ from the name passed to
     * the constructor.  This method may return <tt>null</tt> if the stream has
     * been closed. </p>
     *
     * @return The historical name of this encoding, or possibly
     *         <code>null</code> if the stream has been closed
     *
     * @see java.nio.charset.Charset
     *
     * @revised 1.4
     * @spec JSR-51
     */
    public String getEncoding() {
        return se.getEncoding();
    }

    /**
     * Flushes the output buffer to the underlying byte stream, without flushing
     * the byte stream itself.  This method is non-private only so that it may
     * be invoked by PrintStream.
     */
    void flushBuffer() throws IOException {
        se.flushBuffer();
    }

    /**
     * Writes a single character.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(int c) throws IOException {
        synchronized (lock) {                                                   //IBM-perf_ShortString
    		if (!bufferFlushed)                                          //IBM-perf_ShortString
    			if(1 >= (BUFFER_SIZE - index)) {	//no space left in buffer, so write existing contents  //IBM-perf_ShortString
    				emptyBuffer();                               //IBM-perf_ShortString
    			}                                                    //IBM-perf_ShortString
    		                                                             //IBM-perf_ShortString
    		if(1 > BUFFER_SIZE || streamClosed) {                        //IBM-perf_ShortString
    			se.write(c);                                         //IBM-perf_ShortString
    			return;                                              //IBM-perf_ShortString
    		}                                                            //IBM-perf_ShortString
    		bufferFlushed = false;                                       //IBM-perf_ShortString
    		buffer[++index] = (char)c;                                   //IBM-perf_ShortString
    	}                                                                    //IBM-perf_ShortString
    }

    /**
     * Writes a portion of an array of characters.
     *
     * @param  cbuf  Buffer of characters
     * @param  off   Offset from which to start writing characters
     * @param  len   Number of characters to write
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(char cbuf[], int off, int len) throws IOException {
        synchronized (lock) {                                                   //IBM-perf_ShortString
    		if (!bufferFlushed)                                          //IBM-perf_ShortString
    			if(cbuf.length >= (BUFFER_SIZE - index)) {	//no space left in buffer, so write existing contents  //IBM-perf_ShortString
    				emptyBuffer();                               //IBM-perf_ShortString
    			}                                                    //IBM-perf_ShortString
    		                                                             //IBM-perf_ShortString
    		if(cbuf.length > BUFFER_SIZE || streamClosed) {              //IBM-perf_ShortString
    			se.write(cbuf, off, len);                            //IBM-perf_ShortString
    			return;                                              //IBM-perf_ShortString
    		}                                                            //IBM-perf_ShortString
    		System.arraycopy(cbuf, off, buffer, index + 1, len);         //IBM-perf_ShortString
    		bufferFlushed = false;                                       //IBM-perf_ShortString
    		index += len ;		//set position for next writing  //IBM-perf_ShortString
    	}                                                                    //IBM-perf_ShortString
    }

    /**
     * Writes a portion of a string.
     *
     * @param  str  A String
     * @param  off  Offset from which to start writing characters
     * @param  len  Number of characters to write
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(String str, int off, int len) throws IOException {
      synchronized (lock) {                                                     //IBM-perf_ShortString
    		if (!bufferFlushed)                                          //IBM-perf_ShortString
    		if(str.length() >= (BUFFER_SIZE - index)) {       //no space left in buffer, so write existing contents  //IBM-perf_ShortString
    				emptyBuffer();                               //IBM-perf_ShortString
    			}                                                    //IBM-perf_ShortString
    		                                                             //IBM-perf_ShortString
    		if(str.length() > BUFFER_SIZE || streamClosed) {             //IBM-perf_ShortString
    			se.write(str, off, len);                             //IBM-perf_ShortString
    			return;                                              //IBM-perf_ShortString
    		}                                                            //IBM-perf_ShortString
    		str.getChars(off, off+len, buffer, index + 1);            //IBM-perf_ShortString
    		bufferFlushed = false;                                       //IBM-perf_ShortString
    		index += len ;          //set position for next writing      //IBM-perf_ShortString
    	}                                                                   //IBM-perf_ShortString
     }                                                                          //IBM-perf_ShortString

    /**
     * Flushes the stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void flush() throws IOException {
     synchronized (lock) {                                                      //IBM-perf_ShortString
			emptyBuffer();                                           //IBM-perf_ShortString
			se.flush();                                              //IBM-perf_ShortString
		}                                                               //IBM-perf_ShortString
    }

    public void close() throws IOException {
     synchronized (lock) {                                                      //IBM-perf_ShortString
			emptyBuffer();                                           //IBM-perf_ShortString
			streamClosed = true;                                     //IBM-perf_ShortString
			se.close();                                              //IBM-perf_ShortString
		}                                                               //IBM-perf_ShortString
    }
                                                                                //IBM-perf_ShortString
    private void emptyBuffer() throws IOException {                             //IBM-perf_ShortString
		synchronized (lock) {                                            //IBM-perf_ShortString
			if (!streamClosed)                                       //IBM-perf_ShortString
				se.write(buffer, 0, index + 1);                  //IBM-perf_ShortString
			bufferFlushed = true;                                    //IBM-perf_ShortString
			index = -1;                                              //IBM-perf_ShortString
		}                                                                //IBM-perf_ShortString
	}                                                                        //IBM-perf_ShortString
}
//IBM-perf_ShortString
