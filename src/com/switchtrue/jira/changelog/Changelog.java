package com.switchtrue.jira.changelog;

import java.io.File;
import com.atlassian.oauth.client.example.AtlassianOAuthClient;
import com.atlassian.oauth.client.example.TokenSecretVerifierHolder;

/**
 * Main class for creating a changelog from JIRA.
 *
 * @author apigram
 */
public class Changelog {

  public static String FIX_VERSION_RESTICT_MODE_STARTS_WITH        = "SW";
  public static String FIX_VERSION_RESTICT_MODE_LESS_THAN_OR_EQUAL = "LTE";
  
  public static final String CONSUMER_KEY = "JIRAChangelogBuilderFivium";

  /**
   * Show usage of the application.
   */
  public static void showUsage() {
    System.out.println("\nUsage:");
    System.out.println("java -jar jira-changelog-builder.jar <JIRA_URL> <OAuth_private_key> <OAuth_access_token> <JIRA_project_key> <version> <template_list> [<flags>]");
    System.out.println("<JIRA_URL>: The URL of the JIRA instance (e.g. https://somecompany.atlassian.net).");
    System.out.println("<OAuth_private_key>: RSA private key matching the public key entered into Application Link's Incoming Authentication on JIRA, used for OAuth.");
    System.out.println("<OAuth_access_token>: Access token provided by JIRA after successful OAuth authorisation by JIRA administrator.");
    System.out.println("<JIRA_project_key>: The key of the project in JIRA.");
    System.out.println("<version>: Specifies up to which version the changelog should be generated.");
    System.out.println("<template_root>: The path on disk to the directory that contains the template files.");
    System.out.println("<template_list>: A CSV list of template file names. Each templated changelog is saved into a new file which can be processed at a later stage.");
    System.out.println("<flags> (optional): One or more of the following flags:");
    // TODO: If this JQL causes no issues to be returned, it causes a hard
    // error. Handle this more nicely.
    System.out.println("\t--jql 'some arbitrary JQL': Append the given JQL to the issue filter. eg 'status = \"Ready for Build\"'");
    System.out.println("\t--object-cache-path /some/path: The path on disk to the cache, if you do not use this, no cache will be used. Using a cache is highly recommended.");
    System.out.println("\t--debug: Print debug/logging information to standard out. This will also force errors to go to the standard out and exit with code 0 rather than 1.");
    System.out.println("\t--changelog-description-field 'field_name': The name of the field in JIRA you wish to use as the changelog description field. If you do not use this, it will default to the summary field.");
    System.out.println("\t--eol-style (NATIVE|CRLF|LF): The type of line endings you wish the changelog files to use. Valid values are NATIVE (system line endings), CRLF (Windows line endings) or LF (UNIX line endings). If you do not use this, the changelogs will use the default system line endings.");
    System.out.println("\t--version-starts-with 'Version name prefix': Only display versions in the changelog that have a name starting with 'Version name prefix'. This cannot be used with --version-less-than-or-equal. This is useful for restricting what goes in the changelog if you are producing different version side-by-side.");
    System.out.println("\t--version-less-than-or-equal 'Version name': Only display versions in the changelog that have a name less than or equal to 'Version name'. This cannot be used with --version-starts-with. This uses a Java string comparison. This is useful for restricting what goes in the changelog if you are producing different version side-by-side.");
  }

  /**
   * Main function.
   *
   * @param args Arguments passed in from the command line
   */
  public static void main(String[] args) {
    int currentArgument = 0;
    if (args.length == 1) {
      if (args[0].equals("--help")) {
        showUsage();
        System.exit(0);
      } else if (args[0].equals("--print-consumer-key")) {
      System.out.println("Consumer key is: " + CONSUMER_KEY);
      System.exit(0);
      }
    }
    if (args.length == 3 && args[0].equals("-r")){
      currentArgument++;
      final String jiraUrl = args[currentArgument++];
      final String oAuthPrivateKey = args[currentArgument++];
      AtlassianOAuthClient jiraOAuthClient = new AtlassianOAuthClient(CONSUMER_KEY, oAuthPrivateKey, jiraUrl, "oob");
      TokenSecretVerifierHolder requestToken = jiraOAuthClient.getRequestToken();
      String authorizeUrl = jiraOAuthClient.getAuthorizeUrlForToken(requestToken.token);
      System.out.println("REQUEST_TOKEN is " + requestToken.token);
      System.out.println("TOKEN_SECRET is " + requestToken.secret);
      System.out.println("Go to and 'Allow' to obtain VERIFIER:\n\n" + authorizeUrl);
      System.exit(0);
    }
    if (args.length == 6 && args[0].equals("-a")) {
      currentArgument++;
      final String jiraUrl = args[currentArgument++];
      final String oAuthPrivateKey = args[currentArgument++];
      final String requestToken = args[currentArgument++];
      final String tokenSecret = args[currentArgument++];
      final String oAuthVerifer = args[currentArgument++];
      AtlassianOAuthClient jiraOAuthClient = new AtlassianOAuthClient(CONSUMER_KEY, oAuthPrivateKey, jiraUrl, null);
      String accessToken = jiraOAuthClient.swapRequestTokenForAccessToken(requestToken, tokenSecret, oAuthVerifer);
      System.out.println("ACCESS_TOKEN is " + accessToken);
      System.exit(0);
    }
    if (args.length < 6) {
      System.out.println("Not enough arguments given.");
      showUsage();
      System.exit(1);
    }

    final String jiraURL = args[currentArgument++];
    final String oAuthPrivateKey = args[currentArgument++]; //TODO: Read private key from file?
    final String oAuthAccessToken = args[currentArgument++];
    final String jiraProjectKey = args[currentArgument++];
    final String versionName = args[currentArgument++];
    final String templateRoot = args[currentArgument++];
    final String templateList = args[currentArgument++];

    String[] templates = templateList.split(",");

    // Handle optional flags
    String jql = "";
    String filenameList = null;
    String files[] = null;
    String objectCachePath = null;
    String descriptionField = null;
    String fixVersionRestrictMode = null;
    String fixVersionRestrictTerm = null;
    LineEnding ending = LineEnding.NATIVE; // default to native line endings
    for (; currentArgument < args.length; currentArgument++) {
      try {
        if (args[currentArgument].equals("--debug")) {
          Logger.enable();
          Logger.log("--debug flag found. Debug logging enabled.");
        } else if (args[currentArgument].equals("--jql")) {
          // extract the JQL string, replace the *s with spaces, and replace 
          // brackets with quotation marks (maven strips quotation marks)
          jql = args[++currentArgument];
          jql = jql.replaceAll("_", " ");
          jql = jql.replaceAll("(\\[|\\])", "'");
          Logger.log("--jql flag found. Appending JQL: " + jql);
        } else if (args[currentArgument].equals("--object-cache-path")) {
          objectCachePath = args[++currentArgument];
          Logger.log("--object-cache-path flag found. Using " + objectCachePath + " as the object cache.");
        } else if (args[currentArgument].equals("--changelog-file-name")) {
          filenameList = args[++currentArgument];
          filenameList = filenameList.replace("\"", "");
          files = filenameList.split(",");
          if (files.length != templates.length) {
            Logger.err("Output file list does not match template file list.");
            System.exit(2);
          }
          Logger.log("--changelog-file-name found. Using " + filenameList + " as changelog files.");
        } else if (args[currentArgument].equals("--eol-style")) {
          ending = LineEnding.getEnding(args[++currentArgument]);
          if (ending == null) {
            // invalid style, log error and terminate
            Logger.err("Unknown line ending style flag.");
            System.exit(4);
          }
        } else if (args[currentArgument].equals("--changelog-description-field")) {
          descriptionField = args[++currentArgument];
          Logger.log("--changelog-description-field found. Using " + descriptionField + " as the Changelog Description field.");
        } else if (args[currentArgument].equals("--version-starts-with")) {
          if (fixVersionRestrictMode != null) {
            Logger.err("You cannot use both --version-starts-with and --version-less-than-or-equal at the same time or supply either of them more than once.");  
            System.exit(2);
          }
          fixVersionRestrictMode = FIX_VERSION_RESTICT_MODE_STARTS_WITH;
          fixVersionRestrictTerm = args[++currentArgument];
          Logger.log("--version-starts-with found. Only inlcude versions starting with " + fixVersionRestrictTerm + " in the Changelog.");
        } else if (args[currentArgument].equals("--version-less-than-or-equal")) {
          if (fixVersionRestrictMode != null) {
            Logger.err("You cannot use both --version-starts-with and --version-less-than-or-equal at the same time or supply either of them more than once.");  
            System.exit(2);
          }
          fixVersionRestrictMode = FIX_VERSION_RESTICT_MODE_LESS_THAN_OR_EQUAL;
          fixVersionRestrictTerm = args[++currentArgument];
          Logger.log("--version-less-than-or-equal found. Only inlcude versions with a name less than or equal to " + fixVersionRestrictTerm + " in the Changelog.");
        }
        else {
          Logger.err("Unknown argument: " + args[currentArgument]);
          System.exit(2);
        }
      } catch (ArrayIndexOutOfBoundsException e) {
        // Assuming this has come from args[++currentArgument] in the above try
        // block
        Logger.err("Malformed arguments. '" + args[currentArgument - 1] + "' requires a following argument.");
        System.exit(3);
      }
    }
    Logger.log("Starting with parameters: "
            + "\n  Version: " + versionName
            + "\n  JIRA Project Key: " + jiraProjectKey
            + "\n  JIRA URL: " + jiraURL
            + "\n  Template files: " + templateList);

    File f;
    for (int i = 0; i < templates.length; i++) {
      f = new File(templateRoot + '/' + templates[i]);
      if (!f.exists()) {
        Logger.err("Template file " + f.getName() + " does not exist!");
        System.exit(2);
      }
    }
    
    JiraAPI jiraApi = new JiraAPIOAuth(CONSUMER_KEY, oAuthPrivateKey, oAuthAccessToken, null, jiraURL, jql, descriptionField, fixVersionRestrictMode, fixVersionRestrictTerm);

    if (objectCachePath != null) {
      VersionInfoCache cache = new VersionInfoCache(jiraProjectKey, objectCachePath);
      jiraApi.setVersionInfoCache(cache);
    }
    jiraApi.fetchVersionDetails(jiraProjectKey, versionName);

    ChangelogBuilder clWriter = new ChangelogBuilder();
    Logger.log("Building changelog files.");

    if (filenameList == null) {
      // default all filenames to changelog#.txt if none have been specified
      files = new String[templates.length];
      for (int i = 0; i < files.length; i++) {
        files[i] = "changelog" + i + ".txt";
      }
    }
    clWriter.build(jiraApi.getVersionInfoList(), files, templateRoot, templates, ending);

    Logger.log("Done - Success!");
    
    System.exit(0);
  }
  
  /** Maven tests fail if you have System.exit(0) in main(): "test failed: The forked VM terminated without saying properly goodbye. VM crash or System.exit called ?" */
  public static void mainWrapper(String[] args) {
	  
	  
  }
}
