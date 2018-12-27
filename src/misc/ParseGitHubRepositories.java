package misc;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

public class ParseGitHubRepositories
{
    public static final String LS = System.getProperty("line.separator");
    public static final String COMMA = ",";

    private static final String DEFAULT_OWNER = "KennethEvans";
    private static final String DEFAULT_IN_FILE = "C:/Scratch/AAA/github-repos-2018-12-22.json";
    private static final String inFile = DEFAULT_IN_FILE;
    private static String owner = DEFAULT_OWNER;

    private static final boolean USE_PASSWORD = true;
    /**
     * Note that user is the different from owner, even though they may
     * typically be the same. User is used for logging in. Owner is the owner of
     * the GitHUb account.
     */
    private static char[] userName = DEFAULT_OWNER.toCharArray();
    private static char[] password = "".toCharArray();
    private static boolean havePassword = false;

    private static final boolean GET_RELEASES = USE_PASSWORD ? true : false;
    private static final boolean GET_README = USE_PASSWORD ? true : false;

    private static boolean CREATE_SPREADSHEET = true;
    private static final String SPREADSHEET_DIR = "C:/Scratch/Git/GitHub/Repository Information/";
    private static final String CVS_FILENAME_TEMPLATE = SPREADSHEET_DIR
        + "RepositoryInfo-%s.csv";

    public static final String csvFileFormat = "yyyy-MM-dd-hh-mm";
    public static final SimpleDateFormat csvFileFormatter = new SimpleDateFormat(
        csvFileFormat);

    public static void listRepositories() {
        List<SpreadsheetRow> rows = null;
        SpreadsheetRow row = null;
        if(CREATE_SPREADSHEET) {
            rows = new ArrayList<SpreadsheetRow>();
        }
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
            Readme readme = null;
            int releaseCount = 0;
            String readmeName = null;
            for(Repro repro : results) {
                readme = null;
                releases = null;
                releaseCount = 0;
                readmeName = "";
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
                System.out.println("    created_at=" + repro.created_at + "  ("
                    + SpreadsheetRow.convertToLocal(repro.created_at)
                    + " local)");
                System.out.println("    updated_at=" + repro.updated_at + "  ("
                    + SpreadsheetRow.convertToLocal(repro.updated_at)
                    + " local)");
                System.out.println("    pushed_at=" + repro.pushed_at + "  ("
                    + SpreadsheetRow.convertToLocal(repro.pushed_at)
                    + " local)");
                System.out.println("    size=" + repro.size);
                if(GET_README) {
                    readme = getReadme(repro.name);
                    if(readme == null) {
                        System.out.println("    No readme");
                        readmeName = "";
                    } else {
                        System.out.println("    readme=" + readme.path);
                        readmeName = readme.path;
                    }
                }
                if(GET_RELEASES) {
                    releases = getRelease(repro.name);
                    if(releases == null || releases.length == 0) {
                        System.out.println("    No releases");
                    } else {
                        releaseCount = releases.length;
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
                if(CREATE_SPREADSHEET) {
                    // Fill in spreadsheet row;
                    row = new SpreadsheetRow();
                    rows.add(row);
                    row.name = repro.name;
                    if(repro.description == null) {
                        row.description = "";
                    } else {
                        row.description = repro.description;
                    }
                    row.readme = readmeName;
                    row.created_at = SpreadsheetRow
                        .convertToLocal(repro.created_at);
                    row.updated_at = SpreadsheetRow
                        .convertToLocal(repro.updated_at);
                    row.pushed_at = SpreadsheetRow
                        .convertToLocal(repro.pushed_at);
                    if(repro.language == null) {
                        row.language = "";
                    } else {
                        row.language = repro.language;
                    }
                    if(repro.license == null) {
                        row.license = "";
                    } else {
                        row.license = repro.license.name;
                    }
                    row.size = Integer.toString(repro.size);
                    if(releaseCount == 0) {
                        row.releases = "";
                    } else {
                        row.releases = Integer.toString(releaseCount);
                    }
                }
            }
            if(CREATE_SPREADSHEET) {
                // Create spreadsheet
                writeCSV(rows);
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

    private static void writeCSV(List<SpreadsheetRow> rows) {
        String fileName = String.format(CVS_FILENAME_TEMPLATE,
            csvFileFormatter.format(new Date()));
        File file = new File(fileName);
        if(file.exists()) {
            int selection = JOptionPane.showConfirmDialog(null,
                "File already exists:" + LS + file.getPath()
                    + "\nOK to replace?",
                "Warning", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if(selection != JOptionPane.OK_OPTION) return;
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
            // Headings
            String[] headings = SpreadsheetRow.CVS_HEADINGS;
            for(String heading : headings) {
                writer.print(heading + COMMA);
            }
            writer.println();
            for(SpreadsheetRow row : rows) {
                writer.print(row.name + COMMA);
                writer.print("\"" + row.description + "\"" + COMMA);
                writer.print(row.language + COMMA);
                writer.print(row.releases + COMMA);
                writer.print(row.readme + COMMA);
                writer.print(row.license + COMMA);
                writer.print(row.created_at + COMMA);
                writer.print(row.updated_at + COMMA);
                writer.print(row.pushed_at + COMMA);
                writer.print(row.size + COMMA);
                writer.println();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            return;
        } finally {
            if(writer != null) {
                writer.flush();
                writer.close();
            }
        }
        System.out.println();
        System.out.println("Wrote " + file.getPath());
        System.out.println(
            "    [LibreOffice: Use \"Format quoted field as text with String delimiter=\"]");
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

class SpreadsheetRow
{
    public static final String[] CVS_HEADINGS = {"name", "description",
        "language", "releases", "readme", "license", "created_at", "updated_at",
        "pushed_at", "size"};
    public static final DateFormat formatParse = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
    public static final DateFormat formatWrite = new SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss", Locale.US);

    String name;
    String description;
    String language;
    String releases;
    String readme;
    String license;
    String created_at;
    String updated_at;
    String pushed_at;
    String size;

    public static String convertToLocal(String time) {
        try {
            Date date = formatParse.parse(time);
            return formatWrite.format(date);
        } catch(Exception ex) {
            return "Invalid";
        }
    }
}

///////////////// Classes used for deserialization

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
