package app;

import image.Image;
import image.ImageDenoiserPCA;
import image.MultiScaleImageDenoiser;
import patch.MetricsUtils;
import patch.PatchManager;
import thresholding.ThresholdCalculationType;
import thresholding.ThresholdingFunctionType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Classe pour comparer automatiquement différentes approches de débruitage
 * et générer un rapport statistique.
 */
public class DenoiseComparer {

    public static class DenoiseResult {
        private final String approachName;
        private final Image denoisedImage;
        private final double mse;
        private final double psnr;
        private final double ssim;
        private final long executionTimeMs;

        public DenoiseResult(String approachName, Image denoisedImage, double mse, double psnr, double ssim, long executionTimeMs) {
            this.approachName = approachName;
            this.denoisedImage = denoisedImage;
            this.mse = mse;
            this.psnr = psnr;
            this.ssim = ssim;
            this.executionTimeMs = executionTimeMs;
        }

        public String getApproachName() { return approachName; }
        public Image getDenoisedImage() { return denoisedImage; }
        public double getMse() { return mse; }
        public double getPsnr() { return psnr; }
        public double getSsim() { return ssim; }
        public long getExecutionTimeMs() { return executionTimeMs; }

        @Override
        public String toString() {
            return String.format("%s: MSE=%.2f, PSNR=%.2f dB, SSIM=%.4f, Time=%d ms", 
                    approachName, mse, psnr, ssim, executionTimeMs);
        }
    }

    private final Image originalImage;
    private final Image noisyImage;
    private final List<DenoiseResult> results = new ArrayList<>();
    private final String outputDir;
    private final int threadCount;

    /**
     * Constructeur pour le comparateur de débruitage
     * 
     * @param originalImage L'image originale (sans bruit)
     * @param noisyImage L'image bruitée à débruiter
     * @param outputDir Le répertoire de sortie pour sauvegarder les images et les rapports
     * @param threadCount Nombre de threads à utiliser pour le parallélisme
     */
    public DenoiseComparer(Image originalImage, Image noisyImage, String outputDir, int threadCount) {
        this.originalImage = originalImage;
        this.noisyImage = noisyImage;
        this.outputDir = outputDir;
        this.threadCount = threadCount;
        
        // Création du répertoire de sortie s'il n'existe pas
        new File(outputDir).mkdirs();
    }

    /**
     * Ajoute une approche de débruitage à comparer (approche globale)
     * 
     * @param name Nom de l'approche
     * @param patchSize Taille des patches
     * @param coverageMode Mode de couverture des patches
     * @param overlapPercentage Pourcentage de chevauchement
     * @param thresholdingFunction Type de fonction de seuillage
     * @param thresholdCalculation Type de calcul du seuil
     * @param percentToKeep Pourcentage de composantes à conserver
     */
    public void addGlobalApproach(String name, int patchSize, PatchManager.CoverageMode coverageMode, 
                              double overlapPercentage, ThresholdingFunctionType thresholdingFunction, 
                              ThresholdCalculationType thresholdCalculation, double percentToKeep) {
        
        ImageDenoiserPCA denoiser = new ImageDenoiserPCA();
        denoiser.setPatchSize(patchSize);
        denoiser.setCoverageMode(convertCoverageModeToInt(coverageMode));
        denoiser.setOverlapPercentage(overlapPercentage);
        denoiser.setThresholdingFunctionType(thresholdingFunction);
        denoiser.setThresholdCalculationType(thresholdCalculation);
        denoiser.setPercentToKeep(percentToKeep);
        
        long startTime = System.currentTimeMillis();
        Image denoisedImage = denoiser.denoise(noisyImage);
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Calcul des métriques
        double mse = MetricsUtils.calculateMSE(originalImage.getProcessor(), denoisedImage.getProcessor());
        double psnr = MetricsUtils.calculatePSNR(originalImage.getProcessor(), denoisedImage.getProcessor());
        double ssim = MetricsUtils.calculateSSIM(originalImage.getProcessor(), denoisedImage.getProcessor());
        
        DenoiseResult result = new DenoiseResult(name, denoisedImage, mse, psnr, ssim, executionTime);
        results.add(result);
        System.out.println("Completed: " + result);
        
        // Sauvegarde de l'image
        String fileName = outputDir + "/denoised_" + sanitizeFileName(name) + ".jpg";
        denoisedImage.save(fileName);
    }
    
    /**
     * Convertit le PatchManager.CoverageMode en int pour ImageDenoiserPCA
     * @param mode Le mode de couverture PatchManager.CoverageMode
     * @return Le mode de couverture en int
     */
    private int convertCoverageModeToInt(PatchManager.CoverageMode mode) {
        switch (mode) {
            case EXACT_N: return PatchManager.EXACT_N;
            case OVERLAP_FULL: return PatchManager.OVERLAP_FULL;
            case NO_OVERLAP: return PatchManager.NO_OVERLAP;
            default: return PatchManager.OVERLAP_FULL;
        }
    }
    
    /**
     * Ajoute une approche multi-échelle de débruitage à comparer
     * 
     * @param name Nom de l'approche
     * @param patchSizes Liste des tailles de patches
     * @param weights Liste des poids pour chaque taille
     * @param coverageMode Mode de couverture des patches
     * @param overlapPercentage Pourcentage de chevauchement
     * @param thresholdingFunction Type de fonction de seuillage
     * @param thresholdCalculation Type de calcul du seuil
     * @param percentToKeep Pourcentage de composantes à conserver
     */
    public void addMultiScaleApproach(String name, List<Integer> patchSizes, List<Double> weights, 
                                   PatchManager.CoverageMode coverageMode, double overlapPercentage, 
                                   ThresholdingFunctionType thresholdingFunction, 
                                   ThresholdCalculationType thresholdCalculation, double percentToKeep) {
        
        MultiScaleImageDenoiser denoiser = new MultiScaleImageDenoiser(originalImage, noisyImage, patchSizes, weights);
        denoiser.configureDenoiser(coverageMode, overlapPercentage, thresholdingFunction, thresholdCalculation, percentToKeep);
        
        long startTime = System.currentTimeMillis();
        Image denoisedImage = denoiser.denoise();
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Calcul des métriques
        double mse = MetricsUtils.calculateMSE(originalImage.getProcessor(), denoisedImage.getProcessor());
        double psnr = MetricsUtils.calculatePSNR(originalImage.getProcessor(), denoisedImage.getProcessor());
        double ssim = MetricsUtils.calculateSSIM(originalImage.getProcessor(), denoisedImage.getProcessor());
        
        DenoiseResult result = new DenoiseResult(name, denoisedImage, mse, psnr, ssim, executionTime);
        results.add(result);
        System.out.println("Completed: " + result);
        
        // Sauvegarde de l'image
        String fileName = outputDir + "/denoised_" + sanitizeFileName(name) + ".jpg";
        denoisedImage.save(fileName);
    }
    
    /**
     * Lance plusieurs tâches de débruitage en parallèle pour une comparaison rapide
     * 
     * @param approaches Liste des configurations à tester
     */
    public void runParallelComparison(List<Map<String, Object>> approaches) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (Map<String, Object> approach : approaches) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                String name = (String) approach.get("name");
                
                if (approach.containsKey("patchSizes")) {
                    // Multi-scale approach
                    @SuppressWarnings("unchecked")
                    List<Integer> patchSizes = (List<Integer>) approach.get("patchSizes");
                    @SuppressWarnings("unchecked")
                    List<Double> weights = (List<Double>) approach.get("weights");
                    PatchManager.CoverageMode coverageMode = (PatchManager.CoverageMode) approach.get("coverageMode");
                    double overlapPercentage = (double) approach.get("overlapPercentage");
                    ThresholdingFunctionType thresholdingFunction = (ThresholdingFunctionType) approach.get("thresholdingFunction");
                    ThresholdCalculationType thresholdCalculation = (ThresholdCalculationType) approach.get("thresholdCalculation");
                    double percentToKeep = (double) approach.get("percentToKeep");
                    
                    addMultiScaleApproach(name, patchSizes, weights, coverageMode, overlapPercentage, 
                            thresholdingFunction, thresholdCalculation, percentToKeep);
                } else {
                    // Global approach
                    int patchSize = (int) approach.get("patchSize");
                    PatchManager.CoverageMode coverageMode = (PatchManager.CoverageMode) approach.get("coverageMode");
                    double overlapPercentage = (double) approach.get("overlapPercentage");
                    ThresholdingFunctionType thresholdingFunction = (ThresholdingFunctionType) approach.get("thresholdingFunction");
                    ThresholdCalculationType thresholdCalculation = (ThresholdCalculationType) approach.get("thresholdCalculation");
                    double percentToKeep = (double) approach.get("percentToKeep");
                    
                    addGlobalApproach(name, patchSize, coverageMode, overlapPercentage, 
                            thresholdingFunction, thresholdCalculation, percentToKeep);
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Attendre que toutes les tâches soient terminées
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();
        
        executor.shutdown();
    }
    
    /**
     * Génère un rapport complet avec les résultats de la comparaison
     * 
     * @return Le chemin du fichier de rapport généré
     */
    public String generateReport() {
        // Tri des résultats par PSNR décroissant (meilleur en premier)
        results.sort(Comparator.comparingDouble(DenoiseResult::getPsnr).reversed());
        
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String reportFile = outputDir + "/comparison_report_" + timestamp + ".md";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            // En-tête du rapport
            writer.println("# Rapport de Comparaison des Approches de Débruitage");
            writer.println("\nDate: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
            writer.println("\n## Information sur l'image");
            writer.println("- Dimensions: " + originalImage.getWidth() + "x" + originalImage.getHeight() + " pixels");
            
            // Tableau des résultats
            writer.println("\n## Résultats de la comparaison");
            writer.println("\n| Approche | MSE | PSNR (dB) | SSIM | Temps (ms) |");
            writer.println("| --- | ---: | ---: | ---: | ---: |");
            
            for (DenoiseResult result : results) {
                writer.printf("| %s | %.2f | %.2f | %.4f | %d |\n", 
                        result.getApproachName(), result.getMse(), result.getPsnr(), 
                        result.getSsim(), result.getExecutionTimeMs());
            }
            
            // Meilleure approche
            DenoiseResult best = results.get(0);
            writer.println("\n## Meilleure approche");
            writer.println("\nLa meilleure approche basée sur le PSNR est **" + best.getApproachName() + "**");
            writer.println("\n- MSE: " + String.format("%.2f", best.getMse()));
            writer.println("- PSNR: " + String.format("%.2f dB", best.getPsnr()));
            writer.println("- SSIM: " + String.format("%.4f", best.getSsim()));
            writer.println("- Temps d'exécution: " + best.getExecutionTimeMs() + " ms");
            
            // Visualisation des résultats
            writer.println("\n## Visualisation");
            writer.println("\n### Image originale");
            writer.println("![Original](" + getRelativePath(outputDir + "/original.jpg") + ")");
            
            writer.println("\n### Image bruitée");
            writer.println("![Noisy](" + getRelativePath(outputDir + "/noisy.jpg") + ")");
            
            // Génération des liens vers les images débruitées
            writer.println("\n### Images débruitées");
            for (DenoiseResult result : results) {
                String imagePath = outputDir + "/denoised_" + sanitizeFileName(result.getApproachName()) + ".jpg";
                writer.println("\n#### " + result.getApproachName());
                writer.println("![" + result.getApproachName() + "](" + getRelativePath(imagePath) + ")");
                writer.println("- PSNR: " + String.format("%.2f dB", result.getPsnr()));
                writer.println("- SSIM: " + String.format("%.4f", result.getSsim()));
            }
            
        } catch (IOException e) {
            System.err.println("Erreur lors de la génération du rapport: " + e.getMessage());
            return null;
        }
        
        // Sauvegarde des images originales et bruitées pour référence
        originalImage.save(outputDir + "/original.jpg");
        noisyImage.save(outputDir + "/noisy.jpg");
        
        System.out.println("Rapport généré: " + reportFile);
        return reportFile;
    }
    
    /**
     * Sanitize un nom de fichier en remplaçant les caractères non valides
     */
    private String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    /**
     * Obtient le chemin relatif d'un fichier par rapport au répertoire de sortie
     */
    private String getRelativePath(String fullPath) {
        return new File(fullPath).getName();
    }
    
    /**
     * Crée des configurations de test standard pour différentes approches
     * 
     * @return Liste des configurations à tester
     */
    public static List<Map<String, Object>> createStandardConfigurations() {
        List<Map<String, Object>> configs = new ArrayList<>();
        
        // Différentes tailles de patch pour l'approche globale
        for (int patchSize : new int[]{4, 8, 12, 16}) {
            Map<String, Object> config = new HashMap<>();
            config.put("name", "Global-Patch" + patchSize + "-Hard-Universal");
            config.put("patchSize", patchSize);
            config.put("coverageMode", PatchManager.CoverageMode.OVERLAP_FULL);
            config.put("overlapPercentage", 0.5);
            config.put("thresholdingFunction", ThresholdingFunctionType.HARD);
            config.put("thresholdCalculation", ThresholdCalculationType.UNIVERSAL);
            config.put("percentToKeep", 0.95);
            configs.add(config);
        }
        
        // Différentes fonctions de seuillage
        for (ThresholdingFunctionType thresholdType : ThresholdingFunctionType.values()) {
            Map<String, Object> config = new HashMap<>();
            config.put("name", "Global-Patch8-" + thresholdType.name() + "-Universal");
            config.put("patchSize", 8);
            config.put("coverageMode", PatchManager.CoverageMode.OVERLAP_FULL);
            config.put("overlapPercentage", 0.5);
            config.put("thresholdingFunction", thresholdType);
            config.put("thresholdCalculation", ThresholdCalculationType.UNIVERSAL);
            config.put("percentToKeep", 0.95);
            configs.add(config);
        }
        
        // Différents types de calcul de seuil
        for (ThresholdCalculationType calcType : ThresholdCalculationType.values()) {
            Map<String, Object> config = new HashMap<>();
            config.put("name", "Global-Patch8-Hard-" + calcType.name());
            config.put("patchSize", 8);
            config.put("coverageMode", PatchManager.CoverageMode.OVERLAP_FULL);
            config.put("overlapPercentage", 0.5);
            config.put("thresholdingFunction", ThresholdingFunctionType.HARD);
            config.put("thresholdCalculation", calcType);
            config.put("percentToKeep", 0.95);
            configs.add(config);
        }
        
        // Approche multi-échelle avec différentes configurations
        Map<String, Object> multiConfig1 = new HashMap<>();
        multiConfig1.put("name", "MultiScale-4-8-12-Equal");
        multiConfig1.put("patchSizes", Arrays.asList(4, 8, 12));
        multiConfig1.put("weights", Arrays.asList(1.0, 1.0, 1.0));
        multiConfig1.put("coverageMode", PatchManager.CoverageMode.OVERLAP_FULL);
        multiConfig1.put("overlapPercentage", 0.5);
        multiConfig1.put("thresholdingFunction", ThresholdingFunctionType.HARD);
        multiConfig1.put("thresholdCalculation", ThresholdCalculationType.UNIVERSAL);
        multiConfig1.put("percentToKeep", 0.95);
        configs.add(multiConfig1);
        
        Map<String, Object> multiConfig2 = new HashMap<>();
        multiConfig2.put("name", "MultiScale-4-8-16-Weighted");
        multiConfig2.put("patchSizes", Arrays.asList(4, 8, 16));
        multiConfig2.put("weights", Arrays.asList(0.2, 0.5, 0.3));
        multiConfig2.put("coverageMode", PatchManager.CoverageMode.OVERLAP_FULL);
        multiConfig2.put("overlapPercentage", 0.5);
        multiConfig2.put("thresholdingFunction", ThresholdingFunctionType.SOFT);
        multiConfig2.put("thresholdCalculation", ThresholdCalculationType.UNIVERSAL);
        multiConfig2.put("percentToKeep", 0.95);
        configs.add(multiConfig2);
        
        return configs;
    }
} 