package app;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import image.Image;
import image.Imagette;
import image.ImageDenoiserPCA;
import image.ImageReconstructor;
import image.Patch;
import patch.IPatchOperation;
import patch.MetricsUtils;
import patch.PatchManager;
import pca.PCAEngine;
import thresholding.ThresholdCalculationType;
import thresholding.ThresholdingFunctionType;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Classe de test pour le débruitage d'images par ACP
 * Démontre les approches globale et locale
 */
public class TestDenoising {
    
    // Répertoire pour les tests
    private static String currentTestDir;
    private static final Scanner scanner = new Scanner(System.in);
    
    // Paramètres configurables
    private static double sigma = 20.0; // Niveau de bruit par défaut
    private static int patchSize = 8;   // Taille des patchs par défaut
    private static int imagetteSize = 32; // Taille des imagettes par défaut
    private static int overlapPercentage = 50; // Pourcentage de chevauchement par défaut
    private static ThresholdingFunctionType thresholdingType = ThresholdingFunctionType.SEUILLAGE_DUR;
    private static ThresholdCalculationType thresholdCalcType = ThresholdCalculationType.SEUIL_V_VISUSHRINK;
    private static boolean mirrorEdges = false; // Par défaut: retour en arrière pour les bords
    
    /**
     * Point d'entrée du programme
     * @param args arguments de la ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        try {
            // Création du dossier de test numéroté
            createNewTestDirectory();
            System.out.println("Dossier de test créé : " + currentTestDir);
            
            // Création des sous-dossiers de test
            createTestDirectories();
            
            // Interface utilisateur interactive
            menuPrincipal();
            
            System.out.println("Tests terminés avec succès !");
            
        } catch (Exception e) {
            System.err.println("Erreur pendant les tests : " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    /**
     * Affiche le menu principal et traite les choix de l'utilisateur
     */
    private static void menuPrincipal() throws Exception {
        while (true) {
            System.out.println("\n===== DÉBRUITAGE D'IMAGES PAR ACP =====");
            System.out.println("1. Sélectionner une image");
            System.out.println("2. Configurer les paramètres");
            System.out.println("3. Exécuter le débruitage avec l'approche globale");
            System.out.println("4. Exécuter le débruitage avec l'approche locale");
            System.out.println("5. Afficher les paramètres actuels");
            System.out.println("6. Visualiser les composantes principales");
            System.out.println("0. Quitter");
            System.out.print("Votre choix : ");
            
            int choix;
            try {
                choix = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Entrée invalide. Veuillez entrer un nombre.");
                continue;
            }
            
            switch (choix) {
                case 0:
                    System.out.println("Au revoir !");
                    return;
                case 1:
                    selectionnerImage();
                    break;
                case 2:
                    menuParametres();
                    break;
                case 3:
                    if (imageSelectionnee == null) {
                        System.out.println("Veuillez d'abord sélectionner une image.");
                    } else {
                        executerApprochGlobale();
                    }
                    break;
                case 4:
                    if (imageSelectionnee == null) {
                        System.out.println("Veuillez d'abord sélectionner une image.");
                    } else {
                        executerApprochLocale();
                    }
                    break;
                case 5:
                    afficherParametres();
                    break;
                case 6:
                    if (imageSelectionnee == null) {
                        System.out.println("Veuillez d'abord sélectionner une image.");
                    } else {
                        visualiserComposantesPrincipales();
                    }
                    break;
                default:
                    System.out.println("Choix invalide. Veuillez réessayer.");
                    break;
            }
        }
    }

    // Variables pour stocker l'image sélectionnée et son chemin
    private static String imageSelectionnee = null;
    private static String imageBaseName = null;

    /**
     * Permet à l'utilisateur de sélectionner une image
     */
    private static void selectionnerImage() {
        System.out.println("\n=== SÉLECTION D'IMAGE ===");
        System.out.println("Images disponibles dans le dossier 'images/test/original/' :");
        
        File directory = new File("images/test/original/");
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Le dossier d'images n'existe pas. Création...");
            directory.mkdirs();
            System.out.println("Veuillez ajouter des images dans " + directory.getAbsolutePath());
            return;
        }
        
        File[] files = directory.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".jpg") || 
            name.toLowerCase().endsWith(".jpeg") || 
            name.toLowerCase().endsWith(".png"));
        
        if (files == null || files.length == 0) {
            System.out.println("Aucune image trouvée dans " + directory.getAbsolutePath());
            return;
        }
        
        for (int i = 0; i < files.length; i++) {
            System.out.println((i+1) + ". " + files[i].getName());
        }
        
        System.out.print("Sélectionnez le numéro de l'image (1-" + files.length + ") : ");
        try {
            int choix = Integer.parseInt(scanner.nextLine().trim());
            if (choix >= 1 && choix <= files.length) {
                imageSelectionnee = "images/test/original/" + files[choix-1].getName();
                imageBaseName = stripExtension(files[choix-1].getName());
                System.out.println("Image sélectionnée : " + imageSelectionnee);
            } else {
                System.out.println("Numéro invalide.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrée invalide. Veuillez entrer un nombre.");
        }
    }

    /**
     * Affiche le menu des paramètres et traite les choix de l'utilisateur
     */
    private static void menuParametres() {
        while (true) {
            System.out.println("\n=== CONFIGURATION DES PARAMÈTRES ===");
            System.out.println("1. Niveau de bruit sigma (actuel: " + sigma + ")");
            System.out.println("2. Taille des patchs (actuel: " + patchSize + "x" + patchSize + ")");
            System.out.println("3. Taille des imagettes (actuel: " + imagetteSize + "x" + imagetteSize + ")");
            System.out.println("4. Pourcentage de chevauchement (actuel: " + overlapPercentage + "%)");
            System.out.println("5. Type de seuillage (actuel: " + 
                              (thresholdingType == ThresholdingFunctionType.SEUILLAGE_DUR ? "Dur" : "Doux") + ")");
            System.out.println("6. Méthode de calcul du seuil (actuel: " + 
                              (thresholdCalcType == ThresholdCalculationType.SEUIL_V_VISUSHRINK ? "VisuShrink" : "BayesShrink") + ")");
            System.out.println("7. Gestion des bords (actuel: " + 
                              (mirrorEdges ? "Effet miroir" : "Retour en arrière") + ")");
            System.out.println("0. Retour au menu principal");
            System.out.print("Votre choix : ");
            
            int choix;
            try {
                choix = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Entrée invalide. Veuillez entrer un nombre.");
                continue;
            }
            
            switch (choix) {
                case 0:
                    return;
                case 1:
                    configurerSigma();
                    break;
                case 2:
                    configurerTaillePatchs();
                    break;
                case 3:
                    configurerTailleImagettes();
                    break;
                case 4:
                    configurerChevauchement();
                    break;
                case 5:
                    configurerTypeSeuillage();
                    break;
                case 6:
                    configurerMethodeCalculSeuil();
                    break;
                case 7:
                    configurerGestionBords();
                    break;
                default:
                    System.out.println("Choix invalide. Veuillez réessayer.");
                    break;
            }
        }
    }
    
    /**
     * Configure le niveau de bruit sigma
     */
    private static void configurerSigma() {
        System.out.print("Entrez le niveau de bruit sigma (valeur recommandée: entre 10 et 50) : ");
        try {
            double nouveauSigma = Double.parseDouble(scanner.nextLine().trim());
            if (nouveauSigma > 0) {
                sigma = nouveauSigma;
                System.out.println("Niveau de bruit sigma configuré à " + sigma);
            } else {
                System.out.println("La valeur doit être positive.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrée invalide. Veuillez entrer un nombre.");
        }
    }
    
    /**
     * Configure la taille des patchs
     */
    private static void configurerTaillePatchs() {
        System.out.print("Entrez la taille des patchs (valeur recommandée: entre 4 et 16) : ");
        try {
            int nouveauPatchSize = Integer.parseInt(scanner.nextLine().trim());
            if (nouveauPatchSize > 0) {
                patchSize = nouveauPatchSize;
                System.out.println("Taille des patchs configurée à " + patchSize + "x" + patchSize);
            } else {
                System.out.println("La valeur doit être positive.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrée invalide. Veuillez entrer un nombre entier.");
        }
    }
    
    /**
     * Configure la taille des imagettes
     */
    private static void configurerTailleImagettes() {
        System.out.print("Entrez la taille des imagettes (valeur recommandée: entre 16 et 64) : ");
        try {
            int nouveauImagetteSize = Integer.parseInt(scanner.nextLine().trim());
            if (nouveauImagetteSize > 0) {
                imagetteSize = nouveauImagetteSize;
                System.out.println("Taille des imagettes configurée à " + imagetteSize + "x" + imagetteSize);
            } else {
                System.out.println("La valeur doit être positive.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrée invalide. Veuillez entrer un nombre entier.");
        }
    }
    
    /**
     * Configure le pourcentage de chevauchement
     */
    private static void configurerChevauchement() {
        System.out.print("Entrez le pourcentage de chevauchement (0-100) : ");
        try {
            int nouveauPourcentage = Integer.parseInt(scanner.nextLine().trim());
            if (nouveauPourcentage >= 0 && nouveauPourcentage <= 100) {
                overlapPercentage = nouveauPourcentage;
                System.out.println("Pourcentage de chevauchement configuré à " + overlapPercentage + "%");
            } else {
                System.out.println("La valeur doit être entre 0 et 100.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrée invalide. Veuillez entrer un nombre entier.");
        }
    }
    
    /**
     * Configure le type de seuillage
     */
    private static void configurerTypeSeuillage() {
        System.out.println("Choisissez le type de seuillage :");
        System.out.println("1. Seuillage dur (Hard Thresholding)");
        System.out.println("2. Seuillage doux (Soft Thresholding)");
        System.out.print("Votre choix : ");
        
        try {
            int choix = Integer.parseInt(scanner.nextLine().trim());
            if (choix == 1) {
                thresholdingType = ThresholdingFunctionType.SEUILLAGE_DUR;
                System.out.println("Type de seuillage configuré sur Dur");
            } else if (choix == 2) {
                thresholdingType = ThresholdingFunctionType.SEUILLAGE_DOUX;
                System.out.println("Type de seuillage configuré sur Doux");
            } else {
                System.out.println("Choix invalide.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrée invalide. Veuillez entrer un nombre.");
        }
    }
    
    /**
     * Configure la méthode de calcul du seuil
     */
    private static void configurerMethodeCalculSeuil() {
        System.out.println("Choisissez la méthode de calcul du seuil :");
        System.out.println("1. VisuShrink (seuil universel)");
        System.out.println("2. BayesShrink (seuil adaptatif)");
        System.out.print("Votre choix : ");
        
        try {
            int choix = Integer.parseInt(scanner.nextLine().trim());
            if (choix == 1) {
                thresholdCalcType = ThresholdCalculationType.SEUIL_V_VISUSHRINK;
                System.out.println("Méthode de calcul du seuil configurée sur VisuShrink");
            } else if (choix == 2) {
                thresholdCalcType = ThresholdCalculationType.SEUIL_B_BAYESSHRINK;
                System.out.println("Méthode de calcul du seuil configurée sur BayesShrink");
            } else {
                System.out.println("Choix invalide.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrée invalide. Veuillez entrer un nombre.");
        }
    }

    /**
     * Configure la gestion des bords
     */
    private static void configurerGestionBords() {
        System.out.println("Choisissez la méthode de gestion des bords :");
        System.out.println("1. Retour en arrière (recul pour les patchs/imagettes de bord)");
        System.out.println("2. Effet miroir (réflexion des pixels)");
        System.out.print("Votre choix : ");
        
        try {
            int choix = Integer.parseInt(scanner.nextLine().trim());
            if (choix == 1) {
                mirrorEdges = false;
                System.out.println("Gestion des bords configurée sur Retour en arrière");
            } else if (choix == 2) {
                mirrorEdges = true;
                System.out.println("Gestion des bords configurée sur Effet miroir");
            } else {
                System.out.println("Choix invalide.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrée invalide. Veuillez entrer un nombre.");
        }
    }

    /**
     * Affiche les paramètres actuels
     */
    private static void afficherParametres() {
        System.out.println("\n=== PARAMÈTRES ACTUELS ===");
        System.out.println("Image sélectionnée : " + (imageSelectionnee != null ? imageSelectionnee : "Aucune"));
        System.out.println("Niveau de bruit sigma : " + sigma);
        System.out.println("Taille des patchs : " + patchSize + "x" + patchSize);
        System.out.println("Taille des imagettes : " + imagetteSize + "x" + imagetteSize);
        System.out.println("Pourcentage de chevauchement : " + overlapPercentage + "%");
        System.out.println("Type de seuillage : " + (thresholdingType == ThresholdingFunctionType.SEUILLAGE_DUR ? "Dur" : "Doux"));
        System.out.println("Méthode de calcul du seuil : " + (thresholdCalcType == ThresholdCalculationType.SEUIL_V_VISUSHRINK ? "VisuShrink" : "BayesShrink"));
        System.out.println("Gestion des bords : " + (mirrorEdges ? "Effet miroir" : "Retour en arrière"));
    }
    
    /**
     * Exécute le débruitage avec l'approche globale
     */
    private static void executerApprochGlobale() throws Exception {
        System.out.println("\n=== APPROCHE GLOBALE ===");
        System.out.println("Chargement de l'image originale...");
        
        // Charger l'image originale
        Image X0 = loadImage(imageSelectionnee);
        if (X0 == null) {
            System.out.println("Erreur lors du chargement de l'image. Veuillez réessayer.");
            return;
        }
        
        System.out.println("Ajout du bruit avec sigma = " + sigma + "...");
        Image Xb = ajouterBruit(X0, sigma);
        
        // Enregistrer l'image bruitée
        saveImage(Xb, "noisy/" + imageBaseName + "_noisy_sigma" + sigma + ".jpg");
        System.out.println("Image bruitée sauvegardée.");
        
        // Créer un débruiter PCA
        ImageDenoiserPCA denoiser = new ImageDenoiserPCA();
        
        // Créer un gestionnaire de patches avec le chevauchement configuré
        PatchManager patchManager = new PatchManager();
        
        // Convertir le pourcentage de chevauchement en mode
        int coverageMode;
        if (overlapPercentage == 0) {
            coverageMode = PatchManager.NO_OVERLAP;
        } else {
            coverageMode = PatchManager.OVERLAP_FULL;
            // Le pourcentage sera utilisé dans l'extraction des patches
        }
        
        System.out.println("Extraction des patches avec taille = " + patchSize + 
                          " et chevauchement = " + overlapPercentage + "%...");
        
        // Extraire les patches avec le bon mode de chevauchement
        List<Patch> patches = patchManager.ExtractPatchs(Xb, patchSize, coverageMode, overlapPercentage, mirrorEdges);
        System.out.println(patches.size() + " patches extraits.");
        
        System.out.println("Débruitage en cours avec seuillage " + 
                         (thresholdingType == ThresholdingFunctionType.SEUILLAGE_DUR ? "dur" : "doux") + 
                         " et calcul de seuil " + 
                         (thresholdCalcType == ThresholdCalculationType.SEUIL_V_VISUSHRINK ? "VisuShrink" : "BayesShrink") + 
                         "...");
        
        // Appliquer le débruitage
        Image denoised = denoiser.denoiseImage(Xb, patches, thresholdingType, thresholdCalcType, sigma);
        
        // Nom du mode de seuillage pour le fichier
        String thresholdingName = (thresholdingType == ThresholdingFunctionType.SEUILLAGE_DUR ? "hard" : "soft");
        String calculationName = (thresholdCalcType == ThresholdCalculationType.SEUIL_V_VISUSHRINK ? "universal" : "bayes");
        
        // Enregistrer l'image débruitée
        String outputPath = "global/" + thresholdingName + "_" + calculationName + "/" + 
                           imageBaseName + "_global_" + thresholdingName + "_" + 
                           calculationName + "_sigma" + sigma + ".jpg";
        saveImage(denoised, outputPath);
        
        // Calculer et afficher les métriques
        double mse = MetricsUtils.calculateMSE(X0, denoised);
        double psnr = MetricsUtils.calculatePSNR(mse);
        
        System.out.println("\nMétriques pour l'approche globale :");
        System.out.println("MSE : " + mse);
        System.out.println("PSNR : " + psnr + " dB");
        System.out.println("Image débruitée sauvegardée dans " + outputPath);
    }
    
    /**
     * Exécute le débruitage avec l'approche locale
     */
    private static void executerApprochLocale() throws Exception {
        System.out.println("\n=== APPROCHE LOCALE ===");
        System.out.println("Chargement de l'image originale...");
        
        // Charger l'image originale
        Image X0 = loadImage(imageSelectionnee);
        if (X0 == null) {
            System.out.println("Erreur lors du chargement de l'image. Veuillez réessayer.");
            return;
        }
        
        System.out.println("Ajout du bruit avec sigma = " + sigma + "...");
        Image Xb = ajouterBruit(X0, sigma);
        
        // Enregistrer l'image bruitée
        saveImage(Xb, "noisy/" + imageBaseName + "_noisy_sigma" + sigma + ".jpg");
        System.out.println("Image bruitée sauvegardée.");
        
        // Créer un débruiter PCA
        ImageDenoiserPCA denoiser = new ImageDenoiserPCA();
        
        // Convertir le pourcentage de chevauchement en mode
        int coverageMode;
        if (overlapPercentage == 0) {
            coverageMode = PatchManager.NO_OVERLAP;
        } else {
            coverageMode = PatchManager.OVERLAP_FULL;
            // Le pourcentage sera utilisé dans l'extraction des imagettes
        }
        
        System.out.println("Traitement local avec taille d'imagette = " + imagetteSize + 
                          " et chevauchement = " + overlapPercentage + "%...");
        
        // Calculer le nombre d'imagettes nécessaires (calculé automatiquement dans DecoupeImage)
        int n = 0; // Sera ignoré pour les modes différents de EXACT_N
        
        // Gestionnaire de patches
        PatchManager patchManager = new PatchManager();
        
        // Découper l'image en imagettes
        System.out.println("Découpage de l'image en imagettes...");
        List<Imagette> imagettes = patchManager.DecoupeImage(Xb, imagetteSize, n, coverageMode, overlapPercentage, mirrorEdges);
        System.out.println(imagettes.size() + " imagettes extraites.");
        
        // Traiter chaque imagette
        System.out.println("Traitement des imagettes...");
        int count = 0;
        for (Imagette imagette : imagettes) {
            // Extraire les patches de l'imagette
            Image imagetteImage = new Image(imagette.getData());
            List<Patch> patches = patchManager.ExtractPatchs(imagetteImage, patchSize, coverageMode, overlapPercentage, mirrorEdges);
            
            // Débruiter l'imagette
            Image denoisedImagette = denoiser.denoiseImage(imagetteImage, patches, thresholdingType, thresholdCalcType, sigma);
            
            // Mettre à jour les données de l'imagette
            imagette.setData(denoisedImagette.getData());
            
            // Sauvegarder l'imagette pour visualisation
            if (count < 10) { // Limiter le nombre d'imagettes sauvegardées pour économiser de l'espace
                String coverage = overlapPercentage == 0 ? "no_overlap" : "overlap_" + overlapPercentage;
                String imagettePath = String.format("local/imagettes/%s/%s/imagette%d.jpg", 
                                                 imageBaseName, coverage, count);
                saveImage(denoisedImagette, imagettePath);
            }
            count++;
        }
        
        // Reconstruire l'image finale à partir des imagettes débruitées
        System.out.println("Reconstruction de l'image finale...");
        Image reconstructedImage = ImageReconstructor.reconstruireImageDepuisImagettes(
            imagettes, X0.getHeight(), X0.getWidth());
        
        // Nom du mode de seuillage pour le fichier
        String thresholdingName = (thresholdingType == ThresholdingFunctionType.SEUILLAGE_DUR ? "hard" : "soft");
        String calculationName = (thresholdCalcType == ThresholdCalculationType.SEUIL_V_VISUSHRINK ? "universal" : "bayes");
        String coverage = overlapPercentage == 0 ? "no_overlap" : "overlap_" + overlapPercentage;
        
        // Enregistrer l'image reconstruite
        String outputPath = String.format("local/reconstructed/%s_local_%s_%s_%s_sigma%s.jpg", 
                                       imageBaseName, thresholdingName, calculationName, coverage, sigma);
        saveImage(reconstructedImage, outputPath);
        
        // Calculer et afficher les métriques
        double mse = MetricsUtils.calculateMSE(X0, reconstructedImage);
        double psnr = MetricsUtils.calculatePSNR(mse);
        
        System.out.println("\nMétriques pour l'approche locale :");
        System.out.println("MSE : " + mse);
        System.out.println("PSNR : " + psnr + " dB");
        System.out.println("Image reconstruite sauvegardée dans " + outputPath);
    }

    /**
     * Crée un nouveau répertoire de test avec un numéro incrémenté
     */
    private static void createNewTestDirectory() {
        // Créer le répertoire de base s'il n'existe pas
        File baseDir = new File("images");
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        // Trouver le prochain numéro de test disponible
        int testNumber = 1;
        File testDir;
        
        do {
            testDir = new File(baseDir, "test" + testNumber);
            testNumber++;
        } while (testDir.exists());

        // Créer le nouveau répertoire
        testDir.mkdirs();
        currentTestDir = testDir.getPath();
    }

    /**
     * Crée les dossiers nécessaires pour les tests
     */
    private static void createTestDirectories() {
        String[] dirs = {
            "original",
            "noisy",
            "global/hard_universal",
            "global/soft_universal",
            "global/hard_bayes",
            "global/soft_bayes",
            "local/imagettes",
            "local/reconstructed"
        };
        
        // Créer chaque sous-dossier
        for (String dir : dirs) {
            File directory = new File(currentTestDir, dir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (created) {
                    System.out.println("Dossier créé : " + directory.getPath());
                }
            }
        }
    }
    
    /**
     * Ajoute du bruit gaussien à une image
     * @param image Image originale
     * @param sigma Écart-type du bruit
     * @return Image bruitée
     */
    private static Image ajouterBruit(Image image, double sigma) {
        int height = image.getHeight();
        int width = image.getWidth();
        double[][] noisyData = new double[height][width];
        
        // Générateur de nombres aléatoires
        java.util.Random random = new java.util.Random();
        
        // Ajouter du bruit gaussien à chaque pixel
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                double noise = random.nextGaussian() * sigma;
                noisyData[i][j] = Math.max(0, Math.min(255, image.getPixel(i, j) + noise));
            }
        }
        
        return new Image(noisyData);
    }

    /**
     * Charge une image en niveaux de gris
     */
    private static Image loadImage(String path) {
        try {
        System.out.println("Tentative de chargement de l'image : " + path);
        
        // Vérifier que le fichier existe
        File file = new File(path);
            if (!file.exists()) {
                System.err.println("Le fichier " + path + " n'existe pas");
                return null;
        }
        
        // Charger l'image avec ImageJ
        ImagePlus imp = IJ.openImage(file.getAbsolutePath());
        if (imp == null) {
                System.err.println("Impossible de charger l'image " + file.getAbsolutePath());
                return null;
        }
        
        // Convertir en niveaux de gris
        ImageProcessor ip = imp.getProcessor();
        ip = ip.convertToByte(true);  // Conversion en niveaux de gris
        
        // Créer notre objet Image
        int width = ip.getWidth();
        int height = ip.getHeight();
            
            // Convertir en niveaux de gris et normaliser entre 0 et 255
            double[][] pixels = new double[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y][x] = ip.getPixelValue(x, y);
                }
            }
            
            return new Image(pixels);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sauvegarde une image
     */
    private static void saveImage(Image image, String path) {
        try {
        // Utiliser le dossier de test courant
        String fullPath = currentTestDir + "/" + path;
        
        // Créer le dossier parent si nécessaire
        File file = new File(fullPath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
        
        int width = image.getWidth();
        int height = image.getHeight();
            ImagePlus imp = IJ.createImage("Image", "8-bit", width, height, 1);
            ImageProcessor ip = imp.getProcessor();
            
            // Copier les pixels
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                    // Convertir et limiter aux valeurs possibles pour une image 8 bits
                    int value = (int) Math.round(Math.max(0, Math.min(255, image.getPixel(i, j))));
                    ip.putPixel(j, i, value);
                }
            }
            
            // Sauvegarder l'image
            IJ.saveAs(imp, "jpg", fullPath);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde de l'image: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Retire l'extension d'un nom de fichier
     */
    private static String stripExtension(String filename) {
        int dotPos = filename.lastIndexOf('.');
        if (dotPos > 0) {
            return filename.substring(0, dotPos);
        }
        return filename;
    }

    /**
     * Visualise les composantes principales de l'image sélectionnée
     */
    private static void visualiserComposantesPrincipales() {
        try {
            System.out.println("\n=== VISUALISATION DES COMPOSANTES PRINCIPALES ===");
            
            // Charger l'image
            Image image = loadImage(imageSelectionnee);
            System.out.println("Image chargée : " + imageSelectionnee);
            
            // Créer un répertoire spécifique pour les visualisations
            String outputDir = currentTestDir + "/pca_visualization/" + imageBaseName;
            new File(outputDir).mkdirs();
            
            // Configuration du nombre de composantes à visualiser
            System.out.print("Nombre de composantes principales à visualiser (par défaut: 20): ");
            String input = scanner.nextLine().trim();
            int numComponents = input.isEmpty() ? 20 : Integer.parseInt(input);
            
            System.out.println("Génération des visualisations en cours...");
            
            // Utiliser le débruiseur PCA pour générer les visualisations
            ImageDenoiserPCA denoiser = new ImageDenoiserPCA();
            boolean success = denoiser.generatePCAVisualizations(image, patchSize, numComponents, outputDir);
            
            if (success) {
                System.out.println("Visualisations générées avec succès dans : " + outputDir);
                System.out.println("  - Vecteurs propres : " + outputDir + "/eigenvectors/");
                System.out.println("  - Reconstructions : " + outputDir + "/reconstructions/");
                System.out.println("  - Contribution en variance : " + outputDir + "/variance_explained.jpg");
                
                // Afficher les statistiques de variance expliquée
                double[] varianceExplained = denoiser.analyzeVarianceExplained(image, patchSize);
                if (varianceExplained.length > 0) {
                    System.out.println("\nVariance expliquée par les composantes principales:");
                    System.out.println("  - Top 1 CP: " + String.format("%.2f%%", varianceExplained[0]));
                    System.out.println("  - Top 5 CPs: " + String.format("%.2f%%", varianceExplained[4]));
                    System.out.println("  - Top 10 CPs: " + String.format("%.2f%%", varianceExplained[9]));
                    System.out.println("  - Top 20 CPs: " + String.format("%.2f%%", varianceExplained[Math.min(19, varianceExplained.length-1)]));
                    System.out.println("  - Top 50 CPs: " + String.format("%.2f%%", varianceExplained[Math.min(49, varianceExplained.length-1)]));
                }
            } else {
                System.out.println("Erreur lors de la génération des visualisations.");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la visualisation des composantes principales : " + e.getMessage());
            e.printStackTrace();
        }
    }
} 