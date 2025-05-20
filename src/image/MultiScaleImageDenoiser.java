package image;

import ij.process.ImageProcessor;
import pca.PCAEngine;
import thresholding.ThresholdCalculationType;
import thresholding.ThresholdingFunctionType;
import patch.PatchManager;
import patch.MetricsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Classe pour le débruitage multi-échelle qui combine plusieurs tailles de patches
 * pour capturer les détails à différentes résolutions.
 */
public class MultiScaleImageDenoiser {
    
    private final Image originalImage;
    private final Image noisyImage;
    private final List<Integer> patchSizes;
    private final List<Double> weights;
    private ImageDenoiserPCA denoiser;
    
    /**
     * Constructeur pour le débruitage multi-échelle
     * 
     * @param originalImage l'image originale (pour les métriques)
     * @param noisyImage l'image bruitée à débruiter
     * @param patchSizes liste des tailles de patches à utiliser
     * @param weights poids relatifs de chaque échelle (doit être de même taille que patchSizes)
     */
    public MultiScaleImageDenoiser(Image originalImage, Image noisyImage, List<Integer> patchSizes, List<Double> weights) {
        if (patchSizes.size() != weights.size()) {
            throw new IllegalArgumentException("Le nombre de tailles de patches et de poids doit être identique");
        }
        
        // Normalisation des poids pour qu'ils somment à 1
        double sumWeights = weights.stream().mapToDouble(Double::doubleValue).sum();
        List<Double> normalizedWeights = weights.stream()
                .map(w -> w / sumWeights)
                .collect(Collectors.toList());
        
        this.originalImage = originalImage;
        this.noisyImage = noisyImage;
        this.patchSizes = patchSizes;
        this.weights = normalizedWeights;
        this.denoiser = new ImageDenoiserPCA();
    }
    
    /**
     * Configure les paramètres du débruiseur pour toutes les échelles
     * 
     * @param coverageMode le mode de couverture pour l'extraction des patches
     * @param overlapPercentage le pourcentage de chevauchement des patches
     * @param thresholdingFunctionType le type de fonction de seuillage
     * @param thresholdCalculationType le type de calcul du seuil
     * @param percentToKeep le pourcentage de composantes principales à conserver
     */
    public void configureDenoiser(PatchManager.CoverageMode coverageMode, 
                                 double overlapPercentage,
                                 ThresholdingFunctionType thresholdingFunctionType,
                                 ThresholdCalculationType thresholdCalculationType,
                                 double percentToKeep) {
        denoiser.setCoverageMode(convertCoverageMode(coverageMode));
        denoiser.setOverlapPercentage(overlapPercentage);
        denoiser.setThresholdingFunctionType(thresholdingFunctionType);
        denoiser.setThresholdCalculationType(thresholdCalculationType);
        denoiser.setPercentToKeep(percentToKeep);
    }
    
    /**
     * Convertit le PatchManager.CoverageMode en patch.CoverageMode
     * @param mode Le mode de couverture PatchManager.CoverageMode
     * @return Le mode de couverture patch.CoverageMode correspondant
     */
    private patch.CoverageMode convertCoverageMode(PatchManager.CoverageMode mode) {
        switch (mode) {
            case EXACT_N: return patch.CoverageMode.EXACT_N;
            case OVERLAP_FULL: return patch.CoverageMode.OVERLAP_FULL;
            case NO_OVERLAP: return patch.CoverageMode.NO_OVERLAP;
            default: return patch.CoverageMode.OVERLAP_FULL;
        }
    }
    
    /**
     * Effectue le débruitage multi-échelle
     * 
     * @return l'image débruitée combinée
     */
    public Image denoise() {
        List<Image> denoisedImages = new ArrayList<>();
        Map<Integer, Double> metricsMap = new HashMap<>();
        
        // Débruitage à chaque échelle
        for (int i = 0; i < patchSizes.size(); i++) {
            int patchSize = patchSizes.get(i);
            double weight = weights.get(i);
            
            System.out.println("Débruitage avec patches de taille " + patchSize + " (poids: " + weight + ")");
            
            denoiser.setPatchSize(patchSize);
            Image denoisedImage = denoiser.denoise(noisyImage);
            denoisedImages.add(denoisedImage);
            
            // Calcul des métriques pour information
            if (originalImage != null) {
                double mse = MetricsUtils.calculateMSE(originalImage.getProcessor(), denoisedImage.getProcessor());
                double psnr = MetricsUtils.calculatePSNR(originalImage.getProcessor(), denoisedImage.getProcessor());
                double ssim = MetricsUtils.calculateSSIM(originalImage.getProcessor(), denoisedImage.getProcessor());
                
                System.out.println("Échelle " + patchSize + ": MSE = " + mse + ", PSNR = " + psnr + " dB, SSIM = " + ssim);
                metricsMap.put(patchSize, psnr); // On utilise le PSNR comme indicateur de qualité
            }
        }
        
        // Fusion des images débruitées avec pondération fixe ou adaptative
        return fuseImages(denoisedImages, metricsMap);
    }
    
    /**
     * Fusionne les images débruitées en utilisant les poids fournis
     * ou en ajustant automatiquement en fonction des métriques
     * 
     * @param images la liste des images débruitées
     * @param metricsMap la map des métriques par taille de patch
     * @return l'image fusionnée
     */
    private Image fuseImages(List<Image> images, Map<Integer, Double> metricsMap) {
        // Créer une nouvelle image pour la fusion
        int width = noisyImage.getWidth();
        int height = noisyImage.getHeight();
        Image fusedImage = new Image(width, height);
        ImageProcessor fusedProcessor = fusedImage.getProcessor();
        
        // Si nous avons des métriques, on peut utiliser des poids adaptatifs
        boolean useAdaptiveWeights = !metricsMap.isEmpty() && originalImage != null;
        List<Double> adaptiveWeights = new ArrayList<>();
        
        if (useAdaptiveWeights) {
            // Calcul des poids en fonction des PSNR normalisés
            double totalPSNR = metricsMap.values().stream().mapToDouble(Double::doubleValue).sum();
            for (int i = 0; i < patchSizes.size(); i++) {
                double psnr = metricsMap.get(patchSizes.get(i));
                adaptiveWeights.add(psnr / totalPSNR);
            }
            System.out.println("Poids adaptatifs: " + adaptiveWeights);
        }
        
        // Fusion pixel par pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double pixelValue = 0.0;
                
                for (int i = 0; i < images.size(); i++) {
                    double weight = useAdaptiveWeights ? adaptiveWeights.get(i) : weights.get(i);
                    pixelValue += images.get(i).getProcessor().getPixelValue(x, y) * weight;
                }
                
                fusedProcessor.putPixelValue(x, y, pixelValue);
            }
        }
        
        return fusedImage;
    }
} 