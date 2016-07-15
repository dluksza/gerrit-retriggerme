// Copyright (C) 2014 Dariusz Luksza
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.luksza.gerrit.config;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;

public class ConfigurationProvider {

  private static final String JENKINSURL = "jenkinsUrl";
  private static final String SELFNAME = "selfName";
  private static final String DEFAULT_JENKINSURL = "http://localhost:9090/";
  private static final String DEFAULT_SELFNAME = "gerrit";

  private final PluginConfigFactory configFactory;
  private final String pluginName;
  private final String defaultJenkinsUrl;
  private final String defaultSelfName;

  @Inject
  ConfigurationProvider(
      PluginConfigFactory configFactory,
      @PluginName String pluginName) {
    this.configFactory = configFactory;
    this.pluginName = pluginName;

    // Read default configuration from gerrit.config, for backward compatibility
    PluginConfig pluginConfig = configFactory.getFromGerritConfig(pluginName);
    this.defaultJenkinsUrl = pluginConfig.getString(JENKINSURL, DEFAULT_JENKINSURL);
    this.defaultSelfName = pluginConfig.getString(SELFNAME, DEFAULT_SELFNAME);
  }

  public String getJenkinsUrl(Project.NameKey project) {
    try {
      return configFactory.getFromProjectConfigWithInheritance(project, pluginName)
        .getString(JENKINSURL, defaultJenkinsUrl);
    } catch(NoSuchProjectException e) {
      return defaultJenkinsUrl;
    }
  }

  public String getSelfName(Project.NameKey project) {
    try {
      return configFactory.getFromProjectConfigWithInheritance(project, pluginName)
        .getString(SELFNAME, defaultSelfName);
    } catch(NoSuchProjectException e) {
      return defaultSelfName;
    }
  }
}
