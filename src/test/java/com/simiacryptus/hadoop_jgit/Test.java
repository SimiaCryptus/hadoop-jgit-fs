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

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.Charset;

public class Test {
  protected static final Logger logger = LoggerFactory.getLogger(Test.class);
  
  
  public static void main(String[] args) {
    try {
      URI url = new URI("https://github.com/SimiaCryptus/MindsEye.git/master/README.md");
  
      String pathString = "git://github.com/SimiaCryptus/MindsEye.git/master/README.md";
      Charset utf8 = Charset.forName("UTF-8");
      GitFileSystem gitFileSystem = new GitFileSystem();
      gitFileSystem.setConf(new Configuration());
      IOUtils.readLines(gitFileSystem.open(new Path(pathString)), utf8).forEach(line -> {
        logger.info(line);
      });

//      GitRepoFileSystem jGitFileSystem = new GitRepoFileSystem(url);
//      logger.info("Local Path: " + new Path(jGitFileSystem.convertUrl(url)));
//      Desktop.getDesktop().open(jGitFileSystem.gitDir);
//
//      logger.info(jGitFileSystem.toString());
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
