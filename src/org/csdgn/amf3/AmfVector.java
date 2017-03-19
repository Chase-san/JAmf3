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

/**
 * Associated with the AMF undefined type. A AmfVector is a dense array of
 * values of the same type (similar to a Java List). This class is the vectors
 * base type. There are several specializations of the vector for integers,
 * unsigned integers, doubles and a general type for any kind of AmfValue.
 * 
 * @author Robert Maupin
 *
 * @param <E>
 *            The type this AmfVector is specialized for.
 */
public abstract class AmfVector<E> extends AmfValue {
	/**
	 * A specialized version of the AmfVector for Double values.
	 * 
	 * @author Robert Maupin
	 * @see AmfVector
	 */
	public static class Double extends AmfVector<AmfDouble> {
		@Override
		public AmfType getType() {
			return AmfType.VectorDouble;
		}
	}

	/**
	 * A version of the AmfVector for general values.
	 * 
	 * @author Robert Maupin
	 * @see AmfVector
	 */
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

	/**
	 * A specialized version of the AmfVector for Integer values.
	 * 
	 * @author Robert Maupin
	 * @see AmfVector
	 */
	public static class Integer extends AmfVector<AmfInteger> {
		@Override
		public AmfType getType() {
			return AmfType.VectorInt;
		}
	}

	/**
	 * A specialized version of the AmfVector for Unsigned Integer values.
	 * 
	 * @author Robert Maupin
	 * @see AmfVector
	 */
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
	 * Constructs a fixed length vector of the specified size.
	 * 
	 * @param size
	 *            the size of the vector.
	 */
	public AmfVector(int size) {
		list = new ArrayList<E>(size);
		capacity = size;
	}

	/**
	 * Appends the specified value to the end of this vector.
	 * 
	 * @param value
	 *            The value to add.
	 * @throws UnsupportedOperationException
	 *             if the vector has a fixed length and adding this value to the
	 *             vector would cause it to exceed its capacity. See
	 *             {@link #setFixedLength(boolean)} to change this property
	 *             and/or {@link #setCapacity(int)} to change the capacity.
	 */
	public void add(E value) {
		if(size() + 1 > capacity) {
			String msg = String.format("This vector is fixed length and cannot contain more than %d entries.", capacity);
			throw new UnsupportedOperationException(msg);
		}
		list.add(value);
	}

	@Override
	public boolean equals(AmfValue value) {
		if(value.getType() == getType()) {
			AmfVector<?> vec = (AmfVector<?>) value;
			if(vec.size() == this.size() && vec.isFixedLength() == isFixedLength()) {
				// check if all the entries match
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

	/**
	 * Returns the element at the specified position in this vector.
	 * 
	 * @param index
	 *            The index to get the value from.
	 * @return The value associated with the specified index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range (index < 0 || index >= size())
	 */
	public E get(int index) {
		return list.get(index);
	}

	/**
	 * Returns the capacity of this vector.
	 * 
	 * @param capacity
	 * @return The capacity of this vector, or -1 if no capacity has been
	 *         specified.
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * Indicates if this vector has a fixed length.
	 * 
	 * @return true if it has a fixed length, false otherwise.
	 */
	public boolean isFixedLength() {
		return fixedLength;
	}

	/**
	 * Removes the element at the specified position in this vector.
	 * 
	 * @param index
	 *            Returns the element at the specified position in this
	 * @return the element previously at the specified position
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range (index < 0 || index >= size())
	 */
	public E remove(int index) {
		return list.remove(index);
	}

	/**
	 * Sets the capacity of this vector to the specified value. The capacity
	 * will not be used unless {@link #isFixedLength()} returns true.
	 * 
	 * @param capacity
	 *            The capacity to set this vector to have.
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	/**
	 * Sets this vector to be fixed length or not. If being changed to fixed
	 * length and the vector is larger than the currently set capacity, the
	 * capacity will be set to equal the size of the vector.
	 * 
	 * @param fixedLength
	 *            If true, sets this vector to be fixed length, sets this vector
	 *            to have a dynamic length otherwise.
	 */
	public void setFixedLength(boolean fixedLength) {
		this.fixedLength = fixedLength;
		if(fixedLength && capacity < 0) {
			capacity = list.size();
		}
	}

	/**
	 * Gets the size of this vector.
	 * 
	 * @return the vector's size
	 */
	public int size() {
		return list.size();
	}
}
