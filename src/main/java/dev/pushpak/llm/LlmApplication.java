package dev.pushpak.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ImportRuntimeHints;

import dev.pushpak.llm.gh.GitHubConfiguration;

@ImportRuntimeHints(ResourceBundleRuntimeHints.class)
@EnableConfigurationProperties(GitHubConfiguration.class)
@SpringBootApplication
public class LlmApplication {

	public static void main(String[] args) {
		SpringApplication.run(LlmApplication.class, args);
	}

}
