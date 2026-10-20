package com.example.madprojectnamedepackaged;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.Locale;

/** Shows the nutrition facts and the unhealthiness highlights for one product. */
public class ResultActivity extends BaseActivity {

    public static final String EXTRA_BARCODE = "barcode";

    private Product product;
    private boolean perPack = false;
    private boolean savedToHistory = false;

    private TextView tvCalories, tvCaloriesPct, tvSugar, tvSugarPct, tvSodium, tvSodiumPct;
    private TextView tvSatFat, tvSatFatPct, tvTotalFat, tvTotalFatPct, tvProtein, tvProteinPct;
    private TextView tvRedFlags;
    private CheckBox cbSaveHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        setupToolbar(true);

        String barcode = getIntent().getStringExtra(EXTRA_BARCODE);
        product = ProductRepository.findByBarcode(this, barcode);
        if (product == null) {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViews();
        showHeader();
        showScore();
        showNutrition();

        RadioGroup rgServing = findViewById(R.id.rgServing);
        if (product.packGrams <= 0) {
            // Pack size unknown: per-pack numbers would be wrong, so keep per 100 g only
            findViewById(R.id.rbPerPack).setEnabled(false);
        }
        rgServing.setOnCheckedChangeListener((group, checkedId) -> {
            perPack = (checkedId == R.id.rbPerPack);
            showNutrition();
        });

        cbSaveHistory.setOnCheckedChangeListener((button, checked) ->
                Toast.makeText(this,
                        checked ? "This scan will be saved" : "This scan will not be saved",
                        Toast.LENGTH_SHORT).show());

        setupBrandCard();

        Button btnAlternatives = findViewById(R.id.btnAlternatives);
        btnAlternatives.setOnClickListener(v -> {
            Intent i = new Intent(this, AlternativesActivity.class);
            i.putExtra(EXTRA_BARCODE, product.barcode);
            startActivity(i);
        });

        Button btnScanAgain = findViewById(R.id.btnScanAgain);
        btnScanAgain.setOnClickListener(v -> {
            startActivity(new Intent(this, ScannerActivity.class));
            finish();
        });
    }

    /** Shows the owner company and a button that opens its "Behind the brand" entry. */
    private void setupBrandCard() {
        View card = findViewById(R.id.cardBrand);
        if (product.company == null || product.company.isEmpty()) {
            card.setVisibility(View.GONE);
            return;
        }
        ((TextView) findViewById(R.id.tvOwner)).setText(
                product.name + " is made by " + product.company + ".");

        Button btnBrand = findViewById(R.id.btnBehindBrand);
        btnBrand.setText("How has " + product.company + " behaved?");
        btnBrand.setOnClickListener(v -> {
            Intent i = new Intent(this, EthicsActivity.class);
            i.putExtra(EthicsActivity.EXTRA_COMPANY, product.company);
            startActivity(i);
        });
    }

    private void findViews() {
        tvCalories = findViewById(R.id.tvCalories);
        tvCaloriesPct = findViewById(R.id.tvCaloriesPct);
        tvSugar = findViewById(R.id.tvSugar);
        tvSugarPct = findViewById(R.id.tvSugarPct);
        tvSodium = findViewById(R.id.tvSodium);
        tvSodiumPct = findViewById(R.id.tvSodiumPct);
        tvSatFat = findViewById(R.id.tvSatFat);
        tvSatFatPct = findViewById(R.id.tvSatFatPct);
        tvTotalFat = findViewById(R.id.tvTotalFat);
        tvTotalFatPct = findViewById(R.id.tvTotalFatPct);
        tvProtein = findViewById(R.id.tvProtein);
        tvProteinPct = findViewById(R.id.tvProteinPct);
        tvRedFlags = findViewById(R.id.tvRedFlags);
        cbSaveHistory = findViewById(R.id.cbSaveHistory);
    }

    private void showHeader() {
        ((TextView) findViewById(R.id.tvProductName)).setText(product.name);
        ((TextView) findViewById(R.id.tvBrand)).setText(product.brand);
        ((TextView) findViewById(R.id.tvVegTag)).setText(product.veg ? "VEG" : "NON-VEG");

        ImageView ivProduct = findViewById(R.id.ivProduct);
        ivProduct.setImageResource(ProductRepository.imageRes(
                this, product.imageName, android.R.drawable.ic_menu_gallery));

        // Touch listener: press and hold the picture to enlarge it
        ivProduct.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(1.35f).scaleY(1.35f).setDuration(150).start();
                    return true;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    // fall through
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                    return true;
                default:
                    return true;
            }
        });
    }

    private void showScore() {
        int score = ScoreCalculator.score(product);
        int color = ScoreCalculator.colorFor(this, ScoreCalculator.ratingFor(score));

        TextView tvScore = findViewById(R.id.tvScore);
        tvScore.setText(score + "/10");
        tvScore.setTextColor(ContextCompat.getColor(this, R.color.cream));
        tvScore.getBackground().mutate().setTint(color); // graphics: circle takes the rating colour

        ProgressBar pbUnhealth = findViewById(R.id.pbUnhealth);
        pbUnhealth.setProgress(score * 10);
        pbUnhealth.setProgressTintList(ColorStateList.valueOf(color));

        ((TextView) findViewById(R.id.tvVerdict)).setText(ScoreCalculator.verdict(score));
    }

    /** Fills the table and the red flags for the chosen serving (per 100 g or per pack). */
    private void showNutrition() {
        double f = product.factor(perPack);

        setRow(tvCalories, tvCaloriesPct, product.calories * f, "kcal", 0, ScoreCalculator.DAILY_CALORIES);
        setRow(tvSugar, tvSugarPct, product.sugarG * f, "g", 1, ScoreCalculator.DAILY_SUGAR_G);
        setRow(tvSodium, tvSodiumPct, product.sodiumMg * f, "mg", 0, ScoreCalculator.DAILY_SODIUM_MG);
        setRow(tvSatFat, tvSatFatPct, product.satFatG * f, "g", 1, ScoreCalculator.DAILY_SAT_FAT_G);
        setRow(tvTotalFat, tvTotalFatPct, product.fatG * f, "g", 1, ScoreCalculator.DAILY_FAT_G);
        setRow(tvProtein, tvProteinPct, product.proteinG * f, "g", 1, ScoreCalculator.DAILY_PROTEIN_G);

        tvRedFlags.setText(buildRedFlags(f));
    }

    private void setRow(TextView amount, TextView pct, double value, String unit,
                        int decimals, double dailyAmount) {
        amount.setText(String.format(Locale.US, "%." + decimals + "f %s", value, unit));
        pct.setText(ScoreCalculator.percent(value, dailyAmount) + "%");
    }

    private String buildRedFlags(double factor) {
        StringBuilder sb = new StringBuilder();

        long teaspoons = Math.round(ScoreCalculator.teaspoons(product.sugarG * factor));
        if (teaspoons >= 1) {
            sb.append("• That is as much sugar as ").append(teaspoons)
                    .append(teaspoons == 1 ? " teaspoon" : " teaspoons")
                    .append(perPack ? " in the whole pack." : " in every 100 g.")
                    .append("\n");
        }
        if (ScoreCalculator.highSodium(product)) {
            sb.append("• High in sodium: more than 0.6 g in every 100 g.\n");
        }
        if (ScoreCalculator.highSatFat(product)) {
            sb.append("• High in saturated fat: more than 5 g in every 100 g.\n");
        }
        if (ScoreCalculator.highCalories(product)) {
            sb.append("• Energy-dense: ").append(Math.round(product.calories))
                    .append(" kcal in every 100 g.\n");
        }
        if (sb.length() == 0) {
            return "• No major red flags. Still read the label.";
        }
        return sb.toString().trim();
    }

    /** Saves the scan to history when the screen is left, if the box is ticked. */
    @Override
    protected void onStop() {
        super.onStop();
        if (product != null && !savedToHistory && cbSaveHistory.isChecked()) {
            HistoryManager.add(this, product.barcode, product.name, ScoreCalculator.score(product));
            savedToHistory = true;
        }
    }
}