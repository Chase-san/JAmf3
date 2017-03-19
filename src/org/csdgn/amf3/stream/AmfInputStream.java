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
import java.util.zip.DataFormatException;
import org.csdgn.amf3.AmfValue;
import org.csdgn.amf3.ExternalizableFactory;

/**
 * Reads an Amf File or Stream from the given input stream.
 * @author Robert Maupin
 */
public class AmfInputStream implements Closeable, AutoCloseable {
	private List<ExternalizableFactory> factories;
	private DataInputStream in;
	private String name;
	private boolean started;
	
	/**
	 * Creates an AmfInputStream with the given InputStream as input.
	 * @param in the InputStream to read from.
	 */
	protected AmfInputStream(InputStream in) {
		this(in, true);
	}
	
	/**
	 * Creates an AmfInputStream with the given InputStream as input.
	 * @param in the InputStream to read from.
	 * @param file if the stream is reading from a SOL formatted file.
	 */
	protected AmfInputStream(InputStream in, boolean file) {
		if(!(in instanceof BufferedInputStream)) {
			in = new BufferedInputStream(in);
		}
		this.in = new DataInputStream(in);
		factories = new ArrayList<ExternalizableFactory>();
		name = null;
		started = !file;
	}
	
	public void addExternalizableFactory(ExternalizableFactory factory) {
		factories.add(factory);
	}
	
	@Override
	public void close() throws IOException {
		in.close();
	}
	
	public ExternalizableFactory[] getExternalizableFactories() {
		return factories.toArray(new ExternalizableFactory[factories.size()]);
	}
	
	/**
	 * Gets the name stored in the amf file.
	 * @return the file name, or null if the stream does not represent a file.
	 * @throws DataFormatException 
	 * @throws IOException 
	 */
	public String getName() throws IOException, DataFormatException {
		if(!started) {
			readFileHeader();
		}
		return name;
	}
	
	public boolean hasNext() throws IOException, DataFormatException {
		if(!started) {
			readFileHeader();
		}
		return false;
	}
	
	public String nextKey() throws IOException, DataFormatException {
		if(!started) {
			readFileHeader();
		}
		
		return null;
	}
	
	public AmfValue nextValue() throws IOException, DataFormatException {
		if(!started) {
			readFileHeader();
		}
		return null;
	}
	
	private void readFileHeader() throws IOException, DataFormatException {
		//calls are evaluated in left to right order
		if(in.readUnsignedByte() != 0x0 || in.readUnsignedByte() != 0xBF) {
			throw new DataFormatException("Unknown Endianness");
		}
		
		// Size
        int size = in.readInt();
        //TODO
        //if (size + 6 != _reader.BaseStream.Length) throw new InvalidOperationException("Wrong file size");

        // Magic signature
        String magic = readString(4);
        if (!"TCSO".equals(magic)) {
        	throw new DataFormatException("Wrong file tag");
        }
        in.skipBytes(6);

        // Read name
        size = in.readUnsignedShort();
        name = readString(size);

        // Version
        int version = (int)in.readInt();
        if (version < 3) {
        	throw new DataFormatException("Wrong AMF version");
        }
	}
	
	protected String readString(int length) throws IOException {
		StringBuilder buf = new StringBuilder();
		for(int i = 0; i < length; ++i) {
			buf.append((char)in.readUnsignedByte());
		}
		return buf.toString();
	}
	
	public void removeExternalizableFactory(ExternalizableFactory factory) {
		factories.remove(factory);
	}
	
	public void skipNext() throws IOException, DataFormatException {
		if(!started) {
			readFileHeader();
		}
	}

}
