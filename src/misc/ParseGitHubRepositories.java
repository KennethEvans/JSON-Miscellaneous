package misc;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/*
 * Created on Dec 22, 2018
 * By Kenneth Evans, Jr.
 */

public class ParseGitHubRepositories
{
    public static final String LS = System.getProperty("line.separator");
    public static final String COMMA = ",";

    private static final boolean USE_PASSWORD = true;
    /**
     * Used in per_page= for repositories. It should be 100, but is included for
     * debugging the page logic with a number less than 100.
     */
    private static int REPO_PAGES = 100;
    private static final String DEFAULT_OWNER = "KennethEvans";
    private static String owner = DEFAULT_OWNER;
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

    /**
     * Finds the number of pages needed for repositories. Asks for per_page=100,
     * hard coded. Will not work if it is > 100 (since that is th maximum
     * allowed). Default is 30 if not specified;
     * 
     * @return Page(-1, 1) if no extra pages needed , else Page(next, last);
     */
    public static Pages getReproPages() {
        String endpoint = "https://api.github.com/user" + "/repos?per_page="
            + REPO_PAGES;
        String cmd = generateCurlCommand("-I", endpoint);
        Process proc;
        try {
            proc = Runtime.getRuntime().exec(cmd);
        } catch(IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
            return new Pages(-1, 1);
        }
        BufferedReader stdInput = new BufferedReader(
            new InputStreamReader(proc.getInputStream()));
        String line;
        String nextString = null, lastString = null;
        int next = -1, last = 1;
        try {
            while((line = stdInput.readLine()) != null) {
                if(line.startsWith("Link: ")) {
                    // DEBUG
                    // System.out.println("Line: " + line);
                    String regex = ".*page=([0-9]+).*page=([0-9]+).*";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(line);
                    while(matcher.find()) {
                        nextString = matcher.group(1);
                        lastString = matcher.group(2);
                        // Debug
                        // System.out.println("nextString=" + nextString
                        // + " lastString=" + lastString);
                    }
                    if(nextString != null && lastString != null) {
                        next = Integer.parseInt(nextString);
                        last = Integer.parseInt(lastString);
                        break;
                    }
                }
            }
        } catch(NumberFormatException | IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        return new Pages(next, last);
    }

    public static void listRepositories() {
        Pages pages = getReproPages();
        // Get the list of repositories
        String json;
        Repro[] repos = null;
        for(int i = 0; i < pages.last; i++) {
            json = null;
            // This command does not get private repos
            // String cmd = "https://api.github.com/users/" + owner
            // + "/repos?per_page=100";
            String endpoint = "https://api.github.com/user" + "/repos?per_page="
                + REPO_PAGES + "&page=" + (i + 1);
            String cmd = generateCurlCommand(null, endpoint);
            // DEBUG
            // System.out.println(cmd);
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
                if(repos == null) {
                    repos = results;
                } else {
                    int oldLen = repos.length;
                    repos = Arrays.copyOf(repos, repos.length + results.length);
                    System.arraycopy(results, 0, repos, oldLen, results.length);
                }
            } catch(Exception ex) {
                ex.printStackTrace();
                return;
            }
        }

        // Loop over repositories
        List<SpreadsheetRow> rows = null;
        SpreadsheetRow row = null;
        if(CREATE_SPREADSHEET) {
            rows = new ArrayList<SpreadsheetRow>();
        }
        // try {
        Release[] releases = null;
        Readme readme = null;
        int releaseCount = 0;
        String readmeName = null;
        for(Repro repro : repos) {
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
                + SpreadsheetRow.convertToLocal(repro.created_at) + " local)");
            System.out.println("    updated_at=" + repro.updated_at + "  ("
                + SpreadsheetRow.convertToLocal(repro.updated_at) + " local)");
            System.out.println("    pushed_at=" + repro.pushed_at + "  ("
                + SpreadsheetRow.convertToLocal(repro.pushed_at) + " local)");
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
                        System.out
                            .println("          tag_name=" + release.tag_name);
                        System.out.println("          body=" + release.body);
                        System.out.println(
                            "           created_at=" + release.created_at);
                        System.out.println(
                            "           published_at=" + release.published_at);
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
                if(repro.privateStr.toLowerCase().equals("true")) {
                    row.privateStr = repro.privateStr;
                } else {
                    row.privateStr = "";
                }
                row.readme = readmeName;
                row.created_at = SpreadsheetRow
                    .convertToLocal(repro.created_at);
                row.updated_at = SpreadsheetRow
                    .convertToLocal(repro.updated_at);
                row.pushed_at = SpreadsheetRow.convertToLocal(repro.pushed_at);
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
        System.out.println();
        System.out.println("Number of repositories: " + repos.length);
        if(CREATE_SPREADSHEET)

        {
            // Create spreadsheet
            writeCSV(rows);
        }
        // } catch(Exception ex) {
        // ex.printStackTrace();
        // }
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
        String endpoint = "https://api.github.com/repos/" + owner + "/"
            + repoName + "/releases?per_page=100";
        String cmd = generateCurlCommand(null, endpoint);
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
        String endpoint = "https://api.github.com/repos/" + owner + "/"
            + repoName + "/readme?per_page=100";
        String cmd = generateCurlCommand(null, endpoint);
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
        String endpoint = "https://api.github.com/rate_limit";
        String cmd = generateCurlCommand(null, endpoint);
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

    /**
     * Generates a curl command for the given args and endpoint, adding the
     * authorization.
     * 
     * @param args Args to add to the start of the curl command.
     * @param endpoint The endpoint to use.
     * @return
     */
    public static String generateCurlCommand(String args, String endpoint) {
        String cmd = null;
        String argStr = "";
        if(args != null && !args.isEmpty()) {
            argStr = args + " ";
        }
        if(USE_PASSWORD) {
            if(!havePassword) {
                if(!getPassword()) {
                    cmd = "curl " + argStr + endpoint;
                } else {
                    cmd = "curl " + argStr + "-H \"Authorization: token "
                        + String.valueOf(password) + "\" " + endpoint;
                }
            } else {
                cmd = "curl " + argStr + "-H \"Authorization: token "
                    + String.valueOf(password) + "\" " + endpoint;
            }
        } else {
            cmd = "curl " + argStr + endpoint;
        }
        // DEBUG
        // System.out.println(cmd);
        return cmd;
    }

    public static boolean getPassword() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("Username", SwingConstants.RIGHT));
        label.add(new JLabel("Access Token", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField usernameField = new JTextField();
        usernameField.setText(String.valueOf(userName));
        controls.add(usernameField);
        JPasswordField passwordField = new JPasswordField();
        controls.add(passwordField);
        panel.add(controls, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(null, panel, "Authorization",
            JOptionPane.OK_CANCEL_OPTION);
        if(option == JOptionPane.OK_OPTION) {
            password = passwordField.getPassword();
            if(!isValidToken(password)) {
                System.out.println(
                    "Invalid password: |" + String.valueOf(password) + "|");
                return false;
            }
            userName = usernameField.getText().toCharArray();
            havePassword = true;
            return true;
        }
        return false;
    }

    public static String execCmd(String cmd) throws java.io.IOException {
        if(cmd == null) {
            System.out.println("Failed to get command");
            return null;
        }
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
                writer.print(row.privateStr + COMMA);
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
        System.out.println(fileName);
        try {
            Gson gson = new Gson();
            Repro[] results = gson.fromJson(new FileReader(fileName),
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
     * Checks if all characters of the given String are hexadecimal characters
     * 
     * @param token
     * @return
     */
    private static boolean isValidToken(char[] token) {
        if(token == null || token.length == 0) return false;
        for(int i = 0; i < token.length; i++)
            if(Character.digit(token[i], 16) == -1) return false;
        return true;
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
        if(USE_PASSWORD) {
            if(!getPassword()) {
                System.out.println("Failed to get password");
                System.out.println();
                System.out.println("Aborted");
                return;
            }
        }
        // Pages pages = getReproPages();
        // System.out
        // .println("Repro Pages: next=" + pages.next + " last=" + pages.last);
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
        "private", "language", "releases", "readme", "license", "created_at",
        "updated_at", "pushed_at", "size (KB)"};
    public static final DateFormat formatParse = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
    public static final DateFormat formatWrite = new SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss", Locale.US);

    String name;
    String description;
    String privateStr;
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

class Pages
{
    int next;
    int last;

    public Pages(int next, int last) {
        this.next = next;
        this.last = last;
    }
}

///////////////// Classes used for deserialization

class Repro
{
    int id;
    String name;
    String full_name;
    @SerializedName(value = "private")
    String privateStr;
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
