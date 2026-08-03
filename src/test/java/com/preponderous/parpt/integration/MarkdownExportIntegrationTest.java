package com.preponderous.parpt.integration;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.repo.ProjectMarkdownWriter;
import com.preponderous.parpt.score.ScoreCalculator;
import com.preponderous.parpt.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "app.projects.file=integration-test-projects.json",
        "app.projects.markdown-file=integration-test-projects.md"
})
class MarkdownExportIntegrationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectMarkdownWriter markdownWriter;

    @Autowired
    private ScoreCalculator scoreCalculator;

    @Test
    void endToEndMarkdownExport_ShouldCreateReadableMarkdownFile() throws Exception {
        // Clean up any existing files
        Files.deleteIfExists(Paths.get("integration-test-projects.json"));
        Files.deleteIfExists(Paths.get("integration-test-projects.md"));

        // Create test projects with different scores
        projectService.createProject(
                "High Impact App", 
                "A mobile app with massive user base potential",
                5, 4, 3, 5, 4  // ICE: 60, RICE: 25
        );

        projectService.createProject(
                "Quick Win Feature", 
                "Easy feature that users will love",
                3, 5, 5, 3, 2  // ICE: 75, RICE: 22.5
        );

        projectService.createProject(
                "Technical Debt Fix", 
                "Important but not user-facing improvement",
                2, 4, 2, 1, 3  // ICE: 16, RICE: 2.67
        );

        // Export to markdown sorted by ICE
        List<Project> projects = projectService.getProjects();
        markdownWriter.writeMarkdown(projects, false);

        // Verify the markdown file was created and has correct content
        assertTrue(Files.exists(Paths.get("integration-test-projects.md")));
        
        String content = Files.readString(Paths.get("integration-test-projects.md"));
        
        // Check header and metadata
        assertTrue(content.contains("# Project Priorities"));
        assertTrue(content.contains("Sorted by ICE score"));
        
        // Check that projects are in the correct order (highest ICE first)
        // Quick Win Feature (ICE: 75) should come before High Impact App (ICE: 60)
        int quickWinIndex = content.indexOf("Quick Win Feature");
        int highImpactIndex = content.indexOf("High Impact App"); 
        int techDebtIndex = content.indexOf("Technical Debt Fix");
        
        assertTrue(quickWinIndex < highImpactIndex, "Quick Win should come before High Impact");
        assertTrue(highImpactIndex < techDebtIndex, "High Impact should come before Tech Debt");
        
        // Check that all expected content is present
        assertTrue(content.contains("Easy feature that users will love"));
        assertTrue(content.contains("ICE Score"));
        assertTrue(content.contains("RICE Score"));
        assertTrue(content.contains("Components"));
        assertTrue(content.contains("Impact:** 5/5 (very high)"));
        
        // Test RICE sorting
        markdownWriter.writeMarkdown(projects, true);
        String riceContent = Files.readString(Paths.get("integration-test-projects.md"));
        assertTrue(riceContent.contains("Sorted by RICE score"));
        
        // Clean up
        Files.deleteIfExists(Paths.get("integration-test-projects.json"));
        Files.deleteIfExists(Paths.get("integration-test-projects.md"));
    }
}