package org.jboss.test;

import com.google.common.base.Strings;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogoutOnNonOwnerNodeDifferentAppTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutOnNonOwnerNodeDifferentAppTest.class);

    // the nodes should be on the same host - else the cookies for a single node won't matter to the other
    private static final String TEST_PROP_HOST = "testProp.host";
    private static final String TEST_PROP_PORT_1 = "testProp.port1";
    private static final String TEST_PROP_PORT_2 = "testProp.port2";
    private static final String HOST_DEFAULT = "127.0.0.1";
    private static final String PORT_1_DEFAULT = "8080";
    private static final String PORT_2_DEFAULT = "8180";

    private static final String AUTH_CHALLENGE_STRING = "loginForm";
    private static final String WEBAPP_A_CONTEXT = "/webapp_a/index.jsf";
    private static final String WEBAPP_B_CONTEXT = "/webapp_b/index.jsf";
    private static final String WEBAPP_A_AUTH_CONTEXT = WEBAPP_A_CONTEXT.replace("index.jsf", "login.jsf");

    private static final String USER = "user";
    private static final String PASSWORD = "secret_Passw0rd";

    private static String node1Address;
    private static String node2Address;

    @BeforeClass
    public static void setUpClass() {
        node1Address = "http://" + getPropertyOrDefault(TEST_PROP_HOST, HOST_DEFAULT) + ":" + getPropertyOrDefault(TEST_PROP_PORT_1, PORT_1_DEFAULT);
        node2Address = "http://" + getPropertyOrDefault(TEST_PROP_HOST, HOST_DEFAULT) + ":" + getPropertyOrDefault(TEST_PROP_PORT_2, PORT_2_DEFAULT);

        LOGGER.debug("Node 1: {}", node1Address);
        LOGGER.debug("Node 2: {}", node2Address);
    }

    @Test
    public void logoutOnNonOwnerNodeDifferentAppTest() throws IOException {
        CookieStore basicCookieStore = new BasicCookieStore();
        try (CloseableHttpClient closeableHttpClient = HttpClients.custom().setDefaultCookieStore(basicCookieStore).build()) {
        	String step1 = "1.- GET  - (node1-webapp_a) - verify we're not authenticated";
            String body = getResponseBody(closeableHttpClient.execute(new HttpGet(node1Address + WEBAPP_A_CONTEXT)));
			logRequestResponse(step1, node1Address + WEBAPP_A_CONTEXT, body);
            Assert.assertTrue("The first request does not require authentication", body.contains(AUTH_CHALLENGE_STRING));
            String viewState = getViewState(body);

            String step2 = "2.- POST - (node1-webapp_a) - authenticate ";
            HttpUriRequest httpUriRequest = RequestBuilder
                    .post()
                    .setUri(node1Address + WEBAPP_A_AUTH_CONTEXT)
                    .addParameter("loginForm", "loginForm")
                    .addParameter("loginForm:txtUsername", USER)
                    .addParameter("loginForm:txtPassword", PASSWORD)
                    .addParameter("loginForm:btnIngresar", "login")
                    .addParameter("javax.faces.ViewState", viewState)
                    .build();
            body = getResponseBody(closeableHttpClient.execute(httpUriRequest));
            logRequestResponse(step2, node1Address + WEBAPP_A_AUTH_CONTEXT, body);
            Assert.assertFalse("First authentication did not succeed", body.contains(AUTH_CHALLENGE_STRING));

            String step3 = "3.- GET  - (node1-webapp_a) - verify we're authenticated";
            body = getResponseBody(closeableHttpClient.execute(new HttpGet(node1Address + WEBAPP_A_CONTEXT)));
            logRequestResponse(step3, node1Address + WEBAPP_A_CONTEXT, body);
            Assert.assertFalse("First authentication did not succeed - subsequent GET failed", body.contains(AUTH_CHALLENGE_STRING));

            String step4 ="4.-      - (node1-webapp_a) - we extract the session id.";
            String sessionIdAfterLogin = getSessionIDFromBody(body);
            LOGGER.info(" Step: {} sessionIdAfterLogin:{}", step4, sessionIdAfterLogin);

            String step5 ="5.- GET  - (node2-webapp_b) - verify we're authenticated";
            body = getResponseBody(closeableHttpClient.execute(new HttpGet(node2Address + WEBAPP_B_CONTEXT)));
            logRequestResponse(step5, node2Address + WEBAPP_B_CONTEXT, body);
            Assert.assertFalse("First authentication did not succeed - we're not authenticated on second node", body.contains(AUTH_CHALLENGE_STRING));

            String step6 = "6.- POST - (node2-webapp_b) - log out";
        	viewState = getViewState(body);
            httpUriRequest = RequestBuilder
                    .post()
                    .setUri(node2Address + WEBAPP_B_CONTEXT)
                    .addParameter("frmCerrarSesion", "frmCerrarSesion")
                    .addParameter("javax.faces.ViewState", viewState)
                    .addParameter("frmCerrarSesion:lnkLogout", "frmCerrarSesion:lnkLogout")
                    .build();
            body = getResponseBody(closeableHttpClient.execute(httpUriRequest));
            logRequestResponse(step6, node2Address + WEBAPP_B_CONTEXT, body);

            String step7 = "7.- GET  - (node2-webapp_b) - verify we need to authenticate";
            body = getResponseBody(closeableHttpClient.execute(new HttpGet(node2Address + WEBAPP_B_CONTEXT)));
            logRequestResponse(step7, node2Address + WEBAPP_B_CONTEXT, body);
            Assert.assertTrue("We're still authenticated after we logged out (invalidated the last session)", body.contains(AUTH_CHALLENGE_STRING));

            String step8 = "8.- GET  - (node1-webapp_a) - verify we need to authenticate";
            body = getResponseBody(closeableHttpClient.execute(new HttpGet(node1Address + WEBAPP_A_CONTEXT)));
            logRequestResponse(step8, node1Address + WEBAPP_A_CONTEXT, body);
            Assert.assertTrue("We're still authenticated after we logged out (invalidated the last session)", body.contains(AUTH_CHALLENGE_STRING));

            String step9 = "9.-      - (node1-webapp_a) - we extract the session id.";
            String sessionIdAfterLogout = getSessionIDFromBody(body);
            LOGGER.info("Step: {} sessionIdAfterLogout:{}", step9, sessionIdAfterLogout);

            String step10 ="10.-     - (node1-webapp_a) - we validate that the session id from the first login is different after logout.";
            LOGGER.info("Step: {}  \n sessionIdAfterLogin: {} \n sessionIdAfterLogout:{}", step10, sessionIdAfterLogin, sessionIdAfterLogout);
            Assert.assertFalse("The session id since login is still the same after logout on node1-webapp_a.", sessionIdAfterLogin.equals(sessionIdAfterLogout));
        }
    }

	private String getSessionIDFromBody(String body) {
		Pattern pattern = Pattern.compile("<label>SessionID:.*?</label>");
		Matcher matcher = pattern.matcher(body);
		matcher.find();
		return matcher.group();
	}

	private String getViewState(String body) {
		Pattern pattern = Pattern.compile("-?\\d+:-?\\d+");
		Matcher matcher = pattern.matcher(body);
		matcher.find();
		return matcher.group();
	}

    private static String getPropertyOrDefault(String propName, String defaultValue) {
        String propValue = System.getProperty(propName);
        return Strings.isNullOrEmpty(propValue) ? defaultValue : propValue;
    }

    private static void logRequestResponse(String step, String address, String responseBody) {
        LOGGER.info("Step: {}", step);
    	LOGGER.trace("Request to {}, response:\n{}", address, responseBody);
    }

    private static void logCookies(CookieStore cookieStore) {
        LOGGER.trace("Cookies present in the cookie store: {}", cookieStore.getCookies());
    }

    private String getResponseBody(CloseableHttpResponse closeableHttpResponse) throws IOException {
        StatusLine statusLine = closeableHttpResponse.getStatusLine();

        String content;
        try {
            HttpEntity httpEntity = closeableHttpResponse.getEntity();
            content = IOUtils.toString(httpEntity.getContent(), Charset.defaultCharset());
            EntityUtils.consume(httpEntity);
        } finally {
            closeableHttpResponse.close();
        }

        if (closeableHttpResponse.getStatusLine().getStatusCode() >= 400) {
            LOGGER.error("Request failed with {}, response body is: \n{}", statusLine, content);
        }

        return content;
    }
}
