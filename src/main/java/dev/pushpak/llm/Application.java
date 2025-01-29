package dev.pushpak.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportRuntimeHints;

import dev.pushpak.llm.gh.GitHubConfiguration;
import dev.pushpak.llm.lib.ResourceBundleRuntimeHints;

@ImportRuntimeHints(ResourceBundleRuntimeHints.class)
@EnableConfigurationProperties(GitHubConfiguration.class)
@SpringBootApplication
@ComponentScan(basePackages = "dev.pushpak.llm.controller")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
