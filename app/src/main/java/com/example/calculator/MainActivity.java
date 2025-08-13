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

public class MainActivity extends AppCompatActivity {

    private TextView tvExpression, tvResult, tvModeIndicator;
    private Button btnToggleMode;
    private LinearLayout layoutAdvanced1, layoutAdvanced2, layoutAdvanced3;

    private boolean isAdvancedMode = false;
    private boolean errorState = false;
    private boolean justCalculated = false;
    private boolean isDegreeMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        setupListeners();
        updateModeIndicator();
    }

    private void initializeViews() {
        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);
        tvModeIndicator = findViewById(R.id.tvModeIndicator);
        btnToggleMode = findViewById(R.id.btnToggleMode);
        layoutAdvanced1 = findViewById(R.id.layoutAdvanced1);
        layoutAdvanced2 = findViewById(R.id.layoutAdvanced2);
        layoutAdvanced3 = findViewById(R.id.layoutAdvanced3);
    }

    private void setupListeners() {
        // Toggle Mode
        btnToggleMode.setOnClickListener(v -> toggleCalculatorMode());

        // Number buttons - ALWAYS WORK
        findViewById(R.id.btn0).setOnClickListener(v -> onNumberClick("0"));
        findViewById(R.id.btn1).setOnClickListener(v -> onNumberClick("1"));
        findViewById(R.id.btn2).setOnClickListener(v -> onNumberClick("2"));
        findViewById(R.id.btn3).setOnClickListener(v -> onNumberClick("3"));
        findViewById(R.id.btn4).setOnClickListener(v -> onNumberClick("4"));
        findViewById(R.id.btn5).setOnClickListener(v -> onNumberClick("5"));
        findViewById(R.id.btn6).setOnClickListener(v -> onNumberClick("6"));
        findViewById(R.id.btn7).setOnClickListener(v -> onNumberClick("7"));
        findViewById(R.id.btn8).setOnClickListener(v -> onNumberClick("8"));
        findViewById(R.id.btn9).setOnClickListener(v -> onNumberClick("9"));

        // Basic operators - ALWAYS WORK
        findViewById(R.id.btnAdd).setOnClickListener(v -> onOperatorClick("+"));
        findViewById(R.id.btnSubtract).setOnClickListener(v -> onOperatorClick("-"));
        findViewById(R.id.btnMultiply).setOnClickListener(v -> onOperatorClick("×"));
        findViewById(R.id.btnDivide).setOnClickListener(v -> onOperatorClick("÷"));

        // Basic functions - ALWAYS WORK
        findViewById(R.id.btnDecimal).setOnClickListener(v -> onDecimalClick());
        findViewById(R.id.btnAC).setOnClickListener(v -> clearAll());
        findViewById(R.id.btnBackspace).setOnClickListener(v -> deleteLast());
        findViewById(R.id.btnPercent).setOnClickListener(v -> applyPercentage());
        findViewById(R.id.btnEquals).setOnClickListener(v -> showFinalResult());

        // Advanced function buttons
        findViewById(R.id.btnSin).setOnClickListener(v -> onTrigFunction("sin"));
        findViewById(R.id.btnCos).setOnClickListener(v -> onTrigFunction("cos"));
        findViewById(R.id.btnTan).setOnClickListener(v -> onTrigFunction("tan"));
        findViewById(R.id.btnLog).setOnClickListener(v -> onFunctionClick("log10"));
        findViewById(R.id.btnLn).setOnClickListener(v -> onFunctionClick("log"));
        findViewById(R.id.btnSqrt).setOnClickListener(v -> onFunctionClick("sqrt"));
        findViewById(R.id.btnLeftParen).setOnClickListener(v -> onAppendText("("));
        findViewById(R.id.btnRightParen).setOnClickListener(v -> onAppendText(")"));
        findViewById(R.id.btnPower).setOnClickListener(v -> onOperatorClick("^"));
        findViewById(R.id.btnPi).setOnClickListener(v -> onConstantClick("π"));
        findViewById(R.id.btnFactorial).setOnClickListener(v -> onFactorialClick());
        findViewById(R.id.btnE).setOnClickListener(v -> onConstantClick("e"));
        findViewById(R.id.btnExp).setOnClickListener(v -> onFunctionClick("exp"));
        findViewById(R.id.btnMod).setOnClickListener(v -> onOperatorClick("mod"));
        findViewById(R.id.btnAbs).setOnClickListener(v -> onFunctionClick("abs"));

        // Live calculation
        tvExpression.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tvExpression.removeCallbacks(liveCalculationRunnable);
                tvExpression.postDelayed(liveCalculationRunnable, 150);
            }
        });
    }

    private final Runnable liveCalculationRunnable = this::calculateLiveResult;

    private void toggleCalculatorMode() {
        isAdvancedMode = !isAdvancedMode;

        // ONLY show/hide advanced rows - DON'T touch basic buttons
        layoutAdvanced1.setVisibility(isAdvancedMode ? View.VISIBLE : View.GONE);
        layoutAdvanced2.setVisibility(isAdvancedMode ? View.VISIBLE : View.GONE);
        layoutAdvanced3.setVisibility(isAdvancedMode ? View.VISIBLE : View.GONE);

        btnToggleMode.setText(isAdvancedMode ? "BASIC" : "ADV");
        updateModeIndicator();
    }

    private void updateModeIndicator() {
        String angleMode = isDegreeMode ? "DEG" : "RAD";
        String calcMode = isAdvancedMode ? "ADVANCED" : "BASIC";
        tvModeIndicator.setText(angleMode + " | " + calcMode);
    }

    private void onNumberClick(String number) {
        if (errorState) clearAll();
        if (justCalculated) {
            tvExpression.setText("");
            justCalculated = false;
        }
        tvExpression.append(number);
    }

    private void onOperatorClick(String operator) {
        if (errorState) clearAll();

        String currentExpression = tvExpression.getText().toString();
        if (justCalculated) justCalculated = false;

        if (currentExpression.isEmpty() && operator.equals("-")) {
            tvExpression.append(operator);
            return;
        }

        if (!currentExpression.isEmpty()) {
            char lastChar = currentExpression.charAt(currentExpression.length() - 1);
            if ("+-×÷^".contains(String.valueOf(lastChar))) {
                tvExpression.setText(currentExpression.substring(0, currentExpression.length() - 1) + operator);
            } else {
                tvExpression.append(operator);
            }
        }
    }

    private void onTrigFunction(String function) {
        if (errorState) clearAll();
        if (justCalculated) {
            tvExpression.setText("");
            justCalculated = false;
        }
        tvExpression.append(function + "(");
    }

    private void onFunctionClick(String function) {
        if (errorState) clearAll();
        if (justCalculated) {
            tvExpression.setText("");
            justCalculated = false;
        }
        tvExpression.append(function + "(");
    }

    private void onDecimalClick() {
        if (errorState) clearAll();

        String currentExpression = tvExpression.getText().toString();
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

    private void onConstantClick(String constant) {
        if (errorState) clearAll();
        if (justCalculated) {
            tvExpression.setText("");
            justCalculated = false;
        }

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
        if (errorState) clearAll();
        String currentExpression = tvExpression.getText().toString();
        if (!currentExpression.isEmpty()) {
            char lastChar = currentExpression.charAt(currentExpression.length() - 1);
            if (Character.isDigit(lastChar) || lastChar == ')') {
                tvExpression.append("!");
            }
        }
    }

    private void onAppendText(String text) {
        if (errorState) clearAll();
        if (justCalculated && text.equals("(")) {
            tvExpression.setText("");
            justCalculated = false;
        }
        tvExpression.append(text);
    }

    private boolean isOperator(char c) {
        return "+-×÷^()".contains(String.valueOf(c));
    }

    private void clearAll() {
        tvExpression.setText("");
        tvResult.setText("0");
        errorState = false;
        justCalculated = false;
    }

    private void deleteLast() {
        String currentExpression = tvExpression.getText().toString();
        if (!currentExpression.isEmpty()) {
            if (currentExpression.endsWith("sin(") || currentExpression.endsWith("cos(") ||
                    currentExpression.endsWith("tan(") || currentExpression.endsWith("log(") ||
                    currentExpression.endsWith("exp(") || currentExpression.endsWith("abs(")) {
                tvExpression.setText(currentExpression.substring(0, currentExpression.length() - 4));
            } else if (currentExpression.endsWith("sqrt(") || currentExpression.endsWith("log10(")) {
                tvExpression.setText(currentExpression.substring(0, currentExpression.length() - 5));
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
                String[] parts = expression.split("[+\\-×÷^()]");
                if (parts.length > 0) {
                    String lastNumber = parts[parts.length - 1].trim();
                    if (!lastNumber.isEmpty() && isNumeric(lastNumber)) {
                        double value = Double.parseDouble(lastNumber);
                        double percentValue = value / 100;
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
        } catch (Exception e) {
            tvResult.setText("Error: " + e.getMessage());
            errorState = true;
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
        char lastChar = expression.charAt(expression.length() - 1);
        return !"+-×÷^(".contains(String.valueOf(lastChar));
    }

    private double evaluateExpression(String expressionStr) throws ArithmeticException {
        String parsableStr = prepareExpression(expressionStr);

        try {
            Expression expression = new ExpressionBuilder(parsableStr).build();
            ValidationResult validation = expression.validate();
            if (!validation.isValid()) {
                throw new ArithmeticException("Invalid expression");
            }

            double result = expression.evaluate();

            if (Double.isNaN(result)) {
                throw new ArithmeticException("Result is undefined");
            }
            if (Double.isInfinite(result)) {
                throw new ArithmeticException("Division by zero");
            }

            return result;
        } catch (Exception e) {
            if (e instanceof ArithmeticException) throw e;
            throw new ArithmeticException("Calculation error");
        }
    }

    private String prepareExpression(String expressionStr) {
        String result = expressionStr
                .replace('×', '*')
                .replace('÷', '/')
                .replace('−', '-')
                .replace("π", String.valueOf(Math.PI))
                .replace("e", String.valueOf(Math.E))
                .replace("mod", "%");

        result = handleFactorial(result);
        result = handleTrigFunctions(result);

        return result;
    }

    private String handleTrigFunctions(String expression) {
        if (isDegreeMode) {
            expression = expression.replaceAll("sin\\(([^)]+)\\)", "sin(($1)*" + Math.PI + "/180)");
            expression = expression.replaceAll("cos\\(([^)]+)\\)", "cos(($1)*" + Math.PI + "/180)");
            expression = expression.replaceAll("tan\\(([^)]+)\\)", "tan(($1)*" + Math.PI + "/180)");
        }
        return expression;
    }

    private String handleFactorial(String expression) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            if (expression.charAt(i) == '!') {
                int j = i - 1;
                while (j >= 0 && (Character.isDigit(expression.charAt(j)) || expression.charAt(j) == '.')) {
                    j--;
                }

                String numberStr = expression.substring(j + 1, i);
                if (!numberStr.isEmpty()) {
                    try {
                        int number = (int) Double.parseDouble(numberStr);
                        long factorial = calculateFactorial(number);
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
        if (n > 20) throw new ArithmeticException("Factorial too large");

        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    private String formatResult(double result) {
        if (Math.abs(result) >= 1e10 || (Math.abs(result) < 1e-4 && result != 0)) {
            return String.format(Locale.US, "%.6E", result);
        }

        DecimalFormat df = new DecimalFormat("#.##########");
        df.setMaximumFractionDigits(10);

        if (result == Math.floor(result) && !Double.isInfinite(result)) {
            return String.format(Locale.US, "%.0f", result);
        } else {
            return df.format(result);
        }
    }
}
