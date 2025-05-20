package image;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import patch.MetricsUtils;
import java.util.concurrent.CompletableFuture;

/**
 * Classe pour étendre le débruitage aux images couleur (RGB)
 * Implémente deux approches :
 * 1. Débruitage par canal (Rouge, Vert, Bleu séparément)
 * 2. Débruitage vectoriel (traite chaque patch comme un vecteur 3D)
 */
public class ColorImageDenoiser {
    
    /**
     * Énumération pour les différentes stratégies de débruitage couleur
     */
    public enum ColorStrategy {
        PER_CHANNEL,    // Traite chaque canal séparément
        RGB_VECTORIAL   // Traite les trois canaux ensemble (approche vectorielle)
    }
    
    private final ImageDenoiserPCA denoiser;
    private ColorStrategy strategy;
    private boolean useParallelProcessing;
    
    /**
     * Constructeur par défaut
     */
    public ColorImageDenoiser() {
        this.denoiser = new ImageDenoiserPCA();
        this.strategy = ColorStrategy.PER_CHANNEL;
        this.useParallelProcessing = true;
    }
    
    /**
     * Définit la stratégie de débruitage couleur
     * @param strategy la stratégie à utiliser
     */
    public void setColorStrategy(ColorStrategy strategy) {
        this.strategy = strategy;
    }
    
    /**
     * Active ou désactive le traitement parallèle des canaux
     * @param useParallel true pour activer le traitement parallèle
     */
    public void setUseParallelProcessing(boolean useParallel) {
        this.useParallelProcessing = useParallel;
    }
    
    /**
     * Configure les paramètres du débruisage
     * (délégue la configuration au débruiseur interne)
     */
    public void setPatchSize(int patchSize) {
        denoiser.setPatchSize(patchSize);
    }
    
    public void setPercentToKeep(double percentToKeep) {
        denoiser.setPercentToKeep(percentToKeep);
    }
    
    public void setThresholdingFunctionType(thresholding.ThresholdingFunctionType type) {
        denoiser.setThresholdingFunctionType(type);
    }
    
    public void setThresholdCalculationType(thresholding.ThresholdCalculationType type) {
        denoiser.setThresholdCalculationType(type);
    }
    
    public void setCoverageMode(patch.PatchManager.CoverageMode mode) {
        denoiser.setCoverageMode(convertCoverageMode(mode));
    }
    
    /**
     * Convertit le PatchManager.CoverageMode en patch.CoverageMode
     * @param mode Le mode de couverture PatchManager.CoverageMode
     * @return Le mode de couverture patch.CoverageMode correspondant
     */
    private patch.CoverageMode convertCoverageMode(patch.PatchManager.CoverageMode mode) {
        switch (mode) {
            case EXACT_N: return patch.CoverageMode.EXACT_N;
            case OVERLAP_FULL: return patch.CoverageMode.OVERLAP_FULL;
            case NO_OVERLAP: return patch.CoverageMode.NO_OVERLAP;
            default: return patch.CoverageMode.OVERLAP_FULL;
        }
    }
    
    public void setOverlapPercentage(double percentage) {
        denoiser.setOverlapPercentage(percentage);
    }
    
    /**
     * Débruite une image couleur (RGB) selon la stratégie choisie
     * @param colorImage l'image couleur bruitée à débruiter
     * @return l'image couleur débruitée
     */
    public ImagePlus denoiseColorImage(ImagePlus colorImage) {
        if (colorImage == null) {
            throw new IllegalArgumentException("L'image ne peut pas être null");
        }
        
        // S'assurer que l'image est bien une image couleur
        if (colorImage.getType() != ImagePlus.COLOR_RGB) {
            throw new IllegalArgumentException("L'image doit être de type RGB");
        }
        
        ColorProcessor cp = (ColorProcessor) colorImage.getProcessor();
        
        // Créer une nouvelle image couleur pour le résultat
        ColorProcessor resultCP = new ColorProcessor(cp.getWidth(), cp.getHeight());
        
        switch (strategy) {
            case PER_CHANNEL:
                // Débruitage canal par canal
                if (useParallelProcessing) {
                    denoisePerChannelParallel(cp, resultCP);
                } else {
                    denoisePerChannelSequential(cp, resultCP);
                }
                break;
                
            case RGB_VECTORIAL:
                // Non implémenté pour l'instant - fallback sur per-channel
                System.out.println("Stratégie RGB_VECTORIAL non implémentée - utilisation de PER_CHANNEL");
                if (useParallelProcessing) {
                    denoisePerChannelParallel(cp, resultCP);
                } else {
                    denoisePerChannelSequential(cp, resultCP);
                }
                break;
        }
        
        // Créer une nouvelle ImagePlus avec le résultat
        ImagePlus result = new ImagePlus("Denoised_" + colorImage.getTitle(), resultCP);
        
        return result;
    }
    
    /**
     * Débruite une image couleur canal par canal (séquentiel)
     * @param cp le ColorProcessor de l'image bruitée
     * @param resultCP le ColorProcessor qui contiendra le résultat
     */
    private void denoisePerChannelSequential(ColorProcessor cp, ColorProcessor resultCP) {
        int width = cp.getWidth();
        int height = cp.getHeight();
        
        // Extraire les canaux R, G et B
        byte[] R = new byte[width * height];
        byte[] G = new byte[width * height];
        byte[] B = new byte[width * height];
        cp.getRGB(R, G, B);
        
        // Créer des ImageProcessors pour chaque canal
        ByteProcessor redBP = new ByteProcessor(width, height, R, null);
        ByteProcessor greenBP = new ByteProcessor(width, height, G, null);
        ByteProcessor blueBP = new ByteProcessor(width, height, B, null);
        
        // Débruiter chaque canal
        Image redImage = new Image(new ImagePlus("Red", redBP));
        Image greenImage = new Image(new ImagePlus("Green", greenBP));
        Image blueImage = new Image(new ImagePlus("Blue", blueBP));
        
        Image denoisedRed = denoiser.denoise(redImage);
        Image denoisedGreen = denoiser.denoise(greenImage);
        Image denoisedBlue = denoiser.denoise(blueImage);
        
        // Extraire les pixels débruités
        byte[] denoisedR = new byte[width * height];
        byte[] denoisedG = new byte[width * height];
        byte[] denoisedB = new byte[width * height];
        
        ImageProcessor redIP = denoisedRed.getProcessor();
        ImageProcessor greenIP = denoisedGreen.getProcessor();
        ImageProcessor blueIP = denoisedBlue.getProcessor();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                denoisedR[index] = (byte) redIP.getPixel(x, y);
                denoisedG[index] = (byte) greenIP.getPixel(x, y);
                denoisedB[index] = (byte) blueIP.getPixel(x, y);
            }
        }
        
        // Recombiner les canaux
        resultCP.setRGB(denoisedR, denoisedG, denoisedB);
    }
    
    /**
     * Débruite une image couleur canal par canal (parallèle)
     * @param cp le ColorProcessor de l'image bruitée
     * @param resultCP le ColorProcessor qui contiendra le résultat
     */
    private void denoisePerChannelParallel(ColorProcessor cp, ColorProcessor resultCP) {
        int width = cp.getWidth();
        int height = cp.getHeight();
        
        // Extraire les canaux R, G et B
        byte[] R = new byte[width * height];
        byte[] G = new byte[width * height];
        byte[] B = new byte[width * height];
        cp.getRGB(R, G, B);
        
        // Créer des ImageProcessors pour chaque canal
        ByteProcessor redBP = new ByteProcessor(width, height, R, null);
        ByteProcessor greenBP = new ByteProcessor(width, height, G, null);
        ByteProcessor blueBP = new ByteProcessor(width, height, B, null);
        
        // Créer des copies du débruiseur pour le traitement parallèle
        ImageDenoiserPCA redDenoiser = new ImageDenoiserPCA();
        ImageDenoiserPCA greenDenoiser = new ImageDenoiserPCA();
        ImageDenoiserPCA blueDenoiser = new ImageDenoiserPCA();
        
        // Copier les paramètres
        redDenoiser.setPatchSize(denoiser.getPatchSize());
        redDenoiser.setPercentToKeep(denoiser.getPercentToKeep());
        redDenoiser.setThresholdingFunctionType(denoiser.getThresholdingFunctionType());
        redDenoiser.setThresholdCalculationType(denoiser.getThresholdCalculationType());
        redDenoiser.setCoverageMode(denoiser.getCoverageMode());
        redDenoiser.setOverlapPercentage(denoiser.getOverlapPercentage());
        
        greenDenoiser.setPatchSize(denoiser.getPatchSize());
        greenDenoiser.setPercentToKeep(denoiser.getPercentToKeep());
        greenDenoiser.setThresholdingFunctionType(denoiser.getThresholdingFunctionType());
        greenDenoiser.setThresholdCalculationType(denoiser.getThresholdCalculationType());
        greenDenoiser.setCoverageMode(denoiser.getCoverageMode());
        greenDenoiser.setOverlapPercentage(denoiser.getOverlapPercentage());
        
        blueDenoiser.setPatchSize(denoiser.getPatchSize());
        blueDenoiser.setPercentToKeep(denoiser.getPercentToKeep());
        blueDenoiser.setThresholdingFunctionType(denoiser.getThresholdingFunctionType());
        blueDenoiser.setThresholdCalculationType(denoiser.getThresholdCalculationType());
        blueDenoiser.setCoverageMode(denoiser.getCoverageMode());
        blueDenoiser.setOverlapPercentage(denoiser.getOverlapPercentage());
        
        // Créer des images pour chaque canal
        Image redImage = new Image(new ImagePlus("Red", redBP));
        Image greenImage = new Image(new ImagePlus("Green", greenBP));
        Image blueImage = new Image(new ImagePlus("Blue", blueBP));
        
        // Débruitage parallèle
        CompletableFuture<Image> redFuture = CompletableFuture.supplyAsync(() -> redDenoiser.denoise(redImage));
        CompletableFuture<Image> greenFuture = CompletableFuture.supplyAsync(() -> greenDenoiser.denoise(greenImage));
        CompletableFuture<Image> blueFuture = CompletableFuture.supplyAsync(() -> blueDenoiser.denoise(blueImage));
        
        // Attendre que tous les canaux soient traités
        CompletableFuture.allOf(redFuture, greenFuture, blueFuture).join();
        
        try {
            Image denoisedRed = redFuture.get();
            Image denoisedGreen = greenFuture.get();
            Image denoisedBlue = blueFuture.get();
            
            // Extraire les pixels débruités
            byte[] denoisedR = new byte[width * height];
            byte[] denoisedG = new byte[width * height];
            byte[] denoisedB = new byte[width * height];
            
            ImageProcessor redIP = denoisedRed.getProcessor();
            ImageProcessor greenIP = denoisedGreen.getProcessor();
            ImageProcessor blueIP = denoisedBlue.getProcessor();
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    denoisedR[index] = (byte) redIP.getPixel(x, y);
                    denoisedG[index] = (byte) greenIP.getPixel(x, y);
                    denoisedB[index] = (byte) blueIP.getPixel(x, y);
                }
            }
            
            // Recombiner les canaux
            resultCP.setRGB(denoisedR, denoisedG, denoisedB);
        } catch (Exception e) {
            System.err.println("Erreur pendant le débruitage parallèle : " + e.getMessage());
            e.printStackTrace();
            
            // En cas d'erreur, revenir au traitement séquentiel
            denoisePerChannelSequential(cp, resultCP);
        }
    }
    
    /**
     * Calcule les métriques de qualité pour une image couleur débruitée
     * @param original l'image originale (sans bruit)
     * @param denoised l'image débruitée
     * @return un tableau avec [MSE, PSNR, SSIM]
     */
    public double[] calculateColorMetrics(ImagePlus original, ImagePlus denoised) {
        if (original == null || denoised == null) {
            throw new IllegalArgumentException("Les images ne peuvent pas être null");
        }
        
        if (original.getWidth() != denoised.getWidth() || original.getHeight() != denoised.getHeight()) {
            throw new IllegalArgumentException("Les images doivent avoir les mêmes dimensions");
        }
        
        ColorProcessor cpOrig = (ColorProcessor) original.getProcessor();
        ColorProcessor cpDen = (ColorProcessor) denoised.getProcessor();
        
        int width = cpOrig.getWidth();
        int height = cpOrig.getHeight();
        
        // Extraire les canaux
        byte[] R1 = new byte[width * height];
        byte[] G1 = new byte[width * height];
        byte[] B1 = new byte[width * height];
        cpOrig.getRGB(R1, G1, B1);
        
        byte[] R2 = new byte[width * height];
        byte[] G2 = new byte[width * height];
        byte[] B2 = new byte[width * height];
        cpDen.getRGB(R2, G2, B2);
        
        // Créer des processeurs pour chaque canal
        ByteProcessor redOrig = new ByteProcessor(width, height, R1, null);
        ByteProcessor greenOrig = new ByteProcessor(width, height, G1, null);
        ByteProcessor blueOrig = new ByteProcessor(width, height, B1, null);
        
        ByteProcessor redDen = new ByteProcessor(width, height, R2, null);
        ByteProcessor greenDen = new ByteProcessor(width, height, G2, null);
        ByteProcessor blueDen = new ByteProcessor(width, height, B2, null);
        
        // Calculer les métriques pour chaque canal
        double mseRed = MetricsUtils.calculateMSE(redOrig, redDen);
        double mseGreen = MetricsUtils.calculateMSE(greenOrig, greenDen);
        double mseBlue = MetricsUtils.calculateMSE(blueOrig, blueDen);
        
        double psnrRed = MetricsUtils.calculatePSNR(redOrig, redDen);
        double psnrGreen = MetricsUtils.calculatePSNR(greenOrig, greenDen);
        double psnrBlue = MetricsUtils.calculatePSNR(blueOrig, blueDen);
        
        double ssimRed = MetricsUtils.calculateSSIM(redOrig, redDen);
        double ssimGreen = MetricsUtils.calculateSSIM(greenOrig, greenDen);
        double ssimBlue = MetricsUtils.calculateSSIM(blueOrig, blueDen);
        
        // Calculer les moyennes
        double mseMean = (mseRed + mseGreen + mseBlue) / 3.0;
        double psnrMean = (psnrRed + psnrGreen + psnrBlue) / 3.0;
        double ssimMean = (ssimRed + ssimGreen + ssimBlue) / 3.0;
        
        return new double[] { mseMean, psnrMean, ssimMean };
    }
} 