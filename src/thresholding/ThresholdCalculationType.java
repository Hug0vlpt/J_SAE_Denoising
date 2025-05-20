package thresholding;

/**
 * Énumération des types de calcul de seuil
 */
public enum ThresholdCalculationType {
    /**
     * Seuil universel (VisuShrink)
     * λ = σ * sqrt(2*log(N))
     */
    SEUIL_V_VISUSHRINK,
    
    /**
     * Seuil BayesShrink
     * λ = σ² / σₓ
     */
    SEUIL_B_BAYESSHRINK;
    
    // Constantes pour la compatibilité avec les codes existants
    public static final ThresholdCalculationType UNIVERSAL = SEUIL_V_VISUSHRINK;
    public static final ThresholdCalculationType BAYES = SEUIL_B_BAYESSHRINK;
} 