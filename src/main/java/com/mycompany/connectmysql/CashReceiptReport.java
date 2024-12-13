package com.mycompany.connectmysql;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Cell;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;

public class CashReceiptReport extends javax.swing.JFrame {
    private JTable cashTable;
    private DefaultTableModel tableModel;
    private JTextField startDateField, endDateField;
    private JLabel totalAdmissionsLabel, totalCashLabel;
    private Connection con;
    private JPanel chartPanelContainer;
    private JComboBox<String> courseComboBox;
    private int count = 0;
    private String companyDatabase;
    private static String companyName;
    private JButton downloadButton;

    public CashReceiptReport(String companyName) {
        setCompanyDatabase(companyName);
        setTitle("Cash Receipt Report- " + companyName);
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top panel for date selection
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

        // Table for displaying cash data
        tableModel = new DefaultTableModel(
                new String[] { "Sno.", "Admission ID", "Student Name", "Course Name", "Date", "Amount Paid" }, 0);
        cashTable = new JTable(tableModel);

        JScrollPane tableScrollPane = new JScrollPane(cashTable);
        tableScrollPane.setPreferredSize(new Dimension(600, 0));
        add(tableScrollPane, BorderLayout.WEST);

        // Bottom panel for totals
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        totalAdmissionsLabel = new JLabel("Total Admissions payment: 0");
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
                fetchCashData();
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

    private void fetchCashData() {
        String startDate = startDateField.getText();
        String endDate = endDateField.getText();
        String selectedCourse = courseComboBox.getSelectedItem().toString();

        if (startDate.isEmpty() || endDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both start and end dates.");
            return;
        }

        connectToDatabase();
        try {
            // Updated SQL query to fetch records with non-zero payment
            String cashQuery = "SELECT a.id AS admission_id, u.name AS student_name, c.name AS course_name, " +
                    "a.date_of_admission, a.amount_paid " +
                    "FROM admissions a " +
                    "JOIN users u ON a.user_id = u.user_id " +
                    "JOIN courses c ON a.course_id = c.id " +
                    "WHERE a.date_of_admission BETWEEN ? AND ? AND a.amount_paid > 0";
            if (!selectedCourse.equals("All Courses")) {
                cashQuery += " AND c.name = ?";
            }
            PreparedStatement cashStmt = con.prepareStatement(cashQuery);
            cashStmt.setDate(1, Date.valueOf(LocalDate.parse(startDate)));
            cashStmt.setDate(2, Date.valueOf(LocalDate.parse(endDate)));
            if (!selectedCourse.equals("All Courses")) {
                cashStmt.setString(3, selectedCourse);
            }
            ResultSet cashRs = cashStmt.executeQuery();
            count = 0;
            // Clear previous data
            tableModel.setRowCount(0);

            int totalAdmissions = 0;
            double totalCash = 0.0;

            // Populate the table and calculate totals
            while (cashRs.next()) {
                int admissionId = cashRs.getInt("admission_id");
                String studentName = cashRs.getString("student_name");
                String courseName = cashRs.getString("course_name");
                String dateOfAdmission = cashRs.getDate("date_of_admission").toString();
                double amountPaid = cashRs.getDouble("amount_paid");
                count++;
                tableModel.addRow(
                        new Object[] { count, admissionId, studentName, courseName, dateOfAdmission, amountPaid });
                totalAdmissions++;
                totalCash += amountPaid;
            }

            // Update total labels
            totalAdmissionsLabel.setText("Total Admissions with payment: " + totalAdmissions);
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

            if (selectedCourse != null && !selectedCourse.equalsIgnoreCase("All Courses")) {
                graphQuery += "JOIN courses c ON a.course_id = c.id ";
            }

            graphQuery += "WHERE a.date_of_admission BETWEEN ? AND ? AND a.amount_paid > 0 ";

            if (selectedCourse != null && !selectedCourse.equalsIgnoreCase("All Courses")) {
                graphQuery += "AND c.name = ? ";
            }

            graphQuery += "GROUP BY a.date_of_admission ORDER BY a.date_of_admission";

            PreparedStatement cashStmt = con.prepareStatement(graphQuery);
            cashStmt.setDate(1, Date.valueOf(startDate));
            cashStmt.setDate(2, Date.valueOf(endDate));
            if (selectedCourse != null && !selectedCourse.equalsIgnoreCase("All Courses")) {
                cashStmt.setString(3, selectedCourse);
            }

            ResultSet cashRs = cashStmt.executeQuery();

            DefaultCategoryDataset admissionsDataset = new DefaultCategoryDataset();
            DefaultCategoryDataset cashDataset = new DefaultCategoryDataset();

            // Map to store data for plotting
            Map<String, int[]> graphData = new HashMap<>();

            // Process result set
            while (cashRs.next()) {
                String dateOfAdmission = cashRs.getDate("date_of_admission").toString();
                int admissionsCount = cashRs.getInt("admissions_count");
                int totalCash = cashRs.getInt("total_cash");

                graphData.put(dateOfAdmission, new int[] { admissionsCount, totalCash });
            }

            // Ensure all dates in range are represented
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            while (!start.isAfter(end)) {
                String currentDate = start.toString();
                int[] data = graphData.getOrDefault(currentDate, new int[] { 0, 0 });
                admissionsDataset.addValue(data[0], "Admissions", currentDate);
                cashDataset.addValue(data[1], "Total Cash", currentDate);
                start = start.plusDays(1);
            }

            // Generate charts
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

    private void generatePDFWithGraph() {
        // Prepare the PDF document
        Document document = new Document();
        try {
            String outputPath = "D:/Downloads/CashReceiptReport.pdf"; // Specify a valid path
            File pdfFile = new File(outputPath);
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            // Title of the report
            document.add(new com.itextpdf.text.Paragraph("Cash Receipt Report"));
            document.add(new com.itextpdf.text.Paragraph("Company: " + companyName));
            document.add(new com.itextpdf.text.Paragraph("Start Date: " + startDateField.getText()));
            document.add(new com.itextpdf.text.Paragraph("End Date: " + endDateField.getText()));
            document.add(new com.itextpdf.text.Paragraph("Course: " + courseComboBox.getSelectedItem()));
            document.add(new com.itextpdf.text.Paragraph("\n"));

            com.itextpdf.text.Table pdfTable = new com.itextpdf.text.Table(6); // 6 columns for the table
            pdfTable.addCell("Sno.");
            pdfTable.addCell("Admission ID");
            pdfTable.addCell("Student Name");
            pdfTable.addCell("Course Name");
            pdfTable.addCell("Date");
            pdfTable.addCell("Amount Paid");

            // Loop through the table rows and add data to PDF
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                pdfTable.addCell(tableModel.getValueAt(i, 0).toString());
                pdfTable.addCell(tableModel.getValueAt(i, 1).toString());
                pdfTable.addCell(tableModel.getValueAt(i, 2).toString());
                pdfTable.addCell(tableModel.getValueAt(i, 3).toString());
                pdfTable.addCell(tableModel.getValueAt(i, 4).toString());
                pdfTable.addCell(tableModel.getValueAt(i, 5).toString());
            }

            document.add(pdfTable);
            document.add(new com.itextpdf.text.Paragraph("\n"));

            // Graph image as chart for PDF
            BufferedImage graphImage = new BufferedImage(chartPanelContainer.getWidth(),
                    chartPanelContainer.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = graphImage.createGraphics();
            chartPanelContainer.paint(g2d);
            g2d.dispose();

            // Convert graph image to iText Image and add to PDF
            Image pdfGraphImage = Image.getInstance(graphImage, null);
            pdfGraphImage.scaleToFit(500, 400); // Resize graph to fit into PDF
            document.add(pdfGraphImage);

            // Close the document
            document.close();

            JOptionPane.showMessageDialog(this, "PDF report generated successfully at: " + pdfFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating PDF: " + e.getMessage());
        }
    }
    

    public static void main(String[] args) {
        new CashReceiptReport(companyName);
    }
}