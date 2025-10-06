package com.shutu.commons.security.cache;


import com.shutu.commons.security.properties.SecurityProperties;
import com.shutu.commons.security.user.UserDetail;
import com.shutu.commons.tools.redis.RedisUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 认证 Cache
 */

@Component
@AllArgsConstructor
public class TokenStoreCache {
    private final RedisUtils redisUtils;
    private final SecurityProperties securityProperties;

    public void saveUser(String accessToken, UserDetail user) {
        redisUtils.set(getCacheKey(accessToken), user, securityProperties.getAccessTokenExpire());
    }

    public void saveUser(String accessToken, UserDetail user, long expire) {
        redisUtils.set(getCacheKey(accessToken), user, expire);
    }

    public Long getExpire(String accessToken) {
        return redisUtils.getExpire(getCacheKey(accessToken));
    }

    public UserDetail getUser(String accessToken) {
        return (UserDetail) redisUtils.get(getCacheKey(accessToken));
    }

    public void deleteUser(String accessToken) {
        redisUtils.delete(getCacheKey(accessToken));
    }

    private String getCacheKey(String accessToken) {
        return "sys:token:" + accessToken;
    }
}
