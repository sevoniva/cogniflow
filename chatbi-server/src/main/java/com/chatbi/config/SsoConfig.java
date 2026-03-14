package com.chatbi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * SSO OAuth2 配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SsoConfig {

    /**
     * 自定义 OAuth2 用户服务
     */
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        return new CustomOAuth2UserService();
    }

    /**
     * 自定义 OAuth2 用户服务实现
     */
    static class CustomOAuth2UserService extends DefaultOAuth2UserService {

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) {
            OAuth2User oauth2User = super.loadUser(userRequest);

            // 获取用户信息
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            log.info("OAuth2 登录 - Provider: {}", registrationId);

            // 根据 provider 处理不同平台的用户信息
            switch (registrationId) {
                case "google":
                    return processGoogleUser(oauth2User);
                case "github":
                    return processGithubUser(oauth2User);
                case "wechat":
                    return processWechatUser(oauth2User);
                case "dingtalk":
                    return processDingtalkUser(oauth2User);
                default:
                    return oauth2User;
            }
        }

        private OAuth2User processGoogleUser(OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            log.info("Google 用户登录：{}", email != null ? email : "unknown");
            return oauth2User;
        }

        private OAuth2User processGithubUser(OAuth2User oauth2User) {
            String login = oauth2User.getAttribute("login");
            log.info("GitHub 用户登录：{}", login != null ? login : "unknown");
            return oauth2User;
        }

        private OAuth2User processWechatUser(OAuth2User oauth2User) {
            log.info("微信用户登录", new Object[0]);
            return oauth2User;
        }

        private OAuth2User processDingtalkUser(OAuth2User oauth2User) {
            log.info("钉钉用户登录", new Object[0]);
            return oauth2User;
        }
    }
}
