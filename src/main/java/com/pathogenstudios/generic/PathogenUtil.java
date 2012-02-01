package com.pathogenstudios.generic;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PathogenUtil {
    public static void extractResourceIfNotExist(String resourceName, String destination) {
        Log.d("Extracting resource '" + resourceName + "' to '" + destination + "'...");

        try {
            File f = new File(destination);

            Log.d("" + f.length());

            if (f.exists() && (f.length() > 0)) {
                return;
            }

            new File(f.getParent()).mkdirs();    // Make the directory if it doesn't exist.
            f.createNewFile();

            InputStream inputStream = PathogenUtil.class.getClassLoader().getResourceAsStream("lang/" + resourceName);

            if (inputStream == null) {
                Log.e("Resource '" + resourceName + "' was not found!");
            }

            OutputStream outputStream = new FileOutputStream(f);
            int          read         = 0;
            byte[]       bytes        = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

            inputStream.close();
            outputStream.flush();
            outputStream.close();
            Log.m("Extracted resource '" + resourceName + "' to '" + destination + "' sucessfully.");
        } catch (Exception e) {
            Log.e("Could not extract resource '" + resourceName + "' to '" + destination + "'");
            e.printStackTrace();
        }
    }
}
