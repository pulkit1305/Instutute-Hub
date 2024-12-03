/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.connectmysql;

import java.awt.Desktop;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

/**
 *
 * @author PULKIT SHARMA
 */
public class PayFeesForm extends javax.swing.JFrame {

    private JTextField amountField;
    private JLabel remainingFeesLabel, paidAmountLabel, studentIdLabel, studentNameLabel, emailLabel, courseNameLabel;
    private int studentId;
    private Connection con;
    private int currentAmountPaid, remainingFees;
    private AdmissionsList parent;

    public PayFeesForm(int studentId, Connection con, AdmissionsList parent) {
        this.studentId = studentId;
        this.con = con;
        this.parent = parent;
        setTitle("Pay Fees");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        initializeUI();
        fetchStudentData();
    }

    private void initializeUI() {
        setLayout(new GridLayout(9, 6, 15, 9));

        add(new JLabel("Student ID:"));
        studentIdLabel = new JLabel();
        add(studentIdLabel);

        add(new JLabel("Student Name:"));
        studentNameLabel = new JLabel();
        add(studentNameLabel);

        add(new JLabel("Email:"));
        emailLabel = new JLabel();
        add(emailLabel);

        add(new JLabel("Course Name:"));
        courseNameLabel = new JLabel();
        add(courseNameLabel);

        add(new JLabel("Amount Paid:"));
        paidAmountLabel = new JLabel();
        add(paidAmountLabel);

        add(new JLabel("Remaining Fees:"));
        remainingFeesLabel = new JLabel();
        add(remainingFeesLabel);

        add(new JLabel("Enter Amount to Pay:"));
        amountField = new JTextField();
        add(amountField);

        JButton payButton = new JButton("Pay Fees");
        payButton.addActionListener(e -> payFees());
        add(payButton);

        // Second button (logic not implemented as per your request)
        JButton secondButton = new JButton("Print Reciept");
        secondButton.addActionListener(e -> printReciept());
        add(secondButton);

        JButton shareButton = new JButton("Share Receipt");
        shareButton.addActionListener(e -> shareReceipt());
        add(shareButton);
    }

    private void fetchStudentData() {
        String query = "SELECT a.amount_paid, (c.fees - a.amount_paid) AS remaining_fees, u.name, u.email AS email, c.name AS course_name "
                + "FROM admissions a "
                + "JOIN users u ON a.user_id = u.user_id " // Ensure the correct columns
                + "JOIN courses c ON a.course_id = c.id "
                + "WHERE a.user_id = ?";

        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setInt(1, studentId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                currentAmountPaid = rs.getInt("amount_paid");
                remainingFees = rs.getInt("remaining_fees");

                studentIdLabel.setText(String.valueOf(studentId));
                studentNameLabel.setText(rs.getString("name"));
                emailLabel.setText(rs.getString("email"));
                courseNameLabel.setText(rs.getString("course_name"));
                paidAmountLabel.setText("₹ " + currentAmountPaid);
                remainingFeesLabel.setText("₹ " + remainingFees);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshStudentData() {
        fetchStudentData(); // Re-use fetchStudentData to update labels
    }

    private void payFees() {
        try {
            int amountToPay = Integer.parseInt(amountField.getText());

            if (amountToPay <= 0 || amountToPay > remainingFees) {
                JOptionPane.showMessageDialog(this, "Invalid amount. Please enter a valid value.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int newAmountPaid = currentAmountPaid + amountToPay;

            String updateQuery = "UPDATE admissions SET amount_paid = ? WHERE user_id = ?";
            try (PreparedStatement pst = con.prepareStatement(updateQuery)) {
                pst.setInt(1, newAmountPaid);
                pst.setInt(2, studentId);
                pst.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Fees updated successfully!");

            refreshStudentData(); // Refresh the form data

            if (parent != null) {
                parent.refreshData(); // Refresh the parent table data
            }

            amountField.setText(""); // Clear the input field

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating fees.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printReciept() {
        // Creating a simple HTML template for the receipt
        String htmlContent = "<html><body style='font-family: Arial, sans-serif; margin: 20px;'>"
                + "<div style='text-align: center; border-bottom: 2px solid #000; padding-bottom: 10px;'>"
                + "<h1 style='margin: 0;'>BITS COMPUTER INSTITUTE</h1>"
                + "<p style='margin: 5px 0; font-size: 18px;'>Payment Receipt</p>"
                + "</div>"
                + "<div style='margin-top: 20px;'>"
                + "<p><strong>Student ID:</strong> " + studentIdLabel.getText() + "</p>"
                + "<p><strong>Student Name:</strong> " + studentNameLabel.getText() + "</p>"
                + "<p><strong>Email:</strong> " + emailLabel.getText() + "</p>"
                + "<p><strong>Course Name:</strong> " + courseNameLabel.getText() + "</p>"
                + "<p><strong>Amount Paid:</strong> " + paidAmountLabel.getText() + "</p>"
                + "<p><strong>Remaining Fees:</strong> " + remainingFeesLabel.getText() + "</p>"
                + "<p><strong>Date:</strong> " + java.time.LocalDate.now() + "</p>"
                + "</div>"
                + "<div style='text-align: center; margin-top: 30px;'>"
                + "<p>Thank you for your payment!</p>"
                + "<p style='font-size: 14px;'>Please keep this receipt for your records.</p>"
                + "</div>"
                + "</body></html>";

        // Creating a JTextPane and setting the HTML content
        JTextPane jtp = new JTextPane();
        jtp.setContentType("text/html");
        jtp.setText(htmlContent);
        jtp.setEditable(false);

        // Displaying the receipt in a dialog before printing
        JOptionPane.showMessageDialog(this, new JScrollPane(jtp), "Receipt Preview", JOptionPane.INFORMATION_MESSAGE);

        // Attempting to print the receipt
        try {
            jtp.print();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error printing the receipt.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void shareReceipt() {
        try {
            // Generate and save receipt
            File receiptFile = new File("Receipt_" + studentId + ".html");
            try (FileWriter writer = new FileWriter(receiptFile)) {
                writer.write(generateReceiptHTML());
            }

            // Prepare subject and body manually
            String subject = "Payment Receipt";
            String body = "Dear Student,%0A%0A"
                    + "Please find your payment receipt attached.%0A%0A"
                    + "Thank you,%0A"
                    + "BITS COMPUTER INSTITUTE";

            // Construct the mailto URI without using URLEncoder
            String mailtoURI = String.format("mailto:?subject=%s&body=%s",
                    subject.replace(" ", "%20"),
                    body.replace(" ", "%20"));

            Desktop.getDesktop().mail(new URI(mailtoURI));

            JOptionPane.showMessageDialog(this, "Email client opened. Please attach the receipt manually.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error creating receipt file. Please try again.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error sharing the receipt. Please try again.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String generateReceiptHTML() {
        return "<html><body style='font-family: Arial, sans-serif; margin: 20px;'>"
                + "<div style='text-align: center; border-bottom: 2px solid #000; padding-bottom: 10px;'>"
                + "<h1 style='margin: 0;'>BITS COMPUTER INSTITUTE</h1>"
                + "<p style='margin: 5px 0; font-size: 18px;'>Payment Receipt</p>"
                + "</div>"
                + "<div style='margin-top: 20px;'>"
                + "<p><strong>Student ID:</strong> " + studentIdLabel.getText() + "</p>"
                + "<p><strong>Student Name:</strong> " + studentNameLabel.getText() + "</p>"
                + "<p><strong>Email:</strong> " + emailLabel.getText() + "</p>"
                + "<p><strong>Course Name:</strong> " + courseNameLabel.getText() + "</p>"
                + "<p><strong>Amount Paid:</strong> " + paidAmountLabel.getText() + "</p>"
                + "<p><strong>Remaining Fees:</strong> " + remainingFeesLabel.getText() + "</p>"
                + "<p><strong>Date:</strong> " + java.time.LocalDate.now() + "</p>"
                + "</div>"
                + "<div style='text-align: center; margin-top: 30px;'>"
                + "<p>Thank you for your payment!</p>"
                + "<p style='font-size: 14px;'>Please keep this receipt for your records.</p>"
                + "</div>"
                + "</body></html>";
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
         * If Nimbus (int
         * 
         * 
         * or details see
         * http://download.oracle.com/java /tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PayFeesForm.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PayFeesForm.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PayFeesForm.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PayFeesForm.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        }
        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            int studentId;

            Connection con;
            AdmissionsList parent;

            public void run() {
                new PayFeesForm(studentId, con, parent).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
///////////////// sdddddd
