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

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class Test {
  protected static final Logger logger = LoggerFactory.getLogger(Test.class);
  
  
  public static void main(CharSequence[] args) {
    //System.setProperty("fs.jgit.auth.user")
    String pathString = args.length == 0 ? "https://git-codecommit.us-west-2.amazonaws.com/v1/repos/simiacryptus-test:/master/README.md" : args[0];
    //String pathString = args.length == 0 ? "git://github.com/SimiaCryptus/hadoop-jgit-fs.git/acharneski-patch-1/README.md" : args[0];
    try {
      Charset utf8 = Charset.forName("UTF-8");
      GitFileSystem gitFileSystem = new GitFileSystem();
      gitFileSystem.setConf(new Configuration());
      IOUtils.readLines(gitFileSystem.open(new Path(pathString)), utf8).forEach(line -> {
        logger.info(line);
      });
      Thread.sleep(TimeUnit.MINUTES.toMillis(10));
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
