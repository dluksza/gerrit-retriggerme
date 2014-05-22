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
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;

public class ConfigurationProvider {
  private final PluginConfig pluginConfig;

  @Inject
  ConfigurationProvider(
      PluginConfigFactory configFactory,
      @PluginName String pluginName) {
    pluginConfig = configFactory.getFromGerritConfig(pluginName);
  }

  public String getJenkinsUrl() {
    return pluginConfig.getString("jenkinsUrl", "http://localhost:9090/");
  }

  public String getSelfName() {
    return pluginConfig.getString("selfName", "gerrit");
  }
}
