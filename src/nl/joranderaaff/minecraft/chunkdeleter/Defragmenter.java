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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Defragmenter {
	
	private static final int SECTOR_BYTES = 4096;
	private static final int CHUNK_HEADER_SIZE = 5;
	
	public void defragmentWorld(String levelPathString) {
		
		File levelPath = new File(levelPathString, "region");
		
		if(!levelPath.exists()) {
			System.out.println("This path does not exist: " + levelPath.getAbsolutePath());
		}
		
		String[] files = levelPath.list();
		
		if (files != null)
		{
		    for (int i=0; i < files.length; i++)
		    {
		        //total filepath
		        File filePath = new File(levelPath.toString(), files[i]);
		        if (filePath.isFile() && filePath.toString().endsWith("mcr")) {
		        	defragmentChunk(filePath);
		        }
		    }
		}
		System.out.println();
	}
	
	private void defragmentChunk(File regionFilePath) {
		Map<Integer, ChunkData> chunkDataMap = new HashMap<Integer, ChunkData>();
		
		try
		{
			if(regionFilePath.exists())
			{
				System.out.println("Defragmenting: " + regionFilePath.toString());
				//open the file
				RandomAccessFile file = new RandomAccessFile(regionFilePath, "rw");
				
				float originalLength = file.length();
				
				file.seek(0);
				
				int chunksFound = 0;
				//loop trough chunks
	            for (int x=0; x<32; x++) {
	            	for(int z=0; z<32; z++) {
	            		//find the chunk's position and length information
	            		int offsetPosition = 4 * (x + z * 32);
	            		
	            		file.seek(offsetPosition);
						
	            		int offset = file.readInt();
	            		//get the sectornumber and number of sectors
						int sectorNumber = offset >> 8;
	            		int numSectors = offset & 0xFF;
						//if a sectornumber is defined, we will extract the data
	            		if(sectorNumber > 0) {
	            			chunksFound ++;
	            			//seek the sector
							file.seek(sectorNumber * SECTOR_BYTES);
							//read the length
				            int length = file.readInt();
				            //safetycheck
				            if (length > SECTOR_BYTES * numSectors) {
				                System.out.println("READ " + x + " " + z + " invalid length: " + length + " > 4096 * " + numSectors);
				            }
				            //read the chunk version
				            byte version = file.readByte();
				            //read the actual chunk data
				            byte[] data = new byte[length-1];
				            file.read(data);
				            //save it in a helper class
				            ChunkData chunkData = new ChunkData(version, data);
				           //map it for later use
				            chunkDataMap.put(offsetPosition, chunkData);
						}
	            	}
	            }
	            
	            if(chunksFound > 0) {
	            	//clear the complete file of all chunk data
					file.setLength(SECTOR_BYTES * 2);
				    
				    file.seek(0);
				    //delete all chunk offset data, but perserve all timestamps
				    for (int i = 0; i < SECTOR_BYTES; i++) {
						file.writeByte(0);
					}

					//get an iterator of all chunkData helpers
				    Iterator<Integer> offsetPositionIterator = chunkDataMap.keySet().iterator();
				    
				    while(offsetPositionIterator.hasNext()) {
				    	//get the chunk's position and length information
				    	int offsetPosition = offsetPositionIterator.next();
				    	//get the chunkData helper
				    	ChunkData chunkData = chunkDataMap.get(offsetPosition);
				    	//calculate the new position for the chunkdata in the file (Allways the end of the file)
				    	int sectorNumber = (int) Math.ceil(file.length() / SECTOR_BYTES);
				    	//calculate the amount of sectors we will need
				    	int numSectors = (chunkData.data.length + CHUNK_HEADER_SIZE) / SECTOR_BYTES + 1;
				    	//go to the end of the file.
				    	file.seek(file.length());
				    	//write the chunk data length information
				    	file.writeInt(chunkData.data.length + 1);
				    	//write the version
				    	file.writeByte(chunkData.version);
				    	//write the chunk data
				    	file.write(chunkData.data);
				    	//move back to the chunk's position and length information
				    	file.seek(offsetPosition);
				    	//write the new information: sectornumber and number of sectors
				    	file.writeInt((sectorNumber << 8) + (numSectors & 0xFF));
				    	//make sure the file's length is a multiple if 4kB
				    	file.setLength((long) ((sectorNumber + numSectors) * SECTOR_BYTES));
				    }
				    
				    float newLength = file.length();
				    float percentage = Math.round(newLength / originalLength * 10000) / 100;
				    System.out.println("Downsized region file from " + (long) originalLength + " bytes to " + (long) newLength + " bytes (" + percentage + "%)");
				    //close the file
				    file.close();
	            } else {
	            	System.out.println("Region file contains no chunks. Deleted.");
	            	//if no chunks were found, we can close and delete this file
	            	file.close();
					regionFilePath.delete();
	            }	            
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class ChunkData {
		public byte version;
		public byte[] data;
		
		public ChunkData(byte versionIn, byte[] dataIn) {
			data = dataIn;
			version = versionIn;
		}
	}
}
