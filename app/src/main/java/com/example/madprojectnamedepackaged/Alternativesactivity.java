package com.example.madprojectnamedepackaged;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/** Shows a natural swap and a better packaged product for the scanned item. */
public class AlternativesActivity extends BaseActivity {

    private static final String[] TIPS = {
            "Keep fruit in a bowl where you can see it. What is in sight gets eaten.",
            "Start with one swap a week. Small steps last longer than big overhauls.",
            "Next time you shop, read the first three ingredients before you decide.",
            "Pair a sweet craving with fruit first. You may find you need less."
    };

    private int tipIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alternatives);
        setupToolbar(true);

        String barcode = getIntent().getStringExtra(ResultActivity.EXTRA_BARCODE);
        Product product = ProductRepository.findByBarcode(this, barcode);
        if (product == null) {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvInstead)).setText("Instead of " + product.name + ", try:");

        showNaturalAlternative(product);
        showPackagedAlternative(product);

        Button btnBack = findViewById(R.id.btnBackToResult);
        btnBack.setOnClickListener(v -> finish());
    }

    private void showNaturalAlternative(Product product) {
        TextView tvName = findViewById(R.id.tvNaturalName);
        TextView tvWhy = findViewById(R.id.tvNaturalWhy);
        TextView tvTip = findViewById(R.id.tvNaturalTip);
        ImageView ivNatural = findViewById(R.id.ivNatural);

        tvName.setText(product.naturalAlt.isEmpty() ? "A fresh fruit or vegetable" : product.naturalAlt);
        tvWhy.setText(product.naturalWhy.isEmpty()
                ? "Whole foods come with fibre and no added extras." : product.naturalWhy);
        ivNatural.setImageResource(ProductRepository.imageRes(
                this, product.naturalImage, android.R.drawable.ic_menu_gallery));

        // Tap the card to cycle through habit tips
        findViewById(R.id.cardNatural).setOnClickListener(v -> {
            tvTip.setText(TIPS[tipIndex % TIPS.length]);
            tipIndex++;
        });
    }

    private void showPackagedAlternative(Product product) {
        TextView tvName = findViewById(R.id.tvPackagedName);
        TextView tvBrand = findViewById(R.id.tvPackagedBrand);
        TextView tvScore = findViewById(R.id.tvPackagedScore);
        TextView tvWhy = findViewById(R.id.tvPackagedWhy);
        ImageView ivPackaged = findViewById(R.id.ivPackaged);
        Button btnView = findViewById(R.id.btnViewPackaged);

        Product alt = ProductRepository.findByBarcode(this, product.packagedAltBarcode);
        if (alt == null) {
            tvName.setText("No better packaged option yet");
            tvBrand.setText("");
            tvScore.setText("");
            tvWhy.setText("We are still adding products. Try the natural swap above.");
            btnView.setVisibility(View.GONE);
            return;
        }

        tvName.setText(alt.name);
        tvBrand.setText(alt.brand);
        tvScore.setText("Unhealth score: " + ScoreCalculator.score(alt) + "/10");
        tvWhy.setText(product.packagedWhy.isEmpty()
                ? "A lower-scoring choice in the same category." : product.packagedWhy);
        ivPackaged.setImageResource(ProductRepository.imageRes(
                this, alt.imageName, android.R.drawable.ic_menu_gallery));

        View.OnClickListener openAlt = v -> {
            Intent i = new Intent(this, ResultActivity.class);
            i.putExtra(ResultActivity.EXTRA_BARCODE, alt.barcode);
            startActivity(i);
        };
        btnView.setOnClickListener(openAlt);
        findViewById(R.id.cardPackaged).setOnClickListener(openAlt);
    }
}
