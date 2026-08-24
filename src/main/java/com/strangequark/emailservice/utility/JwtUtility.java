// Integration file: Auth

package com.strangequark.emailservice.utility;

import io.jsonwebtoken.Claims; // Integration line: Telemetry
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Key;
import java.util.List;

@Service
public class JwtUtility {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtility.class);

    @Value("${ACCESS_SECRET_KEY}")
    private String SECRET_KEY;

    public boolean validateEmailApiAccess() {
        LOGGER.debug("Attempting to validate email API access");

        try {
            String token = getTokenFromHeader();
            Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            List<String> authorizations = claims.get("authorizations", List.class);

            if(authorizations == null || !authorizations.contains("EMAIL_API_ACCESS")) {
                LOGGER.error("JWT does not have EMAIL_API_ACCESS");
                return false;
            }

            LOGGER.debug("JWT has email API access");
            return true;
        } catch (Exception ex) {
            LOGGER.error("Failed to validate email API access: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return false;
        }
    }

    private String getTokenFromHeader() {
        LOGGER.debug("Attempting to get token from header");
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                throw new IllegalStateException("No request context available");
            }

            HttpServletRequest request = attrs.getRequest();
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Missing or invalid Authorization header");
            }

            LOGGER.debug("Token successfully retrieved from header");
            return authHeader.substring(7); // Remove "Bearer "
        } catch (Exception ex) {
            LOGGER.error("Failed to get token from header: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return null;
        }
    }
    // Integration function start: Telemetry
    public String extractId() {
        LOGGER.debug("Attempting to extract subject from JWT");

        String token = getTokenFromHeader();
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        LOGGER.debug("Subject successfully extracted from JWT");
        return claims.getId();
    }

    public boolean isTokenValid(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));

            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    // Integration function end: Telemetry
}
