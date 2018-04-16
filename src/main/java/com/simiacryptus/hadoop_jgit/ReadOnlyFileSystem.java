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

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

/**
 * The type Read only file system.
 */
public abstract class ReadOnlyFileSystem extends ConfigurableFileSystem {
  
  
  @Override
  public FSDataOutputStream create(final Path f, final FsPermission permission, final boolean overwrite, final int bufferSize, final short replication, final long blockSize, final Progressable progress) {
    throw new RuntimeException("Read-Only Filesystem");
  }
  
  @Override
  public FSDataOutputStream append(final Path f, final int bufferSize, final Progressable progress) {
    throw new RuntimeException("Read-Only Filesystem");
  }
  
  @Override
  public boolean rename(final Path src, final Path dst) {
    throw new RuntimeException("Read-Only Filesystem");
  }
  
  @Override
  public boolean delete(final Path f, final boolean recursive) {
    throw new RuntimeException("Read-Only Filesystem");
  }
  
  @Override
  public boolean mkdirs(final Path f, final FsPermission permission) {
    throw new RuntimeException("Read-Only Filesystem");
  }
}
