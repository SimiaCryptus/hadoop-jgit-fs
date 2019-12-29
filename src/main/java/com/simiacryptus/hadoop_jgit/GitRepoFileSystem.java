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
import java.util.concurrent.TimeUnit;

public class GitRepoFileSystem extends ReadOnlyFileSystem {
  private static final Logger logger = LoggerFactory.getLogger(GitRepoFileSystem.class);
  private final File gitDir;
  private final Repository repository;
  private final RemoteConfig remoteConfig;
  private final ParsePath parsedPath;
  private final RawLocalFileSystem innerFS;
  private final URI localBase;
  private final URI univeralBase;
  private final double eagerPullPeriod;
  private final double dismountPeriod;
  private final boolean dismountDelete;
  private final double lazyPullPeriod;
  private long lastTouch = 0;
  private long lastFetch = 0;

  public GitRepoFileSystem(String url, final GitFileSystem parent) throws IOException, URISyntaxException {
    setConf(parent.getConf());
    statistics = parent.getStats();
    TimeUnit timeUnit = TimeUnit.SECONDS;
    this.lazyPullPeriod = Double.parseDouble(getProperty("fs.jgit.pull.lazy", Double.toString(timeUnit.toSeconds(5))).toString());
    this.eagerPullPeriod = Double.parseDouble(getProperty("fs.jgit.pull.eager", Double.toString(timeUnit.toSeconds(5))).toString());
    this.dismountPeriod = Double.parseDouble(getProperty("fs.jgit.dismount.seconds", Double.toString(timeUnit.toSeconds(60))).toString());
    this.dismountDelete = Boolean.parseBoolean(getProperty("fs.jgit.dismount.delete", Boolean.toString(false)).toString());
    File dataDirectory = new File(getProperty("fs.jgit.datadir", getProperty("java.io.tmpdir")).toString(), "git");
    dataDirectory.mkdirs();
    logger.debug("Git FS: " + url);
    this.parsedPath = new ParsePath(url).invoke();
    logger.debug("Git Repo: " + getParsedPath().getRepoPath());
    logger.debug("Git Branch: " + getParsedPath().getRepoBranch());
    logger.debug("Git File: " + getParsedPath().getFilePath());
    URIish parsedUrl = new URIish(url);
    final URIish sourceUrl = new URIish(String.format("%s://%s/%s", parsedUrl.getScheme(), parsedUrl.getHost(), getParsedPath().getRepoPath()));
    logger.debug("Git Url: " + sourceUrl);
    this.gitDir = new File(dataDirectory, String.format("%s/%s/%s", parsedUrl.getHost(), getParsedPath().getRepoPath(), getParsedPath().getRepoBranch()));
    logger.debug("Temp Git Dir: " + getGitDir().getAbsolutePath());
    this.repository = new RepositoryBuilder().setWorkTree(getGitDir()).build();
    if (!getGitDir().exists()) {
      if (!getGitDir().mkdirs()) {
        throw new RuntimeException(getGitDir().getAbsolutePath());
      }
      getRepository().create(false);
    }
    this.remoteConfig = getRemoteConfig(sourceUrl, getRepository().getConfig());
    pull();
    this.localBase = this.getGitDir().toPath().toUri();
    this.univeralBase = new URI(sourceUrl.toString()).resolve(getParsedPath().getRepoBranch() + "/");
    logger.debug("Local Base: " + localBase());
    logger.debug("Universal Base: " + gitBase());

    this.innerFS = new LocalRepoFileSystem();
    getInnerFS().setWorkingDirectory(new Path(getGitDir().getAbsolutePath()));
    getInnerFS().setConf(parent.getConf());
  }

  public double getDismountPeriod() {
    return dismountPeriod;
  }

  public double getEagerPullPeriod() {
    return eagerPullPeriod;
  }

  public File getGitDir() {
    return gitDir;
  }

  public RawLocalFileSystem getInnerFS() {
    return innerFS;
  }

  public long getLastFetch() {
    return lastFetch;
  }

  public long getLastTouch() {
    return lastTouch;
  }

  public double getLazyPullPeriod() {
    return lazyPullPeriod;
  }

  public ParsePath getParsedPath() {
    return parsedPath;
  }

  public RemoteConfig getRemoteConfig() {
    return remoteConfig;
  }

  public Repository getRepository() {
    return repository;
  }

  @Override
  public URI getUri() {
    return gitBase();
  }

  @Override
  public Path getWorkingDirectory() {
    return new Path(gitBase());
  }

  @Override
  public void setWorkingDirectory(final Path new_dir) {
    throw new RuntimeException("Static Filesystem");
  }

  public boolean isDismountDelete() {
    return dismountDelete;
  }

  @Nonnull
  public Path toLocalPath(final Path path) {
    if (null == path) return null;
    return new Path(toLocalUrl(path.toUri()));
  }

  @Nonnull
  public Path toGitPath(final Path path) {
    if (null == path) return null;
    URI uri = toGitUrl(path.toUri());
    try {
      return new Path(new URI(uri.toString().replaceAll("^https?", "git")));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public URI toLocalUrl(final URI path) {
    if (null == path) return null;
    URI relativized = localBase().resolve(gitBase().relativize(path));
    logger.debug(String.format("Converted %s to %s", path, relativized));
    return relativized;
  }

  @Nonnull
  public URI toGitUrl(final URI path) {
    if (null == path) return null;
    URI relativized = gitBase().resolve(localBase().relativize(path));
    logger.debug(String.format("Converted %s to %s", path, relativized));
    return relativized;
  }

  public void pull() throws IOException {
    this.lastFetch = System.currentTimeMillis();
    CharSequence branch = getParsedPath().getRepoBranch();
    Collection<Ref> fetch = fetch(getRepository(), getRemoteConfig(), branch);
    checkout(getRepository(), fetch.stream().filter(x -> x.getName().equals("refs/heads/" + branch)).findAny()
        .orElseGet(() -> fetch.stream().filter(x -> x.getName().equals("HEAD")).findAny().get()));
  }

  @Override
  public FSDataInputStream open(final Path f, final int bufferSize) throws IOException {
    return getInnerFS().open(toLocalPath(f), bufferSize);
  }

  @Override
  public FileStatus[] listStatus(final Path f) throws IOException {
    return Arrays.stream(getInnerFS().listStatus(toLocalPath(f))).map(this::filter).toArray(i -> new FileStatus[i]);
  }

  @Override
  public FileStatus getFileStatus(final Path f) throws IOException {
    return getInnerFS().getFileStatus(toLocalPath(f));
  }

  public void touch() {
    this.lastTouch = System.currentTimeMillis();
    if (secondsSinceFetch() > getLazyPullPeriod()) try {
      pull();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public double secondsSinceFetch() {
    final long now = System.currentTimeMillis();
    return (now - this.getLastFetch()) / 1e3;
  }

  public double secondsSinceTouch() {
    final long now = System.currentTimeMillis();
    return (now - this.getLastTouch()) / 1e3;
  }

  public URI localBase() {
    return localBase;
  }

  public URI gitBase() {
    return univeralBase;
  }

  protected FileStatus filter(final FileStatus fileStatus) {
    FileStatus newObj = new FileStatus();
    newObj.setPath(toGitPath(fileStatus.getPath()));
    try {
      newObj.setSymlink(toGitPath(fileStatus.getSymlink()));
    } catch (IOException e) {
    }
    return newObj;
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
    logger.debug(String.format("Checked out %s: %s", tagName.getObjectId(), checkout));
    return checkout;
  }

  private Collection<Ref> fetch(final Repository repository, final RemoteConfig remoteConfig, final CharSequence repoBranch) {
    try (Transport transport = Transport.open(repository, remoteConfig)) {
      configure(transport);
      transport.setCheckFetchedObjects(false);
      transport.setRemoveDeletedRefs(false);
      transport.setDryRun(false);
      transport.setTagOpt(TagOpt.FETCH_TAGS);
      transport.setFetchThin(false);
      final ProgressMonitor monitor = new EmptyProgressMonitor() {
      };
      FetchResult result = transport.fetch(monitor, Arrays.asList(
          new RefSpec("refs/heads/" + repoBranch)
      ));
      logger.debug(String.format("Fetched %s: %s", result.getURI(), result.getMessages()));
      result.getAdvertisedRefs().stream().forEach(ref -> {
        logger.debug(String.format("Remote Ref: %s = %s", ref.getName(), ref.getObjectId()));
      });
      return result.getAdvertisedRefs();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private void configure(final Transport transport) {
    String username = getProperty("fs.jgit.auth.user", "").toString();
    if (!username.isEmpty()) {
      String password = getProperty("fs.jgit.auth.pass").toString();
      logger.debug(String.format("Login: %s %s", username, password.replaceAll(".", "*")));
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

  private class LocalRepoFileSystem extends RawLocalFileSystem {
    public LocalRepoFileSystem() {
      statistics = GitRepoFileSystem.this.statistics;
    }
  }
}
