package com.InterviewManager.interview_slot_manager.service;

import com.InterviewManager.interview_slot_manager.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtBlacklistService
{

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String BLACKLIST_PREFIX = "jwt_blacklist:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";

    public void blacklistToken(String token)
    {
        try
        {
            Date expiration = jwtUtil.extractExpiration(token);
            if (expiration != null)
            {
                long ttl = expiration.getTime() - System.currentTimeMillis();
                if (ttl > 0)
                {
                    String key = BLACKLIST_PREFIX + token;
                    redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
                    log.info("Token blacklisted successfully with TTL: {} ms", ttl);
                } else
                {
                    log.warn("Token is already expired, not adding to blacklist");
                }
            }
        } catch (Exception e)
        {
            log.error("Error blacklisting token: {}", e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String token)
    {
        try
        {
            String key = BLACKLIST_PREFIX + token;
            return redisTemplate.hasKey(key);
        } catch (Exception e)
        {
            log.error("Error checking blacklist status: {}", e.getMessage());
            return false;
        }
    }

    public void blacklistUserTokens(String username)
    {
        try
        {
            String userTokensKey = USER_TOKENS_PREFIX + username;

            var tokens = redisTemplate.opsForSet().members(userTokensKey);
            if (tokens != null)
            {
                for (String token : tokens)
                {
                    blacklistToken(token);
                }
                redisTemplate.delete(userTokensKey);
                log.info("Blacklisted {} tokens for user: {}", tokens.size(), username);
            }
        } catch (Exception e)
        {
            log.error("Error blacklisting user tokens: {}", e.getMessage());
        }
    }

    public void addUserToken(String username, String token)
    {
        try
        {
            String userTokensKey = USER_TOKENS_PREFIX + username;
            Date expiration = jwtUtil.extractExpiration(token);

            if (expiration != null)
            {
                long ttl = expiration.getTime() - System.currentTimeMillis();
                if (ttl > 0)
                {
                    redisTemplate.opsForSet().add(userTokensKey, token);

                    redisTemplate.expire(userTokensKey, ttl, TimeUnit.MILLISECONDS);
                    log.info("Added token to user's active tokens set: {}", username);
                }
            }
        } catch (Exception e)
        {
            log.error("Error adding user token: {}", e.getMessage());
        }
    }

    public void removeUserToken(String username, String token)
    {
        try
        {
            String userTokensKey = USER_TOKENS_PREFIX + username;
            redisTemplate.opsForSet().remove(userTokensKey, token);
            log.info("Removed token from user's active tokens set: {}", username);
        } catch (Exception e)
        {
            log.error("Error removing user token: {}", e.getMessage());
        }
    }
}
