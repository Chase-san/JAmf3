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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Robert Maupin
 *
 */
public class AmfDictionary extends AmfValue {
	private Map<AmfValue, AmfValue> map;
	private boolean weakKeys;
	
	public AmfDictionary() {
		map = new HashMap<AmfValue, AmfValue>();
		setWeakKeys(false);
	}
	
	public AmfDictionary(boolean weakKeys) {
		this();
		this.setWeakKeys(weakKeys);
	}
	
	public void clear() {
		map.clear();
	}

	public AmfValue get(AmfValue key) {
		return map.get(key);
	}

	@Override
	public AmfType getType() {
		return AmfType.Dictionary;
	}
	
	public boolean hasWeakKeys() {
		return weakKeys;
	}

	public Set<AmfValue> keySet() {
		return map.keySet();
	}
	
	public AmfValue put(AmfValue key, AmfValue value) {
		return map.put(key, value);
	}
	
	public AmfValue remove(AmfValue key) {
		return map.remove(key);
	}
	
	public void setWeakKeys(boolean weakKeys) {
		this.weakKeys = weakKeys;
	}
	
	public int size() {
		return map.size();
	}

	@Override
	public boolean equals(AmfValue value) {
		if(value instanceof AmfDictionary) {
			AmfDictionary dict = (AmfDictionary)value;
			if(dict.hasWeakKeys() == hasWeakKeys()) {
				return map.equals(dict.map);
			}
		}
		return false;
	}
}