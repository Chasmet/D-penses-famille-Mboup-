package com.chasmet.depensesfamille;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

    private static final String GROUP_CHEIKH = "Monsieur Mboup";
    private static final String GROUP_MEOUBA = "Madame Gomis";
    private static final String GROUP_COMMON = "Commun";
    private static final String GROUP_CREDIT = "Autres frais / crédits";

    private static final String TYPE_INCOME = "Salaire / entrée";
    private static final String TYPE_EXPENSE = "Dépense";

    private static final int COLOR_PAGE = Color.rgb(5, 8, 22);
    private static final int COLOR_CARD = Color.rgb(16, 24, 39);
    private static final int COLOR_ROW = Color.rgb(22, 32, 51);
    private static final int COLOR_TEXT = Color.rgb(248, 250, 252);
    private static final int COLOR_MUTED = Color.rgb(167, 176, 192);
    private static final int COLOR_GREEN = Color.rgb(34, 197, 94);
    private static final int COLOR_RED = Color.rgb(248, 113, 113);
    private static final int COLOR_BLUE = Color.rgb(96, 165, 250);

    private static final String[] GROUPS = new String[]{
            GROUP_CHEIKH,
            GROUP_MEOUBA,
            GROUP_COMMON,
            GROUP_CREDIT
    };

    private static final String[] TYPES = new String[]{
            TYPE_EXPENSE,
            TYPE_INCOME
    };

    private final List<BudgetItem> items = new ArrayList<>();
    private LinearLayout tableContainer;
    private TextView summaryText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tableContainer = findViewById(R.id.tableContainer);
        summaryText = findViewById(R.id.summaryText);
        Button addButton = findViewById(R.id.addButton);
        Button resetButton = findViewById(R.id.resetButton);

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
    }

    private void renderAll() {
        renderSummary();
        tableContainer.removeAllViews();
        tableContainer.addView(createTwoColumnRow(GROUP_CHEIKH, GROUP_MEOUBA));
        tableContainer.addView(createTwoColumnRow(GROUP_COMMON, GROUP_CREDIT));
    }

    private LinearLayout createTwoColumnRow(String leftGroup, String rightGroup) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(2f);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowParams);
        row.addView(createTable(leftGroup, true));
        row.addView(createTable(rightGroup, false));
        return row;
    }

    private void renderSummary() {
        double incomeCheikh = sum(GROUP_CHEIKH, TYPE_INCOME);
        double incomeMeouba = sum(GROUP_MEOUBA, TYPE_INCOME);
        double expenseCheikh = sum(GROUP_CHEIKH, TYPE_EXPENSE);
        double expenseMeouba = sum(GROUP_MEOUBA, TYPE_EXPENSE);
        double expenseCommon = sum(GROUP_COMMON, TYPE_EXPENSE);
        double expenseCredit = sum(GROUP_CREDIT, TYPE_EXPENSE);
        double totalIncome = sumAll(TYPE_INCOME);
        double totalExpense = sumAll(TYPE_EXPENSE);
        double commonShare = (expenseCommon + expenseCredit) / 2.0;
        double balanceCheikh = incomeCheikh - expenseCheikh - commonShare;
        double balanceMeouba = incomeMeouba - expenseMeouba - commonShare;
        double familyBalance = totalIncome - totalExpense;

        StringBuilder sb = new StringBuilder();
        sb.append("Revenus ").append(moneyCompact(totalIncome));
        sb.append("  •  Dépenses ").append(moneyCompact(totalExpense)).append("\n");
        sb.append("Commun + crédits ").append(moneyCompact(expenseCommon + expenseCredit));
        sb.append("  •  Part chacun ").append(moneyCompact(commonShare)).append("\n");
        sb.append("Cheikh ").append(moneyCompact(balanceCheikh));
        sb.append("  |  Méouba ").append(moneyCompact(balanceMeouba)).append("\n");
        sb.append("Final famille : ").append(money(familyBalance));
        summaryText.setText(sb.toString());
        summaryText.setTextColor(familyBalance < 0 ? COLOR_RED : COLOR_TEXT);
    }

    private LinearLayout createTable(final String group, boolean left) {
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackgroundResource(R.drawable.card_background);
        table.setPadding(dp(6), dp(7), dp(6), dp(7));

        LinearLayout.LayoutParams tableParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        if (left) {
            tableParams.setMargins(0, 0, dp(4), 0);
        } else {
            tableParams.setMargins(dp(4), 0, 0, 0);
        }
        table.setLayoutParams(tableParams);

        TextView title = new TextView(this);
        title.setText(shortGroupName(group));
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(14);
        title.setTypeface(null, 1);
        title.setSingleLine(false);
        table.addView(title);

        TextView total = new TextView(this);
        total.setText(buildGroupTotalText(group));
        total.setTextColor(COLOR_MUTED);
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
        addHere.setTextColor(COLOR_TEXT);
        addHere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBudgetDialog(null, group);
            }
        });
        table.addView(addHere);

        return table;
    }

    private String shortGroupName(String group) {
        if (GROUP_CHEIKH.equals(group)) {
            return "M. Mboup";
        }
        if (GROUP_MEOUBA.equals(group)) {
            return "Mme Gomis";
        }
        if (GROUP_CREDIT.equals(group)) {
            return "Frais / crédits";
        }
        return group;
    }

    private String buildGroupTotalText(String group) {
        double income = sum(group, TYPE_INCOME);
        double expense = sum(group, TYPE_EXPENSE);
        double balance = income - expense;
        return "+ " + moneyCompact(income) + "\n- " + moneyCompact(expense) + "\n= " + moneyCompact(balance);
    }

    private View createBudgetRow(final BudgetItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(5), dp(5), dp(5), dp(5));
        row.setBackgroundColor(COLOR_ROW);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(5));
        row.setLayoutParams(rowParams);

        TextView name = new TextView(this);
        name.setText(item.name + (item.fixed ? " • fixe" : ""));
        name.setTextColor(COLOR_TEXT);
        name.setTextSize(11);
        name.setSingleLine(false);
        row.addView(name);

        TextView amount = new TextView(this);
        amount.setText(moneyCompact(item.amount));
        amount.setTextSize(12);
        amount.setTypeface(null, 1);
        amount.setTextColor(TYPE_INCOME.equals(item.type) ? COLOR_GREEN : COLOR_RED);
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

        ArrayAdapter<String> groupAdapter = createDarkSpinnerAdapter(GROUPS);
        groupSpinner.setAdapter(groupAdapter);

        ArrayAdapter<String> typeAdapter = createDarkSpinnerAdapter(TYPES);
        typeSpinner.setAdapter(typeAdapter);

        if (existing != null) {
            nameInput.setText(existing.name);
            amountInput.setText(String.format(Locale.FRANCE, "%.2f", existing.amount));
            groupSpinner.setSelection(indexOf(GROUPS, existing.group));
            typeSpinner.setSelection(indexOf(TYPES, existing.type));
            fixedCheck.setChecked(existing.fixed);
        } else {
            groupSpinner.setSelection(indexOf(GROUPS, forcedGroup == null ? GROUP_COMMON : forcedGroup));
            typeSpinner.setSelection(indexOf(TYPES, TYPE_EXPENSE));
            fixedCheck.setChecked(true);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Ajouter une ligne" : "Modifier la ligne")
                .setView(content)
                .setNegativeButton("Annuler", null);

        if (existing != null) {
            builder.setNeutralButton("Supprimer", null);
        }

        builder.setPositiveButton("Enregistrer", null);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(COLOR_CARD));
                }
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(COLOR_GREEN);
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(COLOR_MUTED);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (saveDialogItem(existing, nameInput, amountInput, groupSpinner, typeSpinner, fixedCheck)) {
                            dialog.dismiss();
                        }
                    }
                });
                if (existing != null) {
                    dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(COLOR_RED);
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

    private ArrayAdapter<String> createDarkSpinnerAdapter(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(COLOR_TEXT);
                    textView.setTextSize(13);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(Color.BLACK);
                    textView.setTextSize(14);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private boolean saveDialogItem(BudgetItem existing, EditText nameInput, EditText amountInput, Spinner groupSpinner, Spinner typeSpinner, CheckBox fixedCheck) {
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

        saveItems();
        renderAll();
        return true;
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Recharger le modèle")
                .setMessage("Cela remplace les lignes actuelles par le modèle de départ. Continuer ?")
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
        String json = preferences.getString(KEY_ITEMS, null);
        if (json == null || json.trim().length() == 0) {
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
                array.put(object);
            } catch (JSONException ignored) {
                // Valeurs simples uniquement.
            }
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    private void seedDefaults() {
        items.clear();
        addDefault("salaire_cheikh", "Salaire Cheikh", GROUP_CHEIKH, TYPE_INCOME, 1900.00, true);
        addDefault("salaire_meouba", "Salaire Méouba", GROUP_MEOUBA, TYPE_INCOME, 2400.00, true);
        addDefault("credit_maison", "Crédit maison", GROUP_COMMON, TYPE_EXPENSE, 1071.00, true);
        addDefault("mutuelle", "Mutuelle", GROUP_COMMON, TYPE_EXPENSE, 0.00, true);
        addDefault("pass_navigo_cheikh", "Pass Navigo Cheikh", GROUP_CHEIKH, TYPE_EXPENSE, 88.00, true);
        addDefault("assurance_habitation", "Assurance habitation", GROUP_COMMON, TYPE_EXPENSE, 0.00, true);
        addDefault("assurance_voiture", "Assurance voiture", GROUP_COMMON, TYPE_EXPENSE, 145.00, true);
        addDefault("ecole_nelvyn", "École Nelvyn", GROUP_COMMON, TYPE_EXPENSE, 0.00, true);
        addDefault("ecole_yvane", "École Yvane", GROUP_COMMON, TYPE_EXPENSE, 0.00, true);
        addDefault("box_orange", "Box Orange", GROUP_COMMON, TYPE_EXPENSE, 50.00, true);
        addDefault("sfr_red_yvane", "SFR / RED Yvane", GROUP_COMMON, TYPE_EXPENSE, 7.99, true);
        addDefault("red_cheikh", "RED Cheikh", GROUP_CHEIKH, TYPE_EXPENSE, 0.00, true);
        addDefault("red_meouba", "RED Méouba", GROUP_MEOUBA, TYPE_EXPENSE, 0.00, true);
        addDefault("engie", "Électricité Engie", GROUP_COMMON, TYPE_EXPENSE, 160.00, true);
        addDefault("nourriture", "Nourriture / courses", GROUP_COMMON, TYPE_EXPENSE, 600.00, false);
        addDefault("carburant", "Carburant", GROUP_COMMON, TYPE_EXPENSE, 250.00, false);
        addDefault("taxe_fonciere", "Taxe foncière", GROUP_COMMON, TYPE_EXPENSE, 188.00, true);
        addDefault("epargne_securite", "Épargne sécurité", GROUP_CREDIT, TYPE_EXPENSE, 100.00, false);
        addDefault("autre_credit", "Autre crédit", GROUP_CREDIT, TYPE_EXPENSE, 0.00, true);
    }

    private void addDefault(String id, String name, String group, String type, double amount, boolean fixed) {
        BudgetItem item = new BudgetItem();
        item.id = id;
        item.name = name;
        item.group = group;
        item.type = type;
        item.amount = amount;
        item.fixed = fixed;
        items.add(item);
    }

    private double sum(String group, String type) {
        double total = 0;
        for (BudgetItem item : items) {
            if (group.equals(item.group) && type.equals(item.type)) {
                total += item.amount;
            }
        }
        return total;
    }

    private double sumAll(String type) {
        double total = 0;
        for (BudgetItem item : items) {
            if (type.equals(item.type)) {
                total += item.amount;
            }
        }
        return total;
    }

    private int indexOf(String[] array, String value) {
        if (value == null) {
            return 0;
        }
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private String money(double value) {
        return String.format(Locale.FRANCE, "%.2f €", value);
    }

    private String moneyCompact(double value) {
        return String.format(Locale.FRANCE, "%.0f€", value);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class BudgetItem {
        String id;
        String name;
        String group;
        String type;
        double amount;
        boolean fixed;
    }
}
