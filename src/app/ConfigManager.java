package app;

import image.ImageDenoiserPCA;
import image.MultiScaleImageDenoiser;
import image.ColorImageDenoiser;
import patch.PatchManager;
import thresholding.ThresholdCalculationType;
import thresholding.ThresholdingFunctionType;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Gestionnaire de configurations pour sauvegarder et charger des paramètres de débruitage
 * Permet de faciliter la reproduction des résultats
 */
public class ConfigManager {
    
    /**
     * Structure pour stocker une configuration complète
     */
    public static class DenoiseConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        
        // Métadonnées
        private String name;
        private Date creationDate;
        private String description;
        
        // Type de débruisage
        private boolean useMultiScale = false;
        private boolean useColorDenoising = false;
        
        // Paramètres généraux
        private int patchSize = 8;
        private PatchManager.CoverageMode coverageMode = PatchManager.CoverageMode.OVERLAP_FULL;
        private double overlapPercentage = 50.0;
        private ThresholdingFunctionType thresholdingFunction = ThresholdingFunctionType.HARD;
        private ThresholdCalculationType thresholdCalculation = ThresholdCalculationType.UNIVERSAL;
        private double percentToKeep = 0.95;
        
        // Paramètres multi-échelle
        private List<Integer> patchSizes = new ArrayList<>();
        private List<Double> patchWeights = new ArrayList<>();
        
        // Paramètres couleur
        private ColorImageDenoiser.ColorStrategy colorStrategy = ColorImageDenoiser.ColorStrategy.PER_CHANNEL;
        
        // Paramètres d'optimisation
        private boolean useParallelProcessing = true;
        
        // Getters & Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Date getCreationDate() { return creationDate; }
        public void setCreationDate(Date date) { this.creationDate = date; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isUseMultiScale() { return useMultiScale; }
        public void setUseMultiScale(boolean useMultiScale) { this.useMultiScale = useMultiScale; }
        
        public boolean isUseColorDenoising() { return useColorDenoising; }
        public void setUseColorDenoising(boolean useColorDenoising) { this.useColorDenoising = useColorDenoising; }
        
        public int getPatchSize() { return patchSize; }
        public void setPatchSize(int patchSize) { this.patchSize = patchSize; }
        
        public PatchManager.CoverageMode getCoverageMode() { return coverageMode; }
        public void setCoverageMode(PatchManager.CoverageMode mode) { this.coverageMode = mode; }
        
        public double getOverlapPercentage() { return overlapPercentage; }
        public void setOverlapPercentage(double percentage) { this.overlapPercentage = percentage; }
        
        public ThresholdingFunctionType getThresholdingFunction() { return thresholdingFunction; }
        public void setThresholdingFunction(ThresholdingFunctionType type) { this.thresholdingFunction = type; }
        
        public ThresholdCalculationType getThresholdCalculation() { return thresholdCalculation; }
        public void setThresholdCalculation(ThresholdCalculationType type) { this.thresholdCalculation = type; }
        
        public double getPercentToKeep() { return percentToKeep; }
        public void setPercentToKeep(double percent) { this.percentToKeep = percent; }
        
        public List<Integer> getPatchSizes() { return patchSizes; }
        public void setPatchSizes(List<Integer> sizes) { this.patchSizes = sizes; }
        
        public List<Double> getPatchWeights() { return patchWeights; }
        public void setPatchWeights(List<Double> weights) { this.patchWeights = weights; }
        
        public ColorImageDenoiser.ColorStrategy getColorStrategy() { return colorStrategy; }
        public void setColorStrategy(ColorImageDenoiser.ColorStrategy strategy) { this.colorStrategy = strategy; }
        
        public boolean isUseParallelProcessing() { return useParallelProcessing; }
        public void setUseParallelProcessing(boolean useParallel) { this.useParallelProcessing = useParallel; }
        
        /**
         * Configure un débruiseur standard avec cette configuration
         * @param denoiser le débruiseur à configurer
         */
        public void configureDenoiser(ImageDenoiserPCA denoiser) {
            denoiser.setPatchSize(patchSize);
            denoiser.setCoverageMode(convertCoverageModeToInt(coverageMode));
            denoiser.setOverlapPercentage(overlapPercentage);
            denoiser.setThresholdingFunctionType(thresholdingFunction);
            denoiser.setThresholdCalculationType(thresholdCalculation);
            denoiser.setPercentToKeep(percentToKeep);
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
         * Configure un débruiseur multi-échelle avec cette configuration
         * @param denoiser le débruiseur multi-échelle à configurer
         */
        public void configureMultiScaleDenoiser(MultiScaleImageDenoiser denoiser) {
            denoiser.configureDenoiser(coverageMode, overlapPercentage, 
                                      thresholdingFunction, thresholdCalculation, percentToKeep);
        }
        
        /**
         * Configure un débruiseur couleur avec cette configuration
         * @param denoiser le débruiseur couleur à configurer
         */
        public void configureColorDenoiser(ColorImageDenoiser denoiser) {
            denoiser.setPatchSize(patchSize);
            denoiser.setCoverageMode(coverageMode);
            denoiser.setOverlapPercentage(overlapPercentage);
            denoiser.setThresholdingFunctionType(thresholdingFunction);
            denoiser.setThresholdCalculationType(thresholdCalculation);
            denoiser.setPercentToKeep(percentToKeep);
            denoiser.setColorStrategy(colorStrategy);
            denoiser.setUseParallelProcessing(useParallelProcessing);
        }
        
        /**
         * Configure le gestionnaire de patches avec cette configuration
         * @param patchManager le gestionnaire de patches à configurer
         */
        public void configurePatchManager(PatchManager patchManager) {
            patchManager.setUseParallelProcessing(useParallelProcessing);
        }
        
        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            StringBuilder sb = new StringBuilder();
            
            sb.append("Configuration: ").append(name).append("\n");
            sb.append("Date de création: ").append(sdf.format(creationDate)).append("\n");
            if (description != null && !description.isEmpty()) {
                sb.append("Description: ").append(description).append("\n");
            }
            
            sb.append("\nType de débruitage: ");
            if (useMultiScale) {
                sb.append("Multi-échelle");
            } else {
                sb.append("Standard");
            }
            
            if (useColorDenoising) {
                sb.append(" (Couleur - ").append(colorStrategy).append(")");
            } else {
                sb.append(" (Niveaux de gris)");
            }
            
            sb.append("\n\nParamètres généraux:\n");
            if (!useMultiScale) {
                sb.append("- Taille des patchs: ").append(patchSize).append("\n");
            }
            sb.append("- Mode de couverture: ").append(coverageMode).append("\n");
            sb.append("- Pourcentage de chevauchement: ").append(overlapPercentage).append("%\n");
            sb.append("- Fonction de seuillage: ").append(thresholdingFunction).append("\n");
            sb.append("- Calcul du seuil: ").append(thresholdCalculation).append("\n");
            sb.append("- Pourcentage de composantes à conserver: ").append(percentToKeep * 100).append("%\n");
            
            if (useMultiScale) {
                sb.append("\nParamètres multi-échelle:\n");
                sb.append("- Tailles de patchs: ").append(patchSizes).append("\n");
                sb.append("- Poids: ").append(patchWeights).append("\n");
            }
            
            sb.append("\nOptimisation:\n");
            sb.append("- Traitement parallèle: ").append(useParallelProcessing ? "Activé" : "Désactivé").append("\n");
            
            return sb.toString();
        }
        
        /**
         * Crée une configuration avec des paramètres par défaut
         * @param name nom de la configuration
         * @return une nouvelle configuration
         */
        public static DenoiseConfig createDefault(String name) {
            DenoiseConfig config = new DenoiseConfig();
            config.name = name;
            config.creationDate = new Date();
            config.description = "Configuration par défaut";
            
            return config;
        }
    }
    
    // Répertoire où stocker les configurations
    private String configDirectory;
    
    /**
     * Constructeur
     * @param configDir répertoire où stocker les configurations
     */
    public ConfigManager(String configDir) {
        this.configDirectory = configDir;
        ensureDirectoryExists();
    }
    
    /**
     * S'assure que le répertoire de configuration existe
     */
    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(Paths.get(configDirectory));
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du répertoire de configuration: " + e.getMessage());
        }
    }
    
    /**
     * Sauvegarde une configuration
     * @param config la configuration à sauvegarder
     * @return true si la sauvegarde a réussi, false sinon
     */
    public boolean saveConfig(DenoiseConfig config) {
        ensureDirectoryExists();
        
        // Générer un nom de fichier unique
        String filename = sanitizeFileName(config.getName()) + ".cfg";
        String filePath = configDirectory + File.separator + filename;
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(config);
            System.out.println("Configuration sauvegardée: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde de la configuration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Charge une configuration depuis un fichier
     * @param filename nom du fichier (sans chemin)
     * @return la configuration chargée, ou null en cas d'erreur
     */
    public DenoiseConfig loadConfig(String filename) {
        String filePath = configDirectory + File.separator + filename;
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            DenoiseConfig config = (DenoiseConfig) ois.readObject();
            System.out.println("Configuration chargée: " + filePath);
            return config;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur lors du chargement de la configuration: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Exporte une configuration au format texte
     * @param config la configuration à exporter
     * @param outputPath chemin du fichier de sortie
     * @return true si l'export a réussi, false sinon
     */
    public boolean exportConfigAsText(DenoiseConfig config, String outputPath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println(config.toString());
            return true;
        } catch (IOException e) {
            System.err.println("Erreur lors de l'export de la configuration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Liste toutes les configurations disponibles
     * @return liste des noms de fichiers de configuration
     */
    public List<String> listConfigurations() {
        ensureDirectoryExists();
        
        File dir = new File(configDirectory);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".cfg"));
        
        List<String> configFiles = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                configFiles.add(file.getName());
            }
        }
        
        return configFiles;
    }
    
    /**
     * Supprime une configuration
     * @param filename nom du fichier à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteConfig(String filename) {
        String filePath = configDirectory + File.separator + filename;
        
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression de la configuration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Crée plusieurs configurations prédéfinies
     * @return liste des configurations créées
     */
    public List<DenoiseConfig> createPredefinedConfigs() {
        List<DenoiseConfig> configs = new ArrayList<>();
        
        // Configuration standard
        DenoiseConfig standard = DenoiseConfig.createDefault("Standard");
        standard.setDescription("Configuration standard pour le débruitage d'images en niveaux de gris");
        standard.setPatchSize(8);
        standard.setCoverageMode(PatchManager.CoverageMode.OVERLAP_FULL);
        standard.setOverlapPercentage(50.0);
        standard.setThresholdingFunction(ThresholdingFunctionType.HARD);
        standard.setThresholdCalculation(ThresholdCalculationType.UNIVERSAL);
        standard.setPercentToKeep(0.95);
        configs.add(standard);
        
        // Configuration haute qualité
        DenoiseConfig highQuality = DenoiseConfig.createDefault("HauteQualite");
        highQuality.setDescription("Configuration pour un débruitage de haute qualité (plus lent)");
        highQuality.setPatchSize(12);
        highQuality.setCoverageMode(PatchManager.CoverageMode.OVERLAP_FULL);
        highQuality.setOverlapPercentage(75.0);
        highQuality.setThresholdingFunction(ThresholdingFunctionType.SOFT);
        highQuality.setThresholdCalculation(ThresholdCalculationType.BAYES);
        highQuality.setPercentToKeep(0.98);
        configs.add(highQuality);
        
        // Configuration multi-échelle
        DenoiseConfig multiScale = DenoiseConfig.createDefault("MultiEchelle");
        multiScale.setDescription("Configuration multi-échelle combinant plusieurs tailles de patches");
        multiScale.setUseMultiScale(true);
        multiScale.setPatchSizes(Arrays.asList(4, 8, 16));
        multiScale.setPatchWeights(Arrays.asList(0.2, 0.5, 0.3));
        multiScale.setCoverageMode(PatchManager.CoverageMode.OVERLAP_FULL);
        multiScale.setOverlapPercentage(50.0);
        multiScale.setThresholdingFunction(ThresholdingFunctionType.HARD);
        multiScale.setThresholdCalculation(ThresholdCalculationType.UNIVERSAL);
        multiScale.setPercentToKeep(0.95);
        configs.add(multiScale);
        
        // Configuration couleur
        DenoiseConfig color = DenoiseConfig.createDefault("Couleur");
        color.setDescription("Configuration pour le débruitage d'images couleur");
        color.setUseColorDenoising(true);
        color.setPatchSize(8);
        color.setCoverageMode(PatchManager.CoverageMode.OVERLAP_FULL);
        color.setOverlapPercentage(50.0);
        color.setThresholdingFunction(ThresholdingFunctionType.HARD);
        color.setThresholdCalculation(ThresholdCalculationType.UNIVERSAL);
        color.setPercentToKeep(0.95);
        color.setColorStrategy(ColorImageDenoiser.ColorStrategy.PER_CHANNEL);
        configs.add(color);
        
        // Configuration optimisée pour la performance
        DenoiseConfig fast = DenoiseConfig.createDefault("Rapide");
        fast.setDescription("Configuration rapide mais moins précise");
        fast.setPatchSize(8);
        fast.setCoverageMode(PatchManager.CoverageMode.NO_OVERLAP);
        fast.setThresholdingFunction(ThresholdingFunctionType.HARD);
        fast.setThresholdCalculation(ThresholdCalculationType.UNIVERSAL);
        fast.setPercentToKeep(0.9);
        configs.add(fast);
        
        return configs;
    }
    
    /**
     * Sanitize un nom de fichier en remplaçant les caractères interdits
     * @param input nom à sanitizer
     * @return nom sanitizé
     */
    private String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
} 