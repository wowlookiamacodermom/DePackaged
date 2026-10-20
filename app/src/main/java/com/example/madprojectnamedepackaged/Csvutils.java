package com.example.madprojectnamedepackaged;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Small helpers for reading .csv files from the assets folder. */
public final class CsvUtils {

    private CsvUtils() {
    }

    /** Splits one CSV line. Handles "quoted, values" and doubled quotes. */
    public static String[] splitLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString().trim());
        return out.toArray(new String[0]);
    }

    /** Wraps a value in quotes when it contains a comma or a quote, and removes line breaks. */
    public static String quote(String value) {
        String v = value == null ? "" : value.replace("\n", " ").replace("\r", " ").trim();
        if (v.contains(",") || v.contains("\"")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    /** Reads every row of a CSV file in the app's private storage (no header row). */
    public static List<String[]> readPrivateRows(Context ctx, String fileName) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                ctx.openFileInput(fileName), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    rows.add(splitLine(line));
                }
            }
        } catch (java.io.FileNotFoundException e) {
            // nothing saved yet
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rows;
    }

    /**
     * Reads every data row of a CSV file in assets.
     * Blank lines and lines starting with # are skipped, and so is the header row.
     */
    public static List<String[]> readAssetRows(Context ctx, String fileName) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                ctx.getAssets().open(fileName), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (header) {
                    header = false;
                    continue;
                }
                rows.add(splitLine(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rows;
    }
}