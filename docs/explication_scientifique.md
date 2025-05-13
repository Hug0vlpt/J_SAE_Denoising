# Débruitage d'Images par Analyse en Composantes Principales

## 1. Introduction

Le débruitage d'images est un problème fondamental en traitement d'images, particulièrement important dans des domaines comme l'imagerie médicale, la photographie astronomique, ou la restauration de documents historiques. Notre projet propose une approche basée sur l'Analyse en Composantes Principales (ACP) appliquée à des patchs d'image, une méthode qui combine efficacité computationnelle et qualité de résultats.

## 2. Le Bruit dans les Images Numériques

### 2.1 Modèle de Bruit Gaussien
Dans notre contexte, nous considérons le modèle de bruit gaussien additif :

$X_b(i,j) = X_0(i,j) + n(i,j)$

où :
- $X_b$ est l'image bruitée
- $X_0$ est l'image originale
- $n(i,j) \sim \mathcal{N}(0,\sigma^2)$ est le bruit gaussien

Ce modèle est particulièrement pertinent car :
- Il approxime bien de nombreux types de bruit réels
- Il a des propriétés mathématiques bien comprises
- Il est facile à simuler pour les tests

## 3. Approche par Patchs

### 3.1 Principe
Au lieu de traiter l'image entière, nous la découpons en petits carrés appelés "patchs" :
- Taille typique : $7 \times 7$ pixels
- Chevauchement possible pour une meilleure reconstruction
- Traitement local qui capture mieux les structures fines

### 3.2 Extraction des Patchs
Deux stratégies possibles :
1. **Extraction globale** : parcours systématique de l'image
2. **Extraction locale** : focus sur des régions spécifiques

### 3.3 Vectorisation
Chaque patch $P_k \in \mathbb{R}^{s \times s}$ est transformé en vecteur :
$v_k = \text{Vec}(P_k) \in \mathbb{R}^{s^2}$

## 4. Analyse en Composantes Principales (ACP)

### 4.1 Principe Mathématique
L'ACP cherche à représenter les données dans une nouvelle base qui :
- Maximise la variance dans les premières directions
- Minimise la redondance entre les composantes
- Permet une réduction de dimension efficace

### 4.2 Étapes de l'ACP
1. **Centrage des données** :
   $m_V = \frac{1}{M} \sum_{k=1}^M v_k$
   $v_k^c = v_k - m_V$

2. **Calcul de la matrice de covariance** :
   $\Gamma = \frac{1}{M} \sum_{k=1}^M v_k^c (v_k^c)^T$

3. **Diagonalisation** :
   $\Gamma u_i = \lambda_i u_i$
   où $\lambda_i$ sont les valeurs propres et $u_i$ les vecteurs propres

4. **Projection** :
   $\alpha_i^{(k)} = u_i^T(v_k - m_V)$

### 4.3 Filtrage du Bruit
- Les premières composantes principales capturent le signal
- Les dernières composantes contiennent principalement du bruit
- Le filtrage se fait par seuillage des coefficients $\alpha_i^{(k)}$

## 5. Reconstruction de l'Image

### 5.1 Reconstruction des Patchs
Pour chaque patch :
$\hat{v}_k = m_V + \sum_{i=1}^{s^2} \alpha_i^{(k)} u_i$

### 5.2 Assemblage Final
- Retransformation des vecteurs en patchs
- Moyenne pondérée dans les zones de chevauchement
- Lissage des transitions entre patchs

## 6. Aspects Éco-responsables

### 6.1 Efficacité Computationnelle
- Traitement par patchs : parallélisation possible
- Réduction de dimension : moins de calculs
- Optimisation mémoire : traitement local

### 6.2 Consommation Énergétique
- Algorithme optimisé pour minimiser les calculs
- Possibilité de traitement par lots
- Compromis qualité/performance ajustable

## 7. Validation et Métriques

### 7.1 Métriques Objectives
- PSNR (Peak Signal-to-Noise Ratio)
- SSIM (Structural Similarity Index)
- MSE (Mean Square Error)

### 7.2 Validation Visuelle
- Comparaison avec l'image originale
- Évaluation des artefacts
- Préservation des détails fins

## Bibliographie

1. Deledalle, C., Salmon, J., & Dalalyan, A. (2011). Image denoising with patch based PCA: local versus global.
2. Donoho, D. L., & Johnstone, I. M. (1995). Adapting to unknown smoothness via wavelet shrinkage.
3. Grace Chang, S., Yu, B., & Vetterli, M. (2000). Adaptive wavelet thresholding for image denoising and compression.
4. Barry, D. (2018). Débruitage par filtrage spatio-fréquentiel. 