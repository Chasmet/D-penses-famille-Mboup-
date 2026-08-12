# Dépenses famille Mboup

Application Android native Java pour suivre les revenus, dépenses personnelles et charges communes de la famille.

## Objectif

- Deux colonnes principales côte à côte : **Monsieur Mboup** et **Madame Gomis**.
- Colonnes complémentaires : **Commun** et **Autres frais / crédits**.
- Saisie manuelle des montants comme dans un tableau Excel.
- Totaux automatiques : revenus, dépenses personnelles, charges communes, reste personnel et reste famille.
- Données sauvegardées localement sur le téléphone avec `SharedPreferences`.

## Compilation APK

Le workflow GitHub Actions `Android APK` compile automatiquement l'APK debug à chaque push sur `main`.

Commande locale Linux/macOS :

```bash
chmod +x gradlew
./gradlew assembleDebug
```

APK généré :

```text
app/build/outputs/apk/debug/app-debug.apk
```
