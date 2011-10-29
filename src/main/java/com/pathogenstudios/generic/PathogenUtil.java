package com.pathogenstudios.generic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PathogenUtil {
	public static void extractResourceIfNotExist(String resourceName,String destination) {
		Log.d("Extracting resource '" + resourceName + "' to '" + destination + "'...");
		
		try {
			File f = new File(destination);
			
			if (f.exists()) {return;}
			
			InputStream inputStream = Class.class.getResourceAsStream(resourceName);
			OutputStream outputStream = new FileOutputStream(f);
			
			int read = 0;
			byte[] bytes = new byte[1024];
			
			while((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes,0,read); 
			}
			
			inputStream.close();
			outputStream.flush();
			outputStream.close();
			
			Log.m("Extracted resource '" + resourceName + "' to '" + destination + "' sucessfully.");
		}
		catch (Exception e) {
			Log.e("Could not extract resource '" + resourceName + "' to '" + destination + "'");
			e.printStackTrace();
		}
	}
}
