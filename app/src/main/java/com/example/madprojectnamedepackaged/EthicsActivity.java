package com.example.madprojectnamedepackaged;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** "Behind the brand": reported concerns about a company, with the source and year. */
public class EthicsActivity extends BaseActivity {

    /** One row of ethics.csv: company, concern, source, year, url. */
    private static class Company {
        String name;
        String concern;
        String source;
        String year;
        String url;
    }

    public static final String EXTRA_COMPANY = "company";

    private final List<Company> companies = new ArrayList<>();

    private TextView tvCompanyName;
    private TextView tvConcern;
    private TextView tvSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ethics);
        setupToolbar(true);

        tvCompanyName = findViewById(R.id.tvCompanyName);
        tvConcern = findViewById(R.id.tvConcern);
        tvSource = findViewById(R.id.tvSource);
        Spinner spCompany = findViewById(R.id.spCompany);

        loadCompanies();

        addCompaniesWithoutEntry();

        List<String> names = new ArrayList<>();
        names.add("Select a company");
        for (Company c : companies) {
            names.add(c.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, names);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spCompany.setAdapter(adapter);
        spCompany.setPopupBackgroundResource(R.color.cream);

        // Opened from a scanned product: jump straight to that company
        String wanted = getIntent().getStringExtra(EXTRA_COMPANY);
        if (wanted != null) {
            for (int i = 0; i < companies.size(); i++) {
                if (companies.get(i).name.equalsIgnoreCase(wanted)) {
                    spCompany.setSelection(i + 1);
                    break;
                }
            }
        }

        spCompany.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    showNothingSelected();
                } else {
                    showCompany(companies.get(position - 1));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadCompanies() {
        for (String[] f : CsvUtils.readAssetRows(this, "ethics.csv")) {
            if (f.length < 4 || f[0].isEmpty()) {
                continue;
            }
            Company c = new Company();
            c.name = f[0];
            c.concern = f[1];
            c.source = f[2];
            c.year = f[3];
            c.url = f.length > 4 ? f[4] : "";
            companies.add(c);
        }
    }

    /** Companies that appear in products.csv but have no row in ethics.csv yet. */
    private void addCompaniesWithoutEntry() {
        List<String> known = new ArrayList<>();
        for (Company c : companies) {
            known.add(c.name.toLowerCase());
        }
        for (Product p : ProductRepository.getAll(this)) {
            if (p.company == null || p.company.isEmpty()) {
                continue;
            }
            if (!known.contains(p.company.toLowerCase())) {
                Company c = new Company();
                c.name = p.company;
                c.concern = "";   // empty concern = no entry yet
                c.source = "";
                c.year = "";
                c.url = "";
                companies.add(c);
                known.add(p.company.toLowerCase());
            }
        }
    }

    private void showNothingSelected() {
        tvCompanyName.setText("Pick a company above");
        tvConcern.setText("-");
        tvSource.setText("-");
    }

    private void showCompany(Company c) {
        tvCompanyName.setText(c.name);
        if (c.concern.isEmpty()) {
            tvConcern.setText("No entry yet. We have not found a well-sourced report about this company.");
            tvSource.setText("-");
            return;
        }
        tvConcern.setText(c.concern);

        String source = c.source + ", " + c.year;
        if (!c.url.isEmpty()) {
            source += "\n" + c.url; // autoLink="web" in the layout makes this tappable
        }
        tvSource.setText(source);
    }
}