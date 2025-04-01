package contact_book_with_database;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.JComboBox; 
import com.toedter.calendar.JDateChooser;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFileChooser;

/**
 * A contact book application that connects to a MySQL database to store and
 * manage contacts.
 *
 * @author jagta
 */


public class Contact_Book_with_database extends javax.swing.JFrame {

    // Database connection parameters
    private static final String DB_URL = "jdbc:mysql://localhost:3308/contact_book";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = ""; 

    private Connection connection; // Database connection

    /**
     * Constructor to initialize the form and connect to the database.
     */
    public Contact_Book_with_database() {
        initComponents();
        connectToDatabase();
        loadContacts();
        setVisible(true);
    }

    /**
     * Connects to the database.
     */
    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); 
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            JOptionPane.showMessageDialog(this, "Connected to the database!");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "MySQL Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error connecting to the database: " + e.getMessage());
        }
    }

    /**
     * Loads contacts from the database into the table.
     */
    private void loadContacts() {
    String query = "SELECT * FROM contacts";
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

        
        DefaultTableModel model = (DefaultTableModel) contactsTable.getModel();
        model.setRowCount(0);

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address"),
                rs.getString("tags"),
                rs.getDate("birthdate"),
                rs.getBoolean("favorite")
            });
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error loading contacts: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}

    private void exportToCSV() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Save as CSV");
    int userSelection = fileChooser.showSaveDialog(this);

    if (userSelection == JFileChooser.APPROVE_OPTION) {
        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        if (!filePath.endsWith(".csv")) {
            filePath += ".csv"; // Ensure the file has a .csv extension
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            DefaultTableModel model = (DefaultTableModel) contactsTable.getModel();

            // Write column headers
            for (int i = 0; i < model.getColumnCount(); i++) {
                writer.write(model.getColumnName(i) + ",");
            }
            writer.write("\n");

            // Write rows
            for (int i = 0; i < model.getRowCount(); i++) {
                for (int j = 0; j < model.getColumnCount(); j++) {
                    Object value = model.getValueAt(i, j);
                    writer.write((value != null ? value.toString() : "") + ",");
                }
                writer.write("\n");
            }

            JOptionPane.showMessageDialog(this, "Contacts exported successfully to: " + filePath);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error exporting contacts: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
    
    private void addContact() {
        String name = nameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
        String address = addressField.getText();
        String tags = (String) tagsComboBox.getSelectedItem(); 
        java.util.Date birthdate = birthdatePicker.getDate();
        boolean favorite = favoriteCheckBox.isSelected();

        // Validate the birthdate
        if (birthdate == null) {
            JOptionPane.showMessageDialog(this, "Please select a valid birthdate.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String query = "INSERT INTO contacts (name, phone, email, address, tags, birthdate, favorite) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, email);
            stmt.setString(4, address);
            stmt.setString(5, tags);
            stmt.setDate(6, new java.sql.Date(birthdate.getTime()));
            stmt.setBoolean(7, favorite);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Contact added successfully!");
            loadContacts();
            clearFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding contact: " + e.getMessage());
        }
    }

    /**
     * Deletes the selected contact from the database.
     */
    private void deleteContact() {
        int selectedRow = contactsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a contact to delete.");
            return;
        }

        int id = (int) contactsTable.getValueAt(selectedRow, 0);

        String query = "DELETE FROM contacts WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Contact deleted successfully!");
            loadContacts();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting contact: " + e.getMessage());
        }
    }

    /**
     * Searches for contacts based on user input.
     */
    private void searchContacts() {
    String searchText = searchField.getText().trim().toLowerCase();
    DefaultTableModel model = (DefaultTableModel) contactsTable.getModel();
    model.setRowCount(0); // Clear the table

    if (searchText.isEmpty()) {
        loadContacts();
    } else {
        // If search field has text, search for contacts
        String query = "SELECT * FROM contacts WHERE LOWER(name) LIKE ? OR LOWER(phone) LIKE ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, "%" + searchText + "%");
            stmt.setString(2, "%" + searchText + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("address"),
                        rs.getString("tags"),
                        rs.getDate("birthdate"),
                        rs.getBoolean("favorite")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error searching contacts: " + e.getMessage());
        }
    }
}

    

    private void editContact() {
    // Get the selected row in the table
    int selectedRow = contactsTable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a contact to edit.");
        return;
    }

    // Get the contact ID from the selected row
    int id = (int) contactsTable.getValueAt(selectedRow, 0);

    // Populate the input fields with the selected contact's data
    nameField.setText((String) contactsTable.getValueAt(selectedRow, 1));
    phoneField.setText((String) contactsTable.getValueAt(selectedRow, 2));
    emailField.setText((String) contactsTable.getValueAt(selectedRow, 3));
    addressField.setText((String) contactsTable.getValueAt(selectedRow, 4));
    tagsComboBox.setSelectedItem(contactsTable.getValueAt(selectedRow, 5)); // Set dropdown value
    birthdatePicker.setDate((java.util.Date) contactsTable.getValueAt(selectedRow, 6));
    favoriteCheckBox.setSelected((Boolean) contactsTable.getValueAt(selectedRow, 7));

    // Enable the Save Changes button to save the edited data
    saveChangesButton.setEnabled(true);
    saveChangesButton.setActionCommand("Save Changes");
}

private void saveChanges() {
    // Get the selected row in the table
    int selectedRow = contactsTable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a contact to save.");
        return;
    }

    // Get the contact ID from the selected row
    int id = (int) contactsTable.getValueAt(selectedRow, 0);

    // Retrieve the data from the fields
    String name = nameField.getText();
    String phone = phoneField.getText();
    String email = emailField.getText();
    String address = addressField.getText();
    String tags = tagsComboBox.getSelectedItem().toString(); 
    java.util.Date birthdate = birthdatePicker.getDate(); 
    boolean favorite = favoriteCheckBox.isSelected();

    if (birthdate == null) {
        JOptionPane.showMessageDialog(this, "Please select a valid birthdate.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    // Prepare SQL query to update contact details
    String query = "UPDATE contacts SET name = ?, phone = ?, email = ?, address = ?, tags = ?, birthdate = ?, favorite = ? WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
        // Set the parameters for the SQL statement
        stmt.setString(1, name);
        stmt.setString(2, phone);
        stmt.setString(3, email);
        stmt.setString(4, address);
        stmt.setString(5, tags);  // Set tags
        stmt.setDate(6, new java.sql.Date(birthdate.getTime()));  // Convert java.util.Date to java.sql.Date
        stmt.setBoolean(7, favorite);
        stmt.setInt(8, id);

        // Execute the update
        int rowsUpdated = stmt.executeUpdate();

        if (rowsUpdated > 0) {
            JOptionPane.showMessageDialog(this, "Contact updated successfully!");
            loadContacts();  // Refresh the table with updated contacts
            clearFields();  // Clear the input fields after saving
            saveChangesButton.setEnabled(false);  // Disable Save Changes button after saving
        } else {
            JOptionPane.showMessageDialog(this, "Error: No rows were updated. Please check the contact ID.");
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error updating contact: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}


    private void clearFields() {
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        addressField.setText("");
        if (tagsComboBox.getItemCount() > 0) {
        tagsComboBox.setSelectedIndex(0); // Select the first item
    }
       birthdatePicker.setDate(null);
        favoriteCheckBox.setSelected(false);
    }

   
    private void initComponents() {
        // Main Frame setup
        setTitle("Contact Book");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel("Contact Book", JLabel.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(new Color(50, 50, 150));
        headerPanel.add(headerLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Contacts Table
        JPanel tablePanel = new JPanel(new BorderLayout());
        contactsTable = new JTable(new DefaultTableModel(new Object[]{"ID", "Name", "Phone", "Email", "Address", "Tags", "Birthdate", "Favorite"}, 0));
        contactsTable.setRowHeight(25);
        contactsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = new JScrollPane(contactsTable);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        // Add Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchField = new JTextField(20);
        searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchContacts());
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        tablePanel.add(searchPanel, BorderLayout.NORTH);

        tabbedPane.addTab("Contacts", tablePanel);

        // Add Contact Panel
        JPanel addContactPanel = new JPanel();
        addContactPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Input Fields
        gbc.gridx = 0;
        gbc.gridy = 0;
        addContactPanel.add(new JLabel("Name: "), gbc);
        nameField = new JTextField(20);
        gbc.gridx = 1;
        addContactPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        addContactPanel.add(new JLabel("Phone: "), gbc);
        phoneField = new JTextField(20);
        gbc.gridx = 1;
        addContactPanel.add(phoneField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        addContactPanel.add(new JLabel("Email: "), gbc);
        emailField = new JTextField(20);
        gbc.gridx = 1;
        addContactPanel.add(emailField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        addContactPanel.add(new JLabel("Address: "), gbc);
        addressField = new JTextField(20);
        gbc.gridx = 1;
        addContactPanel.add(addressField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        addContactPanel.add(new JLabel("Tags: "), gbc);
        tagsComboBox = new JComboBox<>(new String[]{"Family", "Friends", "Work", "Others"});
        gbc.gridx = 1;
        addContactPanel.add(tagsComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        addContactPanel.add(new JLabel("Birthdate: "), gbc);
        birthdatePicker = new JDateChooser();
        gbc.gridx = 1;
        addContactPanel.add(birthdatePicker, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        addContactPanel.add(new JLabel("Favorite: "), gbc);
        favoriteCheckBox = new JCheckBox();
        gbc.gridx = 1;
        addContactPanel.add(favoriteCheckBox, gbc);

        // Add Button
        gbc.gridx = 1;
        gbc.gridy = 7;
        addButton = new JButton("Add Contact");
        addButton.addActionListener(e -> addContact());
        addContactPanel.add(addButton, gbc);

        tabbedPane.addTab("Add Contact", addContactPanel);

        // Footer Panel
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        deleteButton = new JButton("Delete Contact");
        deleteButton.addActionListener(e -> deleteContact());
        footerPanel.add(deleteButton);
        editButton = new JButton("Edit Contact");
        editButton.addActionListener(e -> editContact());
        footerPanel.add(editButton);
        saveChangesButton = new JButton("Save Changes");
        saveChangesButton.setEnabled(false); // Disabled by default
        saveChangesButton.addActionListener(e -> saveChanges());
        footerPanel.add(saveChangesButton); // Add to footer panel

        JButton exportButton = new JButton("Export to CSV");
        exportButton.addActionListener(e -> exportToCSV());
        footerPanel.add(exportButton);
        add(tabbedPane, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    /**
     * Main method to run the application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Contact_Book_with_database());
    }

    // Variables declaration
    private javax.swing.JTextField nameField;
    private javax.swing.JTextField phoneField;
    private javax.swing.JTextField emailField;
    private javax.swing.JTextField addressField;
    private JComboBox<String> tagsComboBox; // Dropdown for tags
    private com.toedter.calendar.JDateChooser birthdatePicker;
    private javax.swing.JCheckBox favoriteCheckBox;
    private javax.swing.JTextField searchField;
    private javax.swing.JTable contactsTable;
    private javax.swing.JButton addButton;
    private javax.swing.JButton deleteButton;
    private javax.swing.JButton searchButton;
    private javax.swing.JButton editButton;
    private javax.swing.JButton saveChangesButton;
    private javax.swing.JButton exportButton;
}



