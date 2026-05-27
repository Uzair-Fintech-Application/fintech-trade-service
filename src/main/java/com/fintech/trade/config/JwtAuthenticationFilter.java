package com.fintech.trade.config;

import com.fintech.trade.constants.TradeConstants;
import com.fintech.trade.model.AuthenticatedUser;
import com.fintech.trade.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MessageSource messageSource;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader(TradeConstants.AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(TradeConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(TradeConstants.BEARER_PREFIX.length());

        if (jwt.isBlank()) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, msg("error.jwt.token.missing"));
            return;
        }

        try {
            if (jwtService.isTokenValid(jwt)) {
                String email = jwtService.extractUsername(jwt);
                Integer userId = jwtService.extractUserId(jwt);
                String role = jwtService.extractRole(jwt);

                AuthenticatedUser user = new AuthenticatedUser(userId, email, role);
                var authToken = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, msg("error.jwt.token.invalid"));
                return;
            }
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("JWT AUTH FAILED | reason=TOKEN_EXPIRED | uri={}", request.getRequestURI());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, msg("error.jwt.token.expired"));
            return;
        } catch (io.jsonwebtoken.MalformedJwtException ex) {
            log.warn("JWT AUTH FAILED | reason=MALFORMED_TOKEN | uri={}", request.getRequestURI());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, msg("error.jwt.token.malformed"));
            return;
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            log.warn("JWT AUTH FAILED | reason=INVALID_SIGNATURE | uri={}", request.getRequestURI());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, msg("error.jwt.token.signature.invalid"));
            return;
        } catch (io.jsonwebtoken.UnsupportedJwtException ex) {
            log.warn("JWT AUTH FAILED | reason=UNSUPPORTED_TOKEN | uri={}", request.getRequestURI());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, msg("error.jwt.token.unsupported"));
            return;
        } catch (Exception ex) {
            log.warn("JWT AUTH FAILED | reason=UNKNOWN | uri={} | error={}", request.getRequestURI(), ex.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, msg("error.jwt.auth.failed"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"Unauthorized\",\"message\":\"%s\"}",
                LocalDateTime.now(), status, message);
        response.getWriter().write(body);
    }

    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, Locale.getDefault());
    }
}
