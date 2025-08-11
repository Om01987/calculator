package com.example.calculator;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.math.BigDecimal;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvExpression, tvResult;
    private Button btnAdd, btnSubtract, btnMultiply, btnDivide;
    private Button selectedOperatorButton = null;

    // Calculator state variables
    private String expression = "";
    private String lastResult = "0";
    private String storedFirstValue = ""; // For CE functionality
    private String storedOperator = ""; // For CE functionality
    private boolean justCalculated = false;
    private boolean errorState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize displays and operator buttons
        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);
        btnAdd = findViewById(R.id.btnAdd);
        btnSubtract = findViewById(R.id.btnSubtract);
        btnMultiply = findViewById(R.id.btnMultiply);
        btnDivide = findViewById(R.id.btnDivide);

        // Set initial display style
        setRuntimeDisplayStyle();

        // Set up button listeners
        setupNumberButtons();
        setupOperatorButtons();
        setupFunctionButtons();
    }

    private void setupNumberButtons() {
        // Number button IDs
        int[] numberButtonIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        View.OnClickListener numberClickListener = v -> {
            Button button = (Button) v;
            String number = button.getText().toString();
            handleNumberInput(number);
        };

        // Assign listeners to number buttons
        for (int id : numberButtonIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        // Decimal button
        findViewById(R.id.btnDecimal).setOnClickListener(v -> handleDecimalInput());
    }

    private void setupOperatorButtons() {
        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            handleOperatorInput("+");
            selectOperatorButton(btnAdd);
        });

        findViewById(R.id.btnSubtract).setOnClickListener(v -> {
            handleOperatorInput("-");
            selectOperatorButton(btnSubtract);
        });

        findViewById(R.id.btnMultiply).setOnClickListener(v -> {
            handleOperatorInput("×");
            selectOperatorButton(btnMultiply);
        });

        findViewById(R.id.btnDivide).setOnClickListener(v -> {
            handleOperatorInput("÷");
            selectOperatorButton(btnDivide);
        });

        findViewById(R.id.btnEquals).setOnClickListener(v -> handleEqualsInput());
    }

    private void setupFunctionButtons() {
        findViewById(R.id.btnAC).setOnClickListener(v -> handleAC());
        findViewById(R.id.btnCE).setOnClickListener(v -> handleCE());
        findViewById(R.id.btnBackspace).setOnClickListener(v -> handleBackspace());
        findViewById(R.id.btnPlusMinus).setOnClickListener(v -> handlePlusMinus());
        findViewById(R.id.btnPercent).setOnClickListener(v -> handlePercentage());

        // Long press backspace for CE functionality
        findViewById(R.id.btnBackspace).setOnLongClickListener(v -> {
            handleCE();
            return true;
        });
    }

    // Display styling methods
    private void setRuntimeDisplayStyle() {
        // During typing: Expression bold, Result dim
        tvExpression.setTextSize(24); // Bigger
        tvExpression.setTextColor(getResources().getColor(android.R.color.black)); // Bold/Dark
        tvResult.setTextSize(32); // Smaller than final result
        tvResult.setTextColor(getResources().getColor(android.R.color.darker_gray)); // Dim
    }

    private void setFinalResultStyle() {
        // After equals: Expression dim, Result bold
        tvExpression.setTextSize(20); // Smaller
        tvExpression.setTextColor(getResources().getColor(android.R.color.darker_gray)); // Dim
        tvResult.setTextSize(42); // Bigger
        tvResult.setTextColor(getResources().getColor(android.R.color.black)); // Bold/Dark
    }

    // Operator selection visual feedback methods
    private void clearOperatorSelection() {
        if (selectedOperatorButton != null) {
            selectedOperatorButton.setSelected(false);
            selectedOperatorButton = null;
        }
    }

    private void selectOperatorButton(Button button) {
        clearOperatorSelection();
        selectedOperatorButton = button;
        selectedOperatorButton.setSelected(true);
    }

    // Error state management
    private boolean isErrorState() {
        return errorState;
    }

    private void setError(String message) {
        errorState = true;
        tvResult.setText(message);
        tvExpression.setText("");
        setRuntimeDisplayStyle(); // Reset to runtime style
    }

    private void clearErrorState() {
        errorState = false;
    }

    // Core input handling methods
    private void handleNumberInput(String number) {
        if (isErrorState()) {
            handleAC();
        }

        if (justCalculated) {
            // Start new expression after calculation
            expression = "";
            justCalculated = false;
            clearOperatorSelection();
            setRuntimeDisplayStyle(); // Switch back to runtime style
        }

        expression += number;
        updateDisplay();
        calculateLiveResult();

        // Make sure we're in runtime display mode
        if (justCalculated) {
            setRuntimeDisplayStyle();
        }
    }

    private void handleDecimalInput() {
        if (isErrorState()) {
            handleAC();
        }

        if (justCalculated) {
            expression = "0";
            justCalculated = false;
            clearOperatorSelection();
            setRuntimeDisplayStyle();
        }

        // Check if current number already has decimal
        String[] parts = expression.split("[+\\-×÷]");
        if (parts.length > 0) {
            String currentNumber = parts[parts.length - 1];
            if (!currentNumber.contains(".")) {
                if (expression.isEmpty() || isLastCharOperator()) {
                    expression += "0.";
                } else {
                    expression += ".";
                }
                updateDisplay();
            }
        }
    }

    private void handleOperatorInput(String operator) {
        if (isErrorState()) return;

        if (justCalculated) {
            // Continue calculation with last result
            expression = lastResult;
            justCalculated = false;
            setRuntimeDisplayStyle(); // Switch back to runtime style
        }

        if (expression.isEmpty()) {
            if (lastResult.equals("0")) return;
            expression = lastResult;
        }

        // Store current state for CE functionality
        if (!expression.isEmpty() && !isLastCharOperator()) {
            String[] parts = expression.split("[+\\-×÷]");
            if (parts.length >= 1) {
                storedFirstValue = parts[0];
                if (parts.length == 1) {
                    storedOperator = operator;
                }
            }
        }

        // Replace last operator if the last character is an operator
        if (isLastCharOperator()) {
            expression = expression.substring(0, expression.length() - 1);
        }

        expression += operator;
        updateDisplay();
        calculateLiveResult();
    }

    private void handleEqualsInput() {
        if (isErrorState() || expression.isEmpty()) return;

        if (isLastCharOperator()) {
            // Remove trailing operator
            expression = expression.substring(0, expression.length() - 1);
        }

        String result = evaluateExpression(expression);
        if (!isErrorState()) {
            lastResult = result;
            justCalculated = true;
            clearOperatorSelection();

            // Switch to final result styling
            setFinalResultStyle();

            tvResult.setText(result);
            updateDisplay();
        }
    }

    // Clear and backspace methods
    private void handleBackspace() {
        if (isErrorState()) {
            handleCE();
            return;
        }

        if (justCalculated) {
            handleAC();
            return;
        }

        if (!expression.isEmpty()) {
            expression = expression.substring(0, expression.length() - 1);
            updateDisplay();
            calculateLiveResult();
        }

        if (expression.isEmpty()) {
            tvResult.setText("0");
        }
    }

    // ✅ FIXED CE: Only clears current entry, keeps stored operation
    private void handleCE() {
        // CE: Clear Entry - only clears current input, keeps stored values
        if (!storedFirstValue.isEmpty() && !storedOperator.isEmpty()) {
            // If we have stored operation, restore it
            expression = storedFirstValue + storedOperator;
            updateDisplay();
        } else {
            // If no stored operation, just clear everything shown
            expression = "";
            tvResult.setText("0");
            updateDisplay();
        }

        justCalculated = false;
        clearErrorState();
        setRuntimeDisplayStyle();
    }

    // ✅ FIXED AC: Clears everything completely
    private void handleAC() {
        // AC: All Clear - completely reset everything
        expression = "";
        lastResult = "0";
        storedFirstValue = ""; // Clear stored values
        storedOperator = ""; // Clear stored operator
        justCalculated = false;
        clearOperatorSelection();
        clearErrorState();
        setRuntimeDisplayStyle();
        updateDisplay();
        tvResult.setText("0");
    }

    private void handlePlusMinus() {
        if (isErrorState()) return;

        // Toggle sign of current number or last result
        if (justCalculated) {
            if (!lastResult.equals("0")) {
                if (lastResult.startsWith("-")) {
                    lastResult = lastResult.substring(1);
                } else {
                    lastResult = "-" + lastResult;
                }
                tvResult.setText(lastResult);
            }
        } else {
            // Find the current number being entered
            String[] parts = expression.split("([+\\-×÷])");
            if (parts.length > 0) {
                String currentNumber = parts[parts.length - 1];
                if (!currentNumber.isEmpty() && !currentNumber.equals("0")) {
                    String newNumber;
                    if (currentNumber.startsWith("-")) {
                        newNumber = currentNumber.substring(1);
                    } else {
                        newNumber = "-" + currentNumber;
                    }

                    // Replace the current number in expression
                    int lastNumberStart = expression.lastIndexOf(currentNumber);
                    expression = expression.substring(0, lastNumberStart) + newNumber;
                    updateDisplay();
                    calculateLiveResult();
                }
            }
        }
    }

    private void handlePercentage() {
        if (isErrorState()) return;

        if (justCalculated) {
            try {
                double value = Double.parseDouble(lastResult);
                double result = value / 100.0;
                lastResult = formatResult(result);
                tvResult.setText(lastResult);
            } catch (NumberFormatException e) {
                setError("Error");
            }
        } else {
            // Apply percentage to current number
            String[] parts = expression.split("([+\\-×÷])");
            if (parts.length > 0) {
                String currentNumber = parts[parts.length - 1];
                if (!currentNumber.isEmpty()) {
                    try {
                        double value = Double.parseDouble(currentNumber);
                        double result = value / 100.0;
                        String newNumber = formatResult(result);

                        // Replace current number with percentage
                        int lastNumberStart = expression.lastIndexOf(currentNumber);
                        expression = expression.substring(0, lastNumberStart) + newNumber;
                        updateDisplay();
                        calculateLiveResult();
                    } catch (NumberFormatException e) {
                        setError("Error");
                    }
                }
            }
        }
    }

    // Utility methods
    private void updateDisplay() {
        tvExpression.setText(expression.isEmpty() ? "" : expression);
    }

    private void calculateLiveResult() {
        if (expression.isEmpty() || isLastCharOperator()) return;

        String result = evaluateExpression(expression);
        if (!isErrorState() && !justCalculated) {
            tvResult.setText(result);
        }
    }

    private String evaluateExpression(String expr) {
        try {
            // Replace display operators with calculation operators
            String calcExpr = expr.replace("×", "*").replace("÷", "/");

            // Simple expression evaluator
            double result = evaluateSimpleExpression(calcExpr);
            return formatResult(result);
        } catch (Exception e) {
            setError("Error");
            return "Error";
        }
    }

    private double evaluateSimpleExpression(String expression) throws Exception {
        // Simple recursive descent parser for basic arithmetic
        return parseExpression(expression.replace(" ", ""), new int[]{0});
    }

    private double parseExpression(String expr, int[] pos) throws Exception {
        double result = parseTerm(expr, pos);

        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op != '+' && op != '-') break;

            pos[0]++;
            double term = parseTerm(expr, pos);
            if (op == '+') {
                result += term;
            } else {
                result -= term;
            }
        }

        return result;
    }

    private double parseTerm(String expr, int[] pos) throws Exception {
        double result = parseFactor(expr, pos);

        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op != '*' && op != '/') break;

            pos[0]++;
            double factor = parseFactor(expr, pos);
            if (op == '*') {
                result *= factor;
            } else {
                if (factor == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                result /= factor;
            }
        }

        return result;
    }

    private double parseFactor(String expr, int[] pos) throws Exception {
        double result;
        boolean negative = false;

        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '-') {
            negative = true;
            pos[0]++;
        }

        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {
            pos[0]++;
            result = parseExpression(expr, pos);
            pos[0]++; // Skip ')'
        } else {
            StringBuilder sb = new StringBuilder();
            while (pos[0] < expr.length() && (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) {
                sb.append(expr.charAt(pos[0]));
                pos[0]++;
            }
            result = Double.parseDouble(sb.toString());
        }

        return negative ? -result : result;
    }

    private boolean isLastCharOperator() {
        if (expression.isEmpty()) return false;
        char lastChar = expression.charAt(expression.length() - 1);
        return lastChar == '+' || lastChar == '-' || lastChar == '×' || lastChar == '÷';
    }

    private String formatResult(double result) {
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            setError("Error");
            return "Error";
        }

        BigDecimal bd = new BigDecimal(result);
        bd = bd.stripTrailingZeros();
        String str = bd.toPlainString();

        if (str.length() > 12) {
            str = String.format(Locale.US, "%.8g", result);
        }

        return str;
    }
}
