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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.mojang.RegionFile;
import com.mojang.Tag;

public class Deleter
{
	private Map<String, Boolean> markedSafe;

	public void deleteChunks(String levelPathString, String[] blockTypes, int border, int minY, int maxY, int radius)
	{
		markedSafe = new HashMap<String, Boolean>();

		HashMap<Integer, Boolean> blockTypesMap = new HashMap<Integer, Boolean>();

		int blockTypesCount = 0;
		for (int i = 0; i < blockTypes.length; i++)
		{
			int blockType = Integer.parseInt(blockTypes[i]);
			blockTypesMap.put(blockType, true);
			blockTypesCount ++;
		}

		if(blockTypesCount == 0) {
			System.out.println("No valid blocktypes given.");
			return;
		}

		ArrayList<RegionFileInfo> regionFileInfos = new ArrayList<RegionFileInfo>();

		System.out.println("Looking for blocktypes: " + blockTypesMap);
		System.out.println("Border to keep around safe chunks: " + border);

		File levelPath = new File(levelPathString, "region");

		if(!levelPath.exists()) {
			System.out.println("This path does not exist: " + levelPath.getAbsolutePath());
		}

		String[] files = levelPath.list();

		if (files != null)
		{
		    for (int i=0; i < files.length; i++)
		    {
		        // Get filename of file or directory
		    	String fileName = files[i];
		    	//total filepath
		        File filePath = new File(levelPath.toString(), files[i]);
		        if (filePath.isFile()) {
		        	final String pathname = filePath.toString();
		        	if (pathname.endsWith(".mcr") || pathname.endsWith(".mca")) {
			        	// Get filename parts
			        	String[] fileNameParts = fileName.split("\\.");
			        	// Get region coordinates from filename
		        		int regionX = Integer.parseInt(fileNameParts[1]);
		        		int regionZ = Integer.parseInt(fileNameParts[2]);
			        	RegionFileInfo info = new RegionFileInfo(filePath, regionX, regionZ);
				        regionFileInfos.add(info);
		        	}
		        }
		    }
		}

		System.out.println("Search between Y: " + minY + " and " + maxY);

		//cap these values!
		minY = Math.max(minY, 0);
		maxY = Math.min(maxY, 127);

		System.out.println("-----------------------------------");

		for (int i = 0; i < regionFileInfos.size(); i++) {
			RegionFileInfo info = regionFileInfos.get(i);
	        System.out.println("Searching blocks in " + info.filePath.toString());
	        //find 'human active' chunks and store them in
	        markSafeChunks(info.filePath, info.regionX, info.regionZ, border, minY, maxY, blockTypesMap);
		}

		System.out.println("-----------------------------------");

		for (int i = 0; i < regionFileInfos.size(); i++) {
			RegionFileInfo info = regionFileInfos.get(i);
	        System.out.println("Deleting chunks in " + info.filePath.toString());
	        //find 'human active' chunks and store them in
	        deleteUnsavedChunks(info.filePath, info.regionX, info.regionZ);
		}
	}

	private void deleteUnsavedChunks(File filePath, int regionX, int regionZ) {
		RegionFile regionFile = new RegionFile(filePath);
		int chunksDeleted = 0;
		for (int x = 0; x < 32; x++)
		{
			for (int z = 0; z < 32; z++)
			{
				if(regionFile.hasChunk(x, z))
				{
					int chunkX = (regionX * 32 + x);
					int chunkZ = (regionZ * 32 + z);

					String id = chunkX + "_" + chunkZ;

					if(!markedSafe.containsKey(id)) {
						chunksDeleted++;
						regionFile.deleteChunck(x, z);
					}
				}
 			}
		}

		System.out.println("Chunks deleted: " + chunksDeleted);

		try {
			regionFile.close();
		} catch (IOException e) {
			System.out.println("error while closing region file");
		}
	}

	private void markSafeChunks(File filePath, int regionX, int regionZ, int border, int minY, int maxY, HashMap<Integer, Boolean> blockTypes)
	{
		RegionFile regionFile = new RegionFile(filePath);
		Boolean validBlockFound;
		for (int x = 0; x < 32; x++)
		{
			for (int z = 0; z < 32; z++)
			{
				if (regionFile.hasChunk(x, z))
				{
					int chunkX = (regionX * 32 + x);
					int chunkZ = (regionZ * 32 + z);

					validBlockFound = false;

					try
					{
						Tag tag = Tag.readFrom(regionFile.getChunkDataInputStream(x, z));
						Tag levelData = tag.findTagByName("Level");
						Tag sections = levelData.findTagByName("Sections");
						Tag[] blocks = (Tag[])sections.getValue();
						
						for (Tag block : blocks)
						{
							int sy = (byte)block.findTagByName("Y").getValue();

							byte[] blockIDs = (byte[]) block.findTagByName("Blocks").getValue();

							for (int bx = 0; bx < 16; bx++)
							{
								for (int bz = 0; bz < 16; bz ++)
								{
									for (int by = 0; by < 16; by++)
									{
										int realy = (sy * 16) + by;
										if ((minY <= realy) && (realy <= maxY))
										{
											// YZX coordinates
											int blockIndex = bx + ( bz * 16 + ( by * 16 * 16 ) );
											byte blockID = blockIDs[blockIndex];
											int blockIDInt = (int)(blockID & 0xFF);
		
											if (blockTypes.containsKey(blockIDInt))
											{
												for (int surX = chunkX - border; surX <= chunkX + border; surX++)
												{
													for (int surZ = chunkZ - border; surZ <= chunkZ + border; surZ++)
													{
														String id = surX + "_" + surZ;
														if (!markedSafe.containsKey(id))
															markedSafe.put(id, true);
													}
												}
												validBlockFound = true;
												break;
											}
										}
									}
									if(validBlockFound)
										break;
								}
								if(validBlockFound)
									break;
							}
							if(validBlockFound)
								break;
						}
					}
					catch (IOException e)
					{
						System.out.println("error while reading Tag");
					}
				}
 			}
		}
		try {
			regionFile.close();
		} catch (IOException e) {
			System.out.println("error while closing region file");
		}
	}

	private class RegionFileInfo {
		public File filePath;
		public int regionX;
		public int regionZ;

		public RegionFileInfo(File filePath, int regionX, int regionZ) {
			this.filePath = filePath;
			this.regionX = regionX;
			this.regionZ = regionZ;
		}
	}
}
