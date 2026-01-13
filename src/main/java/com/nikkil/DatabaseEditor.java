package com.nikkil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class DatabaseEditor {
    private String url;
    private String user;
    private String password;
    private Connection connection;
    private Scanner scanner;

    public DatabaseEditor() throws ClassNotFoundException, IOException {
        Class.forName("org.postgresql.Driver");
        loadEnv();
        scanner = new Scanner(System.in);
    }

    private void loadEnv() throws IOException {
        Map<String, String> env = new HashMap<>();
        // Try multiple possible paths for .env file
        String[] possiblePaths = {
            System.getProperty("user.dir") + "/.env",
            System.getProperty("user.dir") + "/jdbc/.env",
            "/Users/nikkilkumar/Desktop/Java Learnings/JDBC/jdbc/.env"
        };
        
        boolean found = false;
        for (String envPath : possiblePaths) {
            try {
                if (Files.exists(Paths.get(envPath))) {
                    Files.lines(Paths.get(envPath))
                            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                            .forEach(line -> {
                                String[] parts = line.split("=", 2);
                                if (parts.length == 2) {
                                    env.put(parts[0].trim(), parts[1].trim());
                                }
                            });
                    found = true;
                    System.out.println("✓ Loaded environment from: " + envPath);
                    break;
                }
            } catch (IOException ignored) {
            }
        }
        
        if (!found) {
            System.out.println("⚠ Warning: .env file not found at any location. Using default values.");
            env.put("DB_URL", "jdbc:postgresql://localhost:5432/test");
            env.put("DB_USER", "postgres");
            env.put("DB_PASSWORD", "");
        }
        
        this.url = env.getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/test");
        this.user = env.getOrDefault("DB_USER", "postgres");
        this.password = env.getOrDefault("DB_PASSWORD", "");
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(url, user, password);
        System.out.println("✓ Connected to database successfully!");
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("✓ Disconnected from database");
        }
    }

    public void displayMenu() {
        System.out.println("\n========== DATABASE EDITOR ==========");
        System.out.println("1. View all records");
        System.out.println("2. Insert a new record");
        System.out.println("3. Update a record");
        System.out.println("4. Delete a record");
        System.out.println("5. Run custom query");
        System.out.println("6. Exit");
        System.out.println("=====================================");
        System.out.print("Enter your choice (1-6): ");
    }

    public void viewAllRecords() throws SQLException {
        String query = "SELECT * FROM student";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            System.out.println("\n========== ALL RECORDS ==========");
            boolean hasRecords = false;
            while (rs.next()) {
                hasRecords = true;
                System.out.println("ID: " + rs.getString("id") + 
                                 " | Name: " + rs.getString("name") + 
                                 " | Marks: " + rs.getInt("marks"));
            }
            if (!hasRecords) {
                System.out.println("No records found.");
            }
            System.out.println("================================");
        }
    }

    public void insertRecord() throws SQLException {
        System.out.print("Enter Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter Marks: ");
        int marks = Integer.parseInt(scanner.nextLine());

        String query = "INSERT INTO student (name, marks) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, marks);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("✓ Record inserted successfully!");
            }
        }
    }

    public void updateRecord() throws SQLException {
        System.out.print("Enter ID of record to update: ");
        String id = scanner.nextLine();
        System.out.print("Enter new Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter new Marks: ");
        int marks = Integer.parseInt(scanner.nextLine());

        String query = "UPDATE student SET name = ?, marks = ? WHERE id = ?::uuid";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, marks);
            pstmt.setString(3, id);

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✓ Record updated successfully!");
            } else {
                System.out.println("✗ Record with ID " + id + " not found.");
            }
        }
    }

    public void deleteRecord() throws SQLException {
        System.out.print("Enter ID of record to delete: ");
        String id = scanner.nextLine();

        String query = "DELETE FROM student WHERE id = ?::uuid";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, id);

            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("✓ Record deleted successfully!");
            } else {
                System.out.println("✗ Record with ID " + id + " not found.");
            }
        }
    }

    public void runCustomQuery() throws SQLException {
        System.out.print("Enter SQL query: ");
        String sqlQuery = scanner.nextLine();

        try (Statement stmt = connection.createStatement()) {
            if (sqlQuery.trim().toUpperCase().startsWith("SELECT")) {
                ResultSet rs = stmt.executeQuery(sqlQuery);
                ResultSetMetaData metadata = rs.getMetaData();
                int columnCount = metadata.getColumnCount();

                System.out.println("\n========== QUERY RESULTS ==========");
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(metadata.getColumnName(i) + "\t");
                }
                System.out.println();

                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        System.out.print(rs.getString(i) + "\t");
                    }
                    System.out.println();
                }
                System.out.println("================================");
                rs.close();
            } else {
                int rowsAffected = stmt.executeUpdate(sqlQuery);
                System.out.println("✓ Query executed! Rows affected: " + rowsAffected);
            }
        } catch (SQLException e) {
            System.out.println("✗ Error executing query: " + e.getMessage());
        }
    }

    public void start() {
        try {
            connect();
            boolean running = true;

            while (running) {
                displayMenu();
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1" -> viewAllRecords();
                    case "2" -> insertRecord();
                    case "3" -> updateRecord();
                    case "4" -> deleteRecord();
                    case "5" -> runCustomQuery();
                    case "6" -> {
                        System.out.println("Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("✗ Invalid choice. Please try again.");
                }
            }
        } catch (SQLException e) {
            System.out.println("✗ Database Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                disconnect();
                scanner.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
