package project.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import projects.entity.Category;
import projects.entity.Material;
import projects.entity.Project;
import projects.entity.Step;
import project.exception.DbException;
import provided.util.DaoBase;

/**
 * Data Access Object for managing Project data in the database.
 */
public class ProjectDao extends DaoBase {
    private static final String CATEGORY_TABLE = "category";
    private static final String MATERIAL_TABLE = "material";
    private static final String PROJECT_TABLE = "project";
    private static final String PROJECT_CATEGORY_TABLE = "project_category";
    private static final String STEP_TABLE = "step";

    public void executeBatch(List<String> sqlBatch) {
        try (Connection conn = DbConnection.getConnection()) {
            startTransaction(conn);

            try (Statement stmt = conn.createStatement()) {
                for (String sql : sqlBatch) {
                    stmt.addBatch(sql);
                }
                stmt.executeBatch();
                commitTransaction(conn);
            } catch (Exception e) {
                rollbackTransaction(conn);
                throw new DbException(e);
            }
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    /**
     * Inserts a project into the database.
     * 
     * @param project The Project object to insert
     * @return The Project object with the generated ID
     */
    public Project insertProject(Project project) {
        String sql = "INSERT INTO " + PROJECT_TABLE + " "
            + "(project_name, estimated_hours, actual_hours, difficulty, notes) "
            + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DbConnection.getConnection()) {
            startTransaction(conn);

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setParameter(stmt, 1, project.getProjectName(), String.class);
                setParameter(stmt, 2, project.getEstimatedHours(), BigDecimal.class);
                setParameter(stmt, 3, project.getActualHours(), BigDecimal.class);
                setParameter(stmt, 4, project.getDifficulty(), Integer.class);
                setParameter(stmt, 5, project.getNotes(), String.class);

                stmt.executeUpdate();
                
                Integer projectId = getLastInsertId(conn, PROJECT_TABLE);
                commitTransaction(conn);
                
                project.setProjectId(projectId);
                return project;

            } catch (Exception e) {
                rollbackTransaction(conn);
                throw new DbException(e);
            }
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    /**
     * Fetches all projects from the database.
     * 
     * @return A List of Project objects
     */
    public List<Project> fetchAllProjects() {
        String sql = "SELECT * FROM " + PROJECT_TABLE + " ORDER BY project_name";

        List<Project> projects = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection()) {
            startTransaction(conn);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Project project = new Project();
                        project.setProjectId(rs.getInt("project_id"));
                        project.setProjectName(rs.getString("project_name"));
                        project.setEstimatedHours(rs.getBigDecimal("estimated_hours"));
                        project.setActualHours(rs.getBigDecimal("actual_hours"));
                        project.setDifficulty(rs.getInt("difficulty"));
                        project.setNotes(rs.getString("notes"));
                        projects.add(project);
                    }
                } catch (Exception e) {
                    rollbackTransaction(conn);
                    throw new DbException("Error processing result set", e);
                }

                commitTransaction(conn);
            } catch (Exception e) {
                rollbackTransaction(conn);
                throw new DbException("Error executing SQL statement", e);
            }
        } catch (SQLException e) {
            throw new DbException("Error getting connection", e);
        }

        return projects;
    }
    
    public Optional<Project> fetchProjectById(Integer projectId) {
        String sql = "SELECT * FROM " + PROJECT_TABLE + " WHERE project_id = ?";
        
        try (Connection conn = DbConnection.getConnection()) {
            startTransaction(conn);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setParameter(stmt, 1, projectId, Integer.class);
                
                Project project = null;
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        project = extract(rs, Project.class);
                    }
                } catch (Exception e) {
                    rollbackTransaction(conn);
                    throw new DbException("Error processing result set", e);
                }

                if (Objects.nonNull(project)) {
                    project.getMaterials().addAll(fetchMaterialsForProject(conn, projectId));
                    project.getSteps().addAll(fetchStepsForProject(conn, projectId));
                    project.getCategories().addAll(fetchCategoriesForProject(conn, projectId));
                }

                commitTransaction(conn);
                return Optional.ofNullable(project);
            } catch (Exception e) {
                rollbackTransaction(conn);
                throw new DbException("Error executing SQL statement", e);
            }
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    private List<Category> fetchCategoriesForProject(Connection conn, Integer projectId) throws SQLException {
        String sql = "SELECT c.* FROM " + CATEGORY_TABLE + " c "
            + "JOIN " + PROJECT_CATEGORY_TABLE + " pc USING (category_id) "
            + "WHERE project_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameter(stmt, 1, projectId, Integer.class);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<Category> categories = new LinkedList<>();
                
                while (rs.next()) {
                    categories.add(extract(rs, Category.class));
                }
                return categories;
            }
        }
    }

    private List<Step> fetchStepsForProject(Connection conn, Integer projectId) throws SQLException {
        String sql = "SELECT * FROM " + STEP_TABLE + " WHERE project_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameter(stmt, 1, projectId, Integer.class);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<Step> steps = new LinkedList<>();
                
                while (rs.next()) {
                    steps.add(extract(rs, Step.class));
                }
                
                return steps;
            }
        }
    }
    
    private List<Material> fetchMaterialsForProject(Connection conn, Integer projectId) throws SQLException {
        String sql = "SELECT * FROM " + MATERIAL_TABLE + " WHERE project_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameter(stmt, 1, projectId, Integer.class);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<Material> materials = new LinkedList<>();
                
                while (rs.next()) {
                    materials.add(extract(rs, Material.class));
                }
                
                return materials;
            }
        }
    }

    public static String getCategoryTable() {
        return CATEGORY_TABLE;
    }

    public static String getMaterialTable() {
        return MATERIAL_TABLE;
    }

    public static String getProjectCategoryTable() {
        return PROJECT_CATEGORY_TABLE;
    }

    public static String getStepTable() {
        return STEP_TABLE;
    }

    /**
     * Updates project details in the database.
     * 
     * @param updatedProject The Project object with updated details
     * @return true if the update was successful, false otherwise
     */
    public boolean modifyProjectDetails(Project updatedProject) {
        String sql = "UPDATE " + PROJECT_TABLE + " SET "
            + "project_name = ?, "
            + "estimated_hours = ?, "
            + "actual_hours = ?, "
            + "difficulty = ?, "
            + "notes = ? "
            + "WHERE project_id = ?";

        try (Connection conn = DbConnection.getConnection()) {
            startTransaction(conn);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setParameter(stmt, 1, updatedProject.getProjectName(), String.class);
                setParameter(stmt, 2, updatedProject.getEstimatedHours(), BigDecimal.class);
                setParameter(stmt, 3, updatedProject.getActualHours(), BigDecimal.class);
                setParameter(stmt, 4, updatedProject.getDifficulty(), Integer.class);
                setParameter(stmt, 5, updatedProject.getNotes(), String.class);
                setParameter(stmt, 6, updatedProject.getProjectId(), Integer.class);

                int rowsAffected = stmt.executeUpdate();
                boolean success = (rowsAffected == 1);

                if (!success) {
                    rollbackTransaction(conn);
                } else {
                    commitTransaction(conn);
                }

                return success;
                
            } catch (SQLException e) {
                rollbackTransaction(conn);
                throw new DbException("Error executing update statement", e);
            }
        } catch (SQLException e) {
            throw new DbException("Error getting connection", e);
        }
    }

    /**
     * Deletes a project from the database.
     * 
     * @param projectId The ID of the project to delete
     * @return true if the deletion was successful, false otherwise
     */
    public boolean deleteProject(Integer projectId) {
        String sql = "DELETE FROM " + PROJECT_TABLE + " WHERE project_id = ?";

        try (Connection conn = DbConnection.getConnection()) {
            startTransaction(conn);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setParameter(stmt, 1, projectId, Integer.class);

                int rowsAffected = stmt.executeUpdate();
                boolean success = (rowsAffected == 1);

                if (!success) {
                    rollbackTransaction(conn);
                } else {
                    commitTransaction(conn);
                }

                return success;
                
            } catch (SQLException e) {
                rollbackTransaction(conn);
                throw new DbException("Error executing delete statement", e);
            }
        } catch (SQLException e) {
            throw new DbException("Error getting connection", e);
        }
    }
}