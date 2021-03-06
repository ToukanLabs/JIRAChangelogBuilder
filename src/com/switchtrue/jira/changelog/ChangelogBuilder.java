package com.switchtrue.jira.changelog;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Build the changelog as text from a given list of VersionInfo instances.
 *
 * @author mleonard87
 *
 */
public class ChangelogBuilder {

  /**
   * Iterates over a list of JIRA project versions and creates a changelog.
   *
   * @param versionInfoList The list of JIRA versions to build the changelog for
   * as VersionInfo objects.
   * @param files A collection of file names to use when writing the changelog
   * files.
   * @param templates A collection of template files to use for writing the
   * changelog files. The change log will be generated in the order that the
   * VersionInfo objects are in the list.
   * @param filename Output file name.
   * @param filename A list of template names.
   * @param ending A value indicating the kind of newlines to be used in the
   * changelog file.
   */
  public void build(List<VersionInfo> versionInfoList, String[] files, String templateRoot, String[] templates, LineEnding ending) {
    FileWriter writer = null;
    int fileIndex = 0;
    ChangelogTemplate.fileRoot = templateRoot;

    // build the changelog for the file using the file template.
    try {
      for (String t : templates) {
        if (t != null) {
          Logger.log("Writing " + files[fileIndex]);
          writer = new FileWriter(files[fileIndex++]);
          ChangelogTemplate.createChangelog(versionInfoList, writer, t, ending);
          writer.flush();
          writer.close();
        }
      }
    } catch (IOException e) {
      // catch and ignore because we don't care if the file doesn't exist or
      // cannot be written
    }
  }
}
