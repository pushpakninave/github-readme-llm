package dev.pushpak.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.pushpak.llm.gh.GitHubService;
import dev.pushpak.llm.local.LocalFileService;

import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class ContentGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(ContentGeneratorService.class);
    private final GitHubService ghService;
    private final LocalFileService localFileService;
    @Value("${app.output.directory}")
    private String outputDirectory;

    public ContentGeneratorService(GitHubService ghService, LocalFileService localFileService) {
        this.ghService = ghService;
        this.localFileService = localFileService;
    }

    public String generateContent(String githubUrl, String localPath) throws Exception {
        if (githubUrl != null && !githubUrl.isBlank()) {
            log.info("Processing GitHub URL: {}", githubUrl);
            String[] parts = githubUrl.split("/");
            String owner = parts[parts.length - 2];
            String repo = parts[parts.length - 1];
            ghService.downloadRepositoryContents(owner, repo);
            return new String(Files.readAllBytes(Paths.get(outputDirectory, repo + ".md")));
        } else if (localPath != null && !localPath.isBlank()) {
            log.info("Processing local path: {}", localPath);
            String outputName = Paths.get(localPath).getFileName().toString();
            localFileService.processLocalDirectory(localPath, outputName);
            return new String(Files.readAllBytes(Paths.get(outputDirectory, outputName + ".md")));
        } else {
            throw new IllegalArgumentException("Either GitHub URL or local path must be provided");
        }
    }
}
