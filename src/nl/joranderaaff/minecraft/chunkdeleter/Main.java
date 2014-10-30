/*

Copyright (c) 2011 Joran de Raaff, www.joranderaaff.nl

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

*/

package nl.joranderaaff.minecraft.chunkdeleter;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.UnknownOptionException;


public class Main {
	public static void main(String [ ] args)
	{
		Main main = new Main();
		main.start(args);
	}

	public void start(String[] args) {
		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option borderArg = parser.addIntegerOption("border");
		CmdLineParser.Option minYArg = parser.addIntegerOption("minY");
		CmdLineParser.Option maxYArg = parser.addIntegerOption("maxY");
		CmdLineParser.Option blockIDsArg = parser.addStringOption("blockIDs");
		CmdLineParser.Option worldFolderArg = parser.addStringOption("worldFolder");
		CmdLineParser.Option defragmentArg = parser.addBooleanOption("defragment");
		
		try {
			parser.parse(args);
		} catch (IllegalOptionValueException e) {
			System.out.println("one of the arguments passed is not of the correct type");
		} catch (UnknownOptionException e) {
			System.out.println("There was an error parsing the arguments");
		}
		
		//get the level path string
		String levelPathString = (String) parser.getOptionValue(worldFolderArg);
		//get the blocktypes
		String blockIDString = (String)parser.getOptionValue(blockIDsArg);
		
		if(blockIDString == null) {
			System.out.println("There are no blocktypes found in the arguments");
			return;
		}
		
		String[] blockIDs = blockIDString.split(",");
		
		//get the border around each safe block
		int border = (Integer) parser.getOptionValue(borderArg, new Integer(1));
		//get the min and max Y
		int minY = (Integer) parser.getOptionValue(minYArg, new Integer(0));
		int maxY = (Integer) parser.getOptionValue(maxYArg, new Integer(127));
		
		//The actual deletion takes place here.
		Deleter chunkDeleter = new Deleter();
		chunkDeleter.deleteChunks(levelPathString, blockIDs, border, minY, maxY);
		
		//defragmentation of the files. this is still experimental
		Boolean defragment = (Boolean) parser.getOptionValue(defragmentArg, false);
		
		if(defragment) {
			System.out.println("-----------------------------------");
			Defragmenter defragmenter = new Defragmenter();
			defragmenter.defragmentWorld(levelPathString);
		}
	}
}
