package misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.google.gson.Gson;

/*
 * Created on Jul 16, 2019
 * By Kenneth Evans, Jr.
 */

/**
 * MakeSimpleWeightTrackerBackup. Takes a file generated from the first two
 * columns of Weight History.ods and make a file suitable for Restore from
 * Simple Weight Tracker.
 * 
 * @author Kenneth Evans, Jr.
 */
public class MakeSimpleWeightTrackerBackup
{
    private static final String INPUT_DIR = "C:/Scratch/Weight Charts/Simple Weight Tracker";
    private static final String INPUT_FILE_NAME_PREFIX = "SWT_Calc_Data_07_17_2019";

    private static String parseFile(String fileName) {
        Settings settings = new Settings();
        List<Data> data = settings.weights;
        BufferedReader in = null;
        String inputLine;
        String[] tokens;
        boolean first = true;
        try {
            in = new BufferedReader(new FileReader(fileName));
            while((inputLine = in.readLine()) != null) {
                if(first) {
                    first = false;
                    continue;
                }
                tokens = inputLine.split(",");
                System.out
                    .println("Date=" + tokens[0] + " Weight=" + tokens[1]);
                data.add(new Data(tokens[0], tokens[1]));
            }
            in.close();
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }

        // The data in Weight Chart.ods are sorted with latest first
        // Collections.reverse(data);

        Gson gson = new Gson();
        return gson.toJson(settings);
    }

    public static void writeJsonToFile(String json, String fileName)
        throws FileNotFoundException {
        PrintWriter out = new PrintWriter(fileName);
        out.println(json);
        out.close();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            // Set window decorations
            JFrame.setDefaultLookAndFeelDecorated(true);
            // Set the native look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Throwable t) {
            t.printStackTrace();
        }

        String inFileName = INPUT_DIR + "/" + INPUT_FILE_NAME_PREFIX + ".csv";
        String outFileName = INPUT_DIR + "/" + INPUT_FILE_NAME_PREFIX + ".json";
        System.out.println("Processing " + inFileName);
        String json = parseFile(inFileName);
        if(json == null) {
            System.out.println();
            System.out.println("Aborted");
            return;
        }
        try {
            writeJsonToFile(json, outFileName);
            System.out.println();
            System.out.println("Wrote " + outFileName);
        } catch(FileNotFoundException ex) {
            ex.printStackTrace();
            System.out.println();
            System.out.println("Aborted");
            return;
        }

        System.out.println();
        System.out.println("All Done");
    }
}

class SWT
{
    List<Settings> settings = new ArrayList<>();
}

class Settings
{
    List<Parameter> settings = new ArrayList<>();
    int version = 0;
    List<Data> weights = new ArrayList<>();

    Settings() {
        settings.add(new Parameter());
    }
}

class Parameter
{
    String key = "goal";
}

class Data
{
    private static SimpleDateFormat dateFormat = new SimpleDateFormat(
        "yyyy-MM-dd");

    long date;
    double weight;

    public Data(String dateString, String weight) throws ParseException {
        date = dateFormat.parse(dateString).getTime();
        this.weight = Double.parseDouble(weight);
    }
}
