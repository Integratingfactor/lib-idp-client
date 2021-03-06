package com.integratingfactor.idp.lib.client.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.integratingfactor.idp.lib.client.model.IdpTokenValidation;
import com.integratingfactor.idp.lib.client.util.IdpOpenIdConnectClient;

public class IdpOpenIdConnectAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    private static Logger LOG = Logger.getLogger(IdpOpenIdConnectAuthenticationFilter.class.getName());

    @Autowired
    IdpOpenIdConnectClient openidConnectClient;

    @Autowired
    AuthenticationManager authManager;

    public IdpOpenIdConnectAuthenticationFilter() {
        super(new AntPathRequestMatcher(IdpOpenIdConnectClient.pathSuffixLogin, "GET"));
        LOG.info("Filter has been created and in place for openid connect authentication....");
    }

    protected IdpOpenIdConnectAuthenticationFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
        LOG.warning("Not sure why this constructor was called ????");
    }

    @Override
    public void afterPropertiesSet() {
        super.setAuthenticationManager(authManager);
    }

    /**
     * we have registered the filter with method "GET", this will intercept the
     * IDP redirect after openid connect authentication
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        LOG.info("attempt to authenticate request: " + request.getRequestURI());
        IdpTokenValidation userAuth = null;
        // first get user from session if already authenticated
        try {
            userAuth = (IdpTokenValidation) SecurityContextHolder.getContext().getAuthentication();
            if (userAuth != null)
                return userAuth;
        } catch (ClassCastException e) {
            LOG.warning("Found unknown authentication: " + e.getMessage());
        }

        // check if this is a redirect from IDP service after authentication
        // entry point sent there?
        if (IdpOpenIdConnectClient.pathSuffixLogin.equals(request.getRequestURI())) {
            Map<String, String> params = new HashMap<String, String>();
            for (Map.Entry<String, String[]> kv : request.getParameterMap().entrySet()) {
                params.put(kv.getKey(), kv.getValue()[0]);
            }
            userAuth = openidConnectClient.getValidatedUser(params);
        }
        if (userAuth == null) {
            LOG.info("Openid connect user information does not exists, user is not yet authenticated");
            // openid connect authentication is not done yet
            throw new AuthenticationCredentialsNotFoundException("OpenID Connect user not found");
        }
        LOG.info("User has been authenticated with role: " + userAuth.getAuthorities().toArray()[0]);
        return userAuth;
    }
}
