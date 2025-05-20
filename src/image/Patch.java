package image;

/**
 * Un patch est une sous-image carrée de taille s×s extraite de l'image principale.
 * Il peut être vectorisé en un vecteur de taille s² pour le traitement par ACP.
 */
public class Patch {
    private double[][] data;    // données du patch s×s
    private int[] position;     // position (i,j) du coin supérieur gauche dans l'image
    private int s;             // taille du patch (s×s)

    /**
     * Constructeur
     * @param data données du patch
     * @param position position dans l'image originale
     * @param s taille du patch
     */
    public Patch(double[][] data, int[] position, int s) {
        if (data == null || position == null || data.length != s || data[0].length != s) {
            throw new IllegalArgumentException("Données de patch ou dimensions invalides");
        }
        if (position.length != 2) {
            throw new IllegalArgumentException("La position doit être [i,j]");
        }

        this.s = s;
        this.position = position.clone();
        this.data = new double[s][s];
        
        // Copie profonde des données
        for (int i = 0; i < s; i++) {
            System.arraycopy(data[i], 0, this.data[i], 0, s);
        }
    }

    /**
     * Vectorise le patch en un vecteur de taille s²
     * en parcourant le patch ligne par ligne
     * @return vecteur de taille s²
     */
    public double[] toVector() {
        double[] vector = new double[s * s];
        int k = 0;
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < s; j++) {
                vector[k++] = data[i][j];
            }
        }
        return vector;
    }

    /**
     * Reconstruit un patch à partir d'un vecteur
     * @param vector vecteur de taille s²
     * @param s taille du patch
     * @param position position dans l'image
     * @return nouveau patch
     */
    public static Patch fromVector(double[] vector, int s, int[] position) {
        if (vector == null || vector.length != s * s) {
            throw new IllegalArgumentException("Données de vecteur invalides");
        }

        double[][] data = new double[s][s];
        int k = 0;
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < s; j++) {
                data[i][j] = vector[k++];
            }
        }
        return new Patch(data, position, s);
    }

    public double[][] getData() {
        return data.clone();
    }

    public int[] getPosition() {
        return position.clone();
    }

    public int getS() {
        return s;
    }

    /**
     * Convertit le patch en vecteur (linéarisation)
     * @return Vecteur des données
     */
    public double[] vectorize() {
        double[] vector = new double[s * s];
        
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < s; j++) {
                vector[i * s + j] = data[i][j];
            }
        }
        
        return vector;
    }

    /**
     * Calcule la moyenne des pixels du patch
     * @return Moyenne des pixels
     */
    public double getMean() {
        double sum = 0;
        
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < s; j++) {
                sum += data[i][j];
            }
        }
        
        return sum / (s * s);
    }
} 