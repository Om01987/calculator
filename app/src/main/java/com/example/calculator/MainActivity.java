package com.example.calculator;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.math.BigDecimal;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvExpression, tvResult;
    private Button btnAdd, btnSubtract, btnMultiply, btnDivide;
    private Button selectedOperatorButton = null;
    private Button btnToggleMode;
    private LinearLayout layoutAdvanced1, layoutAdvanced2;

    // Calculator state variables
    private String expression = "";
    private String lastResult = "0";
    private String storedFirstValue = "";
    private String storedOperator = "";
    private boolean justCalculated = false;
    private boolean errorState = false;
    private boolean isAdvancedMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        initializeViews();

        // Set initial display style
        setRuntimeDisplayStyle();

        // Set up button listeners
        setupNumberButtons();
        setupOperatorButtons();
        setupFunctionButtons();
        setupAdvancedButtons();
        setupToggleButton();
    }

    private void initializeViews() {
        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);
        btnAdd = findViewById(R.id.btnAdd);
        btnSubtract = findViewById(R.id.btnSubtract);
        btnMultiply = findViewById(R.id.btnMultiply);
        btnDivide = findViewById(R.id.btnDivide);
        btnToggleMode = findViewById(R.id.btnToggleMode);
        layoutAdvanced1 = findViewById(R.id.layoutAdvanced1);
        layoutAdvanced2 = findViewById(R.id.layoutAdvanced2);
    }

    private void setupToggleButton() {
        btnToggleMode.setOnClickListener(v -> toggleCalculatorMode());
    }

    private void toggleCalculatorMode() {
        isAdvancedMode = !isAdvancedMode;

        if (isAdvancedMode) {
            // Show advanced functions
            layoutAdvanced1.setVisibility(View.VISIBLE);
            layoutAdvanced2.setVisibility(View.VISIBLE);
            btnToggleMode.setText("BASIC");
            btnToggleMode.setSelected(true);
        } else {
            // Hide advanced functions
            layoutAdvanced1.setVisibility(View.GONE);
            layoutAdvanced2.setVisibility(View.GONE);
            btnToggleMode.setText("ADV");
            btnToggleMode.setSelected(false);
        }
    }

    private void setupAdvancedButtons() {
        // Trigonometric functions
        findViewById(R.id.btnSin).setOnClickListener(v -> handleAdvancedFunction("sin"));
        findViewById(R.id.btnCos).setOnClickListener(v -> handleAdvancedFunction("cos"));
        findViewById(R.id.btnTan).setOnClickListener(v -> handleAdvancedFunction("tan"));
        findViewById(R.id.btnLog).setOnClickListener(v -> handleAdvancedFunction("log"));
        findViewById(R.id.btnLn).setOnClickListener(v -> handleAdvancedFunction("ln"));

        // Power and root functions
        findViewById(R.id.btnSqrt).setOnClickListener(v -> handleAdvancedFunction("√"));
        findViewById(R.id.btnPower).setOnClickListener(v -> handleAdvancedFunction("²"));
        findViewById(R.id.btnPowerY).setOnClickListener(v -> handleAdvancedFunction("^"));

        // Constants
        findViewById(R.id.btnPi).setOnClickListener(v -> handleConstant("π"));
        findViewById(R.id.btnE).setOnClickListener(v -> handleConstant("e"));
    }

    private void handleAdvancedFunction(String function) {
        if (isErrorState()) return;

        if (justCalculated) {
            expression = lastResult;
            justCalculated = false;
            setRuntimeDisplayStyle();
        }

        switch (function) {
            case "sin":
            case "cos":
            case "tan":
            case "log":
            case "ln":
            case "√":
                // Functions that take the current number as input
                expression += function + "(";
                break;
            case "²":
                // Square current number
                if (!expression.isEmpty() && !isLastCharOperator()) {
                    expression += "²";
                }
                break;
            case "^":
                // Power operator
                if (!expression.isEmpty() && !isLastCharOperator()) {
                    expression += "^";
                }
                break;
        }

        updateDisplay();
        calculateLiveResult();
    }

    private void handleConstant(String constant) {
        if (isErrorState()) {
            handleAC();
        }

        if (justCalculated) {
            expression = "";
            justCalculated = false;
            clearOperatorSelection();
            setRuntimeDisplayStyle();
        }

        String value = "";
        switch (constant) {
            case "π":
                value = String.valueOf(Math.PI);
                break;
            case "e":
                value = String.valueOf(Math.E);
                break;
        }

        expression += value;
        updateDisplay();
        calculateLiveResult();
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
        tvExpression.setTextSize(24);
        tvExpression.setTextColor(getResources().getColor(android.R.color.white));
        tvResult.setTextSize(32);
        tvResult.setTextColor(getResources().getColor(android.R.color.darker_gray));
    }

    private void setFinalResultStyle() {
        // After equals: Expression dim, Result bold
        tvExpression.setTextSize(20);
        tvExpression.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvResult.setTextSize(42);
        tvResult.setTextColor(getResources().getColor(android.R.color.white));
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
        setRuntimeDisplayStyle();
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
            expression = "";
            justCalculated = false;
            clearOperatorSelection();
            setRuntimeDisplayStyle();
        }

        expression += number;
        updateDisplay();
        calculateLiveResult();
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
        String[] parts = expression.split("[+\\-×÷^]");
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
            expression = lastResult;
            justCalculated = false;
            setRuntimeDisplayStyle();
        }

        if (expression.isEmpty()) {
            if (lastResult.equals("0")) return;
            expression = lastResult;
        }

        // Store current state for CE functionality
        if (!expression.isEmpty() && !isLastCharOperator()) {
            String[] parts = expression.split("[+\\-×÷^]");
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
            expression = expression.substring(0, expression.length() - 1);
        }

        String result = evaluateExpression(expression);
        if (!isErrorState()) {
            lastResult = result;
            justCalculated = true;
            clearOperatorSelection();

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

    private void handleCE() {
        if (!storedFirstValue.isEmpty() && !storedOperator.isEmpty()) {
            expression = storedFirstValue + storedOperator;
            updateDisplay();
        } else {
            expression = "";
            tvResult.setText("0");
            updateDisplay();
        }

        justCalculated = false;
        clearErrorState();
        setRuntimeDisplayStyle();
    }

    private void handleAC() {
        expression = "";
        lastResult = "0";
        storedFirstValue = "";
        storedOperator = "";
        justCalculated = false;
        clearOperatorSelection();
        clearErrorState();
        setRuntimeDisplayStyle();
        updateDisplay();
        tvResult.setText("0");
    }

    private void handlePlusMinus() {
        if (isErrorState()) return;

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
            String[] parts = expression.split("([+\\-×÷^])");
            if (parts.length > 0) {
                String currentNumber = parts[parts.length - 1];
                if (!currentNumber.isEmpty() && !currentNumber.equals("0")) {
                    String newNumber;
                    if (currentNumber.startsWith("-")) {
                        newNumber = currentNumber.substring(1);
                    } else {
                        newNumber = "-" + currentNumber;
                    }

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
            String[] parts = expression.split("([+\\-×÷^])");
            if (parts.length > 0) {
                String currentNumber = parts[parts.length - 1];
                if (!currentNumber.isEmpty()) {
                    try {
                        double value = Double.parseDouble(currentNumber);
                        double result = value / 100.0;
                        String newNumber = formatResult(result);

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
            String calcExpr = expr.replace("×", "*")
                    .replace("÷", "/")
                    .replace("²", "^2")
                    .replace("π", String.valueOf(Math.PI))
                    .replace("e", String.valueOf(Math.E));

            // Handle scientific functions (simplified for basic functionality)
            calcExpr = handleScientificFunctions(calcExpr);

            double result = evaluateAdvancedExpression(calcExpr);
            return formatResult(result);
        } catch (Exception e) {
            setError("Error");
            return "Error";
        }
    }

    private String handleScientificFunctions(String expr) {
        // Simplified scientific function handling
        // In a full implementation, you'd need more sophisticated parsing
        return expr;
    }

    private double evaluateAdvancedExpression(String expression) throws Exception {
        return parseExpression(expression.replace(" ", ""), new int[]{0});
    }

    private double parseExpression(String expr, int[] pos) throws Exception {
        double result = parseTerm(expr, pos);

        while (pos[0] < expr.length()) {  // ✅ FIXED: pos[0] instead of pos
            char op = expr.charAt(pos[0]);  // ✅ FIXED: pos[0] instead of pos
            if (op != '+' && op != '-') break;

            pos[0]++;  // ✅ FIXED: pos[0]++ instead of pos++
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
        double result = parsePower(expr, pos);

        while (pos[0] < expr.length()) {  // ✅ FIXED: pos[0] instead of pos
            char op = expr.charAt(pos[0]);  // ✅ FIXED: pos[0] instead of pos
            if (op != '*' && op != '/') break;

            pos[0]++;  // ✅ FIXED: pos[0]++ instead of pos++
            double factor = parsePower(expr, pos);
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

    private double parsePower(String expr, int[] pos) throws Exception {
        double result = parseFactor(expr, pos);

        while (pos[0] < expr.length() && expr.charAt(pos[0]) == '^') {  // ✅ FIXED: pos[0] instead of pos
            pos[0]++;  // ✅ FIXED: pos[0]++ instead of pos++
            double exponent = parseFactor(expr, pos);
            result = Math.pow(result, exponent);
        }

        return result;
    }

    private double parseFactor(String expr, int[] pos) throws Exception {
        double result;
        boolean negative = false;

        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '-') {  // ✅ FIXED: pos[0] instead of pos
            negative = true;
            pos[0]++;  // ✅ FIXED: pos[0]++ instead of pos++
        }

        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {  // ✅ FIXED: pos[0] instead of pos
            pos[0]++;  // ✅ FIXED: pos[0]++ instead of pos++
            result = parseExpression(expr, pos);
            pos[0]++;  // Skip ')' - ✅ FIXED: pos[0]++ instead of pos++
        } else {
            StringBuilder sb = new StringBuilder();
            while (pos[0] < expr.length() && (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) {  // ✅ FIXED: pos[0] instead of pos
                sb.append(expr.charAt(pos[0]));  // ✅ FIXED: pos[0] instead of pos
                pos[0]++;  // ✅ FIXED: pos[0]++ instead of pos++
            }
            if (sb.length() == 0) {
                throw new Exception("Invalid expression");
            }
            result = Double.parseDouble(sb.toString());
        }

        return negative ? -result : result;
    }

// … (rest of your MainActivity.java below remains unchanged)


    private boolean isLastCharOperator() {
        if (expression.isEmpty()) return false;
        char lastChar = expression.charAt(expression.length() - 1);
        return lastChar == '+' || lastChar == '-' || lastChar == '×' || lastChar == '÷' || lastChar == '^';
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
