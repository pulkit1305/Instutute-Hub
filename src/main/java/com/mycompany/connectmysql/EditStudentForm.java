/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.connectmysql;

import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;

/**
 *
 * @author PULKIT SHARMA
 */
public class EditStudentForm extends javax.swing.JFrame {

    private JTextField nameField, emailField, contactField;
    private int studentId;
    private Connection con;
    private AdmissionsList parentFrame;

    private JComboBox<String> courseField;
    private JTextField feeField, paidField;
    int admissionId;

    public EditStudentForm(int studentId, Connection con, AdmissionsList parentFrame) {
        this.studentId = studentId;
        this.con = con;
        this.parentFrame = parentFrame;

        setTitle("Edit Student Details");
        setSize(300, 300);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initializeUI();
        loadStudentDetails();
        loadFeeAndPaidAmount();
    }

    private void initializeUI() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new java.awt.GridLayout(6, 1, 5, 5));

        // Name, Email, and Contact fields
        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Email:"));
        emailField = new JTextField();
        formPanel.add(emailField);

        formPanel.add(new JLabel("Contact:"));
        contactField = new JTextField();
        formPanel.add(contactField);

        // Course selection
        formPanel.add(new JLabel("Course:"));
        courseField = new JComboBox<>(getCourses()); // Get course list from DB
        courseField.addItemListener(e -> updateFeeForSelectedCourse());
        formPanel.add(courseField);

        // Total Fee field
        formPanel.add(new JLabel("Total Fee:"));
        feeField = new JTextField();
        feeField.setEditable(false); // Display only
        formPanel.add(feeField);

        // Amount Paid field
        formPanel.add(new JLabel("Amount Paid:"));
        paidField = new JTextField();
        formPanel.add(paidField);

        add(formPanel, BorderLayout.CENTER);

        JButton submitButton = new JButton("Enroll and Generate Receipt");
        submitButton.addActionListener(e -> enrollInCourse());
        add(submitButton, BorderLayout.SOUTH);
    }

    private void updateFeeForSelectedCourse() {
        String selectedCourse = (String) courseField.getSelectedItem();

        if (selectedCourse != null && !selectedCourse.isEmpty()) {
            String feeQuery = "SELECT fees FROM courses WHERE name = ?";
            try (PreparedStatement pstmt = con.prepareStatement(feeQuery)) {
                pstmt.setString(1, selectedCourse);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int fee = rs.getInt("fees");
                    feeField.setText(String.valueOf(fee)); // Update the fee field
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading fee details.");
            }
        }
    }

    private void enrollInCourse() {
        String selectedCourse = (String) courseField.getSelectedItem();
        String billNumber = generateBillNumber(); // Generate a new bill number

        String courseQuery = "SELECT id FROM courses WHERE name = ?";
        int courseId = -1;
        try (PreparedStatement courseStmt = con.prepareStatement(courseQuery)) {
            courseStmt.setString(1, selectedCourse);
            ResultSet courseRs = courseStmt.executeQuery();
            if (courseRs.next()) {
                courseId = courseRs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error retrieving course details.");
            return;
        }

        // Check if admission already exists for the user and course
        String admissionQuery = "SELECT id FROM admissions WHERE user_id = ? AND course_id = ?";
        admissionId = -1;
        try (PreparedStatement admissionStmt = con.prepareStatement(admissionQuery)) {
            admissionStmt.setInt(1, studentId);
            admissionStmt.setInt(2, courseId);
            ResultSet admissionRs = admissionStmt.executeQuery();
            if (admissionRs.next()) {
                admissionId = admissionRs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error retrieving admission details.");
            return;
        }

        if (admissionId == -1) {
            // Generate the reference number with the format you provided
            String referenceNumber = "REF" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + (new Random().nextInt(9000));

            // Get the amount paid from the form (this can be input by the user)
            double amountPaid = Double.parseDouble(paidField.getText()); // Ensure this field contains a valid number

            String insertAdmissionQuery = "INSERT INTO admissions (user_id, course_id, date_of_admission, amount_paid, reference_number) VALUES (?, ?, NOW(), ?, ?)";
            try (PreparedStatement admissionInsertStmt = con.prepareStatement(insertAdmissionQuery,
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                admissionInsertStmt.setInt(1, studentId);
                admissionInsertStmt.setInt(2, courseId);
                admissionInsertStmt.setDouble(3, amountPaid); // Set the amount paid
                admissionInsertStmt.setString(4, referenceNumber); // Set the reference number

                int rowsAffected = admissionInsertStmt.executeUpdate();
                if (rowsAffected > 0) {
                    ResultSet generatedKeys = admissionInsertStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        admissionId = generatedKeys.getInt(1); // Get the admission ID from the generated keys
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to create a new admission.");
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error while creating admission.");
                return;
            }
        }

        String enrollQuery = "INSERT INTO enrollments (user_id, course_id, enrollment_date, admission_id) VALUES (?, ?, NOW(), ?)";
        try (PreparedStatement enrollStmt = con.prepareStatement(enrollQuery)) {
            enrollStmt.setInt(1, studentId);
            enrollStmt.setInt(2, courseId);
            enrollStmt.setInt(3, admissionId);

            int rowsAffected = enrollStmt.executeUpdate();
            if (rowsAffected > 0) {
                insertPaymentDetails(billNumber);
                JOptionPane.showMessageDialog(this, "Enrollment successful!");
                generateReceipt(selectedCourse, billNumber);
                parentFrame.refreshData();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to enroll in the new course.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error occurred.");
        }
    }

    private void insertPaymentDetails(String billNumber) {
        String admissionIdQuery = "SELECT id FROM admissions WHERE user_id = ?";
        String paymentQuery = "INSERT INTO payments (bill_number, user_id, admission_id, amount_paid, payment_date) VALUES (?, ?, ?, ?, NOW())";

        try (PreparedStatement admissionStmt = con.prepareStatement(admissionIdQuery)) {
            // Step 1: Get admission_id
            admissionStmt.setInt(1, studentId);
            try (ResultSet rs = admissionStmt.executeQuery()) {
                if (rs.next()) {

                    // Step 2: Insert payment details
                    try (PreparedStatement paymentStmt = con.prepareStatement(paymentQuery)) {
                        System.out.println("Bill Number: " + billNumber);
                        System.out.println("Student ID: " + studentId);
                        System.out.println("Amount Paid: " + paidField.getText());

                        BigDecimal amountPaid = new BigDecimal(paidField.getText());
                        paymentStmt.setString(1, billNumber);
                        paymentStmt.setInt(2, studentId);
                        paymentStmt.setInt(3, admissionId); // Set admission_id
                        paymentStmt.setBigDecimal(4, amountPaid);

                        int rowsAffected = paymentStmt.executeUpdate();
                        if (rowsAffected > 0) {
                            System.out.println("Payment record inserted successfully.");
                        } else {
                            System.out.println("No rows affected. Payment insert failed.");
                        }
                    }
                } else {
                    System.out.println("Admission ID not found for student ID: " + studentId);
                    JOptionPane.showMessageDialog(this, "Admission details not found for this student.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving payment details. " + e.getMessage());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid amount entered. Please enter a valid number.");
        }
    }

    private String[] getCourses() {
        String query = "SELECT name FROM courses";
        try (PreparedStatement pstmt = con.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery()) {
            List<String> courses = new ArrayList<>();
            while (rs.next()) {
                courses.add(rs.getString("name"));
            }
            return courses.toArray(new String[0]);
        } catch (SQLException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    private String generateBillNumber() {
        return "BIL" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + new Random().nextInt(9000);
    }

    private void loadStudentDetails() {
        String query = "SELECT u.name, u.email, u.contact, c.name AS course_name FROM users u "
                + "JOIN admissions a ON u.user_id = a.user_id "
                + "JOIN courses c ON a.course_id = c.id WHERE u.user_id = ?";

        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                nameField.setText(rs.getString("name"));
                emailField.setText(rs.getString("email"));
                contactField.setText(rs.getString("contact"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading student details.");
        }
    }

    private void loadFeeAndPaidAmount() {
        String query = "SELECT fees, amount_paid FROM admissions a "
                + "JOIN courses c ON a.course_id = c.id WHERE a.user_id = ?";

        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                feeField.setText(String.valueOf(rs.getInt("fees")));
                paidField.setText(String.valueOf(rs.getInt("amount_paid")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading fee details.");
        }
    }

    private void generateReceipt(String courseName, String billNumber) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Bill Number: ").append(billNumber).append("\n");

        receipt.append("Student: ").append(nameField.getText()).append("\n");
        receipt.append("Course: ").append(courseName).append("\n");
        receipt.append("Total Fee: ").append(feeField.getText()).append("\n");
        receipt.append("Amount Paid: ").append(paidField.getText()).append("\n");
        receipt.append("Date: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))).append("\n");

        JOptionPane.showMessageDialog(this, receipt.toString(), "Receipt", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 400, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 300, Short.MAX_VALUE));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
        // (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default
         * look and feel.
         * For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(EditStudentForm.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(EditStudentForm.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(EditStudentForm.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(EditStudentForm.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        }
        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            int studentId;
            Connection con;
            AdmissionsList parentFrame;

            public void run() {
                new EditStudentForm(studentId, con, parentFrame).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
