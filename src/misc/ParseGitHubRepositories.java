package misc;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.FileReader;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

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

class Readme
{
    String path;
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
    private static final String DEFAULT_OWNER = "KennethEvans";
    private static final String DEFAULT_IN_FILE = "C:/Scratch/AAA/github-repos-2018-12-22.json";
    private static final String inFile = DEFAULT_IN_FILE;

    private static final boolean USE_PASSWORD = true;
    private static final boolean GET_RELEASES = USE_PASSWORD ? true : false;
    private static final boolean GET_README = USE_PASSWORD ? true : false;
    private static String owner = DEFAULT_OWNER;

    /**
     * Note that user is the different from owner, even though they may
     * typically be the same. User is used for logging in. Owner is the owner of
     * the GitHUb account.
     */
    private static char[] userName = DEFAULT_OWNER.toCharArray();
    private static char[] password = "".toCharArray();
    private static boolean havePassword = false;

    public static void listRepositories() {
        String json = null;
        String cmd = generateCurlCommand(generateReproCommand());
        try {
            json = execCmd(cmd);
        } catch(Exception ex) {
            ex.printStackTrace();
            return;
        }
        if(!checkMessage(json, "listRepositories")) return;
        try {
            Gson gson = new Gson();
            Repro[] results = gson.fromJson(json, Repro[].class);
            Release[] releases = null;
            ;
            Readme readme = null;
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
                if(GET_README) {
                    readme = getReadme(repro.name);
                    if(readme == null) {
                        System.out.println("    No readme");
                    } else {
                        System.out.println("    readme=" + readme.path);
                    }
                }
                if(GET_RELEASES) {
                    releases = getRelease(repro.name);
                    if(releases == null || releases.length == 0) {
                        System.out.println("    No releases");
                    } else {
                        System.out.println("    Releases");
                        for(Release release : releases) {
                            System.out.println("      " + release.name);
                            System.out.println(
                                "          tag_name=" + release.tag_name);
                            System.out
                                .println("          body=" + release.body);
                            System.out.println(
                                "           created_at=" + release.created_at);
                            System.out.println("           published_at="
                                + release.published_at);
                        }
                    }
                }
            }

        } catch(Exception ex) {
            ex.printStackTrace();
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

    public static Release[] getRelease(String repoName) {
        Gson gson = new Gson();
        String cmd = generateCurlCommand(generateReleaseCommand(repoName));
        String json;
        try {
            json = execCmd(cmd);
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
        if(!checkMessage(json, "getRelease")) return null;
        return gson.fromJson(json, Release[].class);
    }

    public static Readme getReadme(String repoName) {
        Gson gson = new Gson();
        String cmd = generateCurlCommand(generateReadmeCommand(repoName));
        String json;
        try {
            json = execCmd(cmd);
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
        // Check for not found first
        Message message = getMessage(json);
        if(message != null && message.message != null) {
            if(message.message.toLowerCase().contains("not found")) {
                return null;
            }
        }
        // Check for error
        if(!checkMessage(json, "getRelease")) return null;
        return gson.fromJson(json, Readme.class);
    }

    public static void listRateLimit() {
        Gson gson = new Gson();
        String cmd = generateCurlCommand(generateRateLimitCommand());
        String json;
        try {
            json = execCmd(cmd);
        } catch(Exception ex) {
            ex.printStackTrace();
            return;
        }
        RateLimit rateLimit = gson.fromJson(json, RateLimit.class);
        if(!checkMessage(json, "listRateLimit")) return;
        System.out.println("Rate Limit:");
        System.out.println("    limit=" + rateLimit.rate.limit);
        System.out.println("    remaining=" + rateLimit.rate.remaining);
        long time = rateLimit.rate.reset;
        Date remaining = new Date(time * 1000);
        System.out.println("    reset=" + remaining);
    }

    public static String generateReproCommand() {
        return "https://api.github.com/users/" + owner + "/repos?per_page=1000";
    }

    public static String generateReleaseCommand(String repoName) {
        return "https://api.github.com/repos/" + owner + "/" + repoName
            + "/releases?per_page=1000";
    }

    public static String generateReadmeCommand(String repoName) {
        return "https://api.github.com/repos/" + owner + "/" + repoName
            + "/readme?per_page=1000";
    }

    public static String generateRateLimitCommand() {
        return "https://api.github.com/rate_limit";
    }

    public static String generateCurlCommand(String arg) {
        if(USE_PASSWORD) {
            if(!havePassword) {
                if(!getPassword()) {
                    return "curl " + arg;
                } else {
                    return "curl -u \"" + String.valueOf(userName) + ":"
                        + String.valueOf(password) + "\" " + arg;
                }
            } else {
                return "curl -u \"" + String.valueOf(userName) + ":"
                    + String.valueOf(password) + "\" " + arg;
            }
        }
        return "curl " + arg;
    }

    public static boolean getPassword() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("Username", SwingConstants.RIGHT));
        label.add(new JLabel("Password", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField usernameField = new JTextField();
        usernameField.setText(DEFAULT_OWNER);
        controls.add(usernameField);
        JPasswordField passwordField = new JPasswordField();
        controls.add(passwordField);
        panel.add(controls, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(null, panel, "Login",
            JOptionPane.OK_CANCEL_OPTION);
        if(option == JOptionPane.OK_OPTION) {
            password = passwordField.getPassword();
            userName = usernameField.getText().toCharArray();
            havePassword = true;
            return true;
        }
        return false;
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

    public static boolean checkMessage(String json, String caller) {
        Message message = getMessage(json);
        if(message != null && message.message != null) {
            System.out.println(caller + " received:\n  " + message.message);
            if(message.message.contains("API rate limit exceeded")) {
                listRateLimit();
            }
            return false;
        }
        return true;
    }

    /**
     * Read from a previously-generated file rather than using cURL to access
     * the GitHub API>
     * 
     * @param fileName File with JSON from a repositories request.
     */
    public static void readFile(String fileName) {
        System.out.println(inFile);
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

        System.out.println(new Date());
        // listRateLimit(); // Use to check when rate will reset
        listRepositories();
        // listRelease("VS-ArtPad");
        // listRelease("VS-Fractal");

        System.out.println();
        System.out.println("All Done");
    }
}
