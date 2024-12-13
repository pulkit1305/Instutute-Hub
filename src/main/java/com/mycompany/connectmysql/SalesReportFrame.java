
package com.mycompany.connectmysql;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 *
 * "jdbc:mysql://localhost:3306/pulkit?zeroDateTimeBehavior=CONVERT_TO_NULL",
 * "springstudent",
 * "springstudent"
 * 
 * @author PULKIT SHARMA
 */
public class SalesReportFrame extends javax.swing.JFrame {
    private JTable salesTable;
    private DefaultTableModel tableModel;
    private JTextField startDateField, endDateField;
    private JComboBox<String> courseComboBox;
    private JLabel totalAdmissionsLabel, totalCashLabel;
    private Connection con;
    private JPanel chartPanelContainer;
    private int count = 0;
    private String companyDatabase;
    private static String companyName;

    public SalesReportFrame(String companyName) {
        setCompanyDatabase(companyName);

        setTitle("Sales Report- " + companyName);
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top panel for date and course selection

        JPanel topPanel = new JPanel(new FlowLayout());

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> backButtonAction());
        topPanel.add(backButton);
        topPanel.add(new JLabel("Start Date (YYYY-MM-DD):"));
        startDateField = new JTextField(10);
        topPanel.add(startDateField);

        topPanel.add(new JLabel("End Date (YYYY-MM-DD):"));
        endDateField = new JTextField(10);
        topPanel.add(endDateField);

        topPanel.add(new JLabel("Course:"));
        courseComboBox = new JComboBox<>();
        courseComboBox.addItem("All Courses"); // Default option
        populateCourseComboBox();
        topPanel.add(courseComboBox);

        JButton fetchButton = new JButton("Fetch Report");
        topPanel.add(fetchButton);
        add(topPanel, BorderLayout.NORTH);

        // Table for displaying sales data
        tableModel = new DefaultTableModel(
                new String[] { "Sno.", "Admission ID", "Student Name", "Course Name", "Date", "Amount Paid" }, 0);
        salesTable = new JTable(tableModel);

        JScrollPane tableScrollPane = new JScrollPane(salesTable);
        tableScrollPane.setPreferredSize(new Dimension(600, 0));
        add(tableScrollPane, BorderLayout.WEST);

        // Bottom panel for totals
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        totalAdmissionsLabel = new JLabel("Total Admissions: 0");
        totalCashLabel = new JLabel("Total Cash Received: ₹0.00");
        bottomPanel.add(totalAdmissionsLabel);
        bottomPanel.add(totalCashLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        // Chart container on the right
        chartPanelContainer = new JPanel(new GridLayout(2, 1));
        chartPanelContainer.setPreferredSize(new Dimension(600, 0));
        add(chartPanelContainer, BorderLayout.EAST);

        // Fetch button action
        fetchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fetchSalesAndCashData();
            }
        });

        setVisible(true);
    }

    private void backButtonAction() {
        AdminDashboard window2 = new AdminDashboard(companyName);
        window2.setVisible(true);
        this.dispose();

    }

    private void setCompanyDatabase(String companyName) {
        connectToCentralDatabase(); // Connect to the central_db first
        try {
            PreparedStatement pst = con.prepareStatement("SELECT database_name FROM companies WHERE name = ?");
            pst.setString(1, companyName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                companyDatabase = rs.getString("database_name"); // Get the database name for the company
            } else {
                JOptionPane.showMessageDialog(null, "Company not found in central database!", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Central database connection method for fetching company database
    private void connectToCentralDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/central_db?zeroDateTimeBehavior=CONVERT_TO_NULL",
                    "springstudent", "springstudent");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to connect to central database!", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fetchSalesAndCashData() {
        String startDate = startDateField.getText();
        String endDate = endDateField.getText();
        String selectedCourse = courseComboBox.getSelectedItem().toString();

        if (startDate.isEmpty() || endDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both start and end dates.");
            return;
        }

        connectToDatabase();
        try {
            // SQL query with optional course filter
            String admissionsQuery = "SELECT a.id AS admission_id, u.name AS student_name, c.name AS course_name, " +
                    "a.date_of_admission, a.amount_paid " +
                    "FROM admissions a " +
                    "JOIN users u ON a.user_id = u.user_id " +
                    "JOIN courses c ON a.course_id = c.id " +
                    "WHERE a.date_of_admission BETWEEN ? AND ?";

            if (!selectedCourse.equals("All Courses")) {
                admissionsQuery += " AND c.name = ?";
            }

            PreparedStatement admissionsStmt = con.prepareStatement(admissionsQuery);
            admissionsStmt.setDate(1, Date.valueOf(LocalDate.parse(startDate)));
            admissionsStmt.setDate(2, Date.valueOf(LocalDate.parse(endDate)));

            if (!selectedCourse.equals("All Courses")) {
                admissionsStmt.setString(3, selectedCourse);
            }

            ResultSet admissionsRs = admissionsStmt.executeQuery();
            count = 0;

            // Clear previous data
            tableModel.setRowCount(0);

            int totalAdmissions = 0;
            double totalCash = 0.0;

            // Populate the table and calculate totals
            while (admissionsRs.next()) {
                int admissionId = admissionsRs.getInt("admission_id");
                String studentName = admissionsRs.getString("student_name");
                String courseName = admissionsRs.getString("course_name");
                String dateOfAdmission = admissionsRs.getDate("date_of_admission").toString();
                double amountPaid = admissionsRs.getDouble("amount_paid");
                count++;
                tableModel.addRow(
                        new Object[] { count, admissionId, studentName, courseName, dateOfAdmission, amountPaid });

                totalAdmissions++;
                totalCash += amountPaid;
            }

            // Update total labels
            totalAdmissionsLabel.setText("Total Admissions: " + totalAdmissions);
            totalCashLabel.setText(String.format("Total Cash Received: ₹%.2f", totalCash));

            // Fetch and update graphs
            fetchGraphData(startDate, endDate, selectedCourse.equals("All Courses") ? null : selectedCourse);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching data: " + e.getMessage());
        }
    }

    private void fetchGraphData(String startDate, String endDate, String selectedCourse) {
        try {
            String graphQuery = "SELECT a.date_of_admission, COUNT(a.id) AS admissions_count, SUM(a.amount_paid) AS total_cash "
                    +
                    "FROM admissions a ";

            if (selectedCourse != null) {
                graphQuery += "JOIN courses c ON a.course_id = c.id ";
            }

            graphQuery += "WHERE a.date_of_admission BETWEEN ? AND ? ";

            if (selectedCourse != null) {
                graphQuery += "AND c.name = ? ";
            }

            graphQuery += "GROUP BY a.date_of_admission ORDER BY a.date_of_admission";

            PreparedStatement admissionsStmt = con.prepareStatement(graphQuery);
            admissionsStmt.setDate(1, Date.valueOf(LocalDate.parse(startDate)));
            admissionsStmt.setDate(2, Date.valueOf(LocalDate.parse(endDate)));

            if (selectedCourse != null) {
                admissionsStmt.setString(3, selectedCourse);
            }

            ResultSet admissionsRs = admissionsStmt.executeQuery();

            DefaultCategoryDataset admissionsDataset = new DefaultCategoryDataset();
            DefaultCategoryDataset cashDataset = new DefaultCategoryDataset();

            while (admissionsRs.next()) {
                String dateOfAdmission = admissionsRs.getDate("date_of_admission").toString();
                int admissionsCount = admissionsRs.getInt("admissions_count");
                double dailyCash = admissionsRs.getDouble("total_cash");

                admissionsDataset.addValue(admissionsCount, "Admissions", dateOfAdmission);
                cashDataset.addValue(dailyCash, "Cash Received", dateOfAdmission);
            }

            updateCharts(admissionsDataset, cashDataset);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching graph data: " + e.getMessage());
        }
    }

    private void updateCharts(DefaultCategoryDataset admissionsDataset, DefaultCategoryDataset cashDataset) {
        chartPanelContainer.removeAll();

        // Admissions Line Chart
        JFreeChart admissionsChart = ChartFactory.createLineChart(
                "Admissions Over Time",
                "Date",
                "Admissions Count",
                admissionsDataset);
        ChartPanel admissionsChartPanel = new ChartPanel(admissionsChart);
        chartPanelContainer.add(admissionsChartPanel);

        // Cash Line Chart
        JFreeChart cashChart = ChartFactory.createLineChart(
                "Cash Received Over Time",
                "Date",
                "Cash Received (₹)",
                cashDataset);
        ChartPanel cashChartPanel = new ChartPanel(cashChart);
        chartPanelContainer.add(cashChartPanel);

        chartPanelContainer.revalidate();
        chartPanelContainer.repaint();
    }

    private void populateCourseComboBox() {
        connectToDatabase();
        try {
            String query = "SELECT name FROM courses";
            PreparedStatement stmt = con.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                courseComboBox.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching courses: " + e.getMessage());
        }
    }

    // i am connecting to database
    public void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            if (companyDatabase != null) {
                // Use the dynamically determined database
                con = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/" + companyDatabase + "?zeroDateTimeBehavior=CONVERT_TO_NULL",
                        "springstudent", "springstudent");
            } else {
                throw new Exception("Company database is not set. Please ensure company selection is done first.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to connect to company database!", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        new SalesReportFrame(companyName).setVisible(true);
    }
}
