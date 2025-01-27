package dev.pushpak.llm.gh;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(value = "github")
public record GitHubConfiguration(String token, List<String> includePatterns,
        @DefaultValue("") List<String> excludePatterns) {
    
    // Constructor
    public GitHubConfiguration {
        if (includePatterns == null) {
            throw new IllegalArgumentException("includePatterns must not be null");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("GitHub token must not be null or blank");
        }
        if (excludePatterns == null) {
            excludePatterns = List.of();
        }
    }
}
