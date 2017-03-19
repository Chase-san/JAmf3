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

/**
 * Defines custom data capable of being stored in
 * 
 * @author Robert Maupin
 */
public interface ExternalizableFactory {
	/**
	 * Creates a new externalizable based on the name of the objects trait.
	 * 
	 * @param traitName
	 *            The name of the trait to create an externalizable for.
	 * @return The created externalizable for the given trait name, or
	 *         <code>null</code> if one could not be created. A
	 *         <code>null</code> value will cause an
	 *         {@link org.csdgn.amf3.stream.AmfInputStream} or an
	 *         {@link org.csdgn.amf3.stream.AmfOutputStream} to fail.
	 * 
	 */
	public Externalizable create(String traitName);
}
