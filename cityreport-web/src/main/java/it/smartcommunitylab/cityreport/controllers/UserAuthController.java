/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.cityreport.controllers;

import java.io.IOException;
import java.net.URLEncoder;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eu.trentorise.smartcampus.aac.AACException;
import eu.trentorise.smartcampus.aac.AACService;
import eu.trentorise.smartcampus.aac.model.TokenData;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.profileservice.BasicProfileService;
import eu.trentorise.smartcampus.profileservice.model.BasicProfile;
import it.smartcommunitylab.cityreport.model.Issuer;
import it.smartcommunitylab.cityreport.security.ReportUserDetails;
import it.smartcommunitylab.cityreport.services.IssuerManager;

/**
 * @author raman
 *
 */
@Controller
public class UserAuthController {

	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private RememberMeServices rememberMeServices;
	@Autowired
	private Environment env;
	@Autowired
	private IssuerManager issuerManager;

	private AACService service;
	private BasicProfileService profileService;
	
	@PostConstruct
	private void init() {
		service = new AACService(env.getProperty("ext.aacURL"), env.getProperty("ext.clientId"), env.getProperty("ext.clientSecret"));
		profileService = new BasicProfileService(env.getProperty("ext.aacURL"));
	}
	
	@RequestMapping("/userlogin")
	public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String url = service.generateAuthorizationURIForCodeFlow(env.getProperty("ext.redirect"), null, null, null);
		response.sendRedirect(url);
	}
	
	@RequestMapping("/userlogin/{authority}")
	public void loginAuthority(@PathVariable String authority, @RequestParam(required=false) String token, HttpServletResponse response) throws IOException {
		String url = service.generateAuthorizationURIForCodeFlow(env.getProperty("ext.redirect"), "/"+authority, null, null);
		if (token != null) {
			url += "&token="+token;
		}
		response.sendRedirect(url);
	}
	
	/**
	 * This is a callback for the external AAC OAuth2.0 authentication. 
	 * Exchanges code for token, recover the profile and creates the user.
	 * 
	 * @param request
	 * @param response
	 * @throws AACException 
	 * @throws SecurityException 
	 * @throws IOException 
	 */
	@RequestMapping("/ext_callback")
	public void callback(HttpServletRequest request, HttpServletResponse response) {
		try {
			TokenData tokenData = service.exchngeCodeForToken(request.getParameter("code"), env.getProperty("ext.redirect"));
			BasicProfile basicProfile = profileService.getBasicProfile(tokenData.getAccess_token());
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(basicProfile.getUserId(),basicProfile.getUserId(), ReportUserDetails.REPORTER_AUTHORITIES);
			token.setDetails(new WebAuthenticationDetails(request));                        
			Authentication authenticatedUser = authenticationManager.authenticate(token);
			SecurityContextHolder.getContext().setAuthentication(authenticatedUser);
			
			Issuer issuer = Issuer.fromUserProfile(basicProfile);
			issuerManager.saveIssuer(issuer);
			
			rememberMeServices.loginSuccess(request, response, authenticatedUser);
			response.sendRedirect("userloginsuccess?profile="+URLEncoder.encode(JsonUtils.toJSON(basicProfile),"UTF-8"));
		} catch (Exception e) {
			try {
				response.sendRedirect("userloginerror?error="+e.getMessage());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	@RequestMapping("/userloginsuccess")
	public String success(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return "userloginsuccess";
	}
	
	@RequestMapping("/userloginerror")
	public String error(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return "userloginerror";
	}
}
