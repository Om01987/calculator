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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView tvExpression, tvResult, tvModeIndicator;
    private Button btnToggleMode, btnDegRad;
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
        btnDegRad = findViewById(R.id.btnDegRad);
        layoutAdvanced1 = findViewById(R.id.layoutAdvanced1);
        layoutAdvanced2 = findViewById(R.id.layoutAdvanced2);
        layoutAdvanced3 = findViewById(R.id.layoutAdvanced3);
    }

    private void setupListeners() {
        // Toggle Advanced Mode
        btnToggleMode.setOnClickListener(v -> toggleCalculatorMode());
        // Toggle DEG/RAD
        btnDegRad.setOnClickListener(v -> toggleDegreeMode());

        // Number buttons
        int[] nums = {R.id.btn0,R.id.btn1,R.id.btn2,R.id.btn3,R.id.btn4,R.id.btn5,R.id.btn6,R.id.btn7,R.id.btn8,R.id.btn9};
        for (int id: nums) findViewById(id).setOnClickListener(v -> onNumberClick(((Button)v).getText().toString()));

        // Basic operators
        findViewById(R.id.btnAdd).setOnClickListener(v -> onOperatorClick("+"));
        findViewById(R.id.btnSubtract).setOnClickListener(v -> onOperatorClick("-"));
        findViewById(R.id.btnMultiply).setOnClickListener(v -> onOperatorClick("×"));
        findViewById(R.id.btnDivide).setOnClickListener(v -> onOperatorClick("÷"));

        // Basic functions
        findViewById(R.id.btnDecimal).setOnClickListener(v -> onDecimalClick());
        findViewById(R.id.btnAC).setOnClickListener(v -> clearAll());
        findViewById(R.id.btnBackspace).setOnClickListener(v -> deleteLast());
        findViewById(R.id.btnPercent).setOnClickListener(v -> applySmartPercentage());
        findViewById(R.id.btnEquals).setOnClickListener(v -> showFinalResult());

        // Advanced functions
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

        // Live calculation listener
        tvExpression.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){}
            @Override public void afterTextChanged(Editable s) {
                tvExpression.removeCallbacks(liveCalc);
                tvExpression.postDelayed(liveCalc, 150);
            }
        });
    }

    private final Runnable liveCalc = this::calculateLiveResult;

    private void toggleCalculatorMode() {
        isAdvancedMode = !isAdvancedMode;
        layoutAdvanced1.setVisibility(isAdvancedMode?View.VISIBLE:View.GONE);
        layoutAdvanced2.setVisibility(isAdvancedMode?View.VISIBLE:View.GONE);
        layoutAdvanced3.setVisibility(isAdvancedMode?View.VISIBLE:View.GONE);
        btnToggleMode.setText(isAdvancedMode?"BASIC":"ADV");
        updateModeIndicator();
    }

    private void toggleDegreeMode() {
        isDegreeMode = !isDegreeMode;
        btnDegRad.setText(isDegreeMode?"DEG":"RAD");
        updateModeIndicator();
    }

    private void updateModeIndicator() {
        String mode = (isDegreeMode?"DEG":"RAD") + " | " + (isAdvancedMode?"ADVANCED":"BASIC");
        tvModeIndicator.setText(mode);
    }

    private void onNumberClick(String num) {
        if (errorState) clearAll();
        if (justCalculated) { tvExpression.setText(""); justCalculated=false; }
        tvExpression.append(num);
    }

    private void onOperatorClick(String op) {
        if (errorState) clearAll();
        if (justCalculated) justCalculated=false;
        String expr=tvExpression.getText().toString();
        if (expr.isEmpty() && op.equals("-")) { tvExpression.append(op); return; }
        if (!expr.isEmpty()) {
            char last=expr.charAt(expr.length()-1);
            if ("+-×÷^".contains(""+last)) {
                tvExpression.setText(expr.substring(0,expr.length()-1)+op);
            } else tvExpression.append(op);
        }
    }

    private void onDecimalClick() {
        if (errorState) clearAll();
        String expr=tvExpression.getText().toString();
        String[] parts=expr.split("[+\\-×÷\\^()\\s]");
        if (parts.length>0) {
            String last=parts[parts.length-1].trim();
            if (!last.contains(".")) {
                tvExpression.append(last.isEmpty()?"0.":".");
            }
        } else tvExpression.append("0.");
    }

    private void onTrigFunction(String fn) {
        if (errorState) clearAll();
        if (justCalculated) { tvExpression.setText(""); justCalculated=false; }
        tvExpression.append(fn+"(");
    }

    private void onFunctionClick(String fn) {
        if (errorState) clearAll();
        if (justCalculated) { tvExpression.setText(""); justCalculated=false; }
        tvExpression.append(fn+"(");
    }

    private void onAppendText(String t) {
        if (errorState) clearAll();
        if (justCalculated && t.equals("(")) { tvExpression.setText(""); justCalculated=false; }
        tvExpression.append(t);
    }

    private void onConstantClick(String c) {
        if (errorState) clearAll();
        if (justCalculated) { tvExpression.setText(""); justCalculated=false; }
        String expr=tvExpression.getText().toString();
        if (!expr.isEmpty()) {
            char last=expr.charAt(expr.length()-1);
            if (Character.isDigit(last)|| last==')') {
                tvExpression.append("×"+c);
                return;
            }
        }
        tvExpression.append(c);
    }

    private void onFactorialClick() {
        if (errorState) clearAll();
        String expr=tvExpression.getText().toString();
        if (!expr.isEmpty()) {
            char last=expr.charAt(expr.length()-1);
            if (Character.isDigit(last)|| last==')') tvExpression.append("!");
        }
    }

    private boolean isOperator(char c) { return "+-×÷^()".contains(""+c); }

    private void clearAll() {
        tvExpression.setText("");
        tvResult.setText("0");
        errorState=false;
        justCalculated=false;
    }

    private void deleteLast() {
        String expr=tvExpression.getText().toString();
        if (!expr.isEmpty()) {
            if (expr.endsWith("sin(")||expr.endsWith("cos(")||expr.endsWith("tan(")||expr.endsWith("log("))
                tvExpression.setText(expr.substring(0,expr.length()-4));
            else if (expr.endsWith("sqrt(")||expr.endsWith("log10("))
                tvExpression.setText(expr.substring(0,expr.length()-5));
            else tvExpression.setText(expr.substring(0,expr.length()-1));
        }
        errorState=false;
    }

    private void applySmartPercentage() {
        String exp=tvExpression.getText().toString();
        if (exp.isEmpty()||errorState) return;
        try {
            // pattern: number ± number%
            Pattern p=Pattern.compile("([0-9.]+)([+\\-])([0-9.]+)%$");
            Matcher m=p.matcher(exp);
            if (m.find()) {
                double base=Double.parseDouble(m.group(1));
                String op=m.group(2);
                double pct=Double.parseDouble(m.group(3));
                double res = op.equals("-")
                        ? base - (base*pct/100)
                        : base + (base*pct/100);
                tvExpression.setText(formatResult(res));
                justCalculated=true;
            } else {
                // fallback: simple percent
                String[] parts=exp.split("[+\\-×÷^()]");
                if (parts.length>0) {
                    String ln=parts[parts.length-1].trim();
                    if (!ln.isEmpty()&&isNumeric(ln)) {
                        double val=Double.parseDouble(ln)/100;
                        tvExpression.setText(exp.substring(0,exp.lastIndexOf(ln))+val);
                    }
                }
            }
        } catch(Exception e) {
            tvResult.setText("Error: Invalid %");
            errorState=true;
        }
    }

    private boolean isNumeric(String s) {
        try { Double.parseDouble(s); return true; }
        catch(NumberFormatException e){ return false; }
    }

    private void showFinalResult() {
        try {
            String expr=tvExpression.getText().toString();
            if (expr.isEmpty()) return;
            double res=evaluate(expr);
            tvExpression.setText(formatResult(res));
            tvResult.setText("");
            justCalculated=true;
            errorState=false;
        } catch(Exception e) {
            tvResult.setText("Error");
            errorState=true;
        }
    }

    private void calculateLiveResult() {
        if (justCalculated||errorState) return;
        String expr=tvExpression.getText().toString();
        if (expr.isEmpty()) { tvResult.setText(""); return; }
        try {
            if (isExpressionComplete(expr)) {
                double res=evaluate(expr);
                tvResult.setText("= "+formatResult(res));
            } else tvResult.setText("");
        } catch(Exception e) { tvResult.setText(""); }
    }

    private boolean isExpressionComplete(String e) {
        if (e.isEmpty()) return false;
        char last=e.charAt(e.length()-1);
        return !"+-×÷^(".contains(""+last);
    }

    private double evaluate(String exp) {
        String s=exp.replace('×','*').replace('÷','/').replace('−','-')
                .replace("π",""+Math.PI);
        // factorial
        s=handleFact(s);
        // trig
        if (isDegreeMode) {
            s=s.replaceAll("sin\\(([^)]+)\\)","sin(($1)*"+Math.PI+"/180)")
                    .replaceAll("cos\\(([^)]+)\\)","cos(($1)*"+Math.PI+"/180)")
                    .replaceAll("tan\\(([^)]+)\\)","tan(($1)*"+Math.PI+"/180)");
        }
        Expression ex=new ExpressionBuilder(s).build();
        ValidationResult vr=ex.validate();
        if (!vr.isValid()) throw new IllegalArgumentException();
        double r=ex.evaluate();
        if (Double.isNaN(r)||Double.isInfinite(r)) throw new ArithmeticException();
        return r;
    }

    private String handleFact(String s) {
        StringBuilder sb=new StringBuilder();
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if (c=='!') {
                int j=i-1;
                while(j>=0&&(Character.isDigit(s.charAt(j))||s.charAt(j)=='.')) j--;
                String num=s.substring(j+1,i);
                int n=(int)Double.parseDouble(num);
                long f=1;
                for(int k=2;k<=n;k++) f*=k;
                sb.delete(sb.length()-num.length(),sb.length());
                sb.append(f);
            } else sb.append(c);
        }
        return sb.toString();
    }

    private String formatResult(double r) {
        if (Math.abs(r)>=1e10 || (Math.abs(r)<1e-4&&r!=0))
            return String.format(Locale.US,"%.6E",r);
        DecimalFormat df=new DecimalFormat("#.##########");
        df.setMaximumFractionDigits(10);
        if (r==Math.floor(r)) return String.format(Locale.US,"%.0f",r);
        return df.format(r);
    }
}
