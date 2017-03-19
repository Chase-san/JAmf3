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

import java.time.Instant;

/**
 * Makes use of the newer Instant class instead of the legacy Date class.
 * @author Robert Maupin
 */
public class AmfDate extends AmfDouble {
	public AmfDate() {
		super(0.0);
	}
	
	public AmfDate(double value) {
		super(value);
	}

	@Override
	public AmfType getType() {
		return AmfType.Date;
	}

	public Instant getInstant() {
		return Instant.ofEpochMilli((long)(double)getValue());
	}

	public void setInstant(Instant instant) {
		if(instant == null) {
			throw new UnsupportedOperationException("Instant cannot be null.");
		}
		setValue((double)instant.toEpochMilli());
	}
}
