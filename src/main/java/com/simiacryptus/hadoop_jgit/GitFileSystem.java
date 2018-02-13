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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GitFileSystem extends ProxyFileSystem {
  private static final Map<URI, GitRepoFileSystem> cache = new HashMap<>();
  private static final Map<URI, ScheduledFuture<?>> pollingTasks = new HashMap<>();
  private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setDaemon(true).build());
  
  public GitFileSystem() {
    statistics = new Statistics("");
  }
  
  public Statistics getStats() {
    return statistics;
  }
  
  @Override
  public URI getUri() {
    try {
      return new URI("git:///");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  protected GitRepoFileSystem route(final Path f) {
    URI uri = f.toUri();
    ParsePath parsePath = new ParsePath(uri).invoke();
    try {
      URI basePath = new URI(String.format("https://%s/%s%s/", uri.getRawAuthority(), parsePath.getRepoPath(), parsePath.getRepoBranch()));
      return cache.computeIfAbsent(basePath, path -> {
        try {
          GitRepoFileSystem gitRepoFileSystem = new GitRepoFileSystem(path, GitFileSystem.this);
          gitRepoFileSystem.touch();
          pollingTasks.put(basePath, scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (gitRepoFileSystem.secondsSinceFetch() > gitRepoFileSystem.getEagerFetchPeriod()) {
              gitRepoFileSystem.fetch();
            }
            else if (gitRepoFileSystem.secondsSinceTouch() > gitRepoFileSystem.getDismountPeriod()) {
              if (gitRepoFileSystem.isDismountDelete()) {
                gitRepoFileSystem.getGitDir().delete();
              }
              cache.remove(basePath);
              pollingTasks.remove(basePath).cancel(false);
            }
          }, 1, 1, TimeUnit.SECONDS));
          return gitRepoFileSystem;
        } catch (IOException e) {
          throw new RuntimeException(e);
        } catch (URISyntaxException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  protected Path filter(final Path f) {
    URI uri = f.toUri();
    return new Path("https", uri.getRawAuthority(), uri.getRawPath());
  }
  
  
}
