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
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

public class GitRepoFileSystem extends ReadOnlyFileSystem {
  protected static final Logger logger = LoggerFactory.getLogger(GitRepoFileSystem.class);
  public final File gitDir;
  private final Repository repository;
  private final RemoteConfig remoteConfig;
  private final ParsePath parsedPath;
  private final RawLocalFileSystem innerFS;
  private final URI localBase;
  private final URI univeralBase;
  
  public GitRepoFileSystem(URI url) throws IOException, URISyntaxException {
    File dataDirectory = new File(System.getProperty("java.io.tmpdir"), "git");
    dataDirectory.mkdirs();
    logger.info("Git FS: " + url);
    this.parsedPath = new ParsePath(url).invoke();
    logger.info("Git Repo: " + parsedPath.getRepoPath());
    logger.info("Git Branch: " + parsedPath.getRepoBranch());
    logger.info("Git File: " + parsedPath.getFilePath());
    final URIish sourceUrl = new URIish(String.format("%s://%s/%s", url.getScheme(), url.getHost(), parsedPath.getRepoPath()));
    logger.info("Git Url: " + sourceUrl);
    this.gitDir = new File(dataDirectory, String.format("%s/%s/%s", url.getHost(), parsedPath.getRepoPath(), parsedPath.getRepoBranch()));
    logger.info("Temp Git Dir: " + gitDir.getAbsolutePath());
    this.repository = new RepositoryBuilder().setWorkTree(gitDir).build();
    if (!gitDir.exists()) {
      if (!gitDir.mkdirs()) { throw new RuntimeException(gitDir.getAbsolutePath()); }
      repository.create(false);
    }
    this.remoteConfig = getRemoteConfig(sourceUrl, repository.getConfig());
    update();
    this.innerFS = new RawLocalFileSystem();
    innerFS.setWorkingDirectory(new Path(gitDir.getAbsolutePath()));
    this.localBase = this.gitDir.toPath().toUri();
    this.univeralBase = new URI(sourceUrl.toString()).resolve(parsedPath.getRepoBranch());
    logger.info("Local Base: " + localBase);
    logger.info("Universal Base: " + univeralBase);
  }
  
  @Nonnull
  public Path pathFilter(final Path path) {
    return new Path(convertUrl(path.toUri()));
  }
  
  @Nonnull
  public URI convertUrl(final URI path) {
    URI relativized = localBase.resolve(univeralBase.relativize(path));
    logger.info(String.format("Converted %s to %s", path, relativized));
    return relativized;
  }
  
  private void update() throws IOException {
    Collection<Ref> fetch = fetch(repository, remoteConfig, parsedPath.getRepoBranch());
    checkout(repository, fetch.stream().filter(x -> x.getName().equals("HEAD")).findAny().get());
  }
  
  private boolean checkout(final Repository repository, final Ref tagName) throws IOException {
    if (tagName == null) return false;
    final RevCommit commit;
    try (RevWalk revWalk = new RevWalk(repository)) {
      assert revWalk != null;
      ObjectId resolve = tagName.getObjectId();
      if (resolve == null) return false;
      commit = revWalk.parseCommit(resolve);
      assert commit != null;
    }
    DirCache dirCache = repository.lockDirCache();
    RevTree tree = commit.getTree();
    DirCacheCheckout dirCacheCheckout = new DirCacheCheckout(repository, dirCache, tree);
    dirCacheCheckout.setFailOnConflict(true);
    boolean checkout = dirCacheCheckout.checkout();
    logger.info(String.format("Checked out %s: %s", tagName.getObjectId(), checkout));
    return checkout;
  }
  
  private Collection<Ref> fetch(final Repository repository, final RemoteConfig remoteConfig, final String repoBranch) {
    try (Transport transport = Transport.open(repository, remoteConfig)) {
      configure(transport);
      transport.setCheckFetchedObjects(false);
      transport.setRemoveDeletedRefs(false);
      transport.setDryRun(false);
      transport.setTagOpt(TagOpt.FETCH_TAGS);
      transport.setFetchThin(false);
      final ProgressMonitor monitor = new EmptyProgressMonitor() {};
      FetchResult result = transport.fetch(monitor, Arrays.asList(
        new RefSpec("refs/heads/" + repoBranch)
      ));
      logger.info(String.format("%s: %s", result.getURI(), result.getMessages()));
      result.getAdvertisedRefs().stream().forEach(ref -> {
        logger.info(String.format("%s: %s", ref.getName(), ref.getObjectId()));
      });
      return result.getAdvertisedRefs();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  private void configure(final Transport transport) {
    String username = System.getProperty("git.user");
    if (null != username) {
      String password = System.getProperty("git.pass");
      logger.info(String.format("Login: %s %s", username, password.replaceAll(".", "*")));
      transport.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
    }
  }
  
  @Nonnull
  private RemoteConfig getRemoteConfig(final URIish uri, final StoredConfig config) throws URISyntaxException, IOException {
    RemoteConfig remote = new RemoteConfig(config, "origin");
    remote.addFetchRefSpec(new RefSpec().setForceUpdate(true).setSourceDestination("refs/heads/*", "refs/remotes/origin/*"));
    remote.addURI(uri);
    remote.update(config);
    config.save();
    return remote;
  }
  
  @Override
  public URI getUri() {
    return univeralBase;
  }
  
  @Override
  public FSDataInputStream open(final Path f, final int bufferSize) throws IOException {
    return innerFS.open(pathFilter(f), bufferSize);
  }
  
  @Override
  public FileStatus[] listStatus(final Path f) throws IOException {
    return innerFS.listStatus(pathFilter(f));
  }
  
  @Override
  public Path getWorkingDirectory() {
    return new Path(univeralBase);
  }
  
  @Override
  public void setWorkingDirectory(final Path new_dir) {
    throw new RuntimeException("Static Filesystem");
  }
  
  @Override
  public FileStatus getFileStatus(final Path f) throws IOException {
    return innerFS.getFileStatus(pathFilter(f));
  }
  
}
