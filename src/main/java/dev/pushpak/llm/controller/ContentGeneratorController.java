package dev.pushpak.llm.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import dev.pushpak.llm.gh.GitHubService;
import dev.pushpak.llm.local.LocalFileService;
import dev.pushpak.llm.service.ContentGeneratorService;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

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

    @GetMapping("/test")
    public String testMethod() {
        return new String("works fine..");
    }
    
}
