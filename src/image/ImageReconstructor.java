package image;

import java.util.List;
import java.util.ArrayList;

/**
 * @theory
 * Classe responsable de la reconstruction d'une image
 * à partir d'une collection de patches débruités
 */
public class ImageReconstructor {

    /**
     * Reconstruit une image à partir d'une liste de patches et leurs positions
     * En cas de chevauchement, utilise la moyenne des valeurs
     * 
     * @param patches liste des patches
     * @param width largeur de l'image
     * @param height hauteur de l'image
     * @return image reconstruite
     */
    public Image reconstructImage(List<Patch> patches, int width, int height) {
        if (patches == null || patches.isEmpty()) {
            throw new IllegalArgumentException("La liste de patches ne peut pas être vide");
        }

        // Initialiser l'image et un compteur pour moyenner les valeurs
        Image image = new Image(width, height);
        int[][] count = new int[height][width];

        // Pour chaque patch
        for (Patch patch : patches) {
            double[][] data = patch.getData();
            int[] position = patch.getPosition();
            int s = patch.getS();

            // Ajouter les valeurs du patch à l'image
            for (int i = 0; i < s; i++) {
                for (int j = 0; j < s; j++) {
                    int y = position[0] + i;
                    int x = position[1] + j;
                    
                    if (y < height && x < width) {
                        // Accumuler les valeurs pour calculer la moyenne plus tard
                        double currentValue = image.getPixel(y, x);
                        image.setPixel(y, x, currentValue + data[i][j]);
                        count[y][x]++;
                    }
                }
            }
        }

        // Calculer la moyenne pour les pixels où il y a eu chevauchement
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (count[i][j] > 0) {
                    double value = image.getPixel(i, j) / count[i][j];
                    image.setPixel(i, j, value);
                }
            }
        }

        return image;
    }

    /**
     * Reconstruit une image à partir de vecteurs de patches reconstruits
     * 
     * @param vectors liste des vecteurs de patches
     * @param positions liste des positions des patches
     * @param patchSize taille des patches
     * @param width largeur de l'image
     * @param height hauteur de l'image
     * @return image reconstruite
     */
    public Image reconstructFromVectors(List<double[]> vectors, List<int[]> positions, 
                                        int patchSize, int width, int height) {
        if (vectors == null || vectors.isEmpty() || positions == null || positions.isEmpty()) {
            throw new IllegalArgumentException("Les listes de vecteurs et de positions ne peuvent pas être vides");
        }
        if (vectors.size() != positions.size()) {
            throw new IllegalArgumentException("Le nombre de vecteurs doit correspondre au nombre de positions");
        }

        // Convertir les vecteurs en patches
        List<Patch> patches = vectorsToPatchs(vectors, positions, patchSize);
        
        // Reconstruire l'image
        return reconstructImage(patches, width, height);
    }

    /**
     * Convertit une liste de vecteurs en liste de patches
     * 
     * @param vectors liste des vecteurs
     * @param positions liste des positions
     * @param patchSize taille des patches
     * @return liste de patches
     */
    private List<Patch> vectorsToPatchs(List<double[]> vectors, List<int[]> positions, int patchSize) {
        List<Patch> patches = new ArrayList<>();
        
        for (int i = 0; i < vectors.size(); i++) {
            double[] vector = vectors.get(i);
            int[] position = positions.get(i);
            
            // Convertir le vecteur en patch
            Patch patch = Patch.fromVector(vector, patchSize, position);
            patches.add(patch);
        }
        
        return patches;
    }

    /**
     * Crée une fenêtre de Hanning 2D pour la pondération des imagettes
     * @param height Hauteur de la fenêtre
     * @param width Largeur de la fenêtre
     * @return Matrice de pondération 2D
     */
    private static double[][] hanningWindow2D(int height, int width) {
        double[] winH = new double[height];
        double[] winW = new double[width];
        
        // Calculer les coefficients de Hanning pour chaque dimension
        for (int i = 0; i < height; i++) {
            winH[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (height - 1)));
        }
        for (int j = 0; j < width; j++) {
            winW[j] = 0.5 * (1 - Math.cos(2 * Math.PI * j / (width - 1)));
        }

        // Créer la fenêtre 2D par produit extérieur
        double[][] weights = new double[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                weights[i][j] = winH[i] * winW[j];
            }
        }
        return weights;
    }

    /**
     * Reconstruit une image complète à partir d'une liste d'imagettes débruitées
     * en utilisant une fenêtre de Hanning pour la pondération
     * 
     * @param imagettes Liste des imagettes débruitées avec leurs positions originales
     * @param imageHeight Hauteur de l'image finale
     * @param imageWidth Largeur de l'image finale
     * @return L'image reconstruite
     */
    public static Image reconstruireImageDepuisImagettes(List<Imagette> imagettes, int imageHeight, int imageWidth) {
        double[][] reconstructedData = new double[imageHeight][imageWidth];
        double[][] weightSum = new double[imageHeight][imageWidth];

        for (Imagette imagette : imagettes) {
            double[][] data = imagette.getData();
            int[] position = imagette.getPosition();
            int startRow = position[0];
            int startCol = position[1];
            int imgHeight = data.length;
            int imgWidth = data[0].length;

            // Générer la fenêtre de pondération pour cette imagette
            double[][] window = hanningWindow2D(imgHeight, imgWidth);

            // Appliquer la pondération et accumuler les contributions
            for (int i = 0; i < imgHeight; i++) {
                for (int j = 0; j < imgWidth; j++) {
                    int targetRow = startRow + i;
                    int targetCol = startCol + j;

                    if (targetRow < imageHeight && targetCol < imageWidth) {
                        double weightedValue = data[i][j] * window[i][j];
                        reconstructedData[targetRow][targetCol] += weightedValue;
                        weightSum[targetRow][targetCol] += window[i][j];
                    }
                }
            }
        }

        // Normaliser par la somme des poids
        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {
                if (weightSum[i][j] > 0) {
                    reconstructedData[i][j] /= weightSum[i][j];
                }
            }
        }

        return new Image(reconstructedData);
    }
    
    /**
     * Reconstruit une image à partir de patches débruités
     * 
     * @param patches Liste des patches débruités avec leurs positions originales
     * @param imageHeight Hauteur de l'image finale
     * @param imageWidth Largeur de l'image finale
     * @param patchSize Taille des patches (carré)
     * @return L'image reconstruite
     */
    public static Image reconstruireImageDepuisPatches(List<Patch> patches, int imageHeight, int imageWidth, int patchSize) {
        double[][] reconstructedData = new double[imageHeight][imageWidth];
        double[][] weightSum = new double[imageHeight][imageWidth];

        // Générer la fenêtre de pondération une seule fois car tous les patches ont la même taille
        double[][] window = hanningWindow2D(patchSize, patchSize);

        for (Patch patch : patches) {
            double[][] data = patch.getData();
            int[] position = patch.getPosition();
            int startRow = position[0];
            int startCol = position[1];

            for (int i = 0; i < patchSize; i++) {
                for (int j = 0; j < patchSize; j++) {
                    int targetRow = startRow + i;
                    int targetCol = startCol + j;

                    if (targetRow < imageHeight && targetCol < imageWidth) {
                        double weightedValue = data[i][j] * window[i][j];
                        reconstructedData[targetRow][targetCol] += weightedValue;
                        weightSum[targetRow][targetCol] += window[i][j];
                    }
                }
            }
        }

        // Normaliser par la somme des poids
        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {
                if (weightSum[i][j] > 0) {
                    reconstructedData[i][j] /= weightSum[i][j];
                }
            }
        }

        return new Image(reconstructedData);
    }
} 