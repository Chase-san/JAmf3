package org.csdgn.amf3;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import javax.activation.UnsupportedDataTypeException;

/**
 * This class is obsolete and should not be used.
 * @deprecated Use {@link org.csdgn.amf3.stream.AmfInputStream} instead.
 *
 */
public class AmfReaderClassic {
	private static class Header {
		protected boolean isReference;
		protected int countIndexLength;
		
		protected Header(int u29) {
			this.countIndexLength = u29;
	        this.isReference = !readNextBit();
		}
		
		/**
		 * Reads the next bit and reduces
		 * the countIndexLength by a bit.
		 * @return
		 */
		protected boolean readNextBit() {
			boolean result = (countIndexLength & 1) == 1;
			countIndexLength >>= 1;
            return result;
		}
	}
	
	private DataInput in;
	private ExternalizableFactory factory;
	private List<String> stringTable;
	private List<AmfValue> referenceTable;
	private List<Trait> traitTable;
	
	public AmfReaderClassic(InputStream in) {
		if(in instanceof DataInputStream) {
			this.in = (DataInputStream)in;
		} else {
			this.in = new DataInputStream(in);	
		}
		stringTable = new ArrayList<String>();
		referenceTable = new ArrayList<AmfValue>();
		traitTable = new ArrayList<Trait>();
		factory = null;
	}
	
	public void setExternalizableFactory(ExternalizableFactory factory) {
		this.factory = factory;
	}
	
	public ExternalizableFactory getExternalizableFactory() {
		return factory;
	}
	
	public AmfFile readFile() throws DataFormatException, IOException {
		//calls are evaluated in left to right order
		if(in.readUnsignedByte() != 0x0 || in.readUnsignedByte() != 0xBF) {
			throw new DataFormatException("Unknown Endianness");
		}
		
		// Size
		
        int size = in.readInt();
        //if (size + 6 != _reader.BaseStream.Length) throw new InvalidOperationException("Wrong file size");

        // Magic signature
        String magic = readAsciiString(4);
        if (!"TCSO".equals(magic)) {
        	throw new DataFormatException("Wrong file tag");
        }
        in.skipBytes(6);

        // Read name
        size = in.readUnsignedShort();
        String name = readAsciiString(size);

        // Version
        int version = (int)in.readInt();
        if (version < 3) {
        	throw new DataFormatException("Wrong AMF version");
        }

        AmfFile file = new AmfFile();
        file.setName(name);
        
        // Read content
        try {
	        while (true) {
	        	String key = readString();
	        	AmfValue value = readValue();
	            
	        	file.put(key, value);
	
	            //Skip trailing byte.
	            //No official documentation. Usually zero.
	            in.skipBytes(1);
	        }
        } catch(EOFException e) {
    		//exit loop on EOF
    	}
		
		return file;
	}
	
	protected String readString() throws IOException {
		Header h = readHeader();
		
		// Stored by reference?
		if(h.isReference) {
			return stringTable.get(h.countIndexLength);
		}

        // Empty string (never stored by ref) ?
        if (h.countIndexLength == 0) {
        	return "";
        }

        // Read the string
        String str = readAsciiString(h.countIndexLength);
        stringTable.add(str);
        
        return str;
    }
	
	protected int readS29() throws IOException {
		int result = readU29();
        int maxPositiveInclusive = (1 << 28) - 1;
        if (result <= maxPositiveInclusive) {
        	return result;  // Positive number
        }

        // Negative number. -x is stored as 2^29 - x
        int upperExclusiveBound = 1 << 29;
        return result - upperExclusiveBound;
	}
	
	protected AmfDouble readDouble() throws IOException {
		return new AmfDouble(in.readDouble());
	}
	
	protected AmfInteger readInteger() throws IOException {
		return new AmfInteger(readS29());
	}
	
	protected AmfValue readValue() throws IOException {
		AmfType type = AmfType.get(in.readUnsignedByte());
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
			
		default:
			throw new UnsupportedDataTypeException(); 
		}
	}
	
	protected AmfObject readObject() throws IOException {
		Header h = readHeader();
        if(h.isReference) {
        	return (AmfObject)referenceTable.get(h.countIndexLength);
        }
        
        Trait trait = readTrait(h);
        AmfObject result = new AmfObject();
        result.setDynamic(trait.isDynamic());
        result.setExternalizable(trait.isExternalizable());
        result.setTraitName(trait.getName());
        
        //read sealed properties
        Map<String, AmfValue> map = result.getSealedMap();
        for(String property : trait.getProperties()) {
        	map.put(property, readValue());
        }
        
        //read dynamic properties
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
        
        //read custom data
        if(trait.isExternalizable()) {
        	if(factory == null) {
        		throw new UnsupportedOperationException("No externalizable factory found.");
        	}
        	Externalizable ex = factory.create(trait.getName());
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
	
	protected Trait readTrait(Header h) throws IOException {
		boolean traitReference = h.readNextBit();
        if(!traitReference) {
        	return (Trait)traitTable.get(h.countIndexLength);
        }
        
        boolean ext = h.readNextBit();
        boolean dyn = h.readNextBit();
        String name = readString();
        
        //read properties
        String[] props = new String[h.countIndexLength];
        for(int i = 0; i < props.length; ++i) {
        	props[i] = readString();
        }
        
        Trait trait = new SimpleTrait(name, dyn, ext, props);
        traitTable.add(trait);
        
        return trait;
	}

	protected AmfVector.Integer readVectorInt()  throws IOException {
    	Header h = readHeader();
        if(h.isReference) {
        	return (AmfVector.Integer)referenceTable.get(h.countIndexLength);
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
	
	protected AmfVector.UnsignedInteger readVectorUInt()  throws IOException {
		Header h = readHeader();
        if(h.isReference) {
        	return (AmfVector.UnsignedInteger)referenceTable.get(h.countIndexLength);
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
	
	protected AmfVector.Double readVectorDouble()  throws IOException {
		Header h = readHeader();
        if(h.isReference) {
        	return (AmfVector.Double)referenceTable.get(h.countIndexLength);
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
	
	protected AmfVector.Generic readVectorGeneric() throws IOException {
		Header h = readHeader();
        if(h.isReference) {
        	return (AmfVector.Generic)referenceTable.get(h.countIndexLength);
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
	
	protected Header readHeader() throws IOException {
        return new Header(readU29());
    }
	
	protected int readU29() throws IOException {
        // Unsigned integer encoded on 8 to 32 bits, with 7 to 29 significant bits.
        // The most significant bits are stored on the left (at the beginning).
        // The fourth byte always have 8 significant bits. 
        // 7-7-7-8  or  7-7-7   or 7-7  or 7

        int numBytes = 0;
        int result = 0;
        while (true) {
        	int b = in.readUnsignedByte();
            if (numBytes == 3) {
            	return (result << 8) | b;
            }
            result = (result << 7) | (b & 0x7F);
            if ((b & 0x7F) == b) {
            	return result;
            }
            ++numBytes;
        }
    }
	
	protected String readAsciiString(int length) throws IOException {
		StringBuilder buf = new StringBuilder();
		for(int i = 0; i < length; ++i) {
			buf.append((char)in.readUnsignedByte());
		}
		return buf.toString();
	}
	
	protected AmfArray readArray() throws IOException {
        // Stored by ref?
        Header h = readHeader();
        if(h.isReference) {
        	return (AmfArray)referenceTable.get(h.countIndexLength);
        }

        // Stored by value
        AmfArray result = new AmfArray();
        referenceTable.add(result);

        // Associative part (key-value pairs)
        while (true) {
            String key = readString();
            if (key == "") {
            	break;
            }

            AmfValue value = readValue();
            result.put(key, value);
        }

        // Dense part (consecutive indices >=0 and <count)
        for (int i = 0; i < h.countIndexLength; i++) {
            AmfValue value = readValue();
            result.add(value);
        }

        return result;
    }
	
	protected AmfDate readDate() throws IOException {
        // Stored by ref?
        Header h = readHeader();
        if(h.isReference) {
        	return (AmfDate)referenceTable.get(h.countIndexLength);
        }
        
        // Stored by value
        double elapsed = in.readDouble();
        AmfDate date = new AmfDate(elapsed);
        referenceTable.add(date);
        return date;
    }
	
	protected AmfByteArray readByteArray() throws IOException {
		// Stored by ref?
        Header h = readHeader();
        if(h.isReference) {
        	return (AmfByteArray)referenceTable.get(h.countIndexLength);
        }
        
        // Stored by value
        byte[] array = new byte[h.countIndexLength];
        in.readFully(array);
        
        AmfByteArray aba = new AmfByteArray(array);
        referenceTable.add(aba);
        return aba;
	}
	
	protected AmfDictionary readDictionary() throws IOException {
        // Stored by ref?
		Header h = readHeader();
        if(h.isReference) {
        	return (AmfDictionary)referenceTable.get(h.countIndexLength);
        }

        // Stored by value
        boolean weakKeys = in.readBoolean();
        AmfDictionary result = new AmfDictionary(weakKeys);
        referenceTable.add(result);

        for (int j = 0; j < h.countIndexLength; ++j) {
            AmfValue key = readValue();
            AmfValue value = readValue();
            result.put(key, value);
        }
        
        return result;
    }
	
	protected AmfXml readXml() throws IOException {
		return _readXml(false);
	}
	
	protected AmfXml readXmlDoc() throws IOException {
		return _readXml(true);
	}
	
	private AmfXml _readXml(boolean isDocument) throws IOException {
		// Stored by ref?
		Header h = readHeader();
	    if(h.isReference) {
	    	return (AmfXml)referenceTable.get(h.countIndexLength);
	    }

        // Stored by value
        AmfXml result = new AmfXml(isDocument);
        result.setValue(readAsciiString(h.countIndexLength));
        referenceTable.add(result);
        return result;
    }
}
