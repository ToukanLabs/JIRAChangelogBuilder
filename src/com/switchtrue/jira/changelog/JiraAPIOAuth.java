package com.switchtrue.jira.changelog;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import com.atlassian.httpclient.api.Request;
import com.atlassian.jira.rest.client.AuthenticationHandler;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.JiraRestClientFactory;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.oauth.client.example.AtlassianOAuthClient;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.ParameterStyle;
import net.oauth.client.OAuthClient;
import net.oauth.http.HttpMessage;

public class JiraAPIOAuth extends JiraAPI {
	
	private final String consumerKey_, accessToken_, privateKey_, callbackUrl_; 
	
	public JiraAPIOAuth(String consumerKey, String privateKey, String accessToken, String callbackUrl, String URL, String jql, String descriptionField, String fixVersionRestrictMode, String fixVersionRestrictTerm) {
		super(URL, jql, descriptionField, fixVersionRestrictMode, fixVersionRestrictTerm);
		consumerKey_ = consumerKey;
		accessToken_ = accessToken;
		privateKey_ = privateKey;
		callbackUrl_ = callbackUrl;
	}
	
	@Override
	public JiraRestClient getRestClient() {
		JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
		final AtlassianOAuthClient aoac = new AtlassianOAuthClient(consumerKey_, privateKey_, jiraServerURI_.toString(), callbackUrl_);
  		final JiraRestClient restClient = factory.create(jiraServerURI_, new AuthenticationHandler() {
  			
  			/** 
  			 * Code stolen from stackoverflow.com/q/10701515
  			 * @author Abbas
  			 */
  			public void configure(Request request) {
  				try {
  					OAuthAccessor accessor = aoac.getAccessor();
  					accessor.accessToken = accessToken_;
  					OAuthMessage request2 = accessor.newRequestMessage(null, request.getUri().toString(), Collections.<Map.Entry<?, ?>>emptySet(), request.getEntityStream());
  					Object accepted = accessor.consumer.getProperty(OAuthConsumer.ACCEPT_ENCODING);
  					if (accepted != null) {
  						request2.getHeaders().add(new OAuth.Parameter(HttpMessage.ACCEPT_ENCODING, accepted.toString()));
  					}
  					Object ps = accessor.consumer.getProperty(OAuthClient.PARAMETER_STYLE);
  					ParameterStyle style = (ps == null) ? ParameterStyle.BODY : Enum.valueOf(ParameterStyle.class, ps.toString());
  					HttpMessage httpRequest = HttpMessage.newRequest(request2, style);
  					for (Entry<String, String> ap : httpRequest.headers)
  						request.setHeader(ap.getKey(), ap.getValue());
  					request.setUri(httpRequest.url.toURI());
  				} catch (Exception e) {
  					e.printStackTrace();

  				}
  			}
  		});
        return restClient;
	}
	
}
