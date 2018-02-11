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

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;

public class JGitFileSystem extends org.apache.hadoop.fs.FileSystem implements AutoCloseable {
  protected static final Logger logger = LoggerFactory.getLogger(JGitFileSystem.class);
  
  public JGitFileSystem(String url, final String branch) throws IOException, URISyntaxException {
    File baseDir = File.createTempFile("hadoop", ".git");
    baseDir.delete();
    baseDir.mkdirs();
    logger.info("Temp Git Dir: " + baseDir.getAbsolutePath());
    final URIish uri = new URIish(url);
    logger.info("Git Url: " + uri);
    Repository repo = new RepositoryBuilder().setWorkTree(baseDir).build();
    repo.create(false);
    StoredConfig config = repo.getConfig();
    final String name = "origin";
    RemoteConfig remote = new RemoteConfig(config, name);
    
    RefSpec refSpec = new RefSpec();
    refSpec = refSpec.setForceUpdate(true);
    refSpec = refSpec.setSourceDestination(Constants.R_HEADS + "*", Constants.R_REMOTES + name + "/*");
    remote.addFetchRefSpec(refSpec);
    remote.addURI(uri);
    remote.update(config);
    config.save();
    
    try (Transport transport = Transport.open(repo, remote)) {
      String username = System.getProperty("git.user");
      if (null != username) {
        String password = System.getProperty("git.pass");
        logger.info(String.format("Login: %s %s", username, password.replaceAll(".", "*")));
        transport.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
      }
      transport.setCheckFetchedObjects(true);
      transport.setRemoveDeletedRefs(true);
      transport.setDryRun(false);
      transport.setTagOpt(TagOpt.AUTO_FOLLOW);
      transport.setFetchThin(false);
      final ProgressMonitor monitor = new EmptyProgressMonitor() {};
      FetchResult result = transport.fetch(monitor, Arrays.asList(new RefSpec("refs/heads/" + branch, RefSpec.WildcardMode.REQUIRE_MATCH)));
      logger.info(String.format("%s: %s", result.getURI(), result.getMessages()));
      Map<String, FetchResult> fetchResultMap = result.submoduleResults();
      log("", fetchResultMap);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    
    Desktop.getDesktop().open(baseDir);
    
  }
  
  private void log(String indent, final Map<String, FetchResult> fetchResultMap) {
    fetchResultMap.forEach((submoduleName, fetchResult) -> {
      logger.info(String.format(indent + "%s: %s", submoduleName, fetchResult.getMessages()));
      log(indent + "  ", fetchResult.submoduleResults());
    });
  }
  
  @Override
  public URI getUri() {
    return null;
  }
  
  @Override
  public FSDataInputStream open(Path path, int i) throws IOException {
    return null;
  }
  
  @Override
  public FSDataOutputStream create(Path path, FsPermission fsPermission, boolean b, int i, short i1, long l, Progressable progressable) throws IOException {
    return null;
  }
  
  @Override
  public FSDataOutputStream append(Path path, int i, Progressable progressable) throws IOException {
    return null;
  }
  
  @Override
  public boolean rename(Path path, Path path1) throws IOException {
    return false;
  }
  
  @Override
  public boolean delete(Path path, boolean b) throws IOException {
    return false;
  }
  
  @Override
  public FileStatus[] listStatus(Path path) throws IOException {
    return new FileStatus[0];
  }
  
  @Override
  public Path getWorkingDirectory() {
    return null;
  }
  
  @Override
  public void setWorkingDirectory(Path path) {
  
  }
  
  @Override
  public boolean mkdirs(Path path, FsPermission fsPermission) throws IOException {
    return false;
  }
  
  @Override
  public FileStatus getFileStatus(Path path) throws IOException {
    return null;
  }
}
