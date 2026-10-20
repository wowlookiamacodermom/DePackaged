package com.example.madprojectnamedepackaged;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

/** Home screen: greeting, scan button, manual barcode entry, history button. */
public class MainActivity extends BaseActivity {

    private static final String[] FACTS = {
            "Ingredients are listed from most to least by weight, so the first three tell you most of the story.",
            "Sugar hides under many names: sucrose, glucose syrup, dextrose, maltodextrin.",
            "Compare products using the per 100 g numbers. They stay fair even when pack sizes differ.",
            "A small pack often counts as two or three servings. Check the serving size.",
            "A \"low fat\" label can still hide a lot of sugar. Read the whole panel.",
            "WHO advises keeping salt under 5 g a day. Packaged snacks can use up a lot of that in one go."
    };

    private TextView tvGreeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar(false);

        tvGreeting = findViewById(R.id.tvGreeting);
        TextView tvFact = findViewById(R.id.tvFact);
        EditText etBarcode = findViewById(R.id.etBarcode);
        Button btnScan = findViewById(R.id.btnScan);
        Button btnSearch = findViewById(R.id.btnSearch);
        Button btnHistory = findViewById(R.id.btnHistory);

        tvFact.setText(FACTS[new Random().nextInt(FACTS.length)]);

        btnScan.setOnClickListener(v -> startActivity(new Intent(this, ScannerActivity.class)));

        btnSearch.setOnClickListener(v -> {
            String code = etBarcode.getText().toString().trim();
            if (code.length() < 8) {
                etBarcode.setError("Enter 8 to 13 digits");
                Toast.makeText(this, "That does not look like a barcode", Toast.LENGTH_SHORT).show();
                return;
            }
            openProductOrDialog(code, null);
        });

        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh here so a name changed from the menu shows up when we come back
        String name = UserStore.getName(this);
        tvGreeting.setText("Hi, " + (name == null ? "there" : name));
    }
}