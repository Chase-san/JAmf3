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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Robert Maupin
 *
 */
public class AmfArray extends AmfValue {
	private Map<String, AmfValue> associative;
	private List<AmfValue> dense;

	public AmfArray() {
		dense = new ArrayList<AmfValue>();
		associative = new HashMap<String, AmfValue>();
	}
	
	public void add(AmfValue value) {
		dense.add(value);
	}

	public void clear() {
		dense.clear();
		associative.clear();
	}

	public AmfValue get(int index) {
		return dense.get(index);
	}

	public AmfValue get(String key) {
		return associative.get(key);
	}

	public int getAssociativeSize() {
		return associative.size();
	}
	
	public int getDenseSize() {
		return dense.size();
	}
	
	@Override
	public AmfType getType() {
		return AmfType.Array;
	}
	
	public Set<String> keySet() {
		return associative.keySet();
	}
	
	public AmfValue put(String key, AmfValue value) {
		return associative.put(key, value);
	}
	
	public AmfValue remove(int index) {
		return dense.remove(index);
	}
	
	public AmfValue remove(String key) {
		return associative.remove(key);
	}
	
	public int size() {
		return dense.size() + associative.size();
	}

	@Override
	public boolean equals(AmfValue value) {
		if(value instanceof AmfArray) {
			AmfArray arr = (AmfArray)value;
			if(arr.dense.size() == dense.size()
			&& arr.associative.size() == associative.size()) {
				//check dense
				for(int i = 0; i < dense.size(); ++i) {
					if(!arr.dense.get(i).equals(dense.get(i))) {
						return false;
					}
				}
				return arr.associative.equals(associative);
			}
		}
		return false;
	}
}