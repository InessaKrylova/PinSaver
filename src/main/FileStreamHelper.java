package main;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class FileStreamHelper {
    /**
     * Makes a directory with the filename provided, fails if it already exists
     * TODO: allow arguments for overwrite, subtractive, and additive changes
     *
     * @param name name of the file
     */
    public static boolean makeDir(String name) {
        File file = new File(name);
        if (!file.exists()) {
            if (file.mkdir()) {
                return true;
            } else {
                System.out.println("ERROR: Failed to create directory '" + name + "', aborting.");
            }
        } else {
            System.out.println("ERROR: Directory '" + name + "' already exists, aborting.");
        }
        return false;
    }

    /**
     * Saves an image from the specified URL to the path with the name count
     *
     * @param srcUrl url of image
     * @param path path to save image (in root\board)
     * @param filename name of image
     */
    public static void saveImage(String srcUrl, String path, String filename) {
        try {
            URL url = new URL(srcUrl);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            FileOutputStream fos = new FileOutputStream(path + File.separator + cleanFilename(filename) + "." + srcUrl.substring(srcUrl.length() - 3));
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.out.println("Error saving image: ");
            e.printStackTrace();
        }
    }

    public static String cleanFilename(String name) {
        String tmp = name.replaceAll("[<>\\.\\\\:\"/\\|\\?\\*]", "");
        if (tmp.length() > 100)
            tmp = tmp.substring(0, 100);
        return tmp;
    }
}
