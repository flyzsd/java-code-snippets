package java.util.zip;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2014, 2016  All Rights Reserved
 */

/**
 * General structure of one type of information stored in extra field.
 * Header ID - 2 bytes
 * Data Size - 2 bytes
 * Data Block - (variable) bytes
 */
final class ZipEntryExtraInfo {
	/**
	 * The headerID supported, used in extra field.
	 * Reference link: http://www.info-zip.org/doc/appnote-19970311-iz.zip
	 */
	final static short HEADER_ID_ExtendedTimestamp = 0x5455;
	final static short HEADER_ID_NTFS = 0x000a;

	final byte[] extra;		//The bytes data of the extra field
	final int offset;		//The starting point of this information within the extra.
	final short headerId;	//The header id of this information
	final short dataSize;	//The data size of this information

	/**
	 * @param extra			The extra field data
	 * @param offset		The offset of this information inside of the extra field
	 * @param headerId		The header ID of this information
	 * @param dataSize		The data size of this information.
	 */
	private ZipEntryExtraInfo(byte[] extra, int offset, short headerId, short dataSize) {
		this.extra = extra;
		this.offset = offset;
		this.headerId = headerId;
		this.dataSize = dataSize;
	}

	/**
	 * Read 2 bytes, and return in integer.
	 * Byte order: low-byte in the beginning, high-byte in the end
	 *
	 * @param data			The data to read
	 * @param offset		The offset to start reading
	 * @return				The 2 bytes in integer.
	 */
	static int read2Bytes(byte[] data, int offset) {
		int low = Byte.toUnsignedInt(data[offset]);
		int high = Byte.toUnsignedInt(data[offset + 1]) << 8;
		return high | low;
	}

	/**
	 * Read 4 bytes, and return in long.
	 * Byte order: low-byte in the beginning, high-byte in the end
	 *
	 * @param data			The data to read
	 * @param offset		The offset to start reading
	 * @return				The 4 bytes in long.
	 */
	static long read4Bytes(byte[] data, int offset) {
		int low = read2Bytes(data, offset);
		int high = read2Bytes(data, offset + 2) << 16;
		return (high | low) & 0x00000000FFFFFFFFL;
	}

	/**
	 * Read 8 bytes, and return in long.
	 * Byte order: low-byte in the beginning, high-byte in the end
	 *
	 * @param data			The data to read
	 * @param offset		The offset to start reading
	 * @return				The 8 bytes in long.
	 */
	static long read8Bytes(byte[] data, int offset) {
		long low = read4Bytes(data, offset);
		long high = read4Bytes(data, offset + 4) << 32;
		return high | low;
	}

	/**
	 * Read 2 bytes, and return in short.
	 * Byte order: low-byte in the beginning, high-byte in the end
	 *
	 * @param data			The data to read
	 * @param offset		The offset to start reading
	 * @return				The 2 bytes in short.
	 */
	static short readShort(byte[] data, int offset) {
		return (short)read2Bytes(data, offset);
	}

	/**
	 * Read 4 bytes, and return in integer.
	 * Byte order: low-byte in the beginning, high-byte in the end
	 *
	 * @param data			The data to read
	 * @param offset		The offset to start reading
	 * @return				The 4 bytes in integer.
	 */
	static int readInt(byte[] data, int offset) {
		return (int)read4Bytes(data, offset);
	}

	/**
	 * Read 8 bytes, and return in long.
	 * Byte order: low-byte in the beginning, high-byte in the end
	 *
	 * @param data			The data to read
	 * @param offset		The offset to start reading
	 * @return				The 8 bytes in long.
	 */
	static long readLong(byte[] data, int offset) {
		return read8Bytes(data, offset);
	}

	/**
	 * Construction method to create a ZipEntryExtraInfo
	 *
	 * @param extra		The extra field data
	 * @param offset	The offset of this information inside of the extra field
	 * @return			A new ZipEntryExtraInfo
	 */
	static ZipEntryExtraInfo fromExtra (byte[] extra, int offset) {
		if (null == extra) {
			return null;
		}
		int infoLen = extra.length - offset;
		if (infoLen < 4) {
			return null;
		}
		int pos = offset;
		short headerId = readShort(extra, pos);
		short dataSize = readShort(extra, pos + 2);

		int dataLenLimit = infoLen - 4;
		if (dataLenLimit < dataSize) {
			return null;
		}

		return new ZipEntryExtraInfo(extra, offset, headerId, dataSize);
	}

	/**
	 * Get the data size of this information
	 *
	 * @return		The data size of this information
	 */
	final short getDataSize() {
		return dataSize;
	}

	/**
	 * Get the data offset within the extra field
	 *
	 * @return		The data offset of this information within the extra field
	 */
	final int getDataOffset() {
		return offset + 4;
	}

	/**
	 * Get total length of this information, including segments of: header id, data size, data block
	 *
	 * @return		The total length of this information
	 */
	final int getTotalLength() {
		return dataSize + 4;
	}
}

