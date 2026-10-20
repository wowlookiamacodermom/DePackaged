package com.example.madprojectnamedepackaged;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * File handling: scan history is kept in history.csv in the app's private storage.
 * One line per scan:  timeMillis,barcode,score,name
 * Barcodes that were not found are logged to missing_products.txt.
 */
public final class HistoryManager {

    private static final String HISTORY_FILE = "history.csv";
    private static final String MISSING_FILE = "missing_products.txt";

    private HistoryManager() {
    }

    /** One saved scan. */
    public static class Entry {
        public long time;
        public String barcode;
        public String name;
        public int score;
    }

    public static void add(Context ctx, String barcode, String name, int score) {
        Entry e = new Entry();
        e.time = System.currentTimeMillis();
        e.barcode = barcode;
        e.name = name;
        e.score = score;
        appendEntry(ctx, e);
    }

    /** All saved scans, newest first. */
    public static List<Entry> loadAll(Context ctx) {
        List<Entry> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                ctx.openFileInput(HISTORY_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = line.split(",", 4);
                if (f.length < 4) {
                    continue;
                }
                try {
                    Entry e = new Entry();
                    e.time = Long.parseLong(f[0]);
                    e.barcode = f[1];
                    e.score = Integer.parseInt(f[2]);
                    e.name = f[3];
                    list.add(e);
                } catch (NumberFormatException ignored) {
                    // skip a damaged line
                }
            }
        } catch (FileNotFoundException e) {
            // no history yet
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.reverse(list);
        return list;
    }

    public static void remove(Context ctx, Entry target) {
        List<Entry> all = loadAll(ctx);
        Collections.reverse(all); // oldest first, so the file keeps its order
        ctx.deleteFile(HISTORY_FILE);
        for (Entry e : all) {
            boolean same = e.time == target.time && e.barcode.equals(target.barcode);
            if (!same) {
                appendEntry(ctx, e);
            }
        }
    }

    public static void clear(Context ctx) {
        ctx.deleteFile(HISTORY_FILE);
    }

    /** Remembers a barcode the app did not know, plus the name the user typed. */
    public static void logMissing(Context ctx, String barcode, String typedName) {
        String line = System.currentTimeMillis() + "," + barcode + "," + clean(typedName) + "\n";
        append(ctx, MISSING_FILE, line);
    }

    private static void appendEntry(Context ctx, Entry e) {
        String line = e.time + "," + e.barcode + "," + e.score + "," + clean(e.name) + "\n";
        append(ctx, HISTORY_FILE, line);
    }

    private static void append(Context ctx, String fileName, String text) {
        try (FileOutputStream fos = ctx.openFileOutput(fileName, Context.MODE_APPEND)) {
            fos.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        return s.replace(",", " ").replace("\n", " ").replace("\r", " ").trim();
    }
}
