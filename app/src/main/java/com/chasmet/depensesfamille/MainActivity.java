package com.chasmet.depensesfamille;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
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
        tableContainer.addView(createTable(GROUP_CHEIKH));
        tableContainer.addView(createTable(GROUP_MEOUBA));
        tableContainer.addView(createTable(GROUP_COMMON));
        tableContainer.addView(createTable(GROUP_CREDIT));
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
        sb.append("Revenus famille : ").append(money(totalIncome)).append("\n");
        sb.append("Dépenses famille : ").append(money(totalExpense)).append("\n");
        sb.append("Charges communes + crédits : ").append(money(expenseCommon + expenseCredit)).append("\n");
        sb.append("Part commune par personne : ").append(money(commonShare)).append("\n\n");
        sb.append("Reste Cheikh : ").append(money(balanceCheikh)).append("\n");
        sb.append("Reste Méouba : ").append(money(balanceMeouba)).append("\n");
        sb.append("Reste final famille : ").append(money(familyBalance));
        summaryText.setText(sb.toString());

        if (familyBalance < 0) {
            summaryText.setTextColor(Color.parseColor("#B42318"));
        } else {
            summaryText.setTextColor(Color.parseColor("#18202A"));
        }
    }

    private LinearLayout createTable(final String group) {
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackgroundResource(R.drawable.card_background);
        table.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout.LayoutParams tableParams = new LinearLayout.LayoutParams(dp(292), ViewGroup.LayoutParams.WRAP_CONTENT);
        tableParams.setMargins(0, 0, dp(10), 0);
        table.setLayoutParams(tableParams);

        TextView title = new TextView(this);
        title.setText(group);
        title.setTextColor(Color.parseColor("#18202A"));
        title.setTextSize(18);
        title.setTypeface(null, 1);
        table.addView(title);

        TextView total = new TextView(this);
        total.setText(buildGroupTotalText(group));
        total.setTextColor(Color.parseColor("#667085"));
        total.setTextSize(13);
        total.setPadding(0, dp(3), 0, dp(8));
        table.addView(total);

        for (BudgetItem item : items) {
            if (group.equals(item.group)) {
                table.addView(createBudgetRow(item));
            }
        }

        Button addHere = new Button(this);
        addHere.setText("+ Ligne ici");
        addHere.setAllCaps(false);
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
        double income = sum(group, TYPE_INCOME);
        double expense = sum(group, TYPE_EXPENSE);
        double balance = income - expense;
        return "Entrées : " + money(income) + "  |  Sorties : " + money(expense) + "  |  Solde : " + money(balance);
    }

    private View createBudgetRow(final BudgetItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(8), dp(8), dp(8));
        row.setBackgroundColor(Color.parseColor("#F8FAFC"));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(rowParams);

        TextView name = new TextView(this);
        name.setText(item.name + (item.fixed ? "  • fixe" : ""));
        name.setTextColor(Color.parseColor("#18202A"));
        name.setTextSize(14);
        name.setSingleLine(false);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(name);

        TextView amount = new TextView(this);
        amount.setText(money(item.amount));
        amount.setTextSize(14);
        amount.setTypeface(null, 1);
        amount.setTextColor(TYPE_INCOME.equals(item.type) ? Color.parseColor("#168A5B") : Color.parseColor("#B42318"));
        amount.setGravity(android.view.Gravity.END);
        amount.setLayoutParams(new LinearLayout.LayoutParams(dp(92), ViewGroup.LayoutParams.WRAP_CONTENT));
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

        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, GROUPS);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TYPES);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
                .setPositiveButton("Enregistrer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        saveDialogItem(existing, nameInput, amountInput, groupSpinner, typeSpinner, fixedCheck);
                    }
                })
                .setNegativeButton("Annuler", null);

        if (existing != null) {
            builder.setNeutralButton("Supprimer", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    items.remove(existing);
                    saveItems();
                    renderAll();
                }
            });
        }

        builder.show();
    }

    private void saveDialogItem(BudgetItem existing, EditText nameInput, EditText amountInput, Spinner groupSpinner, Spinner typeSpinner, CheckBox fixedCheck) {
        String name = nameInput.getText().toString().trim();
        String rawAmount = amountInput.getText().toString().trim().replace(',', '.');

        if (name.length() == 0) {
            Toast.makeText(this, "Nom obligatoire", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(rawAmount);
        } catch (NumberFormatException exception) {
            Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show();
            return;
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
                // JSONObject avec valeurs simples ne doit pas échouer ici.
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
        addDefault("taxe_fonciere", "Taxe foncière mensualisée", GROUP_COMMON, TYPE_EXPENSE, 188.00, true);
        addDefault("epargne_securite", "Épargne sécurité", GROUP_CREDIT, TYPE_EXPENSE, 100.00, false);
        addDefault("autre_credit", "Autre crédit à ajouter", GROUP_CREDIT, TYPE_EXPENSE, 0.00, true);
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

    private String money(double value) {
        return String.format(Locale.FRANCE, "%.2f €", value);
    }

    private int indexOf(String[] values, String value) {
        if (value == null) {
            return 0;
        }
        for (int i = 0; i < values.length; i++) {
            if (value.equals(values[i])) {
                return i;
            }
        }
        return 0;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
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
