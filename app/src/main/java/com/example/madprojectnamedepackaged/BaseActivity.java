package com.example.madprojectnamedepackaged;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Shared parent of the screens that have a toolbar.
 * It owns the options menu (main_menu.xml) and the themed pop-up dialogs,
 * so those are written once instead of in every activity.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** What to do when the positive button of a dialog is pressed. */
    protected interface DialogAction {
        void run(String typedText);
    }

    /**
     * Call this after setContentView(). Uses the Toolbar with id "toolbar"
     * as the action bar so the menu appears on it.
     */
    protected void setupToolbar(boolean showBackArrow) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            return;
        }
        CharSequence title = toolbar.getTitle(); // keep the title written in the XML
        toolbar.setPopupTheme(R.style.Depack_PopupTheme); // styled three-dots menu
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(showBackArrow);
        }
    }

    // ---------------------------------------------------------------- menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_home) {
            if (!(this instanceof MainActivity)) {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
            }
            return true;
        } else if (id == R.id.menu_add) {
            if (!(this instanceof Addproductactivity)) {
                startActivity(new Intent(this, Addproductactivity.class));
            }
            return true;
        } else if (id == R.id.menu_history) {
            if (!(this instanceof HistoryActivity)) {
                startActivity(new Intent(this, HistoryActivity.class));
            }
            return true;
        } else if (id == R.id.menu_ethics) {
            if (!(this instanceof EthicsActivity)) {
                startActivity(new Intent(this, EthicsActivity.class));
            }
            return true;
        } else if (id == R.id.menu_about) {
            showThemedDialog("About Depackaged",
                    "Scan a packaged food, see what is really inside, and swap to a better choice. "
                            + "Company information is reported, sourced and dated. It is not a legal finding.",
                    false, "Got it", null, null, null);
            return true;
        } else if (id == R.id.menu_export_csv) {
            exportProductsCsv();
            return true;
        } else if (id == R.id.menu_change_user) {
            Intent i = new Intent(this, UsernameActivity.class);
            i.putExtra(UsernameActivity.EXTRA_CHANGE, true);
            startActivity(i);
            return true;
        } else if (id == R.id.menu_exit) {
            finishAffinity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // toolbar back arrow
        return true;
    }

    /** Exports all products (base + manually added) into a products.csv file and opens the share chooser. */
    protected void exportProductsCsv() {
        try {
            File file = ProductRepository.exportCsvFile(this);
            Uri uri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Depackaged products.csv");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Exported products database from Depackaged.");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Export & Share products.csv"));
            Toast.makeText(this, "Exporting products.csv...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not export products.csv", Toast.LENGTH_SHORT).show();
        }
    }

    // -------------------------------------------------------------- dialogs

    /**
     * A pop-up built from dialog_not_found.xml so it matches the app theme.
     *
     * @param showInput  show the optional text box
     * @param negative   text of the negative button, or null for none
     * @param onPositive called when the positive button is pressed, may be null
     * @param onDismiss  called whenever the dialog closes, may be null
     */
    protected void showThemedDialog(String title, String message, boolean showInput,
                                    String positive, String negative,
                                    DialogAction onPositive, Runnable onDismiss) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_not_found, null);
        ((TextView) view.findViewById(R.id.tvDialogTitle)).setText(title);
        ((TextView) view.findViewById(R.id.tvDialogMessage)).setText(message);
        final EditText input = view.findViewById(R.id.etSuggestName);
        input.setVisibility(showInput ? View.VISIBLE : View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this).setView(view);
        builder.setPositiveButton(positive, (d, which) -> {
            if (onPositive != null) {
                onPositive.run(input.getText().toString().trim());
            }
        });
        if (negative != null) {
            builder.setNegativeButton(negative, null);
        }
        if (onDismiss != null) {
            builder.setOnDismissListener(d -> onDismiss.run());
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(ContextCompat.getColor(this, R.color.mustard)));
        }
        dialog.show();

        int red = ContextCompat.getColor(this, R.color.app_red);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(red);
        if (negative != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(red);
        }
    }

    /**
     * Opens the Result screen if the barcode is in the database,
     * otherwise shows the "product not found" pop-up.
     */
    protected void openProductOrDialog(String barcode, Runnable onDialogClosed) {
        if (ProductRepository.findByBarcode(this, barcode) != null) {
            Intent i = new Intent(this, ResultActivity.class);
            i.putExtra(ResultActivity.EXTRA_BARCODE, barcode);
            startActivity(i);
            return;
        }
        HistoryManager.logMissing(this, barcode, "");
        showThemedDialog("Product not found",
                "We do not have the barcode " + barcode + " yet. Check the number, "
                        + "or add this product yourself so it works next time.",
                false, "Add it", "Not now",
                typed -> {
                    Intent i = new Intent(this, Addproductactivity.class);
                    i.putExtra(Addproductactivity.EXTRA_BARCODE, barcode);
                    startActivity(i);
                },
                onDialogClosed);
    }
}