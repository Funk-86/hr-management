package org.example.hrmanagement.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    //生成签名密钥
    private SecretKey getSignKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    //生成Token（含本次登录选择的角色）
    public String generateToken(Long userId, String username, String roleCode) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roleCode", roleCode)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSignKey())
                .compact();
    }

    //解析Token
    public Claims parseToken(String token){
        try {
            return Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }

    //校验Token是否有效
    public boolean validateToken(String token){
        parseToken(token);
        return true;
    }

    //从Token中获取用户ID
    public Long getUserId(String token){
        Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    //从Token中获取用户名
    public String getUsername(String token){
        Claims claims = parseToken(token);
        return claims.get("username",String.class);
    }

    /** 从 Token 中获取本次登录选择的角色编码 */
    public String getRoleCode(String token) {
        Claims claims = parseToken(token);
        return claims.get("roleCode", String.class);
    }

    //判断Token是否过期
    public boolean isTokenExpired(String token){
        Claims claims = parseToken(token);
        return claims.getExpiration().before(new Date());
    }
}
