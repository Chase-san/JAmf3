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
package org.csdgn.amf3.test;

import java.io.FileInputStream;
import java.io.InputStream;
import org.csdgn.amf3.OldAmfFile;
import org.csdgn.amf3.OldAmfReader;

/**
 * @author Robert Maupin
 *
 */
public class AmfTest {
	public static void main(String[] args) throws Exception {
		//fail("Not yet implemented");
		String filename = "C:/Users/chase/AppData/Roaming/Macromedia/Flash Player/#SharedObjects/QCUXB42F/localhost/TiTs_1.sol";
		try(InputStream in = new FileInputStream(filename)) {
			OldAmfReader reader = new OldAmfReader(in);
			OldAmfFile file = reader.readFile();
			
		}
	}

}
