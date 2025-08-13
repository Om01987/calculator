package com.example.calculator;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ValidationResult;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView tvExpression, tvResult;
    private Button btnToggleMode;
    private LinearLayout layoutAdvanced1, layoutAdvanced2, layoutAdvanced3;

    private boolean isAdvancedMode = false;
    private boolean errorState = false;
    private boolean justCalculated = false;
    private boolean isDegreeMode = true; // Default to degrees for user-friendliness

    // Enhanced error handling
    private String lastError = "";

    // Memory storage for advanced functionality
    private double memoryValue = 0.0;

    // Patterns for input validation
    private static final Pattern CONSECUTIVE_OPERATORS = Pattern.compile("[+\\-×÷\\^]{2,}");
    private static final Pattern INVALID_DECIMAL = Pattern.compile("\\d*\\.\\d*\\.\\d*");
    private static final Pattern EMPTY_PARENTHESES = Pattern.compile("\\(\\s*\\)");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);
        btnToggleMode = findViewById(R.id.btnToggleMode);
        layoutAdvanced1 = findViewById(R.id.layoutAdvanced1);
        layoutAdvanced2 = findViewById(R.id.layoutAdvanced2);

        // Handle third advanced layout if exists
        try {
            layoutAdvanced3 = findViewById(R.id.layoutAdvanced3);
        } catch (Exception ignored) {
            layoutAdvanced3 = null;
        }
    }

    private void setupListeners() {
        // Toggle Mode
        btnToggleMode.setOnClickListener(v -> toggleCalculatorMode());

        // Number buttons
        int[] numberButtonIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        for (int id : numberButtonIds) {
            findViewById(id).setOnClickListener(view -> onNumberClick(((Button) view).getText().toString()));
        }

        // Basic operator buttons
        int[] basicOperatorIds = {R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide};
        for (int id : basicOperatorIds) {
            findViewById(id).setOnClickListener(view -> onOperatorClick(((Button) view).getText().toString()));
        }

        // Advanced function buttons
        findViewById(R.id.btnSin).setOnClickListener(v -> onTrigFunction("sin"));
        findViewById(R.id.btnCos).setOnClickListener(v -> onTrigFunction("cos"));
        findViewById(R.id.btnTan).setOnClickListener(v -> onTrigFunction("tan"));
        findViewById(R.id.btnLog).setOnClickListener(v -> onFunctionClick("log10"));
        findViewById(R.id.btnLn).setOnClickListener(v -> onFunctionClick("log"));
        findViewById(R.id.btnSqrt).setOnClickListener(v -> onFunctionClick("sqrt"));

        // Special buttons
        findViewById(R.id.btnDecimal).setOnClickListener(v -> onDecimalClick());
        findViewById(R.id.btnLeftParen).setOnClickListener(v -> onAppendText("("));
        findViewById(R.id.btnRightParen).setOnClickListener(v -> onAppendText(")"));
        findViewById(R.id.btnPower).setOnClickListener(v -> onOperatorClick("^"));
        findViewById(R.id.btnPi).setOnClickListener(v -> onConstantClick("π"));

        // Advanced function buttons with error handling
        setupAdvancedButtons();

        // Control buttons
        findViewById(R.id.btnAC).setOnClickListener(v -> clearAll());
        findViewById(R.id.btnBackspace).setOnClickListener(v -> deleteLast());
        findViewById(R.id.btnPercent).setOnClickListener(v -> applyPercentage());
        findViewById(R.id.btnEquals).setOnClickListener(v -> showFinalResult());

        // Live calculation listener with debouncing
        tvExpression.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                // Debounce live calculation to improve performance
                tvExpression.removeCallbacks(liveCalculationRunnable);
                tvExpression.postDelayed(liveCalculationRunnable, 150);
            }
        });
    }

    private void setupAdvancedButtons() {
        // Factorial button
        try {
            findViewById(R.id.btnFactorial).setOnClickListener(v -> onFactorialClick());
        } catch (Exception ignored) {}

        // Euler's number constant
        try {
            findViewById(R.id.btnE).setOnClickListener(v -> onConstantClick("e"));
        } catch (Exception ignored) {}

        // Exponential function
        try {
            findViewById(R.id.btnExp).setOnClickListener(v -> onFunctionClick("exp"));
        } catch (Exception ignored) {}

        // Modulo operation
        try {
            findViewById(R.id.btnMod).setOnClickListener(v -> onOperatorClick("mod"));
        } catch (Exception ignored) {}

        // Absolute value function
        try {
            findViewById(R.id.btnAbs).setOnClickListener(v -> onFunctionClick("abs"));
        } catch (Exception ignored) {}
    }

    private final Runnable liveCalculationRunnable = this::calculateLiveResult;

    private void onNumberClick(String number) {
        if (errorState) {
            clearAll();
        }

        if (justCalculated) {
            tvExpression.setText(""); // Start fresh for new numbers
            justCalculated = false;
        }

        // Prevent leading zeros (except for decimal numbers)
        String currentExpr = tvExpression.getText().toString();
        if (number.equals("0") && !currentExpr.isEmpty()) {
            String[] parts = currentExpr.split("[+\\-×÷\\^\\(\\)\\s]");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                if (lastPart.equals("0") && !lastPart.contains(".")) {
                    return; // Don't add leading zero
                }
            }
        }

        tvExpression.append(number);
    }

    private void onOperatorClick(String operator) {
        if (errorState) {
            clearAll();
        }

        String currentExpression = tvExpression.getText().toString();

        // If just calculated, continue with the result
        if (justCalculated) {
            justCalculated = false;
        }

        // Handle empty expression
        if (currentExpression.isEmpty()) {
            if (operator.equals("-")) {
                tvExpression.append(operator); // Allow negative numbers
            }
            return;
        }

        // Prevent consecutive operators (except for negative signs)
        char lastChar = currentExpression.charAt(currentExpression.length() - 1);
        if ("+-×÷^".contains(String.valueOf(lastChar))) {
            if (operator.equals("-") && !"+-".contains(String.valueOf(lastChar))) {
                tvExpression.append(operator); // Allow negative after operators
            } else {
                // Replace the last operator
                tvExpression.setText(currentExpression.substring(0, currentExpression.length() - 1) + operator);
            }
        } else {
            tvExpression.append(operator);
        }
    }

    private void onTrigFunction(String function) {
        if (errorState) {
            clearAll();
        }

        if (justCalculated) {
            tvExpression.setText(""); // Start fresh
            justCalculated = false;
        }

        // Add degree conversion if in degree mode
        if (isDegreeMode) {
            tvExpression.append(function + "(");
        } else {
            tvExpression.append(function + "(");
        }
    }

    private void onFunctionClick(String function) {
        if (errorState) {
            clearAll();
        }

        if (justCalculated) {
            tvExpression.setText(""); // Start fresh
            justCalculated = false;
        }

        tvExpression.append(function + "(");
    }

    private void onDecimalClick() {
        if (errorState) {
            clearAll();
        }

        String currentExpression = tvExpression.getText().toString();

        // Check if the current number already has a decimal point
        String[] parts = currentExpression.split("[+\\-×÷\\^\\(\\)\\s]");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1].trim();
            if (!lastPart.contains(".")) {
                if (lastPart.isEmpty() || isOperator(lastPart.charAt(lastPart.length() - 1))) {
                    tvExpression.append("0.");
                } else {
                    tvExpression.append(".");
                }
            }
        } else {
            tvExpression.append("0.");
        }
    }

    private boolean isOperator(char c) {
        return "+-×÷^()".contains(String.valueOf(c));
    }

    private void onConstantClick(String constant) {
        if (errorState) {
            clearAll();
        }

        if (justCalculated) {
            tvExpression.setText("");
            justCalculated = false;
        }

        // Add multiplication if needed (e.g., "5π" should become "5*π")
        String currentExpr = tvExpression.getText().toString();
        if (!currentExpr.isEmpty()) {
            char lastChar = currentExpr.charAt(currentExpr.length() - 1);
            if (Character.isDigit(lastChar) || lastChar == ')') {
                tvExpression.append("×" + constant);
                return;
            }
        }

        tvExpression.append(constant);
    }

    private void onFactorialClick() {
        if (errorState) {
            clearAll();
        }

        String currentExpression = tvExpression.getText().toString();
        if (!currentExpression.isEmpty()) {
            char lastChar = currentExpression.charAt(currentExpression.length() - 1);
            if (Character.isDigit(lastChar) || lastChar == ')') {
                tvExpression.append("!");
            }
        }
    }

    private void onAppendText(String text) {
        if (errorState) {
            clearAll();
        }

        if (justCalculated && text.equals("(")) {
            tvExpression.setText("");
            justCalculated = false;
        }

        // Smart parentheses handling
        if (text.equals(")")) {
            String currentExpr = tvExpression.getText().toString();
            long openParens = currentExpr.chars().filter(ch -> ch == '(').count();
            long closeParens = currentExpr.chars().filter(ch -> ch == ')').count();

            if (closeParens >= openParens) {
                return; // Don't add extra closing parentheses
            }
        }

        tvExpression.append(text);
    }

    private void toggleCalculatorMode() {
        isAdvancedMode = !isAdvancedMode;
        layoutAdvanced1.setVisibility(isAdvancedMode ? View.VISIBLE : View.GONE);
        layoutAdvanced2.setVisibility(isAdvancedMode ? View.VISIBLE : View.GONE);

        // Handle third layout if exists
        if (layoutAdvanced3 != null) {
            layoutAdvanced3.setVisibility(isAdvancedMode ? View.VISIBLE : View.GONE);
        }

        btnToggleMode.setText(isAdvancedMode ? "BASIC" : "ADV");
    }

    private void clearAll() {
        tvExpression.setText("");
        tvResult.setText("0");
        errorState = false;
        justCalculated = false;
        lastError = "";
    }

    private void deleteLast() {
        String currentExpression = tvExpression.getText().toString();
        if (!currentExpression.isEmpty()) {
            // Handle multi-character functions and constants
            if (currentExpression.endsWith("sin(") || currentExpression.endsWith("cos(") ||
                    currentExpression.endsWith("tan(") || currentExpression.endsWith("log(") ||
                    currentExpression.endsWith("exp(") || currentExpression.endsWith("abs(")) {
                tvExpression.setText(currentExpression.substring(0, currentExpression.length() - 4));
            } else if (currentExpression.endsWith("sqrt(") || currentExpression.endsWith("log10(")) {
                tvExpression.setText(currentExpression.substring(0, currentExpression.length() - 5));
            } else if (currentExpression.endsWith("mod")) {
                tvExpression.setText(currentExpression.substring(0, currentExpression.length() - 3));
            } else {
                tvExpression.setText(currentExpression.substring(0, currentExpression.length() - 1));
            }
        }
        errorState = false;
    }

    private void applyPercentage() {
        String expression = tvExpression.getText().toString();
        if (!expression.isEmpty() && !errorState) {
            try {
                // Get the last number in the expression
                String[] parts = expression.split("[+\\-×÷^()]");
                if (parts.length > 0) {
                    String lastNumber = parts[parts.length - 1].trim();
                    if (!lastNumber.isEmpty() && isNumeric(lastNumber)) {
                        double value = Double.parseDouble(lastNumber);
                        double percentValue = value / 100;

                        // Replace the last number with its percentage
                        String newExpression = expression.substring(0, expression.lastIndexOf(lastNumber)) + percentValue;
                        tvExpression.setText(newExpression);
                    }
                }
            } catch (Exception e) {
                tvResult.setText("Error: Invalid for %");
                errorState = true;
            }
        }
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showFinalResult() {
        try {
            String expression = tvExpression.getText().toString();
            if (expression.isEmpty()) return;

            double result = evaluateExpression(expression);
            String formattedResult = formatResult(result);

            tvExpression.setText(formattedResult);
            tvResult.setText("");
            justCalculated = true;
            errorState = false;
            lastError = "";
        } catch (ArithmeticException e) {
            tvResult.setText("Error: " + e.getMessage());
            errorState = true;
            lastError = e.getMessage();
        } catch (Exception e) {
            tvResult.setText("Error: Invalid expression");
            errorState = true;
            lastError = "Invalid expression";
        }
    }

    private void calculateLiveResult() {
        if (justCalculated || errorState) return;

        String expressionStr = tvExpression.getText().toString();
        if (expressionStr.isEmpty()) {
            tvResult.setText("");
            return;
        }

        try {
            // Only calculate if expression seems complete
            if (isExpressionCalculable(expressionStr)) {
                double result = evaluateExpression(expressionStr);
                tvResult.setText("= " + formatResult(result));
            } else {
                tvResult.setText("");
            }
        } catch (Exception e) {
            tvResult.setText("");
        }
    }

    private boolean isExpressionCalculable(String expression) {
        if (expression.isEmpty()) return false;

        // Check for unmatched parentheses
        long openParens = expression.chars().filter(ch -> ch == '(').count();
        long closeParens = expression.chars().filter(ch -> ch == ')').count();
        if (openParens != closeParens) return false;

        // Don't calculate if expression ends with an operator or open parenthesis
        char lastChar = expression.charAt(expression.length() - 1);
        return !"+-×÷^(".contains(String.valueOf(lastChar));
    }

    private double evaluateExpression(String expressionStr) throws ArithmeticException {
        // Input validation
        if (CONSECUTIVE_OPERATORS.matcher(expressionStr).find()) {
            throw new ArithmeticException("Invalid operator sequence");
        }

        if (INVALID_DECIMAL.matcher(expressionStr).find()) {
            throw new ArithmeticException("Invalid decimal format");
        }

        if (EMPTY_PARENTHESES.matcher(expressionStr).find()) {
            throw new ArithmeticException("Empty parentheses");
        }

        // Prepare the string for evaluation
        String parsableStr = prepareExpression(expressionStr);

        try {
            Expression expression = new ExpressionBuilder(parsableStr).build();

            // Validate expression
            ValidationResult validation = expression.validate();
            if (!validation.isValid()) {
                throw new ArithmeticException("Invalid expression");
            }

            double result = expression.evaluate();

            // Check for mathematical errors
            if (Double.isNaN(result)) {
                throw new ArithmeticException("Result is undefined");
            }
            if (Double.isInfinite(result)) {
                throw new ArithmeticException("Division by zero");
            }

            return result;

        } catch (Exception e) {
            if (e instanceof ArithmeticException) {
                throw e;
            }
            throw new ArithmeticException("Calculation error");
        }
    }

    private String prepareExpression(String expressionStr) {
        String result = expressionStr
                .replace('×', '*')
                .replace('÷', '/')
                .replace('−', '-') // Handle proper minus sign
                .replace("π", String.valueOf(Math.PI))
                .replace("e", String.valueOf(Math.E))
                .replace("mod", "%"); // Handle modulo operation

        // Handle factorial
        result = handleFactorial(result);

        // Handle trigonometric functions with degree conversion
        result = handleTrigFunctions(result);

        return result;
    }

    private String handleTrigFunctions(String expression) {
        if (!isDegreeMode) return expression;

        // Convert degrees to radians for trig functions
        expression = expression.replaceAll("sin\\(([^)]+)\\)", "sin(($1)*" + Math.PI + "/180)");
        expression = expression.replaceAll("cos\\(([^)]+)\\)", "cos(($1)*" + Math.PI + "/180)");
        expression = expression.replaceAll("tan\\(([^)]+)\\)", "tan(($1)*" + Math.PI + "/180)");

        return expression;
    }

    private String handleFactorial(String expression) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            if (expression.charAt(i) == '!') {
                // Find the number before the factorial
                int j = i - 1;
                while (j >= 0 && (Character.isDigit(expression.charAt(j)) || expression.charAt(j) == '.')) {
                    j--;
                }

                String numberStr = expression.substring(j + 1, i);
                if (!numberStr.isEmpty()) {
                    try {
                        int number = (int) Double.parseDouble(numberStr);
                        long factorial = calculateFactorial(number);

                        // Replace the number and ! with the factorial result
                        result.delete(result.length() - numberStr.length(), result.length());
                        result.append(factorial);
                    } catch (Exception e) {
                        throw new ArithmeticException("Invalid factorial");
                    }
                } else {
                    result.append(expression.charAt(i));
                }
            } else {
                result.append(expression.charAt(i));
            }
        }
        return result.toString();
    }

    private long calculateFactorial(int n) {
        if (n < 0) throw new ArithmeticException("Factorial of negative number");
        if (n > 20) throw new ArithmeticException("Factorial too large (max 20!)");

        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    private String formatResult(double result) {
        // Handle very large and very small numbers with scientific notation
        if (Math.abs(result) >= 1e10 || (Math.abs(result) < 1e-4 && result != 0)) {
            return String.format(Locale.US, "%.6E", result);
        }

        DecimalFormat df = new DecimalFormat("#.##########");
        df.setMaximumFractionDigits(10);

        // For whole numbers, don't show decimal
        if (result == Math.floor(result) && !Double.isInfinite(result)) {
            return String.format(Locale.US, "%.0f", result);
        } else {
            return df.format(result);
        }
    }
}
