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

import com.google.common.base.Optional;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.webui.UiAction;
import com.google.gerrit.server.change.ChangeResource;
import com.google.inject.Inject;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.luksza.gerrit.config.ConfigurationProvider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class PostRetrigger implements
    RestModifyView<ChangeResource, PostRetrigger.Input>,
    UiAction<ChangeResource> {
  private final Urls urls;
  private final ConfigurationProvider config;

  static class Input {
    int changeNo;
    int patchSetNo;
    String changeId;
    String revision;
  }

  @Inject
  PostRetrigger(Urls urls, ConfigurationProvider config) {
    this.urls = urls;
    this.config = config;
  }

  @Override
  public Object apply(ChangeResource resource, Input input)
      throws AuthException, BadRequestException, ResourceConflictException,
      Exception {
    HttpClient client = new DefaultHttpClient();
    Optional<String> sessioIdOpt = createSession(client);
    retriggerBuild(input, client, sessioIdOpt);
    return new Output(config.getJenkinsUrl());
  }

  @Override
  public UiAction.Description getDescription(ChangeResource resource) {
    return new Description().setEnabled(true).setLabel("Retrigger Me!")
        .setTitle("Retriggers jenkins verification job for this patch set.");
  }

  private static final String COOKIE = "Cookie";
  private static final String JSESSION = "JSESSION";
  private static final String SET_COOKIE = "Set-Cookie";
  private static final String SELECTED_ID_DELIMITER = ":";
  private static final String SELECTED_IDS = "selectedIds";
  private static final String QUERY_STRING = "queryString";
  private static final String SELECTED_SERVER = "selectedServer";

  private static final String STATUS_OPEN = "status:open";


  private Optional<String> createSession(HttpClient client)
      throws UnsupportedEncodingException, IOException, ClientProtocolException {
    HttpPost search = prepareFormPost(urls.getSearchUrl());
    search.setEntity(new UrlEncodedFormEntity(Arrays.asList(
        new BasicNameValuePair(SELECTED_SERVER, config.getSelfName()),
        new BasicNameValuePair(QUERY_STRING, STATUS_OPEN))));
    HttpResponse searchResp = fireRequest(client, search);
    Header[] cookies = searchResp.getHeaders(SET_COOKIE);
    for (Header cookie : cookies) {
      if (cookie.getValue().contains(JSESSION)) {
        return Optional.of(cookie.getValue());
      }
    }
    return Optional.absent();
  }

  private void retriggerBuild(Input input, HttpClient client,
      Optional<String> sessioIdOpt) throws UnsupportedEncodingException,
      IOException, ClientProtocolException {
    HttpPost build = prepareFormPost(urls.getBuildUrl());
    build.setHeader(COOKIE, sessioIdOpt.get());
    build.setEntity(new UrlEncodedFormEntity(
        Arrays
            .asList(new BasicNameValuePair(SELECTED_IDS, input.changeId
                + SELECTED_ID_DELIMITER + input.revision
                + SELECTED_ID_DELIMITER + input.changeNo
                + SELECTED_ID_DELIMITER + input.patchSetNo + "[]"))));
    fireRequest(client, build);
  }

  private HttpPost prepareFormPost(String url) {
    HttpPost post = new HttpPost(url);
    post.setHeader("Content-Type", "application/x-www-form-urlencoded");
    return post;
  }

  private HttpResponse fireRequest(HttpClient client, HttpPost req)
      throws IOException, ClientProtocolException {
    HttpResponse resp = client.execute(req);
    EntityUtils.consume(resp.getEntity());
    return resp;
  }

  private static class Output {
    String jenkinsUrl;

    Output(String jenkinsUrl) {
      this.jenkinsUrl = jenkinsUrl;
    }
  }
}
