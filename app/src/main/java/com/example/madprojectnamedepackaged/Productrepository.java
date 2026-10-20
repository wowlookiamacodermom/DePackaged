package com.example.madprojectnamedepackaged;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Loads products.csv once and answers barcode look-ups. */
public final class ProductRepository {

    /** Products typed in by the user on this phone (same 19 columns, no header). */
    public static final String USER_FILE = "user_products.csv";

    private static List<Product> cache;

    private ProductRepository() {
    }

    public static synchronized List<Product> getAll(Context ctx) {
        if (cache == null) {
            cache = new ArrayList<>();
            for (String[] row : CsvUtils.readAssetRows(ctx.getApplicationContext(), "products.csv")) {
                Product p = fromRow(row);
                if (p != null) {
                    cache.add(p);
                }
            }
            for (String[] row : CsvUtils.readPrivateRows(ctx.getApplicationContext(), USER_FILE)) {
                Product p = fromRow(row);
                if (p != null) {
                    cache.add(p);
                }
            }
        }
        return cache;
    }

    /** Saves a new product (one value per column, in the products.csv order) and refreshes the list. */
    public static synchronized boolean addUserProduct(Context ctx, String[] columns) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(CsvUtils.quote(columns[i]));
        }
        line.append('\n');
        try (FileOutputStream fos = ctx.getApplicationContext()
                .openFileOutput(USER_FILE, Context.MODE_APPEND)) {
            fos.write(line.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        cache = null; // reload next time so the new product is found
        return true;
    }

    /** Exports all products (base asset products + user-added products) into a products.csv File. */
    public static File exportCsvFile(Context ctx) {
        File file = new File(ctx.getCacheDir(), "products.csv");
        StringBuilder sb = new StringBuilder();
        // Standard header matching products.csv format
        sb.append("barcode,name,brand,company,veg,category,pack_g,calories,sugar_g,sodium_mg,sat_fat_g,fat_g,protein_g,image,natural_alt,natural_why,natural_image,packaged_alt_barcode,packaged_why\n");

        for (Product p : getAll(ctx)) {
            sb.append(CsvUtils.quote(p.barcode)).append(',');
            sb.append(CsvUtils.quote(p.name)).append(',');
            sb.append(CsvUtils.quote(p.brand)).append(',');
            sb.append(CsvUtils.quote(p.company)).append(',');
            sb.append(p.veg ? "1" : "0").append(',');
            sb.append(CsvUtils.quote(p.category)).append(',');
            sb.append(fmtNum(p.packGrams)).append(',');
            sb.append(fmtNum(p.calories)).append(',');
            sb.append(fmtNum(p.sugarG)).append(',');
            sb.append(fmtNum(p.sodiumMg)).append(',');
            sb.append(fmtNum(p.satFatG)).append(',');
            sb.append(fmtNum(p.fatG)).append(',');
            sb.append(fmtNum(p.proteinG)).append(',');
            sb.append(CsvUtils.quote(p.imageName)).append(',');
            sb.append(CsvUtils.quote(p.naturalAlt)).append(',');
            sb.append(CsvUtils.quote(p.naturalWhy)).append(',');
            sb.append(CsvUtils.quote(p.naturalImage)).append(',');
            sb.append(CsvUtils.quote(p.packagedAltBarcode)).append(',');
            sb.append(CsvUtils.quote(p.packagedWhy)).append('\n');
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private static String fmtNum(double v) {
        if (v == Math.rint(v)) {
            return String.valueOf((long) v);
        }
        return String.format(Locale.US, "%.1f", v);
    }

    public static Product findByBarcode(Context ctx, String barcode) {
        if (barcode == null) {
            return null;
        }
        String code = barcode.trim();
        if (code.isEmpty()) {
            return null;
        }
        for (Product p : getAll(ctx)) {
            if (p.barcode.equals(code)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Turns a drawable name from the CSV into a resource id.
     * Returns the fallback if the name is empty or the drawable does not exist.
     */
    public static int imageRes(Context ctx, String drawableName, int fallback) {
        if (drawableName == null || drawableName.isEmpty()) {
            return fallback;
        }
        int id = ctx.getResources().getIdentifier(drawableName, "drawable", ctx.getPackageName());
        return id != 0 ? id : fallback;
    }

    // Column order in products.csv:
    // 0 barcode, 1 name, 2 brand, 3 company, 4 veg, 5 category, 6 pack_g,
    // 7 calories, 8 sugar_g, 9 sodium_mg, 10 sat_fat_g, 11 fat_g, 12 protein_g,
    // 13 image, 14 natural_alt, 15 natural_why, 16 natural_image,
    // 17 packaged_alt_barcode, 18 packaged_why
    private static Product fromRow(String[] f) {
        String barcode = get(f, 0);
        if (barcode.isEmpty()) {
            return null;
        }
        Product p = new Product();
        p.barcode = barcode;
        p.name = get(f, 1);
        p.brand = get(f, 2);
        p.company = get(f, 3);
        String veg = get(f, 4);
        p.veg = veg.equals("1") || veg.equalsIgnoreCase("yes") || veg.equalsIgnoreCase("true");
        p.category = get(f, 5);
        p.packGrams = num(get(f, 6));
        p.calories = num(get(f, 7));
        p.sugarG = num(get(f, 8));
        p.sodiumMg = num(get(f, 9));
        p.satFatG = num(get(f, 10));
        p.fatG = num(get(f, 11));
        p.proteinG = num(get(f, 12));
        p.imageName = get(f, 13);
        p.naturalAlt = get(f, 14);
        p.naturalWhy = get(f, 15);
        p.naturalImage = get(f, 16);
        p.packagedAltBarcode = get(f, 17);
        p.packagedWhy = get(f, 18);
        return p;
    }

    private static String get(String[] f, int index) {
        return index < f.length ? f[index].trim() : "";
    }

    private static double num(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}