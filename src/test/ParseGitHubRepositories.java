package test;

import java.io.FileReader;
import java.util.Date;

import com.google.gson.Gson;

/*
 * Created on Dec 22, 2018
 * By Kenneth Evans, Jr.
 */

class Repro
{
    String full_name;
    String name;
    String created_at;
    String updated_at;
    String pushed_at;
    String language;
    String has_downloads;
    License license;
    int size;
}

class License
{
    String name;
}

public class ParseGitHubRepositories
{
    private static String DEFAULT_IN_FILE = "C:/Scratch/AAA/github-repos-2018-12-22.json";
    private static String REPRO_COMMAND = "curl https://api.github.com/users/kennethevans/repos?per_page=1000";
    private static String inFile = DEFAULT_IN_FILE;

    public static void read1(String fileName) {
        try {
            Gson gson = new Gson();
            Repro[] results = gson.fromJson(new FileReader(DEFAULT_IN_FILE),
                Repro[].class);
            for(Repro repro : results) {
                System.out.println(repro.full_name);
                System.out.println("    name=" + repro.name);
                System.out.println("    language=" + repro.language);
                // Always true ?
                System.out.println("    has_downloads=" + repro.has_downloads);
                if(repro.license != null) {
                    System.out.println("    license=" + repro.license.name);
                } else {
                    System.out.println("    license=");
                }
                System.out.println("    created_at=" + repro.created_at);
                System.out.println("    updated_at=" + repro.updated_at);
                System.out.println("    pushed_at=" + repro.pushed_at);
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void read2(String cmd) {
        String json = null;
        try {
            json = execCmd(cmd);
        } catch(Exception ex) {
            ex.printStackTrace();
            return;
        }
        try {
            Gson gson = new Gson();
            Repro[] results = gson.fromJson(json, Repro[].class);
            for(Repro repro : results) {
                System.out.println(repro.full_name);
                System.out.println("    name=" + repro.name);
                System.out.println("    language=" + repro.language);
                // Always true ?
                System.out.println("    has_downloads=" + repro.has_downloads);
                if(repro.license != null) {
                    System.out.println("    license=" + repro.license.name);
                } else {
                    System.out.println("    license=");
                }
                System.out.println("    created_at=" + repro.created_at);
                System.out.println("    updated_at=" + repro.updated_at);
                System.out.println("    pushed_at=" + repro.pushed_at);
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String execCmd(String cmd) throws java.io.IOException {
        Process proc = Runtime.getRuntime().exec(cmd);
        java.io.InputStream is = proc.getInputStream();
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String val = "";
        if(s.hasNext()) {
            val = s.next();
        } else {
            val = "";
        }
        return val;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(inFile);
        System.out.println(new Date());
        read2(REPRO_COMMAND);
    }
}
