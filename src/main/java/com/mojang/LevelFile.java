package com.mojang;

import java.io.*;
import java.util.zip.*;

/**
 */
public class LevelFile
{
	public Integer x;
	public Integer z;

	/**
	 */
    public LevelFile(File path)
    {
    	try {
            final DataInputStream dis = new DataInputStream(new GZIPInputStream(new FileInputStream(path)));
            try {
            	final Tag root = Tag.readFrom(dis);
            	final Tag spawnX = root.findTagByName("SpawnX");
            	final Tag spawnZ = root.findTagByName("SpawnZ");
            	x = new Integer((int)spawnX.getValue());
            	z = new Integer((int)spawnZ.getValue());
            }
            catch (IOException ex) {
            	ex.printStackTrace();
            }
            finally {
            	dis.close();
            }
        }
    	catch (IOException e) {
            e.printStackTrace();
        }
    	System.out.println("Spawn (" + x + ", " + z + ")");
    }
}