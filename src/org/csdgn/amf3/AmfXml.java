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

public class AmfXml extends AmfString {
	private boolean isXmlDocument;
	
	public AmfXml() {
		this.isXmlDocument = false;
	}

	/**
	 * Determines if this is the older XmlDocument AMF type.
	 * @return true if XmlDocument, false otherwise
	 */
	public boolean isXmlDocument() {
		return isXmlDocument;
	}

	public void setXmlDocument(boolean isXmlDocument) {
		this.isXmlDocument = isXmlDocument;
	}

	public AmfXml(boolean isXmlDocument) {
		this.isXmlDocument = isXmlDocument;
	}
	
	@Override
	public AmfType getType() {
		if(isXmlDocument) {
			return AmfType.XmlDoc;
		}
		return AmfType.Xml;
	}

	@Override
	public boolean equals(AmfValue value) {
		if(value instanceof AmfXml
		&& value.getType() == getType()) {
			AmfXml xml = (AmfXml)value;
			return xml.getValue().equals(getValue());
		}
		return false;
	}
}