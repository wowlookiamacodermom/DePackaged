package com.example.madprojectnamedepackaged;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** File handling: username and password stored in private storage. */
public final class UserStore {

    private static final String USER_FILE = "username.txt";
    private static final String PASS_FILE = "password.txt";

    private UserStore() {
    }

    /** Returns the saved username, or null if the user has not entered one yet. */
    public static String getName(Context ctx) {
        return readPrivateFile(ctx, USER_FILE);
    }

    /** Returns the saved password, or null if none saved yet. */
    public static String getPassword(Context ctx) {
        return readPrivateFile(ctx, PASS_FILE);
    }

    /** Returns true if both username and password exist. */
    public static boolean hasUser(Context ctx) {
        String name = getName(ctx);
        String pass = getPassword(ctx);
        return name != null && !name.isEmpty() && pass != null && !pass.isEmpty();
    }

    /** Validates entered password against stored password. */
    public static boolean checkPassword(Context ctx, String inputPassword) {
        String stored = getPassword(ctx);
        if (stored == null || inputPassword == null) {
            return false;
        }
        return stored.equals(inputPassword.trim());
    }

    public static void saveName(Context ctx, String name) {
        writePrivateFile(ctx, USER_FILE, name);
    }

    public static void savePassword(Context ctx, String password) {
        writePrivateFile(ctx, PASS_FILE, password);
    }

    public static void saveUser(Context ctx, String name, String password) {
        saveName(ctx, name);
        savePassword(ctx, password);
    }

    private static String readPrivateFile(Context ctx, String fileName) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                ctx.openFileInput(fileName), StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (line == null || line.trim().isEmpty()) {
                return null;
            }
            return line.trim();
        } catch (IOException e) {
            return null; // file does not exist yet
        }
    }

    private static void writePrivateFile(Context ctx, String fileName, String content) {
        try (FileOutputStream fos = ctx.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}