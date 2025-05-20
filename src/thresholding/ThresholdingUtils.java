package thresholding;

/**
 * Classe utilitaire pour le seuillage des coefficients dans le cadre du débruitage d'images.
 * 
 * Le seuillage est une étape cruciale du débruitage par ACP. Après projection des patches
 * dans la base des composantes principales, on applique un seuillage sur les coefficients
 * pour éliminer ou réduire ceux qui sont principalement dus au bruit.
 * 
 * Cette classe implémente :
 * 1. Différentes méthodes de calcul de seuil (VisuShrink, BayesShrink)
 * 2. Différentes fonctions de seuillage (dur, doux)
 * 3. Des utilitaires d'estimation de paramètres du bruit et du signal
 */
public class ThresholdingUtils {
    
    /**
     * Calcule le seuil universel VisuShrink (Donoho &amp; Johnstone, 1994).
     * 
     * Le seuil universel est défini par la formule : λ = σ√(2log(L))
     * où σ est l'écart-type du bruit et L est le nombre de pixels dans l'image.
     * 
     * Ce seuil est "universel" car il assure que, asymptotiquement, tous les coefficients
     * qui ne sont que du bruit seront mis à zéro. Cependant, il peut être trop agressif
     * et éliminer aussi des détails du signal.
     * 
     * Référence académique : Donoho, D.L. and Johnstone, I.M. (1994),
     * "Ideal spatial adaptation by wavelet shrinkage", Biometrika, Vol. 81, pp. 425-455.
     * 
     * @param sigma_noise Écart-type du bruit
     * @param L_nbPixelsImage Nombre de pixels dans l'image
     * @return Valeur du seuil λ
     * @throws IllegalArgumentException si les paramètres sont invalides
     */
    public static double SeuilV(double sigma_noise, int L_nbPixelsImage) {
        if (sigma_noise <= 0) {
            throw new IllegalArgumentException("L'écart-type du bruit doit être positif");
        }
        if (L_nbPixelsImage <= 0) {
            throw new IllegalArgumentException("Le nombre de pixels doit être positif");
        }
        
        // Formule du seuil universel VisuShrink: λ = σ√(2log(L))
        return sigma_noise * Math.sqrt(2 * Math.log(L_nbPixelsImage));
    }
    
    /**
     * Calcule le seuil BayesShrink basé sur l'approche bayésienne.
     * 
     * Le seuil BayesShrink est défini par la formule : λ = σ²/σₓ
     * où σ² est la variance du bruit et σₓ est l'écart-type estimé du signal original.
     * 
     * Cette méthode adapte le seuil localement en fonction des caractéristiques
     * statistiques du signal, ce qui permet généralement une meilleure conservation
     * des détails tout en supprimant efficacement le bruit.
     * 
     * Dans le cas où σₓ est très petit (signal quasi constant), on utilise un seuil
     * très élevé pour éliminer tous les coefficients, car il s'agit probablement
     * uniquement de bruit.
     * 
     * Référence académique : Chang, S.G., Yu, B. and Vetterli, M. (2000),
     * "Adaptive wavelet thresholding for image denoising and compression", 
     * IEEE Transactions on Image Processing, Vol. 9, pp. 1532-1546.
     * 
     * @param sigma_squared Variance du bruit (σ²)
     * @param sigma_x Écart-type estimé du signal original
     * @return Valeur du seuil λ
     * @throws IllegalArgumentException si la variance du bruit est négative
     */
    public static double SeuilB(double sigma_squared, double sigma_x) {
        if (sigma_squared < 0) {
            throw new IllegalArgumentException("La variance du bruit ne peut pas être négative");
        }
        
        if (sigma_x <= 0) {
            // Si l'écart-type du signal est trop faible, utiliser un seuil élevé
            // car les coefficients sont probablement uniquement du bruit
            return Double.MAX_VALUE;
        }
        
        // Formule du seuil BayesShrink: λ = σ²/σₓ
        return sigma_squared / sigma_x;
    }
    
    /**
     * Applique un seuillage dur (hard thresholding) aux coefficients.
     * 
     * Formule mathématique :
     * a'ᵢ = { 0        si |aᵢ| ≤ λ
     *       { aᵢ       si |aᵢ| > λ
     * 
     * Le seuillage dur élimine complètement les coefficients inférieurs au seuil,
     * et laisse intacts ceux qui sont supérieurs. Cette approche préserve mieux
     * l'amplitude des coefficients significatifs, mais peut introduire des
     * discontinuités (artifacts visuels) dans l'image reconstruite.
     * 
     * @param coefficients Coefficients à seuiller
     * @param threshold Valeur du seuil λ
     * @return Coefficients seuillés
     * @throws IllegalArgumentException si les paramètres sont invalides
     */
    public static double[] SeuillageDur(double[] coefficients, double threshold) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Les coefficients ne peuvent pas être null");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Le seuil ne peut pas être négatif");
        }
        
        double[] result = new double[coefficients.length];
        
        for (int i = 0; i < coefficients.length; i++) {
            if (Math.abs(coefficients[i]) <= threshold) {
                result[i] = 0; // Coefficients sous le seuil mis à zéro
            } else {
                result[i] = coefficients[i]; // Les autres restent inchangés
            }
        }
        
        return result;
    }
    
    /**
     * Applique un seuillage doux (soft thresholding) aux coefficients.
     * 
     * Formule mathématique :
     * a'ᵢ = { 0                si |aᵢ| ≤ λ
     *       { sign(aᵢ)(|aᵢ|-λ) si |aᵢ| > λ
     * 
     * Le seuillage doux réduit l'amplitude de tous les coefficients conservés
     * d'une valeur égale au seuil. Cette approche produit généralement des résultats
     * plus lisses visuellement, sans discontinuités brutales, mais peut atténuer
     * excessivement les détails de l'image.
     * 
     * Référence académique : Donoho, D.L. (1995), "De-noising by soft-thresholding",
     * IEEE Transactions on Information Theory, Vol. 41, pp. 613-627.
     * 
     * @param coefficients Coefficients à seuiller
     * @param threshold Valeur du seuil λ
     * @return Coefficients seuillés
     * @throws IllegalArgumentException si les paramètres sont invalides
     */
    public static double[] SeuillageDoux(double[] coefficients, double threshold) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Les coefficients ne peuvent pas être null");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Le seuil ne peut pas être négatif");
        }
        
        double[] result = new double[coefficients.length];
        
        for (int i = 0; i < coefficients.length; i++) {
            if (Math.abs(coefficients[i]) <= threshold) {
                result[i] = 0; // Coefficients sous le seuil mis à zéro
            } else if (coefficients[i] > threshold) {
                result[i] = coefficients[i] - threshold; // Réduction des coefficients positifs
            } else {
                result[i] = coefficients[i] + threshold; // Réduction des coefficients négatifs
            }
        }
        
        return result;
    }
    
    /**
     * Estime l'écart-type du signal original à partir de la variance observée et de la variance du bruit.
     * 
     * Pour un modèle additif où y = x + n (signal bruité = signal original + bruit),
     * et en supposant l'indépendance entre le signal et le bruit, on a :
     * σ²ᵧ = σ²ₓ + σ²
     * 
     * On en déduit : σₓ = √max(σ²ᵧ - σ², 0)
     * 
     * La fonction max avec 0 est utilisée pour éviter des valeurs négatives qui 
     * pourraient survenir à cause d'erreurs d'estimation ou de cas particuliers.
     * 
     * @param varianceObserved Variance du signal observé (bruité)
     * @param varianceNoise Variance du bruit
     * @return Écart-type estimé du signal original
     * @throws IllegalArgumentException si les variances sont négatives
     */
    public static double estimerEcartTypeSignal_sigma_x_hat(double varianceObserved, double varianceNoise) {
        if (varianceObserved < 0 || varianceNoise < 0) {
            throw new IllegalArgumentException("Les variances ne peuvent pas être négatives");
        }
        
        // Formule: σₓ = √max(σ²ᵧ - σ², 0)
        double varianceSignal = Math.max(varianceObserved - varianceNoise, 0);
        return Math.sqrt(varianceSignal);
    }
} 