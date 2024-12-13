/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.connectmysql;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author PULKIT SHARMA
 */
public class AdmissionsList extends javax.swing.JFrame {

    private JTable admissionTable;
    private DefaultTableModel tableModel;
    private JScrollPane scrollPane;
    private Connection con;
    private JTextField searchField;
    private JButton searchButton;
    private String companyName;
    private String companyDatabase;

    public AdmissionsList(String companyName) {
        this.companyName = companyName;
        setCompanyDatabase(companyName);
        if (companyDatabase == null) {
            JOptionPane.showMessageDialog(null, "Failed to determine the company database. Please contact support.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            dispose(); // Close the form if the company database is not set
            return;
        }

        setTitle("Admitted Students List- " + companyName);
        setSize(800, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initializeUI();
        connectToDatabase();
        fetchAdmissionData();
    }

    private void initializeUI() {

        JPanel searchPanel = new JPanel();
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> backbutton());
        searchPanel.add(backButton, BorderLayout.WEST);
        searchField = new JTextField(20);
        searchButton = new JButton("Search");
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        add(searchPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Allow editing for "Pay Fees" (column 7), "Edit" (column 8), and "Delete"
                // (column 9)
                return column == 8 || column == 9 || column == 10;
            }
        };

        tableModel.addColumn("Student ID");
        tableModel.addColumn("Admission ID");
        tableModel.addColumn("Name");
        tableModel.addColumn("Email");
        tableModel.addColumn("Course Name");
        tableModel.addColumn("Total Fees");
        tableModel.addColumn("Amount Paid");
        tableModel.addColumn("Remaining Fees");
        tableModel.addColumn("Pay Fees");
        tableModel.addColumn("Edit");
        tableModel.addColumn("Delete");

        admissionTable = new JTable(tableModel);
        admissionTable.getColumn("Pay Fees").setCellRenderer(new ButtonRenderer());
        admissionTable.getColumn("Pay Fees").setCellEditor(new ButtonEditor(new JCheckBox()));

        admissionTable.getColumn("Edit").setCellRenderer(new ButtonRenderer());
        admissionTable.getColumn("Edit").setCellEditor(new ButtonEditor(new JCheckBox()));

        admissionTable.getColumn("Delete").setCellRenderer(new ButtonRenderer());
        admissionTable.getColumn("Delete").setCellEditor(new ButtonEditor(new JCheckBox()));

        scrollPane = new JScrollPane(admissionTable);
        add(scrollPane, BorderLayout.CENTER);

        // I am Adding search functionality
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchAdmissions(searchField.getText().trim());
            }

        });

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

    private void backbutton() {
        AdminDashboard window2 = new AdminDashboard(companyName);
        window2.setVisible(true);
        this.dispose();

    }

    private void searchAdmissions(String query) {
        String searchQuery = "SELECT u.user_id, a.id,u.name, u.email, c.name AS course_name, "
                + "c.fees, a.amount_paid, (c.fees - a.amount_paid) AS remaining_fees "
                + "FROM users u "
                + "JOIN admissions a ON u.user_id = a.user_id "
                + "JOIN courses c ON a.course_id = c.id "
                + "WHERE u.name LIKE ? OR u.email LIKE ? OR c.name LIKE ?";

        try (PreparedStatement pstmt = con.prepareStatement(searchQuery)) {
            pstmt.setString(1, "%" + query + "%");
            pstmt.setString(2, "%" + query + "%");
            pstmt.setString(3, "%" + query + "%");

            ResultSet rs = pstmt.executeQuery();
            tableModel.setRowCount(0); // Clear existing rows

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("user_id"));
                System.out.println("hi" + rs.getInt("id"));
                row.add(rs.getInt("id"));
                row.add(rs.getString("name"));
                row.add(rs.getString("email"));
                row.add(rs.getString("course_name"));
                row.add(rs.getInt("fees"));
                row.add(rs.getInt("amount_paid"));
                row.add(rs.getInt("remaining_fees"));
                row.add("Pay Fees");
                row.add("Edit");
                row.add("Delete");

                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

    private Map<Integer, String> billNumberMap = new HashMap<>();

    private void fetchAdmissionData() {
        String query = "SELECT u.user_id, a.id , p.bill_number, u.name, u.email, c.name AS course_name, "
                + "c.fees, a.amount_paid, (c.fees - a.amount_paid) AS remaining_fees "
                + "FROM users u "
                + "JOIN admissions a ON u.user_id = a.user_id "
                + "JOIN courses c ON a.course_id = c.id "
                + "LEFT JOIN payments p ON a.id = p.admission_id";

        try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            tableModel.setRowCount(0);

            while (rs.next()) {
                int admissionId = rs.getInt("id");
                billNumberMap.put(admissionId, rs.getString("bill_number"));

                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("user_id"));
                row.add(admissionId);
                row.add(rs.getString("name"));
                row.add(rs.getString("email"));
                row.add(rs.getString("course_name"));
                row.add(rs.getInt("fees"));
                row.add(rs.getInt("amount_paid"));
                row.add(rs.getInt("remaining_fees"));
                row.add("Pay Fees");
                row.add("Edit");
                row.add("Delete");

                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setText((value == null) ? "Pay Fees" : value.toString());

            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {

        private JButton button;
        private String label;
        private int selectedRow;
        private int selectedColumn;
        private JTable table;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);

            button.addActionListener(e -> {
                if (table != null) {
                    handleButtonClick(selectedColumn);
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            this.table = table;
            this.selectedRow = row;
            this.selectedColumn = column;
            this.label = (value == null) ? "" : value.toString();
            button.setText(label);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return label;
        }

        private void handleButtonClick(int column) {
            switch (column) {
                case 8 -> openPaymentForm(); // "Pay Fees"
                case 9 -> editStudentDetails(); // "Edit"
                case 10 -> deleteStudent(); // "Delete"
            }
        }

        private void openPaymentForm() {
            int studentId = (int) tableModel.getValueAt(selectedRow, 0);
            int admissionId = (int) tableModel.getValueAt(selectedRow, 1);

            String billNumber = billNumberMap.get(admissionId); // Get Bill Number from the Map
            new PayFeesForm(companyName, studentId, admissionId, billNumber, con, AdmissionsList.this).setVisible(true); // Pass
            // Admission
            // ID
            // and Bill Number
        }
        // private void openPaymentForm() {
        // int admissionId = (int) tableModel.getValueAt(selectedRow, 1); // Get
        // Admission ID
        // String courseName = (String) tableModel.getValueAt(selectedRow, 4); // Course
        // Name
        // new PayFeesForm(admissionId, con, AdmissionsList.this).setVisible(true); //
        // Pass Admission ID
        // }

        private void editStudentDetails() {
            int studentId = (int) tableModel.getValueAt(selectedRow, 0);

            // New Form for Editing and Course Enrollment
            new EditStudentForm(studentId, con, AdmissionsList.this).setVisible(true);
        }

        private void deleteStudent() {
            int studentId = (int) tableModel.getValueAt(selectedRow, 0);
            int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this student?",
                    "Delete Confirmation", JOptionPane.YES_NO_OPTION);

            if (confirmation == JOptionPane.YES_OPTION) {
                try (PreparedStatement pstmt = con.prepareStatement("DELETE FROM admissions WHERE user_id = ?")) {
                    pstmt.setInt(1, studentId);
                    int rowsAffected = pstmt.executeUpdate();

                    if (rowsAffected > 0) {
                        JOptionPane.showMessageDialog(null, "Student deleted successfully.");
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(null, "Failed to delete the student.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Database error occurred.");
                }
            }
        }
    }

    // Refresh data (useful for after an admission is added/updated)
    public void refreshData() {
        fetchAdmissionData();
    }

    // Main method to run the JFrame
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Admitted Students List");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {
                        { null, null, null, null },
                        { null, null, null, null },
                        { null, null, null, null },
                        { null, null, null, null }
                },
                new String[] {
                        "Title 1", "Title 2", "Title 3", "Title 4"
                }));
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(142, 142, 142)
                                                .addComponent(jLabel1))
                                        .addGroup(layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 375,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(19, Short.MAX_VALUE)));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 275,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

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
            java.util.logging.Logger.getLogger(AdmissionsList.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AdmissionsList.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AdmissionsList.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AdmissionsList.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        }
        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            String companyName;

            public void run() {
                new AdmissionsList(companyName).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}

// yy
