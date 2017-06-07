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
package org.csdgn.amf3;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This is an IO class to handle reading and writing from .SOL files and singularly serialized AmfValues.
 * @author Robert Maupin
 */
public class AmfIO {
	/**
	 * Specifies an Entry in an AMF file.
	 * 
	 * @author Robert Maupin
	 */
	private static interface AmfEntry {
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
	
	/**
	 * Contains all Amf Input methods and logic.
	 * @author Robert Maupin
	 */
	private static class AmfInput implements Closeable, AutoCloseable {
		private List<ExternalizableFactory> factories;
		private boolean file;
		private DataInputStream in;
		private String name;
		private List<AmfValue> referenceTable;
		private boolean headerRead;
		private List<String> stringTable;
		private List<Trait> traitTable;

		/**
		 * Creates an AmfInputStream with the given InputStream as input.
		 * 
		 * @param in
		 *            the InputStream to read from.
		 * @param file
		 *            if the stream is reading from a SOL formatted file.
		 */
		protected AmfInput(InputStream in, boolean file) {
			if(!(in instanceof BufferedInputStream)) {
				in = new BufferedInputStream(in);
			}
			this.in = new DataInputStream(in);
			this.stringTable = new ArrayList<String>();
			this.referenceTable = new ArrayList<AmfValue>();
			this.traitTable = new ArrayList<Trait>();
			this.factories = new ArrayList<ExternalizableFactory>();
			this.headerRead = false;
			this.name = null;
			this.file = file;
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
		protected void addExternalizableFactory(ExternalizableFactory factory) {
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
		 * Gets the name stored in the amf file.
		 * 
		 * @return the file name, or null if the stream does not represent a file.
		 * @throws UnexpectedDataException
		 *             the stream data was not in an expected format.
		 * @throws IOException
		 *             the stream has been closed and the contained input stream
		 *             does not support reading after close, or another I/O error
		 *             occurs.
		 */
		protected String getName() throws IOException, UnexpectedDataException {
			if(file && !headerRead) {
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
		 * @throws UnexpectedDataException
		 *             the stream data was not in an expected format.
		 */
		protected boolean hasNext() throws IOException, UnexpectedDataException {
			if(file && !headerRead) {
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
		 * @throws UnexpectedDataException
		 *             the stream data was not in an expected format.
		 */
		protected AmfEntry next() throws IOException, UnexpectedDataException {
			if(file && !headerRead) {
				readFileHeader();
			}

			final String key;
			if(file) {
				key = readString();
			} else {
				key = null;
			}
			final AmfValue value = readValue();
			
			if(file) {
				//trailer, skip byte
				in.skipBytes(1);
			}

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

		private AmfArray readArray() throws IOException, UnexpectedDataException {
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

		private AmfDictionary readDictionary() throws IOException, UnexpectedDataException {
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

		private void readFileHeader() throws IOException, UnexpectedDataException {
			if(in.readUnsignedByte() != 0x0) {
				throw new UnexpectedDataException("Unknown Endianness");
			}
			if(in.readUnsignedByte() != 0xBF) {
				throw new UnexpectedDataException("Unknown Endianness");
			}

			// Size
			int size = in.readInt();
			// TODO
			// if (size + 6 != _reader.BaseStream.Length) throw new
			// InvalidOperationException("Wrong file size");

			// Magic signature
			String magic = readString(4);
			if(!"TCSO".equals(magic)) {
				throw new UnexpectedDataException("Wrong file tag");
			}
			in.skipBytes(6);

			// Read name
			size = in.readUnsignedShort();
			name = readString(size);

			// Version
			int version = (int) in.readInt();
			if(version < 3) {
				throw new UnexpectedDataException("Wrong AMF version");
			}
			
			headerRead = true;
		}

		private Header readHeader() throws IOException {
			return new Header(readU29());
		}

		private AmfInteger readInteger() throws IOException {
			return new AmfInteger(readS29());
		}

		private AmfObject readObject() throws IOException, UnexpectedDataException {
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

		private AmfValue readValue() throws IOException, UnexpectedDataException {
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

			throw new UnexpectedDataException(String.format("Unknown Value Type: 0x%x", typeId));
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

		private AmfVector.Generic readVectorGeneric() throws IOException, UnexpectedDataException {
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
	}
	
	/**
	 * Contains all Amf Output methods and logic.
	 * @author Robert Maupin
	 */
	private static class AmfOutput {

	}

	/**
	 * Reads AMF from the given SOL file.
	 * 
	 * @param file
	 *            The file to read from.
	 * @param ext
	 *            The ExternalizableFactorys to use, if any.
	 * @return The AmfFile read.
	 * @throws FileNotFoundException
	 *             if the file was not found
	 * @throws IOException
	 *             if the program encountered an I/O error during reading.
	 * @throws UnexpectedDataException
	 *             if invalid data was found during the read, often occurs with
	 *             an invalid or unsupported format.
	 */
	public static final AmfFile readFile(File file, ExternalizableFactory... ext)
			throws FileNotFoundException, IOException, UnexpectedDataException {
		return readFile(new FileInputStream(file), ext);
	}

	/**
	 * Reads AMF from the given input stream designating an SOL file.
	 * 
	 * @param input
	 *            The input stream to read from.
	 * @param ext
	 *            The ExternalizableFactorys to use, if any.
	 * @return The AmfFile read.
	 * @throws IOException
	 *             if the program encountered an I/O error during reading.
	 * @throws UnexpectedDataException
	 *             if invalid data was found during the read, often occurs with
	 *             an invalid or unsupported format.
	 */
	public static final AmfFile readFile(InputStream input, ExternalizableFactory... ext)
			throws IOException, UnexpectedDataException {
		AmfFile file = null;
		try (AmfInput in = new AmfInput(input, true)) {
			for (ExternalizableFactory factory : ext) {
				in.addExternalizableFactory(factory);
			}
			file = new AmfFile();
			file.setName(in.getName());
			while (in.hasNext()) {
				AmfEntry e = in.next();
				file.put(e.key(), e.value());
			}
		}
		return file;
	}

	/**
	 * Reads a serialized AmfValue from the given file.
	 * 
	 * @param file
	 *            The file to read from.
	 * @param ext
	 *            The ExternalizableFactorys to use, if any.
	 * @return The AmfValue read.
	 * @throws FileNotFoundException
	 *             if the file was not found
	 * @throws IOException
	 *             if the program encountered an I/O error during reading.
	 * @throws UnexpectedDataException
	 *             if invalid data was found during the read, often occurs with
	 *             an invalid or unsupported format.
	 */
	public static final AmfValue read(File file, ExternalizableFactory... ext)
			throws FileNotFoundException, IOException, UnexpectedDataException {
		return read(new FileInputStream(file), ext);
	}

	/**
	 * Reads a serialized AmfValue from the given input stream.
	 * 
	 * @param input
	 *            The input stream to read from.
	 * @param ext
	 *            The ExternalizableFactorys to use, if any.
	 * @return The AmfValue read.
	 * @throws FileNotFoundException
	 *             if the file was not found
	 * @throws IOException
	 *             if the program encountered an I/O error during reading.
	 * @throws UnexpectedDataException
	 *             if invalid data was found during the read, often occurs with
	 *             an invalid or unsupported format.
	 */
	public static final AmfValue read(InputStream input, ExternalizableFactory... ext)
			throws IOException, UnexpectedDataException {
		AmfValue value = null;
		try (AmfInput in = new AmfInput(input, false)) {
			for (ExternalizableFactory factory : ext) {
				in.addExternalizableFactory(factory);
			}
			value = in.next().value();
		}
		return value;
	}
	
	
}
