package com.example.orange.config;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }

  /*  @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        // Custom logic for authentication if needed
        return super.attemptAuthentication((jakarta.servlet.http.HttpServletRequest) request, (jakarta.servlet.http.HttpServletResponse) response);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        // Generate JWT token and add it to the response header
        String token = "your_generated_jwt_token_here"; // Replace with actual token generation logic
        response.addHeader("Authorization", "Bearer " + token);

        // Call the superclass method if needed, or continue the filter chain
        chain.doFilter(request, response);
    }*/
}