package image;

/**
 * Classe représentant une imagette (sous-image extraite)
 * Une imagette est une sous-image extraite de l'image d'origine
 */
public class Imagette {
    private double[][] data;     // Données de l'imagette
    private int[] position;      // Position de l'imagette dans l'image d'origine
    
    /**
     * Constructeur d'imagette
     * @param data Données de l'imagette
     * @param position Position de l'imagette dans l'image d'origine
     */
    public Imagette(double[][] data, int[] position) {
        this.data = data;
        this.position = position;
    }
    
    /**
     * Retourne les données de l'imagette
     * @return Matrice des données
     */
    public double[][] getData() {
        return data;
    }
    
    /**
     * Modifie les données de l'imagette
     * @param data Nouvelles données
     */
    public void setData(double[][] data) {
        if (data.length != this.data.length || data[0].length != this.data[0].length) {
            throw new IllegalArgumentException("Les dimensions des nouvelles données ne correspondent pas");
        }
        this.data = data;
    }
    
    /**
     * Retourne la position de l'imagette dans l'image d'origine
     * @return Position (i, j)
     */
    public int[] getPosition() {
        return position;
    }
    
    /**
     * Retourne la hauteur de l'imagette
     * @return Hauteur
     */
    public int getHeight() {
        return data.length;
    }
    
    /**
     * Retourne la largeur de l'imagette
     * @return Largeur
     */
    public int getWidth() {
        return data[0].length;
    }
    
    /**
     * Crée une Image à partir de cette imagette
     * @return Une nouvelle Image contenant les données de l'imagette
     */
    public Image toImage() {
        return new Image(data);
    }
} 