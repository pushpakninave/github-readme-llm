File: src/main/java/dev/danvega/cg/Application.java

package dev.danvega.cg;

import dev.danvega.cg.gh.GitHubConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ImportRuntimeHints;

@ImportRuntimeHints(ResourceBundleRuntimeHints.class)
@EnableConfigurationProperties(GitHubConfiguration.class)
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}


File: src/main/java/dev/danvega/cg/ContentGeneratorController.java

package dev.danvega.cg;

import dev.danvega.cg.gh.GitHubService;
import dev.danvega.cg.local.LocalFileService;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class ContentGeneratorController {
    private static final Logger log = LoggerFactory.getLogger(ContentGeneratorController.class);
    private final GitHubService ghService;
    private final LocalFileService localFileService;
    private final TemplateEngine templateEngine;
    private final ContentGeneratorService contentGeneratorService;

    public ContentGeneratorController(GitHubService ghService, LocalFileService localFileService, TemplateEngine templateEngine, ContentGeneratorService contentGeneratorService) {
        this.ghService = ghService;
        this.localFileService = localFileService;
        this.templateEngine = templateEngine;
        this.contentGeneratorService = contentGeneratorService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/generate")
    @ResponseBody
    public ResponseEntity<String> generate(@RequestParam(required = false) String githubUrl,
                                                 @RequestParam(required = false) String localPath) {

        if ((githubUrl == null || githubUrl.isBlank()) && (localPath == null || localPath.isBlank())) {
            return ResponseEntity.badRequest().body("Error: Either GitHub URL or local path must be provided.");
        }


        try {
            String content = contentGeneratorService.generateContent(githubUrl, localPath);
            StringOutput output = new StringOutput();
            templateEngine.render("result.jte", Map.of("content", content), output);
            return ResponseEntity.ok(output.toString());
        } catch (Exception e) {
            log.error("Error generating content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating content: " + e.getMessage());
        }
    }
}



File: src/main/java/dev/danvega/cg/ContentGeneratorService.java

package dev.danvega.cg;

import dev.danvega.cg.gh.GitHubService;
import dev.danvega.cg.local.LocalFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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


File: src/main/java/dev/danvega/cg/ResourceBundleRuntimeHints.java

package dev.danvega.cg;

import gg.jte.generated.precompiled.JteindexGenerated;
import gg.jte.generated.precompiled.JteresultGenerated;
import gg.jte.generated.precompiled.layout.JtepageGenerated;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.stereotype.Component;

@Component
public class ResourceBundleRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources()
                .registerPattern("**/*.bin");

        hints.reflection()
                .registerType(JtepageGenerated.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS)
                .registerType(JteindexGenerated.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS)
                .registerType(JteresultGenerated.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS);
    }
}

File: src/main/java/dev/danvega/cg/gh/GitHubConfiguration.java

package dev.danvega.cg.gh;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(value = "github")
public record GitHubConfiguration(String token, List<String> includePatterns, @DefaultValue("") List<String> excludePatterns) {

    public GitHubConfiguration {
        if (includePatterns == null) {
            throw new IllegalArgumentException("includePatterns must not be null");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("GitHub token must not be null or blank");
        }
        // Ensure excludePatterns is never null
        if (excludePatterns == null) {
            excludePatterns = List.of();
        }
    }
}


File: src/main/java/dev/danvega/cg/gh/GitHubContent.java

package dev.danvega.cg.gh;

import com.fasterxml.jackson.annotation.JsonProperty;

record GitHubContent(
        String name,
        String path,
        String sha,
        String size,
        String url,
        @JsonProperty("html_url")
        String htmlUrl,
        @JsonProperty("git_url")
        String gitUrl,
        @JsonProperty("download_url")
        String downloadUrl,
        String type,
        String content)
{ }

File: src/main/java/dev/danvega/cg/gh/GitHubService.java

package dev.danvega.cg.gh;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;
import java.util.List;

/**
 * Service class for interacting with GitHub API and downloading repository contents.
 * This service allows downloading specified file types from a GitHub repository,
 * with support for both include and exclude patterns.
 */
@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GitHubConfiguration config;

    /**
     * Constructs a new GithubService with the specified dependencies.
     *
     * @param builder       The RestClient.Builder to use for creating the RestClient.
     * @param objectMapper  The ObjectMapper to use for JSON processing.
     * @param config       The GitHub configuration properties.
     */
    public GitHubService(RestClient.Builder builder,
                         ObjectMapper objectMapper,
                         GitHubConfiguration config) {
        this.config = config;
        this.restClient = builder
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version","2022-11-28")
                .defaultHeader("Authorization", "Bearer " + config.token())  // Remove the colon after Bearer
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Downloads the contents of a specified GitHub repository and writes them to a file.
     *
     * @param owner The owner of the repository.
     * @param repo  The name of the repository.
     * @throws IOException If an I/O error occurs.
     */
    public void downloadRepositoryContents(String owner, String repo) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        downloadContentsRecursively(owner, repo, "", contentBuilder);

        Path outputDir = Paths.get("output");
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve(repo + ".md");
        Files.write(outputFile, contentBuilder.toString().getBytes());

        log.info("Repository contents written to: {}", outputFile.toAbsolutePath());
    }

    /**
     * Recursively downloads the contents of a repository directory.
     *
     * @param owner           The owner of the repository.
     * @param repo            The name of the repository.
     * @param path            The path within the repository to download.
     * @param contentBuilder  The StringBuilder to append the content to.
     * @throws IOException If an I/O error occurs.
     */
    private void downloadContentsRecursively(String owner, String repo, String path, StringBuilder contentBuilder) throws IOException {
        List<GitHubContent> contents = getRepositoryContents(owner, repo, path);

        for (GitHubContent content : contents) {
            if ("file".equals(content.type()) && shouldIncludeFile(content.path())) {
                String fileContent = getFileContent(owner, repo, content.path());
                contentBuilder.append("File: ").append(content.path()).append("\n\n");
                contentBuilder.append(fileContent).append("\n\n");
            } else if ("dir".equals(content.type()) && !isExcludedDirectory(content.path())) {
                downloadContentsRecursively(owner, repo, content.path(), contentBuilder);
            } else {
                log.debug("Skipping content: {} of type {}", content.path(), content.type());
            }
        }
    }

    /**
     * Determines whether a file should be included based on include and exclude patterns.
     *
     * @param filePath The file path to check.
     * @return true if the file should be included, false otherwise.
     */
    private boolean shouldIncludeFile(String filePath) {
        // First check if the file is explicitly excluded
        if (matchesPatterns(filePath, config.excludePatterns())) {
            log.debug("File {} excluded by exclude patterns", filePath);
            return false;
        }

        // Then check if it matches include patterns
        return matchesPatterns(filePath, config.includePatterns());
    }

    /**
     * Checks if a directory should be excluded from processing.
     *
     * @param dirPath The directory path to check.
     * @return true if the directory should be excluded, false otherwise.
     */
    private boolean isExcludedDirectory(String dirPath) {
        return matchesPatterns(dirPath, config.excludePatterns());
    }

    /**
     * Checks if a given path matches any of the provided patterns.
     *
     * @param path     The path to check.
     * @param patterns The list of patterns to match against.
     * @return true if the path matches any pattern, false otherwise.
     */
    private boolean matchesPatterns(String path, List<String> patterns) {
        if (patterns.isEmpty()) {
            return patterns == config.excludePatterns(); // Return false for include patterns, true for exclude patterns
        }

        for (String pattern : patterns) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern.trim());
            if (matcher.matches(Paths.get(path))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the contents of a repository directory.
     *
     * @param owner The owner of the repository.
     * @param repo  The name of the repository.
     * @param path  The path within the repository to retrieve.
     * @return A list of GitHubContent objects representing the contents of the directory.
     */
    private List<GitHubContent> getRepositoryContents(String owner, String repo, String path) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GitHubContent>>() {});
    }

    /**
     * Retrieves the content of a specific file from the repository.
     *
     * @param owner The owner of the repository.
     * @param repo  The name of the repository.
     * @param path  The path to the file within the repository.
     * @return The content of the file as a String.
     */
    private String getFileContent(String owner, String repo, String path) {
        GitHubContent response = restClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                .retrieve()
                .body(GitHubContent.class);
        String cleanedString = response.content().replaceAll("[^A-Za-z0-9+/=]", "");
        return new String(Base64.getDecoder().decode(cleanedString));
    }
}

File: src/main/java/dev/danvega/cg/local/LocalFileService.java

package dev.danvega.cg.local;

import dev.danvega.cg.gh.GitHubConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LocalFileService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileService.class);
    private final GitHubConfiguration config;
    private Path sourceDir;
    private final List<PathMatcher> includeMatchers;
    private final List<PathMatcher> excludeMatchers;

    public LocalFileService(GitHubConfiguration config) {
        this.config = config;
        this.includeMatchers = config.includePatterns().stream()
                .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + normalizePattern(pattern)))
                .collect(Collectors.toList());
        this.excludeMatchers = config.excludePatterns().stream()
                .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + normalizePattern(pattern)))
                .collect(Collectors.toList());
    }

    public void processLocalDirectory(String directoryPath, String outputFileName) throws IOException {
        sourceDir = Paths.get(directoryPath).normalize().toAbsolutePath();
        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("Invalid directory path: " + directoryPath);
        }

        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::shouldIncludeFile)
                    .forEach(file -> readFileContent(file, contentBuilder));
        }

        Path outputDir = Paths.get("output");
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve(outputFileName + ".md");
        Files.write(outputFile, contentBuilder.toString().getBytes());

        log.info("Local directory contents written to: {}", outputFile.toAbsolutePath());
    }

    private boolean shouldIncludeFile(Path filePath) {
        String relativePath = normalizePath(sourceDir.relativize(filePath));
        if (excludeMatchers.stream().anyMatch(matcher -> matcher.matches(Paths.get(relativePath)))) {
            return false;
        }
        if (includeMatchers.isEmpty()) {
            return true;
        }
        return includeMatchers.stream().anyMatch(matcher -> matcher.matches(Paths.get(relativePath)));
    }

    private void readFileContent(Path file, StringBuilder builder) {
        try {
            String relativePath = sourceDir.relativize(file).toString();
            String content = Files.readString(file);
            builder.append("File: ").append(relativePath).append("\n\n")
                    .append(content).append("\n\n");
        } catch (IOException e) {
            log.error("Error reading file: {}", file, e);
        }
    }

    private String normalizePattern(String pattern) {
        return pattern.trim()
                .replace('\\', '/');
    }

    private String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }

}


File: src/main/jte/index.jte

@import gg.jte.Content

@template.layout.page(
title = "Repository Content Generator",
content = @`
    <div class="text-center mb-12">
        <h1 class="text-4xl font-bold tracking-tight text-gray-900 mb-2">Content Generator</h1>
        <p class="text-gray-600">Generate a consolidated file from repository or local directory contents</p>
    </div>

    <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
        <div class="mb-6">
            <div class="flex space-x-4">
                <button onclick="switchTab('github')"
                        id="github-tab"
                        class="px-4 py-2 text-sm font-medium rounded-lg transition-colors duration-200 github-tab-button">
                    GitHub Repository
                </button>
                <button onclick="switchTab('local')"
                        id="local-tab"
                        class="px-4 py-2 text-sm font-medium rounded-lg transition-colors duration-200 local-tab-button">
                    Local Directory
                </button>
            </div>
        </div>

        <form hx-post="/generate" hx-target="#result-container" hx-indicator="#spinner">
            <div id="github-input" class="tab-content">
                <div class="mb-6">
                    <label for="githubUrl" class="block text-sm font-medium text-gray-700 mb-2">Repository URL</label>
                    <input
                            type="text"
                            id="githubUrl"
                            name="githubUrl"
                            class="block w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                            placeholder="https://github.com/username/repo"
                    >
                </div>
            </div>

            <div id="local-input" class="tab-content hidden">
                <div class="mb-6">
                    <label for="localPath" class="block text-sm font-medium text-gray-700 mb-2">Local Directory Path</label>
                    <input
                            type="text"
                            id="localPath"
                            name="localPath"
                            class="block w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                            placeholder="/path/to/your/directory"
                    >
                </div>
            </div>

            <button
                    type="submit"
                    class="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-4 rounded-lg transition-colors duration-200 flex items-center justify-center"
            >
                Generate Content
            </button>
        </form>

        <div id="spinner" class="htmx-indicator flex justify-center mt-6">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>

        <div id="result-container" class="mt-6">
            <!-- Generated content will appear here -->
        </div>
    </div>

    <script>
        function switchTab(tab) {
            const githubInput = document.getElementById('github-input');
            const localInput = document.getElementById('local-input');
            const githubTab = document.getElementById('github-tab');
            const localTab = document.getElementById('local-tab');

            if (tab === 'github') {
                githubInput.classList.remove('hidden');
                localInput.classList.add('hidden');
                document.getElementById('localPath').value = '';
                githubTab.classList.add('bg-blue-600', 'text-white');
                githubTab.classList.remove('text-gray-700');
                localTab.classList.remove('bg-blue-600', 'text-white');
                localTab.classList.add('text-gray-700');
            } else {
                githubInput.classList.add('hidden');
                localInput.classList.remove('hidden');
                document.getElementById('githubUrl').value = '';
                localTab.classList.add('bg-blue-600', 'text-white');
                localTab.classList.remove('text-gray-700');
                githubTab.classList.remove('bg-blue-600', 'text-white');
                githubTab.classList.add('text-gray-700');
            }
        }

        // Initialize tabs
        document.addEventListener('DOMContentLoaded', function() {
            switchTab('github');
        });

        async function openDirectoryDialog() {
            try {
                const dirHandle = await window.showDirectoryPicker();
                const fullPath = await getFullPath(dirHandle);
                document.getElementById('localPath').value = fullPath;
            } catch (err) {
                console.error('Error selecting directory:', err);
            }
        }

        async function getFullPath(handle) {
            const segments = [];
            let current = handle;

            while (current) {
                segments.unshift(current.name);
                current = await current.queryPermission({ mode: 'read' }) === 'granted'
                    ? (await current.resolve())?.parent
                    : null;
            }

            return '/' + segments.join('/');
        }
    </script>
`
)

File: src/main/jte/layout/page.jte

@import gg.jte.Content

@param String title
@param Content content

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/htmx/1.9.10/htmx.min.js"></script>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
</head>
<body class="bg-gray-50 min-h-screen flex items-center justify-center p-4 font-sans">
<div class="w-full max-w-3xl mx-auto">
    ${content}
</div>
<script>
    htmx.on('htmx:afterSettle', function(event) {
        if (event.detail.target.id === 'result') {
            event.detail.target.classList.add('fade-in');
        }
    });
</script>
<style>
    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(10px); }
        to { opacity: 1; transform: translateY(0); }
    }
    .fade-in {
        animation: fadeIn 0.4s ease-out forwards;
    }
</style>
</body>
</html>

File: src/main/jte/result.jte

@param String content

<div class="bg-gray-50 rounded-lg border border-gray-200 p-4">
    <div class="relative">
        <textarea
                id="result"
                class="w-full h-96 p-4 font-mono text-sm bg-white rounded-lg border border-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none"
                readonly
        >${content}</textarea>

        <button
                onclick="copyContent()"
                class="mt-4 inline-flex items-center px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition-colors duration-200"
        >
            Copy Content
        </button>
    </div>
</div>

<script>
    function copyContent() {
        const textarea = document.getElementById('result');
        textarea.select();
        document.execCommand('copy');

        // Show feedback
        const button = event.target;
        const originalText = button.textContent;
        button.textContent = 'Copied!';

        setTimeout(() => {
            button.textContent = originalText;
        }, 2000);
    }
</script>

File: src/main/resources/application-prod.yaml

gg:
  jte:
    usePrecompiledTemplates: true
    developmentMode: false

File: src/main/resources/application.yaml

spring:
  application:
    name: repo-content-generator

gg:
  jte:
    developmentMode: true

app:
  output:
    directory: output

github:
  token: ${GITHUB_TOKEN}
  includePatterns:
    - "**/*.md"
    - "**/*.txt"
    - "**/*.xml"
    - "**/*.java"
    - "**/*.jte"
    - "**/*.yaml"
    - "**/*.yml"
    - "**/*.graphqls"
    - "**/*.properties"
  excludePatterns:
    - ".mvn/**"
    - ".idea/**"
    - "target/**"
    - ".gitignore"
    - ".gitattributes"
    - "mvnw"
    - "mvnw.cmd"

logging:
  level:
    dev:
      danvega: debug

File: src/test/java/dev/danvega/cg/ContentGeneratorControllerTest.java

package dev.danvega.cg;

import dev.danvega.cg.gh.GitHubService;
import dev.danvega.cg.local.LocalFileService;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContentGeneratorController.class)
@Import({
        TestConfig.class
})
class ContentGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private LocalFileService localFileService;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private ContentGeneratorService contentGeneratorService;

    @BeforeEach
    void setUp() {
        reset(gitHubService, localFileService, templateEngine, contentGeneratorService);
    }

    @Test
    void index_ShouldReturnIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void generate_WithValidGitHubUrl_ShouldReturnContent() throws Exception {
        // Arrange
        String githubUrl = "https://github.com/user/repo";
        String generatedContent = "Generated content";
        String renderedTemplate = "Rendered template";

        when(contentGeneratorService.generateContent(eq(githubUrl), eq(null)))
                .thenReturn(generatedContent);
        doAnswer(invocation -> {
            StringOutput output = invocation.getArgument(2);
            output.writeContent(renderedTemplate);
            return null;
        }).when(templateEngine).render(eq("result.jte"), any(), any(StringOutput.class));

        // Act & Assert
        mockMvc.perform(post("/generate")
                        .param("githubUrl", githubUrl)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(renderedTemplate));

        verify(contentGeneratorService).generateContent(githubUrl, null);
        verify(templateEngine).render(eq("result.jte"), any(), any(StringOutput.class));
    }

    @Test
    void generate_WithValidLocalPath_ShouldReturnContent() throws Exception {
        // Arrange
        String localPath = "/path/to/local/file";
        String generatedContent = "Generated content";
        String renderedTemplate = "Rendered template";

        when(contentGeneratorService.generateContent(eq(null), eq(localPath)))
                .thenReturn(generatedContent);
        doAnswer(invocation -> {
            StringOutput output = invocation.getArgument(2);
            output.writeContent(renderedTemplate);
            return null;
        }).when(templateEngine).render(eq("result.jte"), any(), any(StringOutput.class));

        // Act & Assert
        mockMvc.perform(post("/generate")
                        .param("localPath", localPath)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(renderedTemplate));

        verify(contentGeneratorService).generateContent(null, localPath);
        verify(templateEngine).render(eq("result.jte"), any(), any(StringOutput.class));
    }

    @Test
    void generate_WithNoInputs_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/generate")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Either GitHub URL or local path must be provided."));

        verifyNoInteractions(contentGeneratorService, templateEngine);
    }

    @Test
    void generate_WithBlankInputs_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/generate")
                        .param("githubUrl", "")
                        .param("localPath", "  ")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Either GitHub URL or local path must be provided."));

        verifyNoInteractions(contentGeneratorService, templateEngine);
    }

    @Test
    void generate_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        String githubUrl = "https://github.com/user/repo";
        String errorMessage = "Service error";
        when(contentGeneratorService.generateContent(eq(githubUrl), eq(null)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act & Assert
        mockMvc.perform(post("/generate")
                        .param("githubUrl", githubUrl)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error generating content: " + errorMessage));

        verify(contentGeneratorService).generateContent(githubUrl, null);
        verifyNoInteractions(templateEngine);
    }
}

File: src/test/java/dev/danvega/cg/GithubConcatApplicationTests.java

package dev.danvega.cg;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GithubConcatApplicationTests {

	@Test
	void contextLoads() {
	}

}


File: src/test/java/dev/danvega/cg/TestConfig.java

package dev.danvega.cg;

import dev.danvega.cg.gh.GitHubService;
import dev.danvega.cg.local.LocalFileService;
import gg.jte.TemplateEngine;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public GitHubService gitHubService() {
        return mock(GitHubService.class);
    }

    @Bean
    @Primary
    public LocalFileService localFileService() {
        return mock(LocalFileService.class);
    }

    @Bean
    @Primary
    public TemplateEngine templateEngine() {
        return mock(TemplateEngine.class);
    }

    @Bean
    @Primary
    public ContentGeneratorService contentGeneratorService() {
        return mock(ContentGeneratorService.class);
    }
}


