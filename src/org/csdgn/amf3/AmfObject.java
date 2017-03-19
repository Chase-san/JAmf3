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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AmfObject extends AmfValue {
	private Map<String, AmfValue> dynamicMap;
	private Externalizable customData;
	private boolean isDynamic;
	private boolean isExternalizable;
	private Map<String, AmfValue> sealedMap;
	private String traitName;

	public AmfObject() {
		isDynamic = false;
		isExternalizable = true;
		traitName = "";
		sealedMap = new LinkedHashMap<String, AmfValue>();
		dynamicMap = new LinkedHashMap<String, AmfValue>();
	}

	@Override
	public boolean equals(AmfValue value) {
		if(value instanceof AmfObject) {
			AmfObject obj = (AmfObject)value;
			if(obj.isDynamic != isDynamic
			&& obj.isExternalizable != isExternalizable
			&& obj.customData != customData
			&& !traitName.equals(traitName)) {
				return false;
			}
			return obj.sealedMap.equals(sealedMap)
				&& obj.dynamicMap.equals(dynamicMap);
		}
		return false;
	}

	/**
	 * Gets the dynamic map associated with this object. If the object is not
	 * dynamic, the map will be empty. Likewise adding to the map will make the
	 * object dynamic.
	 * 
	 * @return
	 */
	public Map<String, AmfValue> getDynamicMap() {
		return dynamicMap;
	}

	public Externalizable getExternalizableObject() {
		return this.customData;
	}

	public Map<String, AmfValue> getSealedMap() {
		return sealedMap;
	}

	/**
	 * The trait generated is backed by this object and changes in this object
	 * will be reflected in the trait.
	 * 
	 * @return the trait associated with this map.
	 */
	public Trait getTrait() {
		return new Trait() {
			@Override
			public String getName() {
				return traitName;
			}

			@Override
			public List<String> getProperties() {
				ArrayList<String> list = new ArrayList<String>();
				list.addAll(sealedMap.keySet());
				return list;
			}

			@Override
			public boolean isDynamic() {
				return isDynamic;
			}

			@Override
			public boolean isExternalizable() {
				return isExternalizable;
			}
		};
	}

	public String getTraitName() {
		return traitName;
	}

	@Override
	public AmfType getType() {
		return AmfType.Object;
	}

	public boolean isDynamic() {
		return isDynamic;
	}

	public boolean isExternalizable() {
		return isExternalizable;
	}

	/**
	 * Determines if this object is dynamic and if the dynamic section will
	 * be written when writing to an AMF.
	 * @param isDynamic
	 */
	public void setDynamic(boolean isDynamic) {
		this.isDynamic = isDynamic;
	}

	/**
	 * Determines if this object has externalizable data and if the externalizable
	 * data will be written when writing to an AMF. If the ExternalizableObject is
	 * null this value will be treated as false when writing, regardless of its actual
	 * value.
	 * @param isExternalizable true to write externalizable data, false otherwise
	 */
	public void setExternalizable(boolean isExternalizable) {
		this.isExternalizable = isExternalizable;
	}

	public void setExternalizableObject(Externalizable ext) {
		this.customData = ext;
	}

	public void setTraitName(String traitName) {
		if(traitName == null) {
			throw new IllegalArgumentException("Trait Name cannot be null.");
		}
		this.traitName = traitName;
	}
}
