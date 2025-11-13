package com.shutu.oauth.service.impl;

import com.shutu.oauth.config.OAuthProperties;
import com.shutu.oauth.model.entity.GiteeUserInfo;
import com.shutu.oauth.model.entity.OAuthUserInfo;
import com.shutu.oauth.service.OAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GiteeOAuthServiceImpl implements OAuthService {

    private final WebClient webClient;
    private final OAuthProperties oAuthProperties;

    @Override
    public String buildAuthorizeUrl(String state) {
        OAuthProperties.ClientProperties clientProperties = getClientProperties();
        return UriComponentsBuilder.fromHttpUrl(clientProperties.getAuthorizeUrl())
                .queryParam("client_id", clientProperties.getClientId())
                .queryParam("redirect_uri", clientProperties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", clientProperties.getScope())
                .queryParam("state", state)
                .build(true)
                .toUriString();
    }

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        String accessToken = getAccessToken(code);
        GiteeUserInfo userInfo = webClient.get()
                .uri(getClientProperties().getUserInfoUrl())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(GiteeUserInfo.class)
                .doOnError(e -> log.error("获取Gitee用户信息失败", e))
                .block();

        Assert.notNull(userInfo, "获取Gitee用户信息失败");
        return userInfo;
    }

    private String getAccessToken(String code) {
        OAuthProperties.ClientProperties clientProperties = getClientProperties();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("client_id", clientProperties.getClientId());
        formData.add("redirect_uri", clientProperties.getRedirectUri());
        formData.add("client_secret", clientProperties.getClientSecret());

        Map<String, Object> response = webClient.post()
                .uri(clientProperties.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .doOnError(e -> log.error("获取Gitee AccessToken失败", e))
                .block();

        Assert.notNull(response, "获取Gitee AccessToken失败");
        return (String) response.get("access_token");
    }

    @Override
    public String getPlatform() {
        return "gitee";
    }

    private OAuthProperties.ClientProperties getClientProperties() {
        return oAuthProperties.getClients().get(getPlatform());
    }
}