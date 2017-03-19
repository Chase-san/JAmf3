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

import java.util.ArrayList;
import java.util.List;

public abstract class AmfVector<E> extends AmfValue {
	public static class Double extends AmfVector<AmfDouble> {
		@Override
		public AmfType getType() {
			return AmfType.VectorDouble;
		}
	}

	public static class Generic extends AmfVector<AmfValue> {
		private String typeName;
		
		public Generic() {
			typeName = "*";
		}
		
		public Generic(String type) {
			typeName = type;
		}
		
		@Override
		public AmfType getType() {
			return AmfType.VectorGeneric;
		}

		public String getTypeName() {
			return typeName;
		}

		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}
	}

	public static class Integer extends AmfVector<AmfInteger> {
		@Override
		public AmfType getType() {
			return AmfType.VectorInt;
		}
	}

	public static class UnsignedInteger extends AmfVector<AmfInteger> {
		@Override
		public AmfType getType() {
			return AmfType.VectorUInt;
		}
	}

	private int capacity;
	private boolean fixedLength;
	private List<E> list;

	/**
	 * Constructs a non-fixed length vector.
	 */
	public AmfVector() {
		list = new ArrayList<E>();
		capacity = -1;
		fixedLength = false;
	}

	/**
	 * Constructs a fixed length vector of the given size.
	 * 
	 * @param size
	 *            the size of the vector.
	 */
	public AmfVector(int size) {
		list = new ArrayList<E>(size);
		capacity = size;
	}
	
	public void add(E value) {
		if(size() + 1 > capacity) {
			String msg = String.format("This vector is fixed length and cannot contain more than %d entries.",
					capacity);
			throw new UnsupportedOperationException(msg);
		}
		list.add(value);
	}
	
	@Override
	public boolean equals(AmfValue value) {
		if(value.getType() == getType()) {
			AmfVector<?> vec = (AmfVector<?>)value;
			if(vec.size() == this.size()
			&& vec.isFixedLength() == isFixedLength()) {
				//check if all the entries match
				for(int i = 0; i < size(); ++i) {
					if(!vec.get(i).equals(get(i))) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	public E get(int index) {
		return list.get(index);
	}

	public boolean isFixedLength() {
		return fixedLength;
	}

	public E remove(int index) {
		return list.remove(index);
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public void setFixedLength(boolean fixedLength) {
		this.fixedLength = fixedLength;
		if(fixedLength && capacity < 0) {
			capacity = list.size();
		}
	}
	
	public int size() {
		return list.size();
	}
}
