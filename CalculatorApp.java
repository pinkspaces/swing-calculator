import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CalculatorApp extends JFrame {
    private static final Path FILE = Paths.get("operations.txt");

    private final JTextField firstNumber = new JTextField();
    private final JTextField secondNumber = new JTextField();
    private final JComboBox<String> operator =
            new JComboBox<>(new String[]{"+", "-", "*", "/"});
    private final DefaultListModel<Operation> historyModel = new DefaultListModel<>();
    private final JList<Operation> historyList = new JList<>(historyModel);

    public CalculatorApp() {
        super("Calculator with Saved History");
        loadHistory();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 400);
        setLocationRelativeTo(null);

        JPanel calculator = new JPanel(new GridLayout(4, 2, 8, 8));
        calculator.setBorder(BorderFactory.createTitledBorder("Calculator"));
        calculator.add(new JLabel("First number:"));
        calculator.add(firstNumber);
        calculator.add(new JLabel("Operation:"));
        calculator.add(operator);
        calculator.add(new JLabel("Second number:"));
        calculator.add(secondNumber);

        JButton calculateButton = new JButton("Calculate and Save");
        JLabel resultLabel = new JLabel("Result: ");
        calculator.add(calculateButton);
        calculator.add(resultLabel);

        calculateButton.addActionListener(e -> {
            try {
                double a = Double.parseDouble(firstNumber.getText().trim());
                double b = Double.parseDouble(secondNumber.getText().trim());
                String op = (String) operator.getSelectedItem();

                double result;
                switch (op) {
                    case "+" -> result = a + b;
                    case "-" -> result = a - b;
                    case "*" -> result = a * b;
                    case "/" -> {
                        if (b == 0) {
                            throw new ArithmeticException("Cannot divide by zero.");
                        }
                        result = a / b;
                    }
                    default -> throw new IllegalStateException("Unknown operation.");
                }

                String expression = a + " " + op + " " + b;
                resultLabel.setText("Result: " + result);
                historyModel.addElement(new Operation(
                        String.valueOf(System.currentTimeMillis()), expression, result));
                saveHistory();
            } catch (NumberFormatException ex) {
                showError("Enter valid numbers.");
            } catch (ArithmeticException ex) {
                showError(ex.getMessage());
            } catch (IOException ex) {
                showError("Could not save history: " + ex.getMessage());
            }
        });

        JPanel historyPanel = new JPanel(new BorderLayout(5, 5));
        historyPanel.setBorder(BorderFactory.createTitledBorder("Saved operations"));
        historyPanel.add(new JScrollPane(historyList), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        buttons.add(updateButton);
        buttons.add(deleteButton);
        historyPanel.add(buttons, BorderLayout.SOUTH);

        updateButton.addActionListener(e -> updateSelected());
        deleteButton.addActionListener(e -> deleteSelected());

        setLayout(new GridLayout(1, 2, 10, 10));
        add(calculator);
        add(historyPanel);
    }

    private void updateSelected() {
        int index = historyList.getSelectedIndex();
        if (index < 0) {
            showError("Select an operation to update.");
            return;
        }

        Operation old = historyModel.getElementAt(index);
        JTextField expression = new JTextField(old.expression);
        JTextField result = new JTextField(String.valueOf(old.result));

        JPanel form = new JPanel(new GridLayout(2, 2, 5, 5));
        form.add(new JLabel("Expression:"));
        form.add(expression);
        form.add(new JLabel("Result:"));
        form.add(result);

        int choice = JOptionPane.showConfirmDialog(
                this, form, "Update operation", JOptionPane.OK_CANCEL_OPTION);

        if (choice == JOptionPane.OK_OPTION) {
            try {
                double updatedResult = Double.parseDouble(result.getText().trim());
                historyModel.set(index, new Operation(
                        old.id, expression.getText().trim(), updatedResult));
                saveHistory();
            } catch (NumberFormatException ex) {
                showError("Result must be a valid number.");
            } catch (IOException ex) {
                showError("Could not save history: " + ex.getMessage());
            }
        }
    }

    private void deleteSelected() {
        int index = historyList.getSelectedIndex();
        if (index < 0) {
            showError("Select an operation to delete.");
            return;
        }

        historyModel.remove(index);
        try {
            saveHistory();
        } catch (IOException ex) {
            showError("Could not save history: " + ex.getMessage());
        }
    }

    private void loadHistory() {
        if (!Files.exists(FILE)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] parts = line.split("\\t", -1);
                if (parts.length != 3) continue;

                try {
                    String expression = new String(
                            Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    double result = Double.parseDouble(parts[2]);
                    historyModel.addElement(new Operation(parts[0], expression, result));
                } catch (IllegalArgumentException ignored) {
                    // Skip malformed entries.
                }
            }
        } catch (IOException ex) {
            showError("Could not load history: " + ex.getMessage());
        }
    }

    private void saveHistory() throws IOException {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < historyModel.size(); i++) {
            Operation op = historyModel.getElementAt(i);
            String encodedExpression = Base64.getEncoder().encodeToString(
                    op.expression.getBytes(StandardCharsets.UTF_8));
            lines.add(op.id + "\t" + encodedExpression + "\t" + op.result);
        }
        Files.write(FILE, lines, StandardCharsets.UTF_8);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class Operation {
        final String id;
        final String expression;
        final double result;

        Operation(String id, String expression, double result) {
            this.id = id;
            this.expression = expression;
            this.result = result;
        }

        @Override
        public String toString() {
            return expression + " = " + result;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculatorApp().setVisible(true));
    }
}
