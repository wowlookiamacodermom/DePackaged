package com.example.madprojectnamedepackaged;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Locale;

/**
 * A form for a product that is not in the database yet.
 * The answers become one more row in user_products.csv (same columns as products.csv)
 * in the app's private storage, so the product is found on the next scan.
 */
public class Addproductactivity extends BaseActivity {

    public static final String EXTRA_BARCODE = "barcode";

    /** Same category names as products.csv. "Beverage" must stay spelled like this. */
    private static final String[] CATEGORIES = {
            "Choose a category", "Beverage", "Noodles", "Biscuit", "Snack", "Dairy", "Sauce",
            "Chocolate", "Spread", "Cereal", "Cheese", "Dessert", "Bar"
    };

    private EditText etBarcode, etName, etBrand, etCompany, etPack;
    private EditText etCalories, etSugar, etSodium, etSatFat, etFat, etProtein;
    private EditText etNaturalAlt, etNaturalWhy;
    private RadioGroup rgVeg;
    private Spinner spCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activityaddproduct);
        setupToolbar(true);

        etBarcode = findViewById(R.id.etBarcode);
        etName = findViewById(R.id.etName);
        etBrand = findViewById(R.id.etBrand);
        etCompany = findViewById(R.id.etCompany);
        etPack = findViewById(R.id.etPack);
        etCalories = findViewById(R.id.etCalories);
        etSugar = findViewById(R.id.etSugar);
        etSodium = findViewById(R.id.etSodium);
        etSatFat = findViewById(R.id.etSatFat);
        etFat = findViewById(R.id.etFat);
        etProtein = findViewById(R.id.etProtein);
        etNaturalAlt = findViewById(R.id.etNaturalAlt);
        etNaturalWhy = findViewById(R.id.etNaturalWhy);
        rgVeg = findViewById(R.id.rgVeg);
        spCategory = findViewById(R.id.spCategory);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, CATEGORIES);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spCategory.setAdapter(adapter);
        spCategory.setPopupBackgroundResource(R.color.cream);

        String barcode = getIntent().getStringExtra(EXTRA_BARCODE);
        if (barcode != null) {
            etBarcode.setText(barcode);
        }

        Button btnSave = findViewById(R.id.btnSaveProduct);
        Button btnCancel = findViewById(R.id.btnCancelAdd);
        btnSave.setOnClickListener(v -> save());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void save() {
        String barcode = etBarcode.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String brand = etBrand.getText().toString().trim();

        // ---- checks, one at a time, with the message on the box that is wrong
        if (!barcode.matches("\\d{8,14}")) {
            fail(etBarcode, "Enter the 8 to 14 digits printed under the barcode");
            return;
        }
        if (ProductRepository.findByBarcode(this, barcode) != null) {
            fail(etBarcode, "This barcode is already in the app");
            return;
        }
        if (name.isEmpty()) {
            fail(etName, "Enter the product name");
            return;
        }
        if (brand.isEmpty()) {
            fail(etBrand, "Enter the brand");
            return;
        }
        if (spCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Choose a category", Toast.LENGTH_SHORT).show();
            spCategory.requestFocus();
            return;
        }

        Double pack = numberOrBlank(etPack, 0, 5000, "Pack size must be between 0 and 5000");
        if (pack == null) {
            return;
        }
        Double kcal = required(etCalories, 900, "Energy is per 100 g, so it cannot be above 900 kcal");
        Double sugar = required(etSugar, 100, "Sugar per 100 g cannot be above 100 g");
        Double sodium = required(etSodium, 40000, "Sodium is in mg per 100 g (salt is about 39000)");
        Double satFat = required(etSatFat, 100, "Saturated fat per 100 g cannot be above 100 g");
        Double fat = required(etFat, 100, "Total fat per 100 g cannot be above 100 g");
        Double protein = required(etProtein, 100, "Protein per 100 g cannot be above 100 g");
        if (kcal == null || sugar == null || sodium == null
                || satFat == null || fat == null || protein == null) {
            return;
        }
        if (satFat > fat) {
            fail(etSatFat, "Saturated fat cannot be more than total fat");
            return;
        }

        // ---- build the row in the products.csv column order
        String category = CATEGORIES[spCategory.getSelectedItemPosition()];
        boolean veg = rgVeg.getCheckedRadioButtonId() == R.id.rbVeg;

        Product p = new Product();
        p.category = category;
        p.calories = kcal;
        p.sugarG = sugar;
        p.sodiumMg = sodium;
        p.satFatG = satFat;
        p.fatG = fat;
        p.proteinG = protein;
        int score = ScoreCalculator.score(p);

        String[] row = new String[19];
        row[0] = barcode;
        row[1] = name;
        row[2] = brand;
        row[3] = etCompany.getText().toString().trim();
        row[4] = veg ? "1" : "0";
        row[5] = category;
        row[6] = fmt(pack);
        row[7] = fmt(kcal);
        row[8] = fmt(sugar);
        row[9] = fmt(sodium);
        row[10] = fmt(satFat);
        row[11] = fmt(fat);
        row[12] = fmt(protein);
        row[13] = "";                                          // image: none for user products
        row[14] = etNaturalAlt.getText().toString().trim();
        row[15] = etNaturalWhy.getText().toString().trim();
        row[16] = "";
        row[17] = betterPackagedBarcode(category, score);
        row[18] = row[17].isEmpty() ? "" : "Lower unhealth score in the same category.";

        if (!ProductRepository.addUserProduct(this, row)) {
            Toast.makeText(this, "Could not save. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Saved. Here is your product.", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(this, ResultActivity.class);
        i.putExtra(ResultActivity.EXTRA_BARCODE, barcode);
        startActivity(i);
        finish();
    }

    /** Picks the lowest-score product of the same category, if it is at least 2 points better. */
    private String betterPackagedBarcode(String category, int score) {
        String best = "";
        int bestScore = score - 2;
        for (Product other : ProductRepository.getAll(this)) {
            if (other.category.equalsIgnoreCase(category)) {
                int s = ScoreCalculator.score(other);
                if (s <= bestScore) {
                    bestScore = s;
                    best = other.barcode;
                }
            }
        }
        return best;
    }

    // ------------------------------------------------------------ helpers

    private void fail(EditText box, String message) {
        box.setError(message);
        box.requestFocus();
    }

    /** A number that must be filled in. Returns null (and shows the error) if it is not valid. */
    private Double required(EditText box, double max, String tooBigMessage) {
        String s = box.getText().toString().trim();
        if (s.isEmpty()) {
            fail(box, "Fill this in (use 0 if the pack says nil)");
            return null;
        }
        return parse(box, s, 0, max, tooBigMessage);
    }

    /** A number that may be left empty (returns 0 then). Returns null if filled but wrong. */
    private Double numberOrBlank(EditText box, double min, double max, String message) {
        String s = box.getText().toString().trim();
        if (s.isEmpty()) {
            return 0.0;
        }
        return parse(box, s, min, max, message);
    }

    private Double parse(EditText box, String s, double min, double max, String message) {
        try {
            double v = Double.parseDouble(s);
            if (v < min || v > max) {
                fail(box, message);
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            fail(box, "Enter a number");
            return null;
        }
    }

    private String fmt(double v) {
        if (v == Math.rint(v)) {
            return String.valueOf((long) v);
        }
        return String.format(Locale.US, "%.1f", v);
    }
}