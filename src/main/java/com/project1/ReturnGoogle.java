package com.project1;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;

import com.db.Connector;
import com.db.User;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Servlet implementation class ReturnGoogle
 */
public class ReturnGoogle extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ReturnGoogle() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String forward="messageBoard.jsp";
		String returnURL = "http://localhost:8080/project1/ReturnGoogle";
		String query1 = request.getRequestURL().append("?").append(request.getQueryString()).toString();
		URI s = null;
		try {
			s = new URI(query1);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AuthenticationResponse authResponse = null;
		try {
			authResponse = AuthenticationResponseParser.parse(s);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (authResponse instanceof AuthenticationErrorResponse) {
			// TODO: error handling: authentication error
		}

		AuthenticationSuccessResponse authzSuccess = (AuthenticationSuccessResponse) authResponse;
		AuthorizationCode code = authzSuccess.getAuthorizationCode();
		com.nimbusds.oauth2.sdk.id.State state = authzSuccess.getState();
		String state1 = state.toString();
		String state2 = request.getSession().getAttribute("state").toString();
		if (state1.equals(state2) == false) {
			response.getWriter().print("CSRF attack attempted");
			forward="/Error.jsp";
			return;
		}

		ClientID clientID = new ClientID("xxx.apps.googleusercontent.com");
		Secret clientSecret = new Secret("xxx");
		ClientAuthentication clientAuth = new ClientSecretPost(clientID, clientSecret);

		URI tokenEndpointURL = null;
		try {
			tokenEndpointURL = new URI("https://www.googleapis.com/oauth2/v4/token");
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TokenRequest accessTokenRequest = null;
		try {
			accessTokenRequest = new TokenRequest(tokenEndpointURL, clientAuth,
					new AuthorizationCodeGrant(code, new URI(returnURL)));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HTTPRequest httpRequest;
		httpRequest = accessTokenRequest.toHTTPRequest();
		HTTPResponse httpResponse = httpRequest.send();

		TokenResponse tokenResponse = null;
		try {
			tokenResponse = OIDCTokenResponseParser.parse(httpResponse);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (tokenResponse instanceof TokenErrorResponse) {
			// do error handling

			TokenErrorResponse tokenError = (TokenErrorResponse) tokenResponse;
			response.getWriter().println("Error on receiving token" + tokenError);
			return;
		}
		OIDCTokenResponse tokenSuccess = (OIDCTokenResponse) tokenResponse;
		BearerAccessToken accessToken = (BearerAccessToken) tokenSuccess.getOIDCTokens().getBearerAccessToken();
		RefreshToken refreshToken = tokenSuccess.getOIDCTokens().getRefreshToken();
		JWT idToken = tokenSuccess.getOIDCTokens().getIDToken();
		// just for testing:
		net.minidev.json.JSONObject jsonObject = null;
		try {
			jsonObject = idToken.getJWTClaimsSet().toJSONObject();
		} catch (java.text.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String email = jsonObject.get("email").toString();
		String name = jsonObject.get("name").toString();

		/* ---- CHECK TO OUR DB BEGIN ---- */
		String realm = "GOOGLE"; // Change this parameter based what you need
		int uid = 0;

		Session session = null;
		Transaction tx = null;
		try {
			Connector conn = new Connector();
			session = conn.configureSessionFactory().openSession();
			tx = session.beginTransaction();

			String hsql = "from User where email = :user_email and realm = :login_realm";
			Query query = session.createQuery(hsql);
			query.setParameter("user_email", email);
			query.setParameter("login_realm", realm);
			List<User> result = query.list();

			if (result.size() != 0) {
				// USER IS EXIST, DO THE AUTHORIZATION METHOD
				for (User u : result) {
					System.out.println("Id: " + u.getId() + " | Name:" + u.getName() + " | Email:" + u.getEmail()
							+ " | Realm:" + u.getRealm());
					uid = u.getId();
					String at=accessToken.toString();
					HttpSession session1 = request.getSession(true);
					session1.setAttribute("realm", "google");
					session1.setAttribute("user", name);
					session1.setAttribute("email", email);
					session1.setAttribute("at", at);
					session1.setAttribute("uid", uid);
				}
			} else {
				// USER IS NOT EXIST, STORE IN DB
				try {
					User newUser = new User(uid, name, email, realm);
					session.save(newUser);
					uid = newUser.getId();

				} catch (Exception ex) {
					ex.printStackTrace();
					tx.rollback();
				}

				HttpSession session1 = request.getSession(true);
				session1.setAttribute("realm", "google");
				session1.setAttribute("user", name);
				session1.setAttribute("email", email);
				session1.setAttribute("at", accessToken);
				session1.setAttribute("uid", uid);
			}
		} catch (Exception ex) {
			response.getWriter().println(ex.getMessage());
			ex.printStackTrace();
		} finally {
			if (session != null && session.isOpen()) {
				tx.commit();
				session.flush();
				session.close();
			}
			response.sendRedirect(forward);
		}
		/* ---- CHECK TO OUR DB END ---- */
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
