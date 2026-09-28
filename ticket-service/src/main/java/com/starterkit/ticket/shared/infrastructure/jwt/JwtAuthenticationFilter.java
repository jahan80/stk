package com.starterkit.ticket.shared.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                     @NonNull HttpServletResponse res,
                                     @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        String token = null;
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        }

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parse(token);
                if (jwtService.isAccessToken(claims)) {
                    UserPrincipal p = new UserPrincipal(
                            jwtService.getUserId(claims),
                            jwtService.getUsername(claims),
                            jwtService.getEmail(claims),
                            jwtService.getRoles(claims),
                            jwtService.getPermissions(claims)
                    );
                    var auth = new UsernamePasswordAuthenticationToken(
                            p, null, p.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ex) {
                log.debug("JWT failed: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
