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
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.webui.UiAction;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.change.ChangeResource;
import com.google.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.luksza.gerrit.RetriggerCapability;
import org.luksza.gerrit.config.ConfigurationProvider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

@RequiresCapability(RetriggerCapability.NAME)
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
    Project.NameKey project = resource.getChange().getProject();
    String jenkinsUrl = config.getJenkinsUrl(project);
    String errorMessage = null;
    try (CloseableHttpClient client = HttpClients.custom().build()) {
      Optional<Header> authOpt = getAuth(jenkinsUrl);
      Optional<Header> crumbOpt = getCrumb(client, project, authOpt);
      Optional<String> sessionIdOpt = createSession(client, project, crumbOpt, authOpt);
      retriggerBuild(input, client, project, sessionIdOpt, crumbOpt, authOpt);
    } catch(IOException e) {
      errorMessage = e.getMessage();
    }
    return new Output(jenkinsUrl, errorMessage);
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

  private Optional<Header> getAuth(String jenkinsUrl) {
    String userName = config.getJenkinsUserName(jenkinsUrl);
    String token = config.getJenkinsToken(jenkinsUrl);
    if (userName == null || token == null) {
      return Optional.absent();
    }

    String auth = userName + ":" + token;
    byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("UTF-8")));
    Header header = new BasicHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuth));
    return Optional.of(header);
  }

  private Optional<Header> getCrumb(HttpClient client, Project.NameKey project, Optional<Header> auth) {
    HttpGet get = new HttpGet(urls.getCrumbUrl(project));
    if (auth.isPresent()) {
      get.setHeader(auth.get());
    }
    try {
  	  HttpResponse response = client.execute(get);
      String[] crumb = EntityUtils.toString(response.getEntity()).split(":", 2);
      Header header = new BasicHeader(crumb[0], crumb[1]);
      return Optional.of(header);
    } catch(IOException e) {
      return Optional.absent();
    }
  }

  private Optional<String> createSession(HttpClient client, Project.NameKey project,
      Optional<Header> crumb, Optional<Header> auth) throws UnsupportedEncodingException,
      IOException, ClientProtocolException {
    HttpPost search = prepareFormPost(urls.getSearchUrl(project), crumb, auth);
    search.setEntity(new UrlEncodedFormEntity(Arrays.asList(
        new BasicNameValuePair(SELECTED_SERVER, config.getSelfName(project)),
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

  private void retriggerBuild(Input input, HttpClient client, Project.NameKey project,
      Optional<String> sessionIdOpt, Optional<Header> crumb, Optional<Header> auth)
      throws UnsupportedEncodingException, IOException, ClientProtocolException {
    HttpPost build = prepareFormPost(urls.getBuildUrl(project), crumb, auth);
    if (sessionIdOpt.isPresent()) {
      build.setHeader(COOKIE, sessionIdOpt.get());
    }
    build.setEntity(new UrlEncodedFormEntity(
        Arrays
            .asList(new BasicNameValuePair(SELECTED_IDS, input.changeId
                + SELECTED_ID_DELIMITER + input.revision
                + SELECTED_ID_DELIMITER + input.changeNo
                + SELECTED_ID_DELIMITER + input.patchSetNo + "[]"))));
    fireRequest(client, build);
  }

  private HttpPost prepareFormPost(String url, Optional<Header> crumb, Optional<Header> auth) {
    HttpPost post = new HttpPost(url);
    post.setHeader("Content-Type", "application/x-www-form-urlencoded");
    if (crumb.isPresent()) {
      post.setHeader(crumb.get());
    }
    if (auth.isPresent()) {
      post.setHeader(auth.get());
    }
    return post;
  }

  private HttpResponse fireRequest(HttpClient client, HttpPost req)
      throws IOException, ClientProtocolException {
    HttpResponse resp = client.execute(req);
    int code = resp.getStatusLine().getStatusCode();
    if (code < HttpStatus.SC_OK || code >= HttpStatus.SC_BAD_REQUEST) {
      throw new IOException(resp.getStatusLine().getReasonPhrase());
    }
    EntityUtils.consume(resp.getEntity());
    return resp;
  }

  private static class Output {
    String jenkinsUrl;
    String errorMessage;

    Output(String jenkinsUrl, String errorMessage) {
      this.jenkinsUrl = jenkinsUrl;
      this.errorMessage = errorMessage;
    }
  }
}
