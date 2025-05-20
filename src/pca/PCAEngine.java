package pca;

import java.util.ArrayList;
import java.util.List;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import thresholding.ThresholdingUtils;

/**
 * Implémentation de l'Analyse en Composantes Principales (ACP) pour le débruitage d'images.
 * 
 * L'ACP est une technique statistique qui permet de transformer un ensemble de variables
 * potentiellement corrélées en un ensemble de variables linéairement décorrélées appelées
 * composantes principales.
 * 
 * Dans le contexte du débruitage d'images, l'ACP permet de :
 * 1. Trouver une base orthonormale adaptée aux données des patchs
 * 2. Représenter les patchs dans cette base où le bruit et le signal sont mieux séparés
 * 3. Faciliter le filtrage du bruit par seuillage des coefficients
 * 4. Reconstruire les patchs débruités en minimisant l'erreur quadratique
 * 
 * Formulation mathématique :
 * - Soit V = {V_1, V_2, ..., V_M} l'ensemble des M patchs vectorisés de dimension d = s²
 * - On cherche à représenter V dans une base orthonormée optimale U = {u_1, u_2, ..., u_d}
 * - Pour chaque vecteur V_k, on aura : V_k = mv + Σ α_i,k * u_i
 * - Après seuillage, on obtient : V_k' = mv + Σ f(α_i,k) * u_i
 * où f est la fonction de seuillage (dur ou doux)
 */
public class PCAEngine {
    private double[] mv;              // Vecteur moyen (my du PDF p.4)
    private double[][] U;             // Matrice des vecteurs propres (base orthonormale U du PDF p.5)
    private double[][] Gamma;         // Matrice de covariance (Gamma du PDF p.4)
    private double[] eigenValues;     // Valeurs propres (λ_i)
    private List<double[]> Vc;        // Collection des vecteurs colonnes centrés (V_k - mv)
    private List<double[]> patchVectors;
    private Matrix meanVector;
    private Matrix eigenVectors;

    /**
     * Constructeur par défaut
     */
    public PCAEngine() {
        this.patchVectors = new ArrayList<>();
    }

    /**
     * Définit les vecteurs de patchs pour l'ACP
     * @param patchVectors Liste des vecteurs de patchs
     */
    public void setPatchVectors(List<double[]> patchVectors) {
        this.patchVectors = patchVectors;
    }

    /**
     * Calcule le vecteur moyen et la matrice de covariance des patchs vectorisés.
     * 
     * Formules mathématiques :
     * 1. Vecteur moyen : mv = (1/M) * Σ V_k
     * 2. Vecteurs centrés : Vc_k = V_k - mv
     * 3. Matrice de covariance : Gamma = (1/M) * Σ (Vc_k * Vc_k^T)
     * 
     * La matrice de covariance Gamma capture les relations entre les différentes
     * dimensions des patchs. Ses valeurs propres représentent la variance dans 
     * chaque direction principale, et ses vecteurs propres définissent ces directions.
     * 
     * @param V_patchs Liste des patchs vectorisés (V_k)
     * @return Un tableau d'objets contenant [mv, Gamma, Vc]
     * @throws IllegalArgumentException si la liste des patchs est vide
     */
    public Object[] MoyCov(List<double[]> V_patchs) {
        if (V_patchs == null || V_patchs.isEmpty()) {
            throw new IllegalArgumentException("La liste des patchs ne peut pas être vide");
        }

        int M = V_patchs.size();          // Nombre de patchs
        int d = V_patchs.get(0).length;   // Dimension des vecteurs (s²)

        // 1. Calcul du vecteur moyen mv
        mv = new double[d];
        for (double[] v : V_patchs) {
            for (int i = 0; i < d; i++) {
                mv[i] += v[i] / M;
            }
        }

        // 2. Centrage des vecteurs
        Vc = new ArrayList<>();
        for (double[] v : V_patchs) {
            double[] v_centre = new double[d];
            for (int i = 0; i < d; i++) {
                v_centre[i] = v[i] - mv[i];
            }
            Vc.add(v_centre);
        }

        // 3. Calcul de la matrice de covariance Gamma
        Gamma = new double[d][d];
        for (double[] v : Vc) {
            for (int i = 0; i < d; i++) {
                for (int j = 0; j < d; j++) {
                    Gamma[i][j] += (v[i] * v[j]) / M;
                }
            }
        }

        return new Object[] { mv, Gamma, Vc };
    }

    /**
     * Calcule les vecteurs propres de la matrice de covariance (Analyse en Composantes Principales).
     * 
     * Cette méthode utilise une approximation de la décomposition en valeurs propres
     * de la matrice de covariance Gamma pour calculer :
     * 1. Les vecteurs propres qui forment la base orthonormale U
     * 2. Les valeurs propres λ_i qui représentent la variance dans chaque direction principale
     * 
     * L'idée est de trouver les directions (vecteurs propres) qui maximisent la variance
     * des données projetées. Ces directions sont les composantes principales.
     * 
     * Note : Dans une implémentation en production, on utiliserait une bibliothèque
     * spécialisée (JAMA, Commons Math) pour calculer les vecteurs propres de manière
     * plus robuste et efficace.
     * 
     * @param V_patchs Liste des patchs vectorisés
     * @return Matrice U des vecteurs propres (base orthonormale)
     */
    public double[][] ACP(List<double[]> V_patchs) {
        if (Gamma == null) {
            MoyCov(V_patchs);
        }

        int d = Gamma.length;
        U = new double[d][d];
        eigenValues = new double[d];

        // Calcul des vecteurs propres et valeurs propres de la matrice de covariance
        // En pratique, on utiliserait une bibliothèque comme JAMA ou Commons Math
        // pour calculer les vecteurs propres de manière robuste.
        // Pour simplifier, on approxime avec une implémentation basique

        // Initialisation
        for (int i = 0; i < d; i++) {
            // Initialisation du vecteur propre
            double[] u = new double[d];
            u[i] = 1.0;  // On commence avec un vecteur de la base canonique

            // Méthode de la puissance itérée
            // Cette méthode approxime le vecteur propre dominant de la matrice
            // En itérant : u_{k+1} = (A·u_k)/||A·u_k||
            for (int iter = 0; iter < 50; iter++) {  // Maximum 50 itérations
                // Multiplication matrice-vecteur : Au = Gamma·u
                double[] Au = new double[d];
                for (int j = 0; j < d; j++) {
                    for (int k = 0; k < d; k++) {
                        Au[j] += Gamma[j][k] * u[k];
                    }
                }

                // Normalisation : u = Au/||Au||
                double norm = 0.0;
                for (int j = 0; j < d; j++) {
                    norm += Au[j] * Au[j];
                }
                norm = Math.sqrt(norm);

                // Mise à jour du vecteur
                for (int j = 0; j < d; j++) {
                    u[j] = Au[j] / norm;
                }
            }

            // Stockage du vecteur propre
            for (int j = 0; j < d; j++) {
                U[i][j] = u[j];
            }

            // Déflation : retrait de la composante trouvée pour trouver le prochain vecteur propre
            // On soustrait la projection sur le vecteur propre trouvé : Gamma -= (Gu·u^T)
            double[] Gu = new double[d];
            for (int j = 0; j < d; j++) {
                for (int k = 0; k < d; k++) {
                    Gu[j] += Gamma[j][k] * u[k];
                }
            }
            for (int j = 0; j < d; j++) {
                for (int k = 0; k < d; k++) {
                    Gamma[j][k] -= Gu[j] * u[k];
                }
            }

            // Stockage de la valeur propre
            eigenValues[i] = Gu[i];
        }

        // Trier les vecteurs propres par ordre décroissant des valeurs propres
        // Ceci permet d'avoir les composantes principales par ordre d'importance
        sortEigensystem(U, eigenValues);

        return U;
    }

    /**
     * Projette les vecteurs centrés sur la base orthonormale obtenue par ACP.
     * 
     * Formule mathématique pour les coefficients de projection :
     * α_i,k = u_i^T · (V_k - mv) = u_i^T · Vc_k
     * 
     * Ces coefficients représentent la contribution de chaque composante principale
     * à la représentation du patch. Les coefficients associés aux plus petites
     * valeurs propres sont généralement dominés par le bruit et peuvent être
     * atténués ou supprimés par seuillage.
     * 
     * @param U_baseOrthonormale Base orthonormale (vecteurs propres)
     * @param Vc_vecteursCentres Liste des vecteurs centrés
     * @return Liste des coefficients de projection (alpha_i pour chaque patch)
     * @throws IllegalArgumentException si les paramètres sont invalides
     */
    public List<double[]> Proj(double[][] U_baseOrthonormale, List<double[]> Vc_vecteursCentres) {
        if (U_baseOrthonormale == null || Vc_vecteursCentres == null || Vc_vecteursCentres.isEmpty()) {
            throw new IllegalArgumentException("Les paramètres ne peuvent pas être null ou vides");
        }

        int d = U_baseOrthonormale.length;    // Dimension de l'espace

        List<double[]> projections = new ArrayList<>();

        // Pour chaque vecteur centré
        for (double[] v : Vc_vecteursCentres) {
            double[] a = new double[d]; // Vecteur de contribution a_i

            // Calculer les coefficients de projection α_i = u_i^T · v
            for (int i = 0; i < d; i++) {
                double dotProduct = 0;
                for (int j = 0; j < d; j++) {
                    dotProduct += v[j] * U_baseOrthonormale[i][j];
                }
                a[i] = dotProduct;
            }

            projections.add(a);
        }

        return projections;
    }

    /**
     * Trie les vecteurs propres et valeurs propres par ordre décroissant des valeurs propres.
     * 
     * Cette étape est cruciale car elle ordonne les composantes principales
     * par ordre d'importance (variance expliquée). Les premières composantes
     * contiennent généralement plus d'information sur le signal, tandis que
     * les dernières capturent principalement le bruit.
     * 
     * @param V Matrice des vecteurs propres
     * @param d Tableau des valeurs propres
     */
    private void sortEigensystem(double[][] V, double[] d) {
        int n = d.length;
        
        // Tri à bulles simple (peut être remplacé par un tri plus efficace)
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (d[j] < d[j + 1]) {
                    // Échanger les valeurs propres
                    double tempVal = d[j];
                    d[j] = d[j + 1];
                    d[j + 1] = tempVal;
                    
                    // Échanger les colonnes correspondantes des vecteurs propres
                    for (int k = 0; k < n; k++) {
                        double tempVec = V[k][j];
                        V[k][j] = V[k][j + 1];
                        V[k][j + 1] = tempVec;
                    }
                }
            }
        }
    }

    /**
     * Retourne une copie du vecteur moyen.
     * @return Vecteur moyen (mv)
     */
    public double[] get_mv() { return mv.clone(); }
    
    /**
     * Retourne une copie de la base orthonormale.
     * @return Base orthonormale (U)
     */
    public double[][] get_U() { return U.clone(); }
    
    /**
     * Retourne une copie de la matrice de covariance.
     * @return Matrice de covariance (Gamma)
     */
    public double[][] get_Gamma() { return Gamma.clone(); }
    
    /**
     * Retourne une copie des valeurs propres.
     * @return Valeurs propres
     */
    public double[] get_eigenValues() { return eigenValues.clone(); }
    
    /**
     * Retourne une copie des vecteurs centrés.
     * @return Liste des vecteurs centrés
     */
    public List<double[]> get_Vc() {
        List<double[]> Vc_copy = new ArrayList<>();
        for (double[] v : Vc) {
            Vc_copy.add(v.clone());
        }
        return Vc_copy;
    }

    /**
     * Calcule la moyenne des vecteurs de patchs
     * @return Vecteur moyen
     */
    private Matrix calculateMean() {
        if (patchVectors.isEmpty()) {
            throw new IllegalStateException("Aucun vecteur de patch à traiter");
        }
        
        int dim = patchVectors.get(0).length;
        double[] mean = new double[dim];
        
        for (double[] vector : patchVectors) {
            for (int i = 0; i < dim; i++) {
                mean[i] += vector[i];
            }
        }
        
        for (int i = 0; i < dim; i++) {
            mean[i] /= patchVectors.size();
        }
        
        return new Matrix(mean, dim);
    }
    
    /**
     * Centre les vecteurs de patchs en soustrayant la moyenne
     * @param mean Vecteur moyen
     * @return Matrice des vecteurs centrés
     */
    private Matrix centerVectors(Matrix mean) {
        int n = patchVectors.size();
        int dim = patchVectors.get(0).length;
        
        double[][] centeredData = new double[n][dim];
        
        for (int i = 0; i < n; i++) {
            double[] vector = patchVectors.get(i);
            for (int j = 0; j < dim; j++) {
                centeredData[i][j] = vector[j] - mean.get(j, 0);
            }
        }
        
        return new Matrix(centeredData);
    }
    
    /**
     * Calcule la matrice de covariance
     * @param centeredData Matrice des vecteurs centrés
     * @return Matrice de covariance
     */
    private Matrix computeCovarianceMatrix(Matrix centeredData) {
        // Transposer pour obtenir les dimensions correctes
        Matrix centeredDataT = centeredData.transpose();
        
        // Calculer la matrice de covariance: C = X^T * X / (n-1)
        return centeredDataT.times(centeredData).times(1.0 / (patchVectors.size() - 1));
    }
    
    /**
     * Calcule l'ACP (Analyse en Composantes Principales)
     * Décompose la matrice de covariance en vecteurs propres et valeurs propres
     */
    public void computePCA() {
        // 1. Calculer la moyenne des vecteurs de patchs
        meanVector = calculateMean();
        
        // 2. Centrer les données
        Matrix centeredData = centerVectors(meanVector);
        
        // 3. Calculer la matrice de covariance
        Matrix covMatrix = computeCovarianceMatrix(centeredData);
        
        // 4. Décomposition en valeurs propres
        EigenvalueDecomposition eigenDecomposition = covMatrix.eig();
        
        // 5. Stocker les vecteurs propres
        eigenVectors = eigenDecomposition.getV();
        
        // 6. Stocker les valeurs propres
        eigenValues = eigenDecomposition.getRealEigenvalues();
        
        // 7. Trier les vecteurs propres par valeurs propres décroissantes
        sortEigenvectors();
    }
    
    /**
     * Trie les vecteurs propres par valeurs propres décroissantes
     */
    private void sortEigenvectors() {
        int n = eigenValues.length;
        
        // Créer des indices pour trier
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        
        // Trier les indices par valeurs propres décroissantes
        java.util.Arrays.sort(indices, (i, j) -> Double.compare(eigenValues[j], eigenValues[i]));
        
        // Créer des tableaux triés
        double[] sortedEigenValues = new double[n];
        Matrix sortedEigenVectors = new Matrix(n, n);
        
        for (int i = 0; i < n; i++) {
            sortedEigenValues[i] = eigenValues[indices[i]];
            
            for (int j = 0; j < n; j++) {
                sortedEigenVectors.set(j, i, eigenVectors.get(j, indices[i]));
            }
        }
        
        // Mettre à jour les valeurs et vecteurs propres
        eigenValues = sortedEigenValues;
        eigenVectors = sortedEigenVectors;
    }
    
    /**
     * Projette les vecteurs de patchs dans l'espace ACP
     * @param vectors Liste des vecteurs à projeter
     * @return Liste des vecteurs projetés
     */
    public List<double[]> projectPatches(List<double[]> vectors) {
        if (eigenVectors == null) {
            throw new IllegalStateException("L'ACP n'a pas encore été calculée");
        }
        
        int dim = vectors.get(0).length;
        int numVectors = vectors.size();
        List<double[]> projectedVectors = new ArrayList<>();
        
        for (int i = 0; i < numVectors; i++) {
            double[] vector = vectors.get(i);
            double[] centered = new double[dim];
            
            // Centrer le vecteur en soustrayant la moyenne
            for (int j = 0; j < dim; j++) {
                centered[j] = vector[j] - meanVector.get(j, 0);
            }
            
            // Projeter le vecteur centré sur les vecteurs propres
            double[] projected = new double[dim];
            for (int j = 0; j < dim; j++) {
                double sum = 0;
                for (int k = 0; k < dim; k++) {
                    sum += centered[k] * eigenVectors.get(k, j);
                }
                projected[j] = sum;
            }
            
            projectedVectors.add(projected);
        }
        
        return projectedVectors;
    }
    
    /**
     * Applique un seuillage dur aux vecteurs projetés
     * @param projectedVectors Liste des vecteurs projetés
     * @param threshold Valeur du seuil
     * @return Liste des vecteurs après seuillage
     */
    public List<double[]> applyHardThresholding(List<double[]> projectedVectors, double threshold) {
        List<double[]> thresholdedVectors = new ArrayList<>();
        
        for (double[] vector : projectedVectors) {
            thresholdedVectors.add(ThresholdingUtils.SeuillageDur(vector, threshold));
        }
        
        return thresholdedVectors;
    }
    
    /**
     * Applique un seuillage doux aux vecteurs projetés
     * @param projectedVectors Liste des vecteurs projetés
     * @param threshold Valeur du seuil
     * @return Liste des vecteurs après seuillage
     */
    public List<double[]> applySoftThresholding(List<double[]> projectedVectors, double threshold) {
        List<double[]> thresholdedVectors = new ArrayList<>();
        
        for (double[] vector : projectedVectors) {
            thresholdedVectors.add(ThresholdingUtils.SeuillageDoux(vector, threshold));
        }
        
        return thresholdedVectors;
    }
    
    /**
     * Reconstruit les vecteurs originaux à partir des vecteurs projetés
     * @param projectedVectors Liste des vecteurs projetés
     * @return Liste des vecteurs reconstruits
     */
    public List<double[]> reconstructPatches(List<double[]> projectedVectors) {
        if (eigenVectors == null) {
            throw new IllegalStateException("L'ACP n'a pas encore été calculée");
        }
        
        int dim = projectedVectors.get(0).length;
        int numVectors = projectedVectors.size();
        List<double[]> reconstructedVectors = new ArrayList<>();
        
        for (int i = 0; i < numVectors; i++) {
            double[] projected = projectedVectors.get(i);
            double[] reconstructed = new double[dim];
            
            // Reconstruire le vecteur centré
            for (int j = 0; j < dim; j++) {
                for (int k = 0; k < dim; k++) {
                    reconstructed[j] += projected[k] * eigenVectors.get(j, k);
                }
            }
            
            // Ajouter la moyenne pour retrouver l'échelle originale
            for (int j = 0; j < dim; j++) {
                reconstructed[j] += meanVector.get(j, 0);
            }
            
            reconstructedVectors.add(reconstructed);
        }
        
        return reconstructedVectors;
    }
    
    /**
     * Retourne les valeurs propres
     * @return Tableau des valeurs propres
     */
    public double[] getEigenvalues() {
        return eigenValues;
    }
    
    /**
     * Retourne les vecteurs propres
     * @return Matrice des vecteurs propres
     */
    public Matrix getEigenvectors() {
        if (eigenVectors == null && U != null) {
            // Convertir notre représentation interne en matrice JAMA
            eigenVectors = new Matrix(U);
        }
        return eigenVectors;
    }

    /**
     * Permet d'obtenir le vecteur moyen sous forme de matrice JAMA
     * 
     * @return vecteur moyen
     */
    public Matrix getMeanVector() {
        if (meanVector == null && mv != null) {
            meanVector = new Matrix(mv, mv.length);
        }
        return meanVector;
    }

    /**
     * Retourne les coefficients de projection d'un patch spécifique
     * Utile pour visualiser la reconstruction avec différents nombres de composantes
     * 
     * @param patchIndex indice du patch dont on veut les coefficients
     * @return coefficients de projection ou null si l'indice est invalide
     */
    public double[] getProjectionCoefficients(int patchIndex) {
        if (Vc == null || patchIndex < 0 || patchIndex >= Vc.size()) {
            return null;
        }
        
        // Projeter le patch spécifié sur la base orthonormale
        double[] v = Vc.get(patchIndex);
        int d = U.length;
        double[] coefficients = new double[d];
        
        for (int i = 0; i < d; i++) {
            double dotProduct = 0;
            for (int j = 0; j < d; j++) {
                dotProduct += v[j] * U[i][j];
            }
            coefficients[i] = dotProduct;
        }
        
        return coefficients;
    }
} 