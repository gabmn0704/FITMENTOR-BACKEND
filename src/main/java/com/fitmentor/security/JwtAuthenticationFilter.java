package com.fitmentor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads {@code Authorization: Bearer <jwt>} and places the verified principal in the
 * security context for this request only (stateless sessions). Revoked tokens are ignored,
 * so the request stays anonymous and gets the usual 401.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final SessionChecker sessions;

    public JwtAuthenticationFilter(JwtService jwtService, SessionChecker sessions) {
        this.jwtService = jwtService;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            jwtService.verify(header.substring(PREFIX.length()).trim())
                .filter(user -> !sessions.isRevoked(user))
                .ifPresent(user -> {
                    var authentication = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
        }
        chain.doFilter(request, response);
    }
}
