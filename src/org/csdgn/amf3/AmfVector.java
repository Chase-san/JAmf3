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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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
public abstract class AmfVector<E> extends AmfValue implements List<E> {
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
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append("VectorDouble{");
			boolean first = true;
			for(AmfDouble val : this) {
				if(!first) {
					buf.append(",");
				}
				buf.append(val.getValue());
			}
			buf.append("}");
			return buf.toString();
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
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append("Vector{");
			boolean first = true;
			for(AmfValue val : this) {
				if(!first) {
					buf.append(",");
				}
				buf.append(val);
			}
			buf.append("}");
			return buf.toString();
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
		
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append("VectorInt{");
			boolean first = true;
			for(AmfInteger val : this) {
				if(!first) {
					buf.append(",");
				}
				buf.append(val.getValue());
			}
			buf.append("}");
			return buf.toString();
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
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append("VectorUnsigned{");
			boolean first = true;
			for(AmfInteger val : this) {
				if(!first) {
					buf.append(",");
				}
				buf.append(val.getUnsignedValue());
			}
			buf.append("}");
			return buf.toString();
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
	 * @return Returns: true (as specified by Collection.add)
	 * 
	 * @throws UnsupportedOperationException
	 *             if the vector has a fixed length and adding this value to the
	 *             vector would cause it to exceed its capacity. See
	 *             {@link #setFixedLength(boolean)} to change this property
	 *             and/or {@link #setCapacity(int)} to change the capacity.
	 */
	@Override
	public boolean add(E value) {
		if(size() + 1 > capacity) {
			String msg = String.format("This vector is fixed length and cannot contain more than %d entries.", capacity);
			throw new UnsupportedOperationException(msg);
		}
		return list.add(value);
	}

	@Override
	public void add(int index, E element) {
		// TODO fix up for capacity!
		list.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return list.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return list.addAll(index, c);
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
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

	@Override
	public E get(int index) {
		return list.get(index);
	}

	/**
	 * Returns the capacity of this vector.
	 * 
	 * @return The capacity of this vector, or -1 if no capacity has been
	 *         specified.
	 */
	public int getCapacity() {
		return capacity;
	}

	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	/**
	 * Indicates if this vector has a fixed length.
	 * 
	 * @return true if it has a fixed length, false otherwise.
	 */
	public boolean isFixedLength() {
		return fixedLength;
	}

	@Override
	public Iterator<E> iterator() {
		return list.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		// TODO handle special add (to respect capacity)
		return list.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return list.listIterator();
	}

	@Override
	public E remove(int index) {
		return list.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		return list.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	@Override
	public E set(int index, E element) {
		return list.set(index, element);
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

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}
}
