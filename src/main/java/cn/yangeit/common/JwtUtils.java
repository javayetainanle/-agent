package cn.yangeit.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtils {
    private static final String DEFAULT_SECRET = "change-me-to-a-long-random-secret";
    private static final String SECRET = loadSecret();
    private static final long EXPIRE_MILLIS = 2_932_000_000L;

    private JwtUtils() {
    }

    private static String loadSecret() {
        String value = System.getenv("APP_JWT_SECRET");
        if (value == null || value.isBlank()) {
            return DEFAULT_SECRET;
        }
        return value;
    }

    /**
     * 生成 JWT 令牌
     */
    public static String generateJwt(Map<String, Object> claims) {
        return Jwts.builder()
                .addClaims(claims)
                .signWith(SignatureAlgorithm.HS256, SECRET.getBytes(StandardCharsets.UTF_8))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_MILLIS))
                .compact();
    }

    /**
     * 解析 JWT 令牌
     */
    public static Claims parseJWT(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }
}
