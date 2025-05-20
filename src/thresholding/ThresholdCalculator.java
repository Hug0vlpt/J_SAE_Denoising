package thresholding;

/**
 * Calcule différents types de seuils pour le débruitage
 */
public class ThresholdCalculator {

    /**
     * Calcule le seuil "VisuShrink" (seuil universel)
     * λ = σ * sqrt(2*log(L))
     * 
     * @param sigma écart-type du bruit
     * @param L nombre de patches
     * @return seuil calculé
     */
    public static double calculateVisuShrinkThreshold(double sigma, int L) {
        return sigma * Math.sqrt(2 * Math.log(L));
    }

    /**
     * Calcule le seuil "BayesShrink"
     * λ = σ² / σX
     * 
     * @param sigma écart-type du bruit
     * @param sigmaX écart-type estimé du signal
     * @return seuil calculé
     */
    public static double calculateBayesShrinkThreshold(double sigma, double sigmaX) {
        if (sigmaX <= 0) {
            return Double.MAX_VALUE; // Si σX est trop petit, tout sera seuillé
        }
        return (sigma * sigma) / sigmaX;
    }

    /**
     * Estime l'écart-type du signal à partir de l'écart-type observé
     * σX = sqrt(max(σXb² - σ², 0))
     * 
     * @param sigmaXb écart-type observé du signal bruité
     * @param sigma écart-type du bruit
     * @return écart-type estimé du signal
     */
    public static double estimateSignalStdDev(double sigmaXb, double sigma) {
        double varianceX = Math.max(sigmaXb * sigmaXb - sigma * sigma, 0);
        return Math.sqrt(varianceX);
    }
} 