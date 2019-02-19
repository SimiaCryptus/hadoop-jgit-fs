/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.hadoop_jgit;

import org.eclipse.jgit.transport.URIish;

import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Parse path.
 */
class ParsePath {
  private static final Pattern gitRegex = Pattern.compile("/(.*\\.git/)([^/]*)/?(.*)");
  private final String url;
  private String repoPath;
  private String repoBranch;
  private String filePath;

  /**
   * Instantiates a new Parse path.
   *
   * @param url the url
   */
  public ParsePath(final String url) {
    if (null == url) throw new IllegalArgumentException();
    this.url = url;
  }

  /**
   * Gets repo path.
   *
   * @return the repo path
   */
  public CharSequence getRepoPath() {
    return repoPath;
  }

  /**
   * Gets repo branch.
   *
   * @return the repo branch
   */
  public String getRepoBranch() {
    return repoBranch;
  }

  /**
   * Gets file path.
   *
   * @return the file path
   */
  public CharSequence getFilePath() {
    return filePath;
  }

  /**
   * Invoke parse path.
   *
   * @return the parse path
   */
  public ParsePath invoke() {
    assert null != gitRegex;
    assert null != url;

    CharSequence path;
    try {
      path = new URIish(url).getPath();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    Matcher matcher = null == path ? null : gitRegex.matcher(path);
    if (null != matcher && matcher.matches()) {
      repoPath = matcher.group(1);
      repoBranch = matcher.group(2);
      filePath = matcher.group(3);
    } else {
      repoPath = "";
      repoBranch = "master";
      filePath = "";
    }
    return this;
  }
}
