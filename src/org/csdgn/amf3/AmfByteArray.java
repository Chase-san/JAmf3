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

import java.util.Arrays;

/**
 * 
 * @author Robert Maupin
 *
 */
public class AmfByteArray extends AmfPrimitive<byte[]> {
	protected AmfByteArray(byte[] value) {
		super(value);
	}

	@Override
	public AmfType getType() {
		return AmfType.ByteArray;
	}
	
	public void push(byte value) {
		byte[] data = getValue();
		byte[] newData = Arrays.copyOf(data, data.length + 1);
		newData[data.length] = value;
		setValue(newData);
	}
	
	public void push(byte[] value) {
		byte[] data = getValue();
		byte[] newData = Arrays.copyOf(data, data.length + value.length);
		System.arraycopy(value, 0, newData, data.length, value.length);
		setValue(newData);
	}
	
	public byte pop() {
		byte[] data = getValue();
		if(data.length == 0) {
			throw new UnsupportedOperationException("Cannot pop byte from zero length byte array.");
		}
		byte[] newData = Arrays.copyOf(data, data.length - 1);
		setValue(newData);
		return data[newData.length];
	}
	
	public byte[] pop(int length) {
		byte[] data = getValue();
		if(length > data.length) {
			String msg = "Cannot pop a number of bytes greater than the length of the byte array.";
			throw new UnsupportedOperationException(msg);
		}
		byte[] newData = Arrays.copyOf(data, data.length - length);
		setValue(newData);
		return Arrays.copyOfRange(data, data.length - length - 1, length);
	}
	
	public boolean equals(AmfValue value) {
		if(value instanceof AmfByteArray) {
			AmfByteArray barr = (AmfByteArray)value;
			return Arrays.equals(barr.getValue(), getValue());
		}
		
		return false;
	}
}