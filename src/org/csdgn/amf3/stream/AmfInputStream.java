/**
 * Copyright (c) 2017 Robert Maupin
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.csdgn.amf3.stream;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.DataFormatException;

import org.csdgn.amf3.*;

/**
 * Reads an Amf File or Stream from the given input stream.
 * 
 * @author Robert Maupin
 */
public class AmfInputStream implements Closeable, AutoCloseable {
	/**
	 * Specifies an Entry in an AMF file.
	 * 
	 * @author Robert Maupin
	 */
	public static interface AmfEntry {
		/**
		 * The key associated with the entry.
		 * 
		 * @return The key or null if there is no associated key, which will
		 *         only occur if the stream does not represent a file.
		 */
		public String key();

		/**
		 * The value associated with the entry.
		 * 
		 * @return The value.
		 */
		public AmfValue value();
	}

	private static class Header {
		protected int countIndexLength;
		protected boolean isReference;

		protected Header(int u29) {
			this.countIndexLength = u29;
			this.isReference = !readNextBit();
		}

		/**
		 * Reads the next bit and reduces the countIndexLength by a bit.
		 * 
		 * @return
		 */
		protected boolean readNextBit() {
			boolean result = (countIndexLength & 1) == 1;
			countIndexLength >>= 1;
			return result;
		}
	}

	private List<ExternalizableFactory> factories;
	private boolean file;
	private DataInputStream in;
	private String name;
	private List<AmfValue> referenceTable;
	private boolean started;
	private List<String> stringTable;

	private List<Trait> traitTable;

	/**
	 * Creates an AmfInputStream with the given InputStream as input, specifies
	 * this stream as containing an SOL formatting.
	 * 
	 * @param in
	 *            the InputStream to read from.
	 */
	protected AmfInputStream(InputStream in) {
		this(in, true);
	}

	/**
	 * Creates an AmfInputStream with the given InputStream as input.
	 * 
	 * @param in
	 *            the InputStream to read from.
	 * @param file
	 *            if the stream is reading from a SOL formatted file.
	 */
	protected AmfInputStream(InputStream in, boolean file) {
		if(!(in instanceof BufferedInputStream)) {
			in = new BufferedInputStream(in);
		}
		this.in = new DataInputStream(in);
		this.stringTable = new ArrayList<String>();
		this.referenceTable = new ArrayList<AmfValue>();
		this.traitTable = new ArrayList<Trait>();
		this.factories = new ArrayList<ExternalizableFactory>();
		this.name = null;
		this.file = file;
		this.started = !file;
	}

	private AmfXml _readXml(boolean isDocument) throws IOException {
		// Stored by ref?
		Header h = readHeader();
		if(h.isReference) {
			return (AmfXml) referenceTable.get(h.countIndexLength);
		}

		// Stored by value
		AmfXml result = new AmfXml(isDocument);
		result.setValue(readString(h.countIndexLength));
		referenceTable.add(result);
		return result;
	}

	/**
	 * Associates the specified ExternalizableFactory with this AmfInputStream.
	 * Every ExternalizableFactory is called in the order they were added in
	 * attempt to find one that will provide a proper Externalizable for use.
	 * 
	 * @param factory
	 *            the ExternalizableFactory to add
	 */
	public void addExternalizableFactory(ExternalizableFactory factory) {
		if(Objects.isNull(factory)) {
			throw new IllegalArgumentException("The factory provided cannot be null.");
		}
		factories.add(factory);
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	/**
	 * Gets the list of ExternalizableFactory that are associated with this
	 * AmfInputStream.
	 * 
	 * @return
	 */
	public ExternalizableFactory[] getExternalizableFactories() {
		return factories.toArray(new ExternalizableFactory[factories.size()]);
	}

	/**
	 * Gets the name stored in the amf file.
	 * 
	 * @return the file name, or null if the stream does not represent a file.
	 * @throws DataFormatException
	 *             the stream data was not in an expected format.
	 * @throws IOException
	 *             the stream has been closed and the contained input stream
	 *             does not support reading after close, or another I/O error
	 *             occurs.
	 */
	public String getName() throws IOException, DataFormatException {
		if(!started) {
			readFileHeader();
		}
		return name;
	}

	/**
	 * Determines if this input stream has another entry in it.
	 * 
	 * @return true if there is another entry, false otherwise.
	 * @throws IOException
	 *             the stream has been closed and the contained input stream
	 *             does not support reading after close, or another I/O error
	 *             occurs.
	 * @throws DataFormatException
	 *             the stream data was not in an expected format.
	 */
	public boolean hasNext() throws IOException, DataFormatException {
		if(!started) {
			readFileHeader();
		}
		// using the absolute simplest method at the moment.
		// this is basically only a isEOF check.
		in.mark(8);
		if(in.read() == -1) {
			return false;
		}
		in.reset();
		return true;
	}

	/**
	 * Returns the next entry in the input stream.
	 * 
	 * @return the next entry, or null of the end of the stream has been
	 *         reached.
	 * @throws IOException
	 *             the stream has been closed and the contained input stream
	 *             does not support reading after close, or another I/O error
	 *             occurs.
	 * @throws DataFormatException
	 *             the stream data was not in an expected format.
	 */
	public AmfEntry next() throws IOException, DataFormatException {
		if(!started) {
			readFileHeader();
		}

		final String key;
		if(file) {
			key = readString();
		} else {
			key = null;
		}
		final AmfValue value = readValue();

		return new AmfEntry() {
			@Override
			public String key() {
				return key;
			}

			@Override
			public AmfValue value() {
				return value;
			}
		};
	}

	private AmfArray readArray() throws IOException, DataFormatException {
		// Stored by ref?
		Header h = readHeader();
		if(h.isReference) {
			return (AmfArray) referenceTable.get(h.countIndexLength);
		}

		// Stored by value
		AmfArray result = new AmfArray();
		referenceTable.add(result);

		// Associative part (key-value pairs)
		while(true) {
			String key = readString();
			if(key == "") {
				break;
			}

			AmfValue value = readValue();
			result.put(key, value);
		}

		// Dense part (consecutive indices >=0 and <count)
		for(int i = 0; i < h.countIndexLength; i++) {
			AmfValue value = readValue();
			result.add(value);
		}

		return result;
	}

	private AmfByteArray readByteArray() throws IOException {
		// Stored by ref?
		Header h = readHeader();
		if(h.isReference) {
			return (AmfByteArray) referenceTable.get(h.countIndexLength);
		}

		// Stored by value
		byte[] array = new byte[h.countIndexLength];
		in.readFully(array);

		AmfByteArray aba = new AmfByteArray();
		aba.push(array);
		referenceTable.add(aba);
		return aba;
	}

	private AmfDate readDate() throws IOException {
		// Stored by ref?
		Header h = readHeader();
		if(h.isReference) {
			return (AmfDate) referenceTable.get(h.countIndexLength);
		}

		// Stored by value
		double elapsed = in.readDouble();
		AmfDate date = new AmfDate(elapsed);
		referenceTable.add(date);
		return date;
	}

	private AmfDictionary readDictionary() throws IOException, DataFormatException {
		// Stored by ref?
		Header h = readHeader();
		if(h.isReference) {
			return (AmfDictionary) referenceTable.get(h.countIndexLength);
		}

		// Stored by value
		boolean weakKeys = in.readBoolean();
		AmfDictionary result = new AmfDictionary(weakKeys);
		referenceTable.add(result);

		for(int j = 0; j < h.countIndexLength; ++j) {
			AmfValue key = readValue();
			AmfValue value = readValue();
			result.getMap().put(key, value);
		}

		return result;
	}

	private AmfDouble readDouble() throws IOException {
		return new AmfDouble(in.readDouble());
	}

	private void readFileHeader() throws IOException, DataFormatException {
		// calls are evaluated in left to right order
		if(in.readUnsignedByte() != 0x0 || in.readUnsignedByte() != 0xBF) {
			throw new DataFormatException("Unknown Endianness");
		}

		// Size
		int size = in.readInt();
		// TODO
		// if (size + 6 != _reader.BaseStream.Length) throw new
		// InvalidOperationException("Wrong file size");

		// Magic signature
		String magic = readString(4);
		if(!"TCSO".equals(magic)) {
			throw new DataFormatException("Wrong file tag");
		}
		in.skipBytes(6);

		// Read name
		size = in.readUnsignedShort();
		name = readString(size);

		// Version
		int version = (int) in.readInt();
		if(version < 3) {
			throw new DataFormatException("Wrong AMF version");
		}
	}

	private Header readHeader() throws IOException {
		return new Header(readU29());
	}

	private AmfInteger readInteger() throws IOException {
		return new AmfInteger(readS29());
	}

	private AmfObject readObject() throws IOException, DataFormatException {
		Header h = readHeader();
		if(h.isReference) {
			return (AmfObject) referenceTable.get(h.countIndexLength);
		}

		Trait trait = readTrait(h);
		AmfObject result = new AmfObject();
		result.setDynamic(trait.isDynamic());
		result.setExternalizable(trait.isExternalizable());
		result.setTraitName(trait.getName());

		// read sealed properties
		Map<String, AmfValue> map = result.getSealedMap();
		for(String property : trait.getProperties()) {
			map.put(property, readValue());
		}

		// read dynamic properties
		map = result.getDynamicMap();
		if(trait.isDynamic()) {
			while(true) {
				String key = readString();
				if(key.length() == 0) {
					break;
				}
				map.put(key, readValue());
			}
		}

		// read custom data
		if(trait.isExternalizable()) {
			Externalizable ex = null;
			for(ExternalizableFactory factory : factories) {
				ex = factory.create(trait.getName());
				if(ex != null) {
					break;
				}
			}
			if(ex == null) {
				throw new UnsupportedOperationException("Externalizable factory does not support the externalizable data.");
			}
			try {
				ex.readExternal(in);
			} catch(UnexpectedDataException e) {
				throw new UnsupportedOperationException("Externalizable cannot read the externalizable data.");
			}
			result.setExternalizableObject(ex);
		}

		referenceTable.add(result);
		return result;
	}

	private int readS29() throws IOException {
		int result = readU29();
		int maxPositiveInclusive = (1 << 28) - 1;
		if(result <= maxPositiveInclusive) {
			return result; // Positive number
		}

		// Negative number. -x is stored as 2^29 - x
		int upperExclusiveBound = 1 << 29;
		return result - upperExclusiveBound;
	}

	private String readString() throws IOException {
		Header h = readHeader();

		// Stored by reference?
		if(h.isReference) {
			return stringTable.get(h.countIndexLength);
		}

		// Empty string (never stored by ref) ?
		if(h.countIndexLength == 0) {
			return "";
		}

		// Read the string
		String str = readString(h.countIndexLength);
		stringTable.add(str);

		return str;
	}

	private String readString(int length) throws IOException {
		StringBuilder buf = new StringBuilder();
		for(int i = 0; i < length; ++i) {
			buf.append((char) in.readUnsignedByte());
		}
		return buf.toString();
	}

	private Trait readTrait(Header h) throws IOException {
		boolean traitReference = h.readNextBit();
		if(!traitReference) {
			return (Trait) traitTable.get(h.countIndexLength);
		}

		boolean ext = h.readNextBit();
		boolean dyn = h.readNextBit();
		String name = readString();

		// read properties
		String[] props = new String[h.countIndexLength];
		for(int i = 0; i < props.length; ++i) {
			props[i] = readString();
		}

		Trait trait = new SimpleTrait(name, dyn, ext, props);
		traitTable.add(trait);

		return trait;
	}

	private int readU29() throws IOException {
		// Unsigned integer encoded on 8 to 32 bits, with 7 to 29 significant
		// bits.
		// The most significant bits are stored on the left (at the beginning).
		// The fourth byte always have 8 significant bits.
		// 7-7-7-8 or 7-7-7 or 7-7 or 7

		int numBytes = 0;
		int result = 0;
		while(true) {
			int b = in.readUnsignedByte();
			if(numBytes == 3) {
				return (result << 8) | b;
			}
			result = (result << 7) | (b & 0x7F);
			if((b & 0x7F) == b) {
				return result;
			}
			++numBytes;
		}
	}

	private AmfValue readValue() throws IOException, DataFormatException {
		int typeId = in.readUnsignedByte();
		AmfType type = AmfType.get(typeId);
		switch(type) {
		case Undefined:
			return new AmfUndefined();

		case Null:
			return new AmfNull();

		case True:
			return new AmfBoolean(true);

		case False:
			return new AmfBoolean(false);

		case Integer:
			return readInteger();

		case Double:
			return readDouble();

		case String:
			return new AmfString(readString());

		case Date:
			return readDate();

		case ByteArray:
			return readByteArray();

		case Array:
			return readArray();

		case Object:
			return readObject();

		case Dictionary:
			return readDictionary();

		case VectorInt:
			return readVectorInt();

		case VectorUInt:
			return readVectorUInt();

		case VectorDouble:
			return readVectorDouble();

		case VectorGeneric:
			return readVectorGeneric();

		case XmlDoc:
			return readXmlDoc();

		case Xml:
			return readXml();
		}

		throw new DataFormatException(String.format("Unknown Value Type: 0x%x", typeId));
	}

	private AmfVector.Double readVectorDouble() throws IOException {
		Header h = readHeader();
		if(h.isReference) {
			return (AmfVector.Double) referenceTable.get(h.countIndexLength);
		}
		// Stored by value
		boolean fixedLength = in.readBoolean();
		AmfVector.Double result = new AmfVector.Double();
		result.setFixedLength(fixedLength);
		result.setCapacity(h.countIndexLength);
		for(int i = 0; i < h.countIndexLength; ++i) {
			result.add(new AmfDouble(in.readDouble()));
		}
		referenceTable.add(result);
		return result;
	}

	private AmfVector.Generic readVectorGeneric() throws IOException, DataFormatException {
		Header h = readHeader();
		if(h.isReference) {
			return (AmfVector.Generic) referenceTable.get(h.countIndexLength);
		}
		// Stored by value
		boolean fixedLength = in.readBoolean();
		String type = readString();
		AmfVector.Generic result = new AmfVector.Generic(type);
		result.setFixedLength(fixedLength);
		result.setCapacity(h.countIndexLength);
		for(int i = 0; i < h.countIndexLength; ++i) {
			result.add(readValue());
		}
		referenceTable.add(result);
		return result;
	}

	private AmfVector.Integer readVectorInt() throws IOException {
		Header h = readHeader();
		if(h.isReference) {
			return (AmfVector.Integer) referenceTable.get(h.countIndexLength);
		}
		// Stored by value
		boolean fixedLength = in.readBoolean();
		AmfVector.Integer result = new AmfVector.Integer();
		result.setFixedLength(fixedLength);
		result.setCapacity(h.countIndexLength);
		for(int i = 0; i < h.countIndexLength; ++i) {
			result.add(new AmfInteger(in.readInt()));
		}
		referenceTable.add(result);
		return result;
	}

	private AmfVector.UnsignedInteger readVectorUInt() throws IOException {
		Header h = readHeader();
		if(h.isReference) {
			return (AmfVector.UnsignedInteger) referenceTable.get(h.countIndexLength);
		}
		// Stored by value
		boolean fixedLength = in.readBoolean();
		AmfVector.UnsignedInteger result = new AmfVector.UnsignedInteger();
		result.setFixedLength(fixedLength);
		result.setCapacity(h.countIndexLength);
		for(int i = 0; i < h.countIndexLength; ++i) {
			result.add(new AmfInteger(in.readInt() & 0xFFFFFFFF));
		}
		referenceTable.add(result);
		return result;
	}

	private AmfXml readXml() throws IOException {
		return _readXml(false);
	}

	private AmfXml readXmlDoc() throws IOException {
		return _readXml(true);
	}

	/**
	 * Removes the specified ExternalizableFactory from this AmfInputStream.
	 * 
	 * @param factory
	 *            factory to remove
	 */
	public void removeExternalizableFactory(ExternalizableFactory factory) {
		factories.remove(factory);
	}

	/**
	 * Skip the next key or value.
	 * 
	 * @throws IOException
	 *             the stream has been closed and the contained input stream
	 *             does not support reading after close, or another I/O error
	 *             occurs.
	 * @throws DataFormatException
	 *             the stream data was not in an expected format.
	 */
	public void skipNext() throws IOException, DataFormatException {
		if(!started) {
			readFileHeader();
		}
	}

}
