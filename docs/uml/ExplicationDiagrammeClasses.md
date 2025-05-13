# **Architecture du module de débruitage**

Ce document présente en détail l'architecture logicielle du système de débruitage d'images que nous avons développée. L'explication s'appuie sur le diagramme de classes UML du projet.

## **Vue d'ensemble**

L'architecture adopte une approche **pipeline** : l'image d'entrée est d'abord décomposée en sous‑blocs (patchs), lesquels subissent une série d'opérations de transformation, de seuillage et de reconstruction. Chaque étape est portée par une classe ou un groupe de classes spécialisées afin de respecter le principe de responsabilité unique.

Image  →  PatchExtractor  →  PCAEngine  →  ThreshHolding  →  Reconstruction  →  Image débruitée

## **Description des principales classes**

| Classe | Rôle | Attributs clés | Méthodes significatives |
| ----- | ----- | ----- | ----- |
| **Image** | Représente une matrice de pixels et fournit l'I/O ainsi que les métriques de qualité. | `data`, `height`, `width` | `loadImage()`, `saveImage()`, `getMSE()`, `getPSNR()` |
| **Patch** | Bloc élémentaire extrait de *Image*. | `data`, `upperLeftCornerX`, `upperLeftCornerY`, `height`, `width` | `vectorize()`, `unvectorize()`, `getHeight()`, `getWidth()` |
| **Imagette** | Sous‑image structurante regroupant plusieurs patchs. | `upperLeftCornerX`, `upperLeftCornerY`, `dataImagette` | `getCorner()`, `getWidth()`, `getHeight()` |
| **PatchExtractor** | Fabrique de patchs ; définit l'API d'extraction/reconstruction. | `patchHeight`, `patchWidth` | `extractPatches()`, `reconstructPatches()` |
| **GlobalPatchExtractor** | Implémentation exhaustive balayant toute l'image. | – | `extractPatches()`, `reconstructPatches()` |
| **LocalPatchExtractor** | Découpe locale à l'intérieur d'une *Imagette*. | `windowHeight`, `windowWidth` | `extractPatches()`, `reconstructPatches()`, `decoupeImage()` |
| **ImageDenoiser** | Point d'orchestration du pipeline complet. | `patchSize`, `windowSize`, `sigma`, etc. | `denoise()`, `processPatches()`, `ApplyThresholding()` |
| **PCAEngine** | Effectue la réduction dimensionnelle des vecteurs de patchs via PCA/SVD. | `meanVector`, `covMatrix`, `eigenVectors`, `eigenValues`, `USVD`, `SIGMA`, `VTSVD` | `computePCA()`, `project()`, `getEigenValues()`, `getEigenVectors()`, `getMeanVector()` |
| **ThreshHolding** | Calcule le seuil optimal et applique un seuillage souple ou dur. | `sigma` | `computeVisuShrinkThreshold()`, `computeBayesVisuShrinkThreshold()`, `softThreshHolding()`, `hardThreshHolding()` |
| **Metrics** | Classe statique qui calcule les indicateurs de performance post‑traitement. | – | `computeMSE()`, `computePSNR()` |

## **Relations entre classes**

### **Généralisation**

`PatchExtractor` est la super‑classe abstraite de deux implémentations :

* **GlobalPatchExtractor** : balayage intégral de l'image, traitant tous les patchs de l'image en une seule fois

* **LocalPatchExtractor** : balayage restreint à une fenêtre (*Imagette*), traitant l'image zone par zone

Cette hiérarchie permet d'étendre le système avec de nouvelles stratégies d'extraction sans modifier le code existant (**principe ouvert/fermé**).

### **Composition et utilisation**

* **Image** utilise **Patch** pour son découpage en sous-blocs
* **Imagette** utilise **Patch** pour représenter ses sous-composants
* **PatchExtractor** crée et manipule des objets **Patch**
* **ImageDenoiser** utilise **PatchExtractor**, **PCAEngine**, **ThreshHolding** et **Metrics** pour orchestrer le pipeline de débruitage

Ces relations favorisent l'injection ou le remplacement de composants, conformément au principe de dépendance faible (*loose coupling*).

## **Scénario de fonctionnement**

1. **Chargement** : `Image.loadImage()` lit l'image bruitée.

2. **Extraction** : le `PatchExtractor` découpe l'image en *Patch* (et éventuellement en *Imagette* pour l'extracteur local).

3. **Vectorisation** : chaque *Patch* est transformé en vecteur avec la méthode `vectorize()`.

4. **PCA** : `PCAEngine.computePCA()` projette les vecteurs dans un espace de plus faible dimension, concentrant l'information utile.

5. **Seuillage** : la classe `ThreshHolding` calcule un seuil (VisuShrink, BayesShrink, etc.) et applique un seuillage (soft/hard) pour supprimer le bruit.

6. **Reconstruction** : les patchs filtrés sont dé‑vectorisés puis placés à leur position d'origine (`PatchExtractor.reconstructPatches()`).

7. **Évaluation** : `Metrics.computeMSE()` et `Metrics.computePSNR()` mesurent la qualité du résultat.

8. **Sauvegarde** : `Image.saveImage()` enregistre l'image débruitée.

## **Conformité aux principes SOLID**

* **S**ingle Responsibility : chaque classe a un rôle métier clairement délimité.

* **O**pen/Closed : la hiérarchie `PatchExtractor` permet d'ajouter des stratégies sans modifier le code client.

* **L**iskov Substitution : les extracteurs spécialisés respectent le contrat de la super‑classe.

* **I**nterface Segregation : les classes clients ne dépendent que des méthodes qu'elles utilisent.

* **D**ependency Inversion : `ImageDenoiser` dépend d'abstractions (interfaces) pour l'extraction et le seuillage.

## **Perspectives d'amélioration**

* **Gestion des exceptions** : implémenter un mécanisme robuste de gestion des erreurs, en particulier pour les opérations I/O.

* **Parallélisation** : explorer les opportunités de traitement parallèle pour l'extraction et le traitement des patchs.

* **Interface utilisateur** : développer une interface graphique pour visualiser le processus de débruitage étape par étape.

* **Métriques avancées** : intégrer des indicateurs supplémentaires de qualité d'image (SSIM, etc.).

* **Persistance des configurations** : permettre la sauvegarde et le chargement de paramètres de débruitage optimaux.

---

Ce document fournit une base complète pour comprendre la logique logicielle de notre module de débruitage et justifie les choix architecturaux retenus.

