# Débruitage d'Images par ACP

Ce projet vise à implémenter une méthode de débruitage d'images basée sur l'Analyse en Composantes Principales (ACP) en utilisant une approche par patchs.

## Description

Le projet implémente une méthode de débruitage qui :
1. Découpe l'image en petits patchs
2. Applique l'ACP pour réduire le bruit
3. Reconstruit l'image à partir des patchs débruités
4. Évalue la qualité du résultat obtenu

## Structure du Projet

```
SAE-PCA-Denoising/
├── README.md                        # Ce fichier
├── docs/                            # Documentation
│   ├── uml/                         # Diagrammes UML et explications
│   │   ├── diagrammeClasses.png     # Diagramme de classes
│   │   ├── diagrammeUseCase.png     # Diagramme de cas d'utilisation
│   │   ├── diagrammeActivite.png    # Diagramme d'activité
│   │   ├── ExplicationDiagrammeClasses.md   # Explication détaillée des classes
│   │   ├── ExplicationDiagrammeUseCase.md   # Explication des cas d'utilisation
│   │   └── ExplicationDiagrammeActivite.md  # Explication du flux d'activité
│   ├── planning/                    # Organisation du projet
│   │   ├── taches_groupe.md         # Répartition des tâches
│   │   └── risques.md               # Analyse des risques
│   ├── explication_scientifique.md  # Fondements mathématiques de l'ACP
│   └── gestion_images.md            # Documentation sur la manipulation d'images
├── images/                          # Base d'images
│   ├── train/                       # Images d'entraînement
│   │   ├── original/                # Images originales
│   │   └── noisy/                   # Images bruitées
│   └── test/                        # Images de test
│       ├── original/                # Images originales
│       └── noisy/                   # Images bruitées
└── meta.json                        # Métadonnées des images
```

## Installation

Pour obtenir une copie du projet :
```bash
git clone https://github.com/Alexkode/SAE-PCA-Denoising.git
```

## Approche technique

Le débruitage d'images par ACP repose sur les principes suivants :
1. **Extraction de patchs** : l'image est divisée en petits blocs qui se chevauchent
2. **Vectorisation** : chaque patch est transformé en vecteur
3. **ACP** : les vecteurs sont projetés dans une base orthonormale qui maximise la variance
4. **Seuillage** : les coefficients de faible amplitude (principalement du bruit) sont supprimés
5. **Reconstruction** : les patchs débruités sont replacés dans l'image

Pour plus de détails sur l'implémentation, consultez notre documentation dans le dossier `docs/`.

## Équipe

- Alexis - Chef de projet (Coordination générale, planning, suivi des deadlines, GitHub)
- Hugo - Développement (Implémentation Java, tests unitaires, revue de code)
- Juan-Manuel - Analyse (Aspects mathématiques, ACP, optimisation algorithmes)
- Clémentine - Documentation (Rédaction rapport, documentation technique, relecture)
- Angie - Documentation (Rédaction rapport, documentation technique, relecture)

## Licence

Ce projet est sous licence MIT - voir le fichier [LICENSE.md](LICENSE.md) pour plus de détails.