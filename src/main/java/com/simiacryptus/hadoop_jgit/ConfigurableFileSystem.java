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

import org.apache.hadoop.fs.FileSystem;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public abstract class ConfigurableFileSystem extends FileSystem implements AutoCloseable {
  protected CharSequence getProperty(@Nonnull final String key) {
    return getProperty(key, () -> {
      throw new RuntimeException("No config found for " + key);
    });
  }

  protected CharSequence getProperty(@Nonnull final String key, CharSequence defaultValue) {
    return getProperty(key, () -> defaultValue);
  }

  protected CharSequence getProperty(@Nonnull final String key, @Nonnull Supplier<CharSequence> defaultValue) {
    CharSequence hadoopValue = getConf().get(key);
    CharSequence javaValue = System.getProperty(key);
    if (null == hadoopValue && null == javaValue) return defaultValue.get();
    if (null != hadoopValue && null != javaValue && !javaValue.equals(hadoopValue))
      throw new RuntimeException("Conflicting configs for " + key);
    return null == hadoopValue ? javaValue : hadoopValue;
  }
}
