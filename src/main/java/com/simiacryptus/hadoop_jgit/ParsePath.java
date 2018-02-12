/*
 * Copyright (c) 2018 by Andrew Charneski.
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

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParsePath {
  private static final Pattern deconstructionPattern = Pattern.compile("/(.*\\.git/)([^/]*)/?(.*)");
  private final URI url;
  private String repoPath;
  private String repoBranch;
  private String filePath;
  
  public ParsePath(final URI url) {
    if (null == url) throw new IllegalArgumentException();
    this.url = url;
  }
  
  public String getRepoPath() {
    return repoPath;
  }
  
  public String getRepoBranch() {
    return repoBranch;
  }
  
  public String getFilePath() {
    return filePath;
  }
  
  public ParsePath invoke() {
    assert null != deconstructionPattern;
    assert null != url;
    String path = url.getPath();
    Matcher matcher = null == path ? null : deconstructionPattern.matcher(path);
    if (null != matcher && matcher.matches()) {
      repoPath = matcher.group(1);
      repoBranch = matcher.group(2);
      filePath = matcher.group(3);
    }
    else {
      repoPath = "";
      repoBranch = "master";
      filePath = "";
    }
    return this;
  }
}
