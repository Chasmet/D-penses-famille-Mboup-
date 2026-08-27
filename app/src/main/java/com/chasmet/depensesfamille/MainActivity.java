package com.chasmet.depensesfamille;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS = "budget_family_prefs";
    private static final String KEY_ITEMS = "budget_items";
    private static final String KEY_DARK = "dark_mode";
    private static final String KEY_SCHEMA = "budget_schema";
    private static final int CURRENT_SCHEMA = 4;

    private static final String GROUP_CHEIKH = "Monsieur Mboup";
    private static final String GROUP_MEOUBA = "Madame Gomis";
    private static final String GROUP_COMMON = "Commun";
    private static final String GROUP_CREDIT = "Autres frais / crédits";

    private static final String TYPE_INCOME = "Salaire / entrée";
    private static final String TYPE_EXPENSE = "Dépense";

    private static final String[] GROUPS = new String[]{GROUP_CHEIKH, GROUP_MEOUBA, GROUP_COMMON, GROUP_CREDIT};
    private static final String[] TYPES = new String[]{TYPE_EXPENSE, TYPE_INCOME};

    private final List<BudgetItem> items = new ArrayList<>();
    private boolean darkMode = true;
    private Palette colors;

    private View rootScroll;
    private LinearLayout rootLayout;
    private LinearLayout summaryBox;
    private LinearLayout tableContainer;
    private TextView titleText;
    private TextView subtitleText;
    private TextView summaryTitle;
    private TextView summaryText;
    private TextView helpText;
    private Button addButton;
    private Button resetButton;
    private Button themeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        darkMode = preferences.getBoolean(KEY_DARK, true);

        rootScroll = findViewById(R.id.rootScroll);
        rootLayout = findViewById(R.id.rootLayout);
        summaryBox = findViewById(R.id.summaryBox);
        tableContainer = findViewById(R.id.tableContainer);
        titleText = findViewById(R.id.titleText);
        subtitleText = findViewById(R.id.subtitleText);
        summaryTitle = findViewById(R.id.summaryTitle);
        summaryText = findViewById(R.id.summaryText);
        helpText = findViewById(R.id.helpText);
        addButton = findViewById(R.id.addButton);
        resetButton = findViewById(R.id.resetButton);
        themeButton = findViewById(R.id.themeButton);

        loadItems();
        renderAll();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBudgetDialog(null, null);
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmReset();
            }
        });

        themeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                darkMode = !darkMode;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_DARK, darkMode).apply();
                renderAll();
            }
        });
    }

    private void renderAll() {
        colors = darkMode ? Palette.dark() : Palette.light();
        applyBaseTheme();
        renderSummary();
        tableContainer.removeAllViews();
        tableContainer.addView(createTwoColumnRow(GROUP_CHEIKH, GROUP_MEOUBA));
        tableContainer.addView(createTwoColumnRow(GROUP_COMMON, GROUP_CREDIT));
    }

    private void applyBaseTheme() {
        rootScroll.setBackgroundColor(colors.page);
        rootLayout.setBackgroundColor(colors.page);
        summaryBox.setBackground(round(colors.card, colors.line, 16));
        titleText.setTextColor(colors.text);
        subtitleText.setTextColor(colors.muted);
        summaryTitle.setTextColor(colors.text);
        helpText.setTextColor(colors.muted);
        themeButton.setText(darkMode ? "Clair" : "Sombre");
        styleButton(addButton, colors.green, Color.WHITE);
        styleButton(resetButton, colors.soft, colors.text);
        styleButton(themeButton, colors.soft, colors.text);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(colors.page);
            getWindow().setNavigationBarColor(colors.page);
        }
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void renderSummary() {
        double incomeCheikh = sum(GROUP_CHEIKH, TYPE_INCOME, true);
        double incomeMeouba = sum(GROUP_MEOUBA, TYPE_INCOME, true);
        double expenseCheikh = sum(GROUP_CHEIKH, TYPE_EXPENSE, true);
        double expenseMeouba = sum(GROUP_MEOUBA, TYPE_EXPENSE, true);
        double commonShare = (sum(GROUP_COMMON, TYPE_EXPENSE, false) + sum(GROUP_CREDIT, TYPE_EXPENSE, false)) / 2.0;

        double balanceCheikh = incomeCheikh - expenseCheikh - commonShare;
        double balanceMeouba = incomeMeouba - expenseMeouba - commonShare;
        double realIncome = sumAll(TYPE_INCOME, false);
        double realExpense = sumAll(TYPE_EXPENSE, false);
        double familyBalance = realIncome - realExpense;
        double internalOut = sumInternal(TYPE_EXPENSE);

        StringBuilder sb = new StringBuilder();
        sb.append("Revenus foyer : ").append(money(realIncome)).append("\n");
        sb.append("Dépenses réelles : ").append(money(realExpense)).append("\n");
        sb.append("Virement interne ignoré : ").append(money(internalOut)).append("\n");
        sb.append("Cheikh : ").append(money(balanceCheikh)).append("  |  Mme Gomis : ").append(money(balanceMeouba)).append("\n");
        sb.append("Reste foyer : ").append(money(familyBalance));
        summaryText.setText(sb.toString());
        summaryText.setTextColor(familyBalance < 0 ? colors.red : colors.text);
    }

    private LinearLayout createTwoColumnRow(String leftGroup, String rightGroup) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(2f);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowParams);
        row.addView(createTable(leftGroup, true));
        row.addView(createTable(rightGroup, false));
        return row;
    }

    private LinearLayout createTable(final String group, boolean left) {
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackground(round(colors.card, colors.line, 14));
        table.setPadding(dp(6), dp(7), dp(6), dp(7));

        LinearLayout.LayoutParams tableParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tableParams.setMargins(left ? 0 : dp(4), 0, left ? dp(4) : 0, 0);
        table.setLayoutParams(tableParams);

        TextView title = new TextView(this);
        title.setText(shortGroupName(group));
        title.setTextColor(colors.text);
        title.setTextSize(14);
        title.setTypeface(null, 1);
        title.setSingleLine(false);
        table.addView(title);

        TextView total = new TextView(this);
        total.setText(buildGroupTotalText(group));
        total.setTextColor(colors.muted);
        total.setTextSize(10);
        total.setPadding(0, dp(2), 0, dp(6));
        total.setSingleLine(false);
        table.addView(total);

        for (BudgetItem item : items) {
            if (group.equals(item.group)) {
                table.addView(createBudgetRow(item));
            }
        }

        Button addHere = new Button(this);
        addHere.setText("+ ici");
        addHere.setTextSize(11);
        addHere.setAllCaps(false);
        addHere.setMinHeight(0);
        addHere.setMinimumHeight(0);
        addHere.setPadding(dp(3), dp(2), dp(3), dp(2));
        styleButton(addHere, colors.soft, colors.text);
        addHere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBudgetDialog(null, group);
            }
        });
        table.addView(addHere);

        return table;
    }

    private String buildGroupTotalText(String group) {
        double income = sum(group, TYPE_INCOME, true);
        double expense = sum(group, TYPE_EXPENSE, true);
        double balance = income - expense;
        return "+ " + moneyCompact(income) + "\n- " + moneyCompact(expense) + "\n= " + moneyCompact(balance);
    }

    private View createBudgetRow(final BudgetItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(5), dp(5), dp(5), dp(5));
        row.setBackground(round(colors.row, colors.line, 10));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(5));
        row.setLayoutParams(rowParams);

        TextView name = new TextView(this);
        String label = item.name;
        if (item.internalTransfer) label += " ↔";
        if (item.fixed) label += " • fixe";
        name.setText(label);
        name.setTextColor(colors.text);
        name.setTextSize(11);
        name.setSingleLine(false);
        row.addView(name);

        TextView amount = new TextView(this);
        amount.setText(moneyCompact(item.amount));
        amount.setTextSize(12);
        amount.setTypeface(null, 1);
        amount.setTextColor(item.internalTransfer ? colors.blue : (TYPE_INCOME.equals(item.type) ? colors.green : colors.red));
        amount.setGravity(android.view.Gravity.END);
        amount.setPadding(0, dp(2), 0, 0);
        row.addView(amount);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBudgetDialog(item, null);
            }
        });
        return row;
    }

    private void showBudgetDialog(final BudgetItem existing, String forcedGroup) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_budget_item, null, false);
        final EditText nameInput = content.findViewById(R.id.nameInput);
        final EditText amountInput = content.findViewById(R.id.amountInput);
        final Spinner groupSpinner = content.findViewById(R.id.groupSpinner);
        final Spinner typeSpinner = content.findViewById(R.id.typeSpinner);
        final CheckBox fixedCheck = content.findViewById(R.id.fixedCheck);
        final CheckBox internalCheck = content.findViewById(R.id.internalCheck);

        content.setBackgroundColor(colors.card);
        applyDialogColors(content);

        groupSpinner.setAdapter(createSpinnerAdapter(GROUPS));
        typeSpinner.setAdapter(createSpinnerAdapter(TYPES));

        if (existing != null) {
            nameInput.setText(existing.name);
            amountInput.setText(String.format(Locale.FRANCE, "%.2f", existing.amount));
            groupSpinner.setSelection(indexOf(GROUPS, existing.group));
            typeSpinner.setSelection(indexOf(TYPES, existing.type));
            fixedCheck.setChecked(existing.fixed);
            internalCheck.setChecked(existing.internalTransfer);
        } else {
            groupSpinner.setSelection(indexOf(GROUPS, forcedGroup == null ? GROUP_COMMON : forcedGroup));
            typeSpinner.setSelection(indexOf(TYPES, TYPE_EXPENSE));
            fixedCheck.setChecked(true);
            internalCheck.setChecked(false);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Ajouter une ligne" : "Modifier la ligne")
                .setView(content)
                .setNegativeButton("Annuler", null);

        if (existing != null) builder.setNeutralButton("Supprimer", null);
        builder.setPositiveButton("Enregistrer", null);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(colors.card));
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(colors.green);
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(colors.muted);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (saveDialogItem(existing, nameInput, amountInput, groupSpinner, typeSpinner, fixedCheck, internalCheck)) {
                            dialog.dismiss();
                        }
                    }
                });
                if (existing != null) {
                    dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(colors.red);
                    dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            items.remove(existing);
                            saveItems();
                            renderAll();
                            dialog.dismiss();
                        }
                    });
                }
            }
        });
        dialog.show();
    }

    private void applyDialogColors(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setTextColor(colors.text);
            if (view instanceof EditText) ((EditText) view).setHintTextColor(colors.muted);
        }
        if (view instanceof CheckBox) ((CheckBox) view).setTextColor(colors.text);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            group.setBackgroundColor(colors.card);
            for (int i = 0; i < group.getChildCount(); i++) applyDialogColors(group.getChildAt(i));
        }
    }

    private ArrayAdapter<String> createSpinnerAdapter(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(colors.text);
                    ((TextView) view).setTextSize(13);
                }
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.BLACK);
                    ((TextView) view).setTextSize(14);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private boolean saveDialogItem(BudgetItem existing, EditText nameInput, EditText amountInput, Spinner groupSpinner, Spinner typeSpinner, CheckBox fixedCheck, CheckBox internalCheck) {
        String name = nameInput.getText().toString().trim();
        String rawAmount = amountInput.getText().toString().trim().replace(',', '.');
        if (name.length() == 0) {
            Toast.makeText(this, "Nom obligatoire", Toast.LENGTH_SHORT).show();
            return false;
        }
        double amount;
        try {
            amount = Double.parseDouble(rawAmount);
        } catch (NumberFormatException exception) {
            Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show();
            return false;
        }

        BudgetItem target = existing;
        if (target == null) {
            target = new BudgetItem();
            target.id = "item_" + System.currentTimeMillis();
            items.add(target);
        }
        target.name = name;
        target.amount = amount;
        target.group = groupSpinner.getSelectedItem().toString();
        target.type = typeSpinner.getSelectedItem().toString();
        target.fixed = fixedCheck.isChecked();
        target.internalTransfer = internalCheck.isChecked();
        saveItems();
        renderAll();
        return true;
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Recharger le modèle")
                .setMessage("Cela remplace les lignes actuelles par le budget exact Cheikh / Madame Gomis. Continuer ?")
                .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        seedDefaults();
                        saveItems();
                        renderAll();
                    }
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void loadItems() {
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        int schema = preferences.getInt(KEY_SCHEMA, 0);
        String json = preferences.getString(KEY_ITEMS, null);
        if (schema < CURRENT_SCHEMA || json == null || json.trim().length() == 0) {
            seedDefaults();
            saveItems();
            return;
        }
        items.clear();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                BudgetItem item = new BudgetItem();
                item.id = object.optString("id", "item_" + i);
                item.name = object.optString("name", "Sans nom");
                item.group = object.optString("group", GROUP_COMMON);
                item.type = object.optString("type", TYPE_EXPENSE);
                item.amount = object.optDouble("amount", 0);
                item.fixed = object.optBoolean("fixed", true);
                item.internalTransfer = object.optBoolean("internalTransfer", false);
                items.add(item);
            }
        } catch (JSONException exception) {
            seedDefaults();
            saveItems();
        }
    }

    private void saveItems() {
        JSONArray array = new JSONArray();
        for (BudgetItem item : items) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", item.id);
                object.put("name", item.name);
                object.put("group", item.group);
                object.put("type", item.type);
                object.put("amount", item.amount);
                object.put("fixed", item.fixed);
                object.put("internalTransfer", item.internalTransfer);
                array.put(object);
            } catch (JSONException ignored) { }
        }
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        editor.putString(KEY_ITEMS, array.toString());
        editor.putInt(KEY_SCHEMA, CURRENT_SCHEMA);
        editor.putBoolean(KEY_DARK, darkMode);
        editor.apply();
    }

    private void seedDefaults() {
        items.clear();
        addDefault("salaire_cheikh", "Salaire", GROUP_CHEIKH, TYPE_INCOME, 1734, true, false);
        addDefault("navigo", "Navigo", GROUP_CHEIKH, TYPE_EXPENSE, 91, true, false);
        addDefault("orange", "Orange", GROUP_CHEIKH, TYPE_EXPENSE, 53, true, false);
        addDefault("red_sfr_cheikh", "RED SFR", GROUP_CHEIKH, TYPE_EXPENSE, 13, true, false);
        addDefault("assurance_vie", "Assurance-vie", GROUP_CHEIKH, TYPE_EXPENSE, 30, true, false);
        addDefault("maison_cheikh", "Maison", GROUP_CHEIKH, TYPE_EXPENSE, 850, true, false);
        addDefault("virement_a_madame", "Virement à Mme Gomis", GROUP_CHEIKH, TYPE_EXPENSE, 112, true, true);

        addDefault("salaire_meouba", "Salaire", GROUP_MEOUBA, TYPE_INCOME, 2300, true, false);
        addDefault("caf", "CAF", GROUP_MEOUBA, TYPE_INCOME, 534, true, false);
        addDefault("virement_cheikh", "Virement Cheikh", GROUP_MEOUBA, TYPE_INCOME, 112, true, true);
        addDefault("autre_revenu", "Autre revenu", GROUP_MEOUBA, TYPE_INCOME, 194, true, false);
        addDefault("impots_taxes", "Impôts / taxes", GROUP_MEOUBA, TYPE_EXPENSE, 199, true, false);
        addDefault("assurance_60", "Assurance", GROUP_MEOUBA, TYPE_EXPENSE, 60, true, false);
        addDefault("college", "Collège", GROUP_MEOUBA, TYPE_EXPENSE, 230, true, false);
        addDefault("creche", "Crèche", GROUP_MEOUBA, TYPE_EXPENSE, 180, true, false);
        addDefault("epargne", "Épargne", GROUP_MEOUBA, TYPE_EXPENSE, 100, true, false);
        addDefault("essence", "Essence", GROUP_MEOUBA, TYPE_EXPENSE, 220, true, false);
        addDefault("assurance_230", "Assurance", GROUP_MEOUBA, TYPE_EXPENSE, 230, true, false);
        addDefault("maison_meouba", "Maison", GROUP_MEOUBA, TYPE_EXPENSE, 269, true, false);
        addDefault("electricite", "Électricité", GROUP_MEOUBA, TYPE_EXPENSE, 145, true, false);
    }

    private void addDefault(String id, String name, String group, String type, double amount, boolean fixed, boolean internalTransfer) {
        BudgetItem item = new BudgetItem();
        item.id = id;
        item.name = name;
        item.group = group;
        item.type = type;
        item.amount = amount;
        item.fixed = fixed;
        item.internalTransfer = internalTransfer;
        items.add(item);
    }

    private double sum(String group, String type, boolean includeInternal) {
        double total = 0;
        for (BudgetItem item : items) {
            if (group.equals(item.group) && type.equals(item.type) && (includeInternal || !item.internalTransfer)) total += item.amount;
        }
        return total;
    }

    private double sumAll(String type, boolean includeInternal) {
        double total = 0;
        for (BudgetItem item : items) {
            if (type.equals(item.type) && (includeInternal || !item.internalTransfer)) total += item.amount;
        }
        return total;
    }

    private double sumInternal(String type) {
        double total = 0;
        for (BudgetItem item : items) {
            if (type.equals(item.type) && item.internalTransfer) total += item.amount;
        }
        return total;
    }

    private String shortGroupName(String group) {
        if (GROUP_CHEIKH.equals(group)) return "M. Mboup";
        if (GROUP_MEOUBA.equals(group)) return "Mme Gomis";
        if (GROUP_CREDIT.equals(group)) return "Frais / crédits";
        return group;
    }

    private String money(double value) {
        if (Math.abs(value - Math.round(value)) < 0.005) return String.format(Locale.FRANCE, "%.0f €", value);
        return String.format(Locale.FRANCE, "%.2f €", value);
    }

    private String moneyCompact(double value) {
        return money(value);
    }

    private int indexOf(String[] array, String value) {
        if (value == null) return 0;
        for (int i = 0; i < array.length; i++) if (array[i].equals(value)) return i;
        return 0;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable round(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private void styleButton(Button button, int background, int text) {
        button.setTextColor(text);
        button.setBackground(round(background, colors.line, 12));
        button.setAllCaps(false);
    }

    private static class BudgetItem {
        String id;
        String name;
        String group;
        String type;
        double amount;
        boolean fixed;
        boolean internalTransfer;
    }

    private static class Palette {
        int page, card, row, soft, text, muted, line, green, red, blue;
        static Palette dark() {
            Palette p = new Palette();
            p.page = Color.rgb(5, 8, 22);
            p.card = Color.rgb(16, 24, 39);
            p.row = Color.rgb(22, 32, 51);
            p.soft = Color.rgb(31, 41, 55);
            p.text = Color.rgb(248, 250, 252);
            p.muted = Color.rgb(167, 176, 192);
            p.line = Color.rgb(38, 50, 71);
            p.green = Color.rgb(34, 197, 94);
            p.red = Color.rgb(248, 113, 113);
            p.blue = Color.rgb(96, 165, 250);
            return p;
        }
        static Palette light() {
            Palette p = new Palette();
            p.page = Color.rgb(244, 246, 248);
            p.card = Color.WHITE;
            p.row = Color.rgb(248, 250, 252);
            p.soft = Color.rgb(229, 231, 235);
            p.text = Color.rgb(24, 32, 42);
            p.muted = Color.rgb(102, 112, 133);
            p.line = Color.rgb(208, 213, 221);
            p.green = Color.rgb(22, 138, 91);
            p.red = Color.rgb(180, 35, 24);
            p.blue = Color.rgb(29, 78, 216);
            return p;
        }
    }
}
