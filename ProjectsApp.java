package project;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import project.exception.DbException;
import projects.entity.Project; 
import project.service.ProjectService; 

public class ProjectsApp {
    private Scanner scanner = new Scanner(System.in); 
    private ProjectService projectService = new ProjectService(); 
    private Project curProject;

    // @formatter:off
    private List<String> operations = List.of(
        "1) Add a project",
        "2) List projects",
        "3) Select a project",
        "4) Update project details",
        "5) Delete a project"
    );
    // @formatter:on

    public static void main(String[] args) {
        new ProjectsApp().processUserSelections(); 
    }

    private void processUserSelections() {
        boolean done = false;

        while (!done) {
            try {
                int selection = getUserSelection();

                switch (selection) {
                    case -1:
                        done = exitMenu();
                        break;

                    case 1:
                        createProject();
                        break;

                    case 2:
                        listProjects();
                        break;

                    case 3:
                        selectProject();
                        break;

                    case 4:
                        updateProjectDetails();
                        break;

                    case 5:
                        deleteProject(); 
                        break;

                    default:
                        System.out.println("\n" + selection + " is not a valid selection. Try again.");
                        break;
                }
            } catch (Exception e) {
                System.out.println("\nError: " + e);
            }
        }
    }

    private void updateProjectDetails() {
        if (curProject == null) {
            System.out.println("\nPlease select a project.");
            return;
        }

        System.out.println("\nCurrent Project Details:");
        System.out.println("Project Name: " + curProject.getProjectName());
        System.out.println("Estimated Hours: " + curProject.getEstimatedHours());
        System.out.println("Actual Hours: " + curProject.getActualHours());
        System.out.println("Difficulty: " + curProject.getDifficulty());
        System.out.println("Notes: " + curProject.getNotes());

        String projectName = getStringInput("Enter new project name (or press enter to keep " + curProject.getProjectName() + ")");
        BigDecimal estimatedHours = getDecimalInput("Enter new estimated hours (or press enter to keep " + curProject.getEstimatedHours() + ")");
        BigDecimal actualHours = getDecimalInput("Enter new actual hours (or press enter to keep " + curProject.getActualHours() + ")");
        Integer difficulty = getIntInput("Enter new difficulty (or press enter to keep " + curProject.getDifficulty() + ")");
        String notes = getStringInput("Enter new notes (or press enter to keep " + curProject.getNotes() + ")");

        Project updatedProject = new Project();
        updatedProject.setProjectId(curProject.getProjectId()); 
        updatedProject.setProjectName(projectName != null ? projectName : curProject.getProjectName());
        updatedProject.setEstimatedHours(estimatedHours != null ? estimatedHours : curProject.getEstimatedHours());
        updatedProject.setActualHours(actualHours != null ? actualHours : curProject.getActualHours());
        updatedProject.setDifficulty(difficulty != null ? difficulty : curProject.getDifficulty());
        updatedProject.setNotes(notes != null ? notes : curProject.getNotes());

        projectService.modifyProjectDetails(updatedProject);

        curProject = projectService.fetchProjectById(curProject.getProjectId());

        System.out.println("Updated project details: " + curProject);
    }

    private void selectProject() {
        listProjects();
        Integer projectId = getIntInput("Enter a project ID to select a project");
        
        curProject = null;
        
        curProject = projectService.fetchProjectById(projectId);
        
        if (Objects.isNull(curProject)) {
            System.out.println("\nYou are not working with a project.");
        } else {
            System.out.println("\nYou are working with project: " + curProject);
        }
    }

    private void deleteProject() {
        listProjects();
        Integer projectId = getIntInput("Enter the project ID to delete");

        projectService.deleteProject(projectId);

        System.out.println("Project deleted successfully.");

        if (curProject != null && curProject.getProjectId().equals(projectId)) {
            curProject = null;
        }
    }

    private int getUserSelection() {
        printOperations(); 
        Integer input = getIntInput("Enter a menu selection"); 

        return Objects.isNull(input) ? -1 : input; 
    }

    private void printOperations() {
        System.out.println("\nThese are the available selections. Press the enter key to quit:"); 
        operations.forEach(line -> System.out.println("   " + line)); 
    }

    private Integer getIntInput(String prompt) {
        String input = getStringInput(prompt);

        if (Objects.isNull(input)) {
            return null; 
        }

        try {
            return Integer.valueOf(input);
        } catch (NumberFormatException e) {
            throw new DbException(input + " is not a valid number. Try again."); 
        }
    }

    private String getStringInput(String prompt) {
        System.out.print(prompt + ": "); 
        String input = scanner.nextLine(); 

        return input.isBlank() ? null : input.trim(); 
    }

    private BigDecimal getDecimalInput(String prompt) {
        String input = getStringInput(prompt);

        if (Objects.isNull(input)) {
            return null;
        }

        try {
            return new BigDecimal(input).setScale(2); 
        } catch (NumberFormatException e) {
            throw new DbException(input + " is not a valid decimal number. Try again."); 
        }
    }

    private void createProject() {
        String projectName = getStringInput("Enter the project name"); 
        BigDecimal estimatedHours = getDecimalInput("Enter the estimated hours"); 
        BigDecimal actualHours = getDecimalInput("Enter the actual hours");
        Integer difficulty = getIntInput("Enter the project difficulty (1-5)");
        String notes = getStringInput("Enter the project notes"); 

        Project project = new Project(); 
        project.setProjectName(projectName);
        project.setEstimatedHours(estimatedHours);
        project.setActualHours(actualHours);
        project.setDifficulty(difficulty);
        project.setNotes(notes);

        Project dbProject = projectService.addProject(project); 
        System.out.println("You have successfully created project: " + dbProject); 
    }

    private void listProjects() {
        List<Project> projects = projectService.fetchAllProjects();

        System.out.println("\nProjects:");

        projects.forEach(project ->
            System.out.println("   " + project.getProjectId() + ": " + project.getProjectName())
        );
    }

    private boolean exitMenu() {
        System.out.println("\nExiting the menu. TTFN!"); 
        return true;
    }
}