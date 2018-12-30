package misc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/*
 * Created on Dec 27, 2018
 * By Kenneth Evans, Jr.
 */

public class MakeItemsForPhotoSwipe
{
    public static final String LS = System.getProperty("line.separator");
    private static final String[] IMAGE_FILE_EXTENSIONS = new String[] {"jpg",
        "jpeg", "png", "gif"};
    /** Directory from which the script is being run. */
    private static final String DEFAULT_PARENT_DIR = "C:/Users/evans/Documents/Web Pages/kenevans.net/Digital Art";
    // private static final String DEFAULT_PARENT_DIR = "C:/Scratch/AAA/Image
    // Gallery Test/Test Site/gallery";
    /** Directory where the images are. */
    private static final String DEFAULT_DIR = DEFAULT_PARENT_DIR + "/images";
    private static final String DEFAULT_RESCALE_EXT = ".jpg";
    private static final String DEFAULT_RESCALE_SUFFIX = "_M";
    /** Directory from which the script is being run. */
    private static final String RESCALE_SUFFIX = DEFAULT_RESCALE_SUFFIX
        + DEFAULT_RESCALE_EXT;

    private static String getItems() throws IOException {
        List<Item> items = new ArrayList<Item>();
        File dir = new File(DEFAULT_DIR);
        File[] files = dir.listFiles();
        Arrays.sort(files,
            Comparator.comparingLong(File::lastModified).reversed());
        BufferedImage bi;
        int width, height;
        Item item;
        String relativePath;
        String fileNameNoExt;
        boolean skip = true;
        for(File file : files) {
            skip = true;
            if(file.isDirectory()) continue;
            // Don't do thumbnails
            if(file.getName().endsWith(RESCALE_SUFFIX)) continue;
            for(String ext : IMAGE_FILE_EXTENSIONS) {
                if(file.getName().toLowerCase().endsWith(ext)) {
                    skip = false;
                    continue;
                }
            }
            if(skip) continue;
            // Is an image file
            bi = ImageIO.read(file);
            width = bi.getWidth();
            height = bi.getHeight();
            relativePath = new File(DEFAULT_PARENT_DIR).toURI()
                .relativize(file.toURI()).getPath();
            fileNameNoExt = file.getName();
            int pos = fileNameNoExt.lastIndexOf(".");
            if(pos > 0 && pos < (fileNameNoExt.length() - 1)) {
                // '.' is not the first or last character
                fileNameNoExt = fileNameNoExt.substring(0, pos);
            }
            item = new Item();
            item.src = relativePath;
            item.h = height;
            item.w = width;
            item.title = fileNameNoExt;
            items.add(item);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return "var items =" + LS + gson.toJson(items);
    }

    public static void main(String[] args) {
        System.out.println("Processing: " + DEFAULT_DIR);
        System.out.println();
        try {
            System.out.println(getItems());
        } catch(Exception ex) {
            ex.printStackTrace();
            System.out.println();
            System.out.println("Aborting");
            return;
        }
        System.out.println();
        System.out.println("All done");
    }
}

class Item
{
    String src;
    int w;
    int h;
    String title;

}
