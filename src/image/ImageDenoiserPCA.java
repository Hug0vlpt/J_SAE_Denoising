package image;

import java.util.List;
import patch.CoverageMode;
import patch.PatchManager;
import pca.PCAEngine;
import thresholding.ThresholdCalculationType;
import thresholding.ThresholdCalculator;
import thresholding.ThresholdingFunctionType;
import thresholding.ThresholdingUtils;

/**
 * Classe responsable du débruitage d'images par ACP
 */
public class ImageDenoiserPCA {
    
    // Niveaux de bruit (écart-type σ) pour calibrer le débruitage
    public static final double SIGMA_1 = 10.0; // Faible bruit
    public static final double SIGMA_2 = 20.0; // Bruit moyen
    public static final double SIGMA_3 = 30.0; // Bruit fort
    
    // Paramètres par défaut
    private double sigma = SIGMA_2;
    private int patchSize = 8;  // Taille par défaut des patchs
    private double percentToKeep = 1.0;  // Pourcentage des composantes à conserver
    private int coverageMode = PatchManager.OVERLAP_FULL;  // Mode de couverture par défaut
    private int overlapPercentage = 50;  // Pourcentage de chevauchement par défaut
    private boolean mirrorEdges = false;  // Gestion des bords
    private ThresholdingFunctionType thresholdingType = ThresholdingFunctionType.SEUILLAGE_DUR;
    private ThresholdCalculationType thresholdCalculationType = ThresholdCalculationType.SEUIL_V_VISUSHRINK;

    /**
     * Constructeur par défaut
     */
    public ImageDenoiserPCA() {
        // Utilise les paramètres par défaut
    }
    
    /**
     * Constructeur avec paramètres
     * @param sigma Écart-type du bruit
     * @param thresholdingType Type de seuillage
     * @param thresholdCalculationType Type de calcul du seuil
     */
    public ImageDenoiserPCA(double sigma, ThresholdingFunctionType thresholdingType, 
                          ThresholdCalculationType thresholdCalculationType) {
        this.sigma = sigma;
        this.thresholdingType = thresholdingType;
        this.thresholdCalculationType = thresholdCalculationType;
    }
    
    /**
     * Débruite une image en utilisant l'ACP avec les paramètres par défaut
     * @param image Image à débruiter
     * @param patches Liste des patchs extraits de l'image
     * @return Image débruitée
     */
    public Image denoiseImage(Image image, List<Patch> patches) {
        return denoiseImage(image, patches, thresholdingType, thresholdCalculationType, sigma);
    }
    
    /**
     * Débruite une image en utilisant l'ACP avec des paramètres spécifiques
     * @param image Image à débruiter
     * @param patches Liste des patchs extraits de l'image
     * @param thresholdingType Type de seuillage à utiliser
     * @param thresholdCalculationType Méthode de calcul du seuil
     * @param sigma Écart-type du bruit
     * @return Image débruitée
     */
    public Image denoiseImage(Image image, List<Patch> patches, 
                             ThresholdingFunctionType thresholdingType,
                             ThresholdCalculationType thresholdCalculationType,
                             double sigma) {
        try {
            // 1. Vectoriser les patchs
            PatchManager patchManager = new PatchManager();
            List<double[]> vectorizedPatches = patchManager.VectorPatchs(patches);
            
            // 2. Créer et initialiser le moteur PCA
            PCAEngine pcaEngine = new PCAEngine();
            pcaEngine.setPatchVectors(vectorizedPatches);
            
            // 3. Calculer les composantes principales
            pcaEngine.computePCA();
            
            // 4. Projeter les patchs dans l'espace PCA
            List<double[]> projectedPatches = pcaEngine.projectPatches(vectorizedPatches);
            
            // 5. Calculer le seuil approprié
            double threshold;
            if (thresholdCalculationType == ThresholdCalculationType.SEUIL_V_VISUSHRINK) {
                // Seuil universel (VisuShrink)
                int patchSize = patches.get(0).getS();
                int L = patchSize * patchSize * patches.size(); // Nombre total de pixels
                threshold = ThresholdingUtils.SeuilV(sigma, L);
            } else {
                // Seuil BayesShrink
                double variance = sigma * sigma; // Variance du bruit
                
                // Calculer la variance observée des coefficients
                double[] eigenvalues = pcaEngine.getEigenvalues();
                double varianceObserved = 0;
                for (double val : eigenvalues) {
                    varianceObserved += val;
                }
                varianceObserved /= eigenvalues.length;
                
                // Estimer l'écart-type du signal original
                double sigma_x = ThresholdingUtils.estimerEcartTypeSignal_sigma_x_hat(varianceObserved, variance);
                
                // Calculer le seuil BayesShrink
                threshold = ThresholdingUtils.SeuilB(variance, sigma_x);
            }
            
            // 6. Appliquer le seuillage aux coefficients
            List<double[]> thresholdedPatches;
            if (thresholdingType == ThresholdingFunctionType.SEUILLAGE_DUR) {
                // Seuillage dur
                thresholdedPatches = pcaEngine.applyHardThresholding(projectedPatches, threshold);
            } else {
                // Seuillage doux
                thresholdedPatches = pcaEngine.applySoftThresholding(projectedPatches, threshold);
            }
            
            // 7. Reconstruire les patchs débruités
            List<double[]> reconstructedVectors = pcaEngine.reconstructPatches(thresholdedPatches);
            
            // 8. Convertir les vecteurs en patchs
            List<int[]> positions = new java.util.ArrayList<>();
            for (Patch patch : patches) {
                positions.add(patch.getPosition());
            }
            
            List<Patch> reconstructedPatches = patchManager.DevectorisePatchs(
                reconstructedVectors, positions, patches.get(0).getS());
            
            // 9. Reconstruire l'image à partir des patchs débruités
            Image denoisedImage = patchManager.ReconstructPatchs(
                reconstructedPatches, image.getHeight(), image.getWidth());
            
            return denoisedImage;
            
        } catch (Exception e) {
            System.err.println("Erreur lors du débruitage : " + e.getMessage());
            e.printStackTrace();
            return image; // Retourner l'image originale en cas d'erreur
        }
    }
    
    /**
     * Débruite une imagette individuelle
     * @param imagette Imagette à débruiter
     * @param patchSize Taille des patchs à extraire
     * @param overlapPercentage Pourcentage de chevauchement des patchs
     * @param mirrorEdges Si true, utilise l'effet miroir pour les bords
     * @return Imagette débruitée
     */
    public Imagette denoiseImagette(Imagette imagette, int patchSize, int overlapPercentage, boolean mirrorEdges) {
        try {
            // Convertir l'imagette en image
            Image imagetteImg = new Image(imagette.getData());
            
            // Extraire les patchs de l'imagette
            PatchManager patchManager = new PatchManager();
            int coverageMode = overlapPercentage == 0 ? PatchManager.NO_OVERLAP : PatchManager.OVERLAP_FULL;
            List<Patch> patches = patchManager.ExtractPatchs(imagetteImg, patchSize, coverageMode, overlapPercentage, mirrorEdges);
            
            // Débruiter l'image de l'imagette
            Image denoisedImage = denoiseImage(imagetteImg, patches, thresholdingType, thresholdCalculationType, sigma);
            
            // Créer une nouvelle imagette avec les données débruitées
            Imagette denoisedImagette = new Imagette(denoisedImage.getData(), imagette.getPosition());
            
            return denoisedImagette;
            
        } catch (Exception e) {
            System.err.println("Erreur lors du débruitage de l'imagette : " + e.getMessage());
            e.printStackTrace();
            return imagette; // Retourner l'imagette originale en cas d'erreur
        }
    }
    
    /**
     * Définit l'écart-type du bruit
     * @param sigma Nouvel écart-type
     */
    public void setSigma(double sigma) {
        this.sigma = sigma;
    }
    
    /**
     * Définit le type de seuillage
     * @param thresholdingType Nouveau type de seuillage
     */
    public void setThresholdingType(ThresholdingFunctionType thresholdingType) {
        this.thresholdingType = thresholdingType;
    }
    
    /**
     * Alias pour setThresholdingType pour la compatibilité
     * @param thresholdingType Nouveau type de seuillage
     */
    public void setThresholdingFunctionType(ThresholdingFunctionType thresholdingType) {
        setThresholdingType(thresholdingType);
    }
    
    /**
     * Définit le type de calcul du seuil
     * @param thresholdCalculationType Nouveau type de calcul du seuil
     */
    public void setThresholdCalculationType(ThresholdCalculationType thresholdCalculationType) {
        this.thresholdCalculationType = thresholdCalculationType;
    }
    
    /**
     * Retourne l'écart-type du bruit actuellement utilisé
     * @return Écart-type du bruit
     */
    public double getSigma() {
        return sigma;
    }
    
    /**
     * Retourne le type de seuillage actuellement utilisé
     * @return Type de seuillage
     */
    public ThresholdingFunctionType getThresholdingType() {
        return thresholdingType;
    }
    
    /**
     * Alias pour getThresholdingType pour la compatibilité
     * @return Type de seuillage
     */
    public ThresholdingFunctionType getThresholdingFunctionType() {
        return getThresholdingType();
    }
    
    /**
     * Retourne le type de calcul du seuil actuellement utilisé
     * @return Type de calcul du seuil
     */
    public ThresholdCalculationType getThresholdCalculationType() {
        return thresholdCalculationType;
    }
    
    /**
     * Génère et sauvegarde les visualisations des composantes principales
     * 
     * @param image Image à analyser
     * @param patchSize Taille des patchs
     * @param numComponentsToVisualize Nombre de composantes à visualiser
     * @param outputDirectory Répertoire où sauvegarder les visualisations
     * @return true si les visualisations ont été générées avec succès, false sinon
     */
    public boolean generatePCAVisualizations(Image image, int patchSize, int numComponentsToVisualize, String outputDirectory) {
        try {
            // 1. Extraire les patchs de l'image
            PatchManager patchManager = new PatchManager();
            List<Patch> patches = patchManager.ExtractPatchs(image, patchSize, PatchManager.EXACT_N, 100, false);
            
            // 2. Vectoriser les patchs
            List<double[]> vectorizedPatches = patchManager.VectorPatchs(patches);
            
            // 3. Créer et initialiser le moteur PCA
            PCAEngine pcaEngine = new PCAEngine();
            pcaEngine.setPatchVectors(vectorizedPatches);
            
            // 4. Calculer les composantes principales
            pcaEngine.computePCA();
            
            // 5. Créer le visualiseur PCA
            pca.PCAVisualizer visualizer = new pca.PCAVisualizer();
            
            // 6. Créer les sous-répertoires pour les différentes visualisations
            String eigenVectorsDir = outputDirectory + "/eigenvectors";
            String reconstructionDir = outputDirectory + "/reconstructions";
            
            // 7. Visualiser les vecteurs propres (composantes principales)
            visualizer.visualizeEigenVectors(
                pcaEngine.getEigenvectors(), 
                numComponentsToVisualize, 
                patchSize, 
                eigenVectorsDir
            );
            
            // 8. Visualiser la reconstruction progressive avec un patch spécifique
            if (patches.size() > 0) {
                // Sélectionner un patch au milieu de l'image (ou le premier si peu de patchs)
                int patchIndex = Math.min(patches.size() / 2, patches.size() - 1);
                
                // Récupérer les coefficients de projection pour ce patch
                double[] coefficients = pcaEngine.getProjectionCoefficients(patchIndex);
                
                // Visualiser la reconstruction progressive
                visualizer.visualizeReconstruction(
                    pcaEngine.getEigenvectors(),
                    pcaEngine.getMeanVector().getRowPackedCopy(),
                    coefficients,
                    patchSize,
                    reconstructionDir
                );
            }
            
            // 9. Visualiser la contribution en variance des composantes principales
            visualizer.visualizeVarianceExplained(
                pcaEngine.getEigenvalues(),
                outputDirectory
            );
            
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération des visualisations PCA : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Analyse une image pour extraire les composantes principales et évaluer leur importance
     * 
     * @param image Image à analyser
     * @param patchSize Taille des patchs à utiliser
     * @return Pourcentage de variance expliquée par les N premières composantes
     */
    public double[] analyzeVarianceExplained(Image image, int patchSize) {
        try {
            // 1. Extraire les patchs de l'image
            PatchManager patchManager = new PatchManager();
            List<Patch> patches = patchManager.ExtractPatchs(image, patchSize, PatchManager.EXACT_N, 100, false);
            
            // 2. Vectoriser les patchs
            List<double[]> vectorizedPatches = patchManager.VectorPatchs(patches);
            
            // 3. Créer et initialiser le moteur PCA
            PCAEngine pcaEngine = new PCAEngine();
            pcaEngine.setPatchVectors(vectorizedPatches);
            
            // 4. Calculer les composantes principales
            pcaEngine.computePCA();
            
            // 5. Récupérer les valeurs propres
            double[] eigenvalues = pcaEngine.getEigenvalues();
            
            // 6. Calculer la variance totale
            double totalVariance = 0;
            for (double val : eigenvalues) {
                totalVariance += val;
            }
            
            // 7. Calculer le pourcentage de variance expliquée par chaque composante
            double[] variance = new double[eigenvalues.length];
            for (int i = 0; i < eigenvalues.length; i++) {
                variance[i] = (eigenvalues[i] / totalVariance) * 100.0;
            }
            
            // 8. Calculer la variance cumulée
            double[] cumulativeVariance = new double[eigenvalues.length];
            double cumSum = 0;
            for (int i = 0; i < eigenvalues.length; i++) {
                cumSum += variance[i];
                cumulativeVariance[i] = cumSum;
            }
            
            return cumulativeVariance;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'analyse des composantes principales : " + e.getMessage());
            e.printStackTrace();
            return new double[0];
        }
    }

    /**
     * Débruite une image en utilisant l'approche PCA
     * Méthode principale utilisée par les autres classes
     * 
     * @param image Image à débruiter
     * @return Image débruitée
     */
    public Image denoise(Image image) {
        try {
            // Extraire les patchs avec les paramètres actuels
            PatchManager patchManager = new PatchManager();
            List<Patch> patches = patchManager.ExtractPatchs(
                image, patchSize, coverageMode, overlapPercentage, mirrorEdges);
            
            // Débruiter avec les patchs extraits
            return denoiseImage(image, patches, thresholdingType, thresholdCalculationType, sigma);
        } catch (Exception e) {
            System.err.println("Erreur lors du débruitage : " + e.getMessage());
            e.printStackTrace();
            return image; // Retourner l'image originale en cas d'erreur
        }
    }

    /**
     * Définit la taille des patchs à utiliser
     * @param patchSize Taille des patchs
     */
    public void setPatchSize(int patchSize) {
        if (patchSize > 0) {
            this.patchSize = patchSize;
        }
    }
    
    /**
     * Définit le pourcentage des composantes principales à conserver
     * @param percentToKeep Pourcentage à conserver (0.0 à 1.0)
     */
    public void setPercentToKeep(double percentToKeep) {
        if (percentToKeep > 0.0 && percentToKeep <= 1.0) {
            this.percentToKeep = percentToKeep;
        }
    }
    
    /**
     * Définit le mode de couverture des patchs
     * @param coverageMode Mode de couverture (EXACT_N, OVERLAP_FULL, NO_OVERLAP)
     */
    public void setCoverageMode(int coverageMode) {
        if (coverageMode == PatchManager.EXACT_N || 
            coverageMode == PatchManager.OVERLAP_FULL || 
            coverageMode == PatchManager.NO_OVERLAP) {
            this.coverageMode = coverageMode;
        }
    }
    
    /**
     * Définit le mode de couverture des patchs par son type d'énumération CoverageMode
     * @param coverageMode Mode de couverture 
     */
    public void setCoverageMode(CoverageMode coverageMode) {
        if (coverageMode != null) {
            switch (coverageMode) {
                case EXACT_N:
                    this.coverageMode = PatchManager.EXACT_N;
                    break;
                case OVERLAP_FULL:
                    this.coverageMode = PatchManager.OVERLAP_FULL;
                    break;
                case NO_OVERLAP:
                    this.coverageMode = PatchManager.NO_OVERLAP;
                    break;
            }
        }
    }
    
    /**
     * Définit le pourcentage de chevauchement des patchs
     * @param overlapPercentage Pourcentage de chevauchement (0 à 100)
     */
    public void setOverlapPercentage(int overlapPercentage) {
        if (overlapPercentage >= 0 && overlapPercentage <= 100) {
            this.overlapPercentage = overlapPercentage;
        }
    }
    
    /**
     * Définit le pourcentage de chevauchement des patchs (version double)
     * @param overlapPercentage Pourcentage de chevauchement (0.0 à 100.0)
     */
    public void setOverlapPercentage(double overlapPercentage) {
        if (overlapPercentage >= 0.0 && overlapPercentage <= 100.0) {
            this.overlapPercentage = (int) Math.round(overlapPercentage);
        }
    }
    
    /**
     * Définit la gestion des bords (effet miroir ou non)
     * @param mirrorEdges Si true, utilise l'effet miroir pour les bords
     */
    public void setMirrorEdges(boolean mirrorEdges) {
        this.mirrorEdges = mirrorEdges;
    }
    
    /**
     * Retourne la taille des patchs utilisée
     * @return Taille des patchs
     */
    public int getPatchSize() {
        return patchSize;
    }
    
    /**
     * Retourne le pourcentage des composantes principales conservées
     * @return Pourcentage conservé
     */
    public double getPercentToKeep() {
        return percentToKeep;
    }
    
    /**
     * Retourne le mode de couverture des patchs
     * @return Mode de couverture
     */
    public int getCoverageMode() {
        return coverageMode;
    }
    
    /**
     * Retourne le pourcentage de chevauchement des patchs
     * @return Pourcentage de chevauchement
     */
    public int getOverlapPercentage() {
        return overlapPercentage;
    }
    
    /**
     * Indique si l'effet miroir est utilisé pour la gestion des bords
     * @return true si l'effet miroir est utilisé
     */
    public boolean getMirrorEdges() {
        return mirrorEdges;
    }
} 