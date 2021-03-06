package org.kylin.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.kylin.infrastructure.exception.InvalidJwtAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class JWTUtil {
    private static String secretKey = Base64.getEncoder().encodeToString("secret".getBytes());

    public static Authentication getJWTAuthentication(ServletRequest req) {
        String token = resolveToken((HttpServletRequest) req);
        if (token != null && validateToken(token)) {
            UserDetails userDetails = new UserDetails() {
                @Override
                public Collection<? extends GrantedAuthority> getAuthorities() {
                    return getRoles(token).stream().map(SimpleGrantedAuthority::new).collect(toList());
                }

                @Override
                public String getPassword() {
                    return "";
                }

                @Override
                public String getUsername() {
                    return JWTUtil.getUsername(token);
                }

                @Override
                public boolean isAccountNonExpired() {
                    return true;
                }

                @Override
                public boolean isAccountNonLocked() {
                    return true;
                }

                @Override
                public boolean isCredentialsNonExpired() {
                    return true;
                }

                @Override
                public boolean isEnabled() {
                    return true;
                }
            };
            return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        }
        return null;
    }

    private static String getUsername(String token) {
        return Jwts.parser().setSigningKey(secretKey)
                .parseClaimsJws(token).getBody().getSubject();
    }

    private static String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7, bearerToken.length());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getRoles(String resolvedToken) {
        Jws<Claims> claims = getClaims(resolvedToken);
        return (List<String>) claims.getBody().get("roles", List.class);
    }

    private static boolean validateToken(String token) {
        try {
            Jws<Claims> claims = getClaims(token);

            return !(claims.getBody().getExpiration().before(new Date()));
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtAuthenticationException("Expired or invalid JWT token");
        }
    }

    private static Jws<Claims> getClaims(String resolvedToken) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(resolvedToken);
    }
}

