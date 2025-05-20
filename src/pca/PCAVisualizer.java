package pca;

import image.Image;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import Jama.Matrix;

/**
 * Classe pour visualiser les composantes principales sous forme d'images
 * Permet de mieux comprendre ce que l'ACP a appris
 */
public class PCAVisualizer {

    /**
     * Génère des images représentant les vecteurs propres/composantes principales
     * 
     * @param eigenVectors les vecteurs propres à visualiser (colonne par colonne)
     * @param nComponents nombre de composantes à visualiser (top N)
     * @param patchSize taille des patches (pour reconstruire des images carrées)
     * @param outputDir répertoire où sauvegarder les images
     * @return liste des images générées
     */
    public List<Image> visualizeEigenVectors(Matrix eigenVectors, int nComponents, int patchSize, String outputDir) {
        // Création du répertoire de sortie s'il n'existe pas
        new File(outputDir).mkdirs();
        
        List<Image> eigenImages = new ArrayList<>();
        
        // Limiter le nombre de composantes à visualiser
        int maxComponents = Math.min(nComponents, eigenVectors.getColumnDimension());
        
        // Pour chaque composante principale (vecteur propre)
        for (int i = 0; i < maxComponents; i++) {
            // Extraire le vecteur propre
            double[] eigenVector = new double[eigenVectors.getRowDimension()];
            for (int j = 0; j < eigenVectors.getRowDimension(); j++) {
                eigenVector[j] = eigenVectors.get(j, i);
            }
            
            // Convertir le vecteur propre en image
            Image eigenImage = createImageFromEigenVector(eigenVector, patchSize);
            
            // Normaliser l'image pour une meilleure visualisation
            normalizeImage(eigenImage);
            
            // Sauvegarder l'image
            String filename = String.format("%s/eigenvector_%03d.jpg", outputDir, i+1);
            eigenImage.save(filename);
            
            eigenImages.add(eigenImage);
        }
        
        return eigenImages;
    }
    
    /**
     * Crée une image à partir d'un vecteur propre
     * 
     * @param eigenVector le vecteur propre (d² éléments pour un patch de taille d×d)
     * @param patchSize taille du patch (pour reconstruire une image carrée)
     * @return l'image reconstruite
     */
    private Image createImageFromEigenVector(double[] eigenVector, int patchSize) {
        if (eigenVector.length != patchSize * patchSize) {
            throw new IllegalArgumentException(
                "La taille du vecteur propre (" + eigenVector.length + 
                ") ne correspond pas au carré de la taille du patch (" + 
                patchSize * patchSize + ")"
            );
        }
        
        // Créer une nouvelle image vide
        Image image = new Image(patchSize, patchSize);
        ImageProcessor processor = image.getProcessor();
        
        // Remplir l'image avec les valeurs du vecteur propre
        for (int i = 0; i < patchSize; i++) {
            for (int j = 0; j < patchSize; j++) {
                double value = eigenVector[i * patchSize + j];
                processor.putPixelValue(j, i, value);
            }
        }
        
        return image;
    }
    
    /**
     * Normalise les valeurs d'une image pour une meilleure visualisation
     * Les valeurs sont mises à l'échelle entre 0 et 255
     * 
     * @param image l'image à normaliser
     */
    private void normalizeImage(Image image) {
        ImageProcessor processor = image.getProcessor();
        
        // Trouver les valeurs min et max
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                double value = processor.getPixelValue(j, i);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }
        
        // Éviter division par zéro
        double range = max - min;
        if (range < 1e-10) {
            range = 1.0;
        }
        
        // Normaliser l'image
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                double value = processor.getPixelValue(j, i);
                double normalized = 255.0 * (value - min) / range;
                processor.putPixelValue(j, i, normalized);
            }
        }
    }
    
    /**
     * Génère une image de reconstruction de qualité croissante en utilisant de plus en plus
     * de composantes principales
     * 
     * @param eigenVectors les vecteurs propres
     * @param meanVector le vecteur moyen
     * @param coefficients les coefficients de projection sur les composantes principales
     * @param patchSize taille du patch
     * @param outputDir répertoire de sortie
     * @return liste des images générées pour différents nombres de composantes
     */
    public List<Image> visualizeReconstruction(Matrix eigenVectors, double[] meanVector, 
                                             double[] coefficients, int patchSize, String outputDir) {
        // Création du répertoire de sortie s'il n'existe pas
        new File(outputDir).mkdirs();
        
        List<Image> reconstructions = new ArrayList<>();
        
        // Nombre total de composantes disponibles
        int totalComponents = eigenVectors.getColumnDimension();
        
        // Générer des reconstructions avec un nombre croissant de composantes
        // Utilisons une échelle logarithmique pour mieux voir les différences
        int[] componentsToVisualize = {1, 2, 3, 5, 10, 15, 20, 30, 50, 75, 100};
        
        for (int numComponents : componentsToVisualize) {
            if (numComponents > totalComponents) {
                break;  // Ne pas dépasser le nombre de composantes disponibles
            }
            
            // Reconstruire avec les N premières composantes
            double[] reconstructed = new double[patchSize * patchSize];
            
            // Ajouter d'abord le vecteur moyen
            for (int i = 0; i < reconstructed.length; i++) {
                reconstructed[i] = meanVector[i];
            }
            
            // Ajouter la contribution de chaque composante principale
            for (int comp = 0; comp < numComponents; comp++) {
                double coefficient = coefficients[comp];
                
                for (int i = 0; i < reconstructed.length; i++) {
                    reconstructed[i] += coefficient * eigenVectors.get(i, comp);
                }
            }
            
            // Convertir en image
            Image reconstructedImage = createImageFromEigenVector(reconstructed, patchSize);
            
            // Sauvegarder l'image
            String filename = String.format("%s/reconstruction_%03d_components.jpg", outputDir, numComponents);
            reconstructedImage.save(filename);
            
            reconstructions.add(reconstructedImage);
        }
        
        return reconstructions;
    }
    
    /**
     * Crée une image montrant la contribution relative des principales composantes
     * 
     * @param eigenValues les valeurs propres (variance expliquée par chaque composante)
     * @param outputDir répertoire de sortie
     * @return l'image générée
     */
    public Image visualizeVarianceExplained(double[] eigenValues, String outputDir) {
        // Création du répertoire de sortie s'il n'existe pas
        new File(outputDir).mkdirs();
        
        // Nombre de composantes à visualiser
        int nComponents = Math.min(100, eigenValues.length);
        
        // Dimensions de l'image
        int width = 500;
        int height = 300;
        
        // Créer une nouvelle image
        Image image = new Image(width, height);
        ImageProcessor processor = image.getProcessor();
        
        // Fond blanc
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                processor.putPixelValue(j, i, 255);
            }
        }
        
        // Calculer la variance totale
        double totalVariance = 0;
        for (double eigenValue : eigenValues) {
            totalVariance += eigenValue;
        }
        
        // Calculer la variance cumulée
        double[] cumulativeVariance = new double[nComponents];
        double cumSum = 0;
        
        for (int i = 0; i < nComponents; i++) {
            cumSum += eigenValues[i] / totalVariance;
            cumulativeVariance[i] = cumSum;
        }
        
        // Marges du graphique
        int marginLeft = 50;
        int marginRight = 20;
        int marginTop = 30;
        int marginBottom = 50;
        
        int graphWidth = width - marginLeft - marginRight;
        int graphHeight = height - marginTop - marginBottom;
        
        // Dessiner les axes
        // Axe horizontal (composantes)
        for (int x = marginLeft; x <= width - marginRight; x++) {
            processor.putPixelValue(x, height - marginBottom, 0);
        }
        
        // Axe vertical (variance cumulée)
        for (int y = marginTop; y <= height - marginBottom; y++) {
            processor.putPixelValue(marginLeft, y, 0);
        }
        
        // Dessiner la courbe de variance cumulée
        for (int i = 0; i < nComponents - 1; i++) {
            int x1 = marginLeft + (i * graphWidth) / (nComponents - 1);
            int y1 = height - marginBottom - (int)(cumulativeVariance[i] * graphHeight);
            int x2 = marginLeft + ((i + 1) * graphWidth) / (nComponents - 1);
            int y2 = height - marginBottom - (int)(cumulativeVariance[i + 1] * graphHeight);
            
            // Ligne de la courbe
            drawLine(processor, x1, y1, x2, y2, 0);
        }
        
        // Sauvegarder l'image
        String filename = String.format("%s/variance_explained.jpg", outputDir);
        image.save(filename);
        
        return image;
    }
    
    /**
     * Dessine une ligne sur l'image
     */
    private void drawLine(ImageProcessor processor, int x1, int y1, int x2, int y2, double value) {
        // Implémentation simple de l'algorithme de Bresenham
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        while (true) {
            processor.putPixelValue(x1, y1, value);
            
            if (x1 == x2 && y1 == y2) {
                break;
            }
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err = err - dy;
                x1 = x1 + sx;
            }
            
            if (e2 < dx) {
                err = err + dx;
                y1 = y1 + sy;
            }
        }
    }
} 