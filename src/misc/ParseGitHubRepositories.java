package misc;

import java.io.FileReader;
import java.util.Date;

import com.google.gson.Gson;

/*
 * Created on Dec 22, 2018
 * By Kenneth Evans, Jr.
 */

class Repro
{
    int id;
    String full_name;
    String name;
    String description;
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

class Release
{
    String name;
    String tag_name;
    String created_at;
    String published_at;
    String body;
}

class Message
{
    String message;
}

class RateLimit
{
    Rate rate;
}

class Rate
{
    int limit;
    int remaining;
    int reset;
}

public class ParseGitHubRepositories
{
    private static String DEFAULT_IN_FILE = "C:/Scratch/AAA/github-repos-2018-12-22.json";
    private static String REPRO_COMMAND = "curl https://api.github.com/users/kennethevans/repos?per_page=1000";
    private static String RELEASE_COMMAND_PRE = "curl https://api.github.com/repos/kennethevans/";
    private static String RELEASE_CMDOMMAND_POST = "/releases?per_page=1000";
    private static String RATE_LIMIT_COMMAND = "curl https://api.github.com/rate_limit";
    private static String inFile = DEFAULT_IN_FILE;

    public static void listRepositories() {
        String json = null;
        try {
            json = execCmd(REPRO_COMMAND);
        } catch(Exception ex) {
            ex.printStackTrace();
            return;
        }
        Message message = getMessage(json);
        if(message != null) {
            System.out
                .println("listRepositories received:\n  " + message.message);
            return;
        }
        try {
            Gson gson = new Gson();
            Repro[] results = gson.fromJson(json, Repro[].class);
            Release[] releases;
            for(Repro repro : results) {
                System.out.println(repro.full_name);
                System.out.println("    name=" + repro.name);
                System.out.println("    description=" + repro.description);
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
                System.out.println("    size=" + repro.size);
                releases = getRelease(repro.name);
                if(releases != null || releases.length == 0) {
                    System.out.println("    No releases");
                } else {
                    System.out.println("    Releases");
                    for(Release release : releases) {
                        System.out.println("      " + release.name);
                        System.out.println(
                            "            tag_name=" + release.tag_name);
                        System.out.println("          body=" + release.body);
                        System.out.println(
                            "           created_at=" + release.created_at);
                        System.out.println(
                            "           published_at=" + release.published_at);
                    }
                }
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Release[] getRelease(String repoName) {
        Gson gson = new Gson();
        String cmd = RELEASE_COMMAND_PRE + repoName + RELEASE_CMDOMMAND_POST;
        String json;
        try {
            json = execCmd(cmd);
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
        Message message = getMessage(json);
        if(message == null) {
            return gson.fromJson(json, Release[].class);
        } else {
            System.out.println("getRelease received:\n  " + message.message);
            return null;
        }
    }

    public static void listRelease(String repoName) {
        Release[] releases = getRelease(repoName);
        if(releases == null) {
            return;
        }
        for(Release release : releases) {
            System.out.println("name=" + release.name);
            System.out.println("    tag_name=" + release.tag_name);
            System.out.println("    body=" + release.body);
            System.out.println("    created_at=" + release.created_at);
            System.out.println("    published_at=" + release.published_at);
        }
    }

    public static void readFile(String fileName) {
        try {
            Gson gson = new Gson();
            Repro[] results = gson.fromJson(new FileReader(DEFAULT_IN_FILE),
                Repro[].class);
            for(Repro repro : results) {
                System.out.println(repro.full_name);
                System.out.println("    name=" + repro.name);
                System.out.println("    description=" + repro.description);
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
                System.out.println("    size=" + repro.size);
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String execCmd(String cmd) throws java.io.IOException {
        Process proc = Runtime.getRuntime().exec(cmd);
        java.io.InputStream is = proc.getInputStream();
        java.util.Scanner scanner = new java.util.Scanner(is);
        scanner.useDelimiter("\\A");
        String val = "";
        if(scanner.hasNext()) {
            val = scanner.next();
        } else {
            val = "";
        }
        scanner.close();
        return val;
    }

    public static Message getMessage(String json) {
        Gson gson = new Gson();
        Message message = null;
        try {
            message = gson.fromJson(json, Message.class);
        } catch(Exception ex) {
            return null;
        }
        return message;
    }

    public static void listRateLimit() {
        Gson gson = new Gson();
        String cmd = RATE_LIMIT_COMMAND;
        String json;
        try {
            json = execCmd(cmd);
        } catch(Exception ex) {
            ex.printStackTrace();
            return;
        }
        RateLimit rateLimit = gson.fromJson(json, RateLimit.class);
        Message message = getMessage(json);
        if(message != null && message.message != null) {
            System.out.println("listRateLimit received:\n  " + message.message);
            return;
        }
        System.out.println("Rate Limit:");
        System.out.println("    limit=" + rateLimit.rate.limit);
        System.out.println("    remaining=" + rateLimit.rate.remaining);
        long time = rateLimit.rate.reset;
        Date remaining = new Date(time * 1000);
        System.out.println("    reset=" + remaining);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(inFile);
        System.out.println(new Date());
        // listRateLimit(); // Use to check when rate will reset
        listRepositories();
        // listRelease("VS-ArtPad");
        // listRelease("VS-Fractal");
        System.out.println();
        System.out.println("All Done");
    }
}
