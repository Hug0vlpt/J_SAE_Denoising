package image;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.IJ;
import java.io.File;

/**
 * @theory
 * Représente une image qui peut être :
 * - X0 (image idéale/originale)
 * - Xb (image bruitée)
 * - XR (image reconstruite/débruitée)
 */
public class Image {
    private double[][] data; // matrice des pixels
    private int width;      // largeur (nombre de colonnes)
    private int height;     // hauteur (nombre de lignes)
    private ImageProcessor processor; // Processeur ImageJ pour les opérations avancées
    
    /**
     * Constructeur à partir d'une matrice de pixels
     * @param pixelData matrice de pixels
     */
    public Image(double[][] pixelData) {
        if (pixelData == null || pixelData.length == 0 || pixelData[0].length == 0) {
            throw new IllegalArgumentException("Données de pixels invalides");
        }
        this.height = pixelData.length;
        this.width = pixelData[0].length;
        this.data = new double[height][width];
        
        // Copie profonde des données
        for (int i = 0; i < height; i++) {
            System.arraycopy(pixelData[i], 0, this.data[i], 0, width);
        }
        
        // Initialiser le processeur
        initProcessor();
    }

    /**
     * Constructeur pour une image vide de taille donnée
     * @param width largeur
     * @param height hauteur
     */
    public Image(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Dimensions invalides");
        }
        this.width = width;
        this.height = height;
        this.data = new double[height][width];
        
        // Initialiser le processeur
        initProcessor();
    }
    
    /**
     * Constructeur à partir d'un ImagePlus d'ImageJ
     * @param imagePlus objet ImagePlus
     */
    public Image(ImagePlus imagePlus) {
        if (imagePlus == null) {
            throw new IllegalArgumentException("ImagePlus est null");
        }
        
        ImageProcessor proc = imagePlus.getProcessor();
        this.width = proc.getWidth();
        this.height = proc.getHeight();
        this.data = new double[height][width];
        
        // Copier les données de l'ImageProcessor
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                this.data[i][j] = proc.getPixelValue(j, i);
            }
        }
        
        // Initialiser le processeur
        initProcessor();
    }

    /**
     * Initialise le processeur ImageJ avec les données actuelles
     */
    private void initProcessor() {
        float[] pixels = new float[width * height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                pixels[i * width + j] = (float) data[i][j];
            }
        }
        this.processor = new FloatProcessor(width, height, pixels);
    }

    /**
     * Récupère la valeur d'un pixel
     * @param i ligne
     * @param j colonne
     * @return valeur du pixel
     */
    public double getPixel(int i, int j) {
        if (i < 0 || i >= height || j < 0 || j >= width) {
            throw new IllegalArgumentException("Coordonnées de pixel hors limites");
        }
        return data[i][j];
    }

    /**
     * Modifie la valeur d'un pixel
     * @param i ligne
     * @param j colonne
     * @param value nouvelle valeur
     */
    public void setPixel(int i, int j, double value) {
        if (i < 0 || i >= height || j < 0 || j >= width) {
            throw new IllegalArgumentException("Coordonnées de pixel hors limites");
        }
        data[i][j] = value;
        if (processor != null) {
            processor.putPixelValue(j, i, value);
        }
    }

    /**
     * @return largeur de l'image
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return hauteur de l'image
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return matrice des pixels
     */
    public double[][] getData() {
        // Retourne une copie profonde pour éviter la modification directe
        double[][] copy = new double[height][width];
        for (int i = 0; i < height; i++) {
            System.arraycopy(data[i], 0, copy[i], 0, width);
        }
        return copy;
    }
    
    /**
     * Obtient le processeur ImageJ de l'image, utilisé pour les opérations avancées
     * @return Processeur ImageJ
     */
    public ImageProcessor getProcessor() {
        // S'assurer que le processeur est synchronisé avec les données
        updateProcessor();
        return processor;
    }
    
    /**
     * Met à jour le processeur avec les données actuelles de l'image
     */
    private void updateProcessor() {
        if (processor == null) {
            initProcessor();
            return;
        }
        
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                processor.putPixelValue(j, i, data[i][j]);
            }
        }
    }
    
    /**
     * Sauvegarde l'image dans un fichier
     * @param path chemin du fichier de sortie
     * @return true si la sauvegarde a réussi, false sinon
     */
    public boolean save(String path) {
        try {
            // Créer le répertoire parent si nécessaire
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            
            // Mettre à jour le processeur
            updateProcessor();
            
            // Créer un ImagePlus et sauvegarder
            ImagePlus imp = new ImagePlus("Image", processor);
            IJ.saveAs(imp, "jpg", path);
            return true; // Si aucune exception n'a été levée, considérons que la sauvegarde a réussi
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde de l'image : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 