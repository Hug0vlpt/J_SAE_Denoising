# Projet de Débruitage d'Images par ACP

Ce projet implémente des techniques de débruitage d'images utilisant l'Analyse en Composantes Principales (ACP) avec différentes approches et méthodes de seuillage.

## Table des matières
- [Présentation](#présentation)
- [Structure du projet](#structure-du-projet)
- [Installation](#installation)
- [Utilisation](#utilisation)
- [Paramètres configurables](#paramètres-configurables)
- [Concepts mathématiques](#concepts-mathématiques)
- [Approches implémentées](#approches-implémentées)
- [Métriques d'évaluation](#métriques-dévaluation)
- [Exemples de résultats](#exemples-de-résultats)

## Présentation

Ce projet implémente un algorithme de débruitage d'images basé sur l'Analyse en Composantes Principales (ACP). Deux approches principales sont proposées :

1. **Approche globale** : Traite l'image entière en une fois
2. **Approche locale** : Découpe l'image en petites zones (imagettes) traitées indépendamment

Pour chaque approche, différentes méthodes de seuillage sont disponibles :
- Seuillage dur avec seuil universel (VisuShrink)
- Seuillage doux avec BayesShrink

## Structure du projet

Le projet est organisé en plusieurs packages spécialisés :

- `app` : Point d'entrée de l'application et classes principales
- `image` : Classes pour la représentation et la manipulation d'images
- `patch` : Extraction et gestion des patches
- `pca` : Implémentation de l'Analyse en Composantes Principales
- `thresholding` : Fonctions et méthodes de seuillage

Chaque package contient des classes dédiées à une fonctionnalité spécifique, ce qui améliore la maintenabilité et la lisibilité du code.

## Installation

### Prérequis
- Java 8 ou supérieur
- ImageJ (inclus dans le dossier `lib/`)

### Compilation
Utilisez le script `build.sh` pour compiler le projet :

```bash
chmod +x build.sh
./build.sh
```

Ce script nettoie les fichiers `.class` existants et compile tous les fichiers Java dans les dossiers appropriés.

### Génération de la documentation
Pour générer la documentation du projet, utilisez le script `generate_docs.sh` :

```bash
chmod +x generate_docs.sh
./generate_docs.sh
```

La documentation sera générée dans le dossier `docs/`.

## Utilisation

### Exécution du programme

Après compilation, exécutez le programme avec la commande :

```bash
java -cp ./bin:./lib/ij.jar app.TestDenoising
```

Le programme traitera automatiquement les images situées dans le dossier `images/test/original/`.

### Structure des données

- Les images d'entrée doivent être placées dans `images/test/original/`
- Les résultats seront stockés dans un nouveau dossier `images/testXX/` où XX est un numéro incrémental
- Chaque dossier de test contient :
  - `original/` : Images originales
  - `noisy/` : Images bruitées avec différents niveaux de bruit
  - `global/hard_universal/` : Résultats du débruitage global avec seuillage dur
  - `global/soft_sure/` : Résultats du débruitage global avec seuillage doux
  - `local/imagettes/` : Imagettes extraites et traitées
  - `local/reconstructed/` : Images reconstruites à partir des imagettes

## Paramètres configurables

Vous pouvez modifier les paramètres suivants dans le code source :

- **Niveaux de bruit (σ)** : 
  - `ImageDenoiserPCA.SIGMA_1` (défaut: 10.0)
  - `ImageDenoiserPCA.SIGMA_2` (défaut: 20.0)
  - `ImageDenoiserPCA.SIGMA_3` (défaut: 30.0)

- **Taille des patches** : 
  - Approche globale : `patchSize` (défaut: 8x8 pixels)

- **Approche locale** :
  - Taille des imagettes : `W` (défaut: 32x32 pixels)
  - Nombre d'imagettes : `n` (défaut: 16)
  - Mode de couverture : 
    - `EXACT_N` : Extraction d'exactement N imagettes
    - `OVERLAP_FULL` : Couverture complète avec chevauchement
    - `NO_OVERLAP` : Couverture complète sans chevauchement

## Concepts mathématiques

### Analyse en Composantes Principales (ACP)

L'ACP est une technique de réduction de dimension qui projette des données à haute dimension dans un nouveau système de coordonnées où la variance est maximisée. Dans le contexte du débruitage d'images :

1. L'image est découpée en petits patches qui se chevauchent
2. Ces patches sont vectorisés (transformés en vecteurs)
3. La moyenne et la matrice de covariance sont calculées
4. Les vecteurs propres et valeurs propres de la matrice de covariance sont déterminés
5. Les patches sont projetés sur les vecteurs propres significatifs
6. Un seuillage est appliqué sur les coefficients
7. Les patches sont reconstruits et combinés pour former l'image débruitée

### Méthodes de seuillage

- **Seuillage dur** :
  ```
  f(x) = x si |x| > λ, 0 sinon
  ```
  
- **Seuillage doux** :
  ```
  f(x) = sign(x) * max(0, |x| - λ)
  ```

### Calcul des seuils

- **VisuShrink (seuil universel)** :
  ```
  λ = σ * sqrt(2 * log(N))
  ```
  où N est le nombre total de pixels et σ est l'écart-type du bruit.

- **BayesShrink** :
  ```
  λ = σ² / σy
  ```
  où σ² est la variance du bruit et σy est l'écart-type du signal sans bruit.

## Approches implémentées

### Approche globale
L'approche globale traite l'image dans son ensemble:
1. Extraction de patches (typiquement 8x8 pixels) avec chevauchement
2. Application de l'ACP sur tous les patches
3. Seuillage des coefficients (dur ou doux)
4. Reconstruction de l'image à partir des patches débruités

### Approche locale
L'approche locale divise l'image en sous-images (imagettes) et applique l'ACP indépendamment sur chacune:

1. **Modes de couverture**:
   - **Couverture complète avec chevauchement** (`OVERLAP_FULL`): Divise l'image en imagettes qui se chevauchent à 50%, couvrant la totalité de l'image
   - **Couverture complète sans chevauchement** (`NO_OVERLAP`): Divise l'image en imagettes sans chevauchement, et complète avec un chevauchement minimal pour couvrir les bords
   - **N imagettes exactes** (`EXACT_N`): Extrait exactement N imagettes, soit sur une grille régulière, soit aléatoirement

2. Pour chaque imagette:
   - Extraction de patches locaux
   - Application de l'ACP
   - Seuillage des coefficients
   - Reconstruction de l'imagette

3. Reconstruction de l'image complète:
   - Placement des imagettes débruitées à leurs positions d'origine
   - Moyenne des pixels dans les zones de chevauchement

## Métriques d'évaluation

Les performances sont mesurées avec deux métriques principales :

- **MSE (Mean Squared Error)** : Erreur quadratique moyenne entre l'image originale et l'image débruitée.
  ```
  MSE = (1/n) * Σ(X₀ - Xd)²
  ```

- **PSNR (Peak Signal-to-Noise Ratio)** : Rapport signal/bruit crête, en décibels (dB).
  ```
  PSNR = 10 * log₁₀(255² / MSE)
  ```

Plus le PSNR est élevé, meilleure est la qualité du débruitage.

## Exemples de résultats

Les performances varient selon les approches et les modes de couverture:

- **Approche globale avec seuillage dur**: Généralement les meilleurs résultats, avec un PSNR entre 20 et 30 dB selon les images et le niveau de bruit.

- **Approche locale**:
  - **Mode couverture complète avec chevauchement**: Excellents résultats, PSNR similaire à l'approche globale mais avec un temps de traitement réduit pour les grandes images.
  - **Mode N imagettes exactes**: Résultats variables selon la position des imagettes. Avec seulement 16 imagettes, le PSNR peut être significativement plus faible (~7-10 dB).

Les résultats visuels montrent que:
- Le seuillage dur préserve mieux les détails mais peut laisser des artefacts
- Le seuillage doux donne des résultats plus lisses mais peut estomper certains détails fins

## Documentation

La documentation complète du projet est disponible dans le dossier `docs/` :
- Théorie du bruit gaussien : `docs/theory/noise_theory.md`
- Théorie des patches : `docs/theory/patches_theory.md`
- Résultats et analyses : `docs/results/`

## Prochaines étapes
- Implémentation d'autres méthodes de seuillage (SURE, BLS-GSM)
- Optimisation des performances pour les grandes images
- Développement d'une interface graphique (IHM)
- Comparaison avec d'autres algorithmes de débruitage (ondelettes, filtrage bilatéral)
