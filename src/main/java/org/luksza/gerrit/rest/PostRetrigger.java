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

package org.luksza.gerrit.rest;

import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.webui.UiAction;
import com.google.gerrit.server.change.ChangeResource;
import com.google.inject.Inject;

import org.luksza.gerrit.config.ConfigurationProvider;

public class PostRetrigger implements
    RestModifyView<ChangeResource, PostRetrigger.Input>,
    UiAction<ChangeResource> {
  private ConfigurationProvider config;

  static class Input {}

  @Inject
  PostRetrigger(ConfigurationProvider config) {
    this.config = config;
  }

  @Override
  public Object apply(ChangeResource resource, Input input)
      throws AuthException, BadRequestException, ResourceConflictException,
      Exception {
    return new Output(config.getJenkinsUrl());
  }

  @Override
  public UiAction.Description getDescription(
      ChangeResource resource) {
    return new Description().setEnabled(true).setLabel("Retrigger Me!")
        .setTitle("Retriggers jenkins verification job for this patch set.");
  }

  private static class Output {
    private String jenkinsUrl;

    Output(String jenkinsUrl) {
      this.jenkinsUrl = jenkinsUrl;
    }
  }
}
