# Gestion de la Base d'Images

## 1. Structure des Répertoires

La base d'images est organisée selon la structure suivante :

```
images/
├── train/
│   ├── original/     # Images d'entraînement originales
│   └── noisy/        # Images d'entraînement bruitées
└── test/
    ├── original/     # Images de test originales
    └── noisy/        # Images de test bruitées
```

## 2. Convention de Nommage

### 2.1 Format Général
- Format : `[catégorie]_[id]_[paramètres].[extension]`
- Exemple : `nature_001_sigma15.png`

### 2.2 Composants du Nom
- **Catégorie** : Type d'image (nature, urban, portrait, etc.)
- **ID** : Numéro unique sur 3 chiffres
- **Paramètres** : Caractéristiques du bruit (si applicable)
- **Extension** : Format de l'image (png)

## 3. Métadonnées

Les métadonnées sont stockées dans `images/meta.json` avec la structure suivante :

```json
{
  "images": [
    {
      "id": "nature_001",
      "category": "nature",
      "size": [512, 512],
      "noise_params": {
        "type": "gaussian",
        "sigma": 15
      },
      "creation_date": "2024-03-15",
      "description": "Paysage forestier"
    }
  ]
}
```

## 4. Critères de Sélection des Images

### 4.1 Images d'Entraînement
- Résolution : 512×512 pixels
- Format : PNG 8-bit grayscale
- Variété de contenus et de textures
- Rapport signal/bruit optimal

### 4.2 Images de Test
- Indépendantes du set d'entraînement
- Représentatives des cas d'usage
- Complexité variable

## 5. Processus de Validation

### 5.1 Contrôle Qualité
- Vérification du format
- Validation des dimensions
- Contrôle des métadonnées

### 5.2 Tests Automatisés
- Vérification de l'intégrité
- Validation du format
- Contrôle des noms

## 6. Gestion des Versions

### 6.1 Versionnement
- Les modifications sont tracées dans Git
- Les fichiers `.keep` maintiennent la structure
- Les métadonnées incluent les versions

### 6.2 Sauvegarde
- Copies de sécurité régulières
- Stockage redondant
- Documentation des changements

## 7. Utilisation

### 7.1 Accès aux Images
```java
// Exemple de code d'accès
ImageLoader loader = new ImageLoader();
Image original = loader.load("nature_001.png");
Image noisy = loader.load("nature_001_sigma15.png");
```

### 7.2 Manipulation
```java
// Exemple de traitement
ImageProcessor processor = new ImageProcessor();
processor.validateFormat(image);
processor.applyNoise(image, noiseParams);
```

## 8. Évolution

### 8.1 Ajout d'Images
1. Respecter les conventions de nommage
2. Mettre à jour meta.json
3. Valider le format
4. Commiter les changements

### 8.2 Maintenance
- Nettoyage périodique
- Mise à jour des métadonnées
- Optimisation de l'espace 