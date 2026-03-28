package com.myapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

    private EditText inputA, inputB;
    private TextView txtResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputA = findViewById(R.id.input_a);
        inputB = findViewById(R.id.input_b);
        txtResult = findViewById(R.id.txt_result);

        findViewById(R.id.btn_add).setOnClickListener(v -> executeCalculation("add"));
        findViewById(R.id.btn_sub).setOnClickListener(v -> executeCalculation("sub"));
        findViewById(R.id.btn_mul).setOnClickListener(v -> executeCalculation("mul"));
        findViewById(R.id.btn_div).setOnClickListener(v -> executeCalculation("div"));
    }

    private void executeCalculation(String op) {
        String rawA = inputA.getText().toString();
        String rawB = inputB.getText().toString();

        if (rawA.isEmpty() || rawB.isEmpty()) {
            txtResult.setText("Error: Input kosong!");
            return;
        }

        try {
            double valA = Double.parseDouble(rawA);
            double valB = Double.parseDouble(rawB);

            CalcResult result = calculate(valA, valB, op);

            if (result.isSuccess) {
                txtResult.setText(String.format("Hasil: %.2f", result.value));
            } else {
                txtResult.setText("Error: " + result.errorMessage);
            }
        } catch (NumberFormatException e) {
            txtResult.setText("Error: Format angka salah");
        }
    }

    private CalcResult calculate(double a, double b, String op) {
        switch (op) {
            case "add": return CalcResult.ok(a + b);
            case "sub": return CalcResult.ok(a - b);
            case "mul": return CalcResult.ok(a * b);
            case "div": 
                return (b == 0) ? CalcResult.err("Pembagian nol dilarang") : CalcResult.ok(a / b);
            default: return CalcResult.err("Operasi tidak valid");
        }
    }

    static class CalcResult {
        final double value;
        final String errorMessage;
        final boolean isSuccess;

        private CalcResult(double value, String errorMessage, boolean isSuccess) {
            this.value = value;
            this.errorMessage = errorMessage;
            this.isSuccess = isSuccess;
        }

        static CalcResult ok(double val) { return new CalcResult(val, null, true); }
        static CalcResult err(String msg) { return new CalcResult(0, msg, false); }
    }
}
