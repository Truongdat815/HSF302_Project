package com.fpt.elearning.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnthropicConfig {

    /**
     * Tao AnthropicClient doc API key tu bien moi truong ANTHROPIC_API_KEY.
     * Chay app: dat bien ANTHROPIC_API_KEY=sk-ant-... truoc khi start.
     *
     * Dung builder() thay fromEnv() de app KHONG crash khi chua co key
     * (chi loi khi that su goi API). ChatRagService da bat loi nay.
     */
    @Bean
    public AnthropicClient anthropicClient() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "missing-api-key"; // placeholder de boot duoc; goi API se loi
        }
        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
