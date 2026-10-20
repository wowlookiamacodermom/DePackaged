package com.example.madprojectnamedepackaged;


import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Past scans read from history.csv. Filter by rating (spinner) and by date (date picker).
 * Tap a row to reopen it, long-press for a context menu to delete it.
 */
public class HistoryActivity extends BaseActivity {

    private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000;
    private static final int MENU_OPEN = 1;
    private static final int MENU_DELETE = 2;

    private final List<HistoryManager.Entry> shown = new ArrayList<>();
    private HistoryAdapter adapter;

    private Spinner spFilter;
    private TextView tvDateSelected;
    private TextView tvEmpty;
    private long dayStart = -1; // -1 means "all dates"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        setupToolbar(true);

        spFilter = findViewById(R.id.spFilter);
        tvDateSelected = findViewById(R.id.tvDateSelected);
        tvEmpty = findViewById(R.id.tvEmpty);
        ListView lvHistory = findViewById(R.id.lvHistory);
        Button btnPickDate = findViewById(R.id.btnPickDate);
        Button btnClear = findViewById(R.id.btnClearHistory);

        // List first, so the spinner callback below has something to refresh
        adapter = new HistoryAdapter(shown);
        lvHistory.setAdapter(adapter);
        lvHistory.setOnItemClickListener((parent, view, position, id) -> openEntry(shown.get(position)));
        registerForContextMenu(lvHistory);

        // Spinner rows use spinner_item.xml so the text is in the logo colour
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, R.layout.spinner_item,
                getResources().getStringArray(R.array.rating_filter));
        filterAdapter.setDropDownViewResource(R.layout.spinner_item);
        spFilter.setAdapter(filterAdapter);
        spFilter.setPopupBackgroundResource(R.color.cream);
        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnPickDate.setOnClickListener(v -> pickDate());

        // Tap the date line to clear the date filter
        tvDateSelected.setOnClickListener(v -> {
            dayStart = -1;
            tvDateSelected.setText("Showing scans from all dates");
            refresh();
        });

        btnClear.setOnClickListener(v -> showThemedDialog(
                "Clear history?",
                "All saved scans will be removed from this phone.",
                false, "Clear", "Cancel",
                typed -> {
                    HistoryManager.clear(this);
                    refresh();
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                },
                null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    // ------------------------------------------------------------- filtering

    private void pickDate() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day, 0, 0, 0);
            picked.set(Calendar.MILLISECOND, 0);
            dayStart = picked.getTimeInMillis();
            tvDateSelected.setText(String.format(Locale.getDefault(),
                    "Showing scans from %02d/%02d/%d (tap here to clear)", day, month + 1, year));
            refresh();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void refresh() {
        int filter = spFilter.getSelectedItemPosition(); // 0 all, 1 green, 2 amber, 3 red

        shown.clear();
        for (HistoryManager.Entry e : HistoryManager.loadAll(this)) {
            if (filter > 0 && ScoreCalculator.ratingFor(e.score) != filter - 1) {
                continue;
            }
            if (dayStart >= 0 && (e.time < dayStart || e.time >= dayStart + ONE_DAY_MS)) {
                continue;
            }
            shown.add(e);
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openEntry(HistoryManager.Entry entry) {
        if (ProductRepository.findByBarcode(this, entry.barcode) == null) {
            Toast.makeText(this, "That product is no longer in the database", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, ResultActivity.class);
        i.putExtra(ResultActivity.EXTRA_BARCODE, entry.barcode);
        startActivity(i);
    }

    // ---------------------------------------------------------- context menu

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("This scan");
        menu.add(0, MENU_OPEN, 0, "Open result");
        menu.add(0, MENU_DELETE, 1, "Delete this scan");
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info == null || info.position >= shown.size()) {
            return super.onContextItemSelected(item);
        }
        HistoryManager.Entry entry = shown.get(info.position);

        if (item.getItemId() == MENU_OPEN) {
            openEntry(entry);
            return true;
        } else if (item.getItemId() == MENU_DELETE) {
            HistoryManager.remove(this, entry);
            refresh();
            Toast.makeText(this, "Scan deleted", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    // --------------------------------------------------------------- adapter

    /** Fills item_history.xml for each saved scan. */
    private class HistoryAdapter extends ArrayAdapter<HistoryManager.Entry> {

        HistoryAdapter(List<HistoryManager.Entry> items) {
            super(HistoryActivity.this, 0, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View row = convertView != null
                    ? convertView
                    : getLayoutInflater().inflate(R.layout.item_history, parent, false);

            HistoryManager.Entry e = getItem(position);
            if (e == null) {
                return row;
            }
            int rating = ScoreCalculator.ratingFor(e.score);

            TextView dot = row.findViewById(R.id.tvScoreDot);
            dot.setText(String.valueOf(e.score));
            dot.getBackground().mutate().setTint(ScoreCalculator.colorFor(getContext(), rating));

            ((TextView) row.findViewById(R.id.tvItemName)).setText(e.name);
            ((TextView) row.findViewById(R.id.tvItemDate)).setText(
                    new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            .format(new Date(e.time)));
            ((TextView) row.findViewById(R.id.tvItemRating)).setText(ScoreCalculator.ratingLabel(rating));
            return row;
        }
    }
}
