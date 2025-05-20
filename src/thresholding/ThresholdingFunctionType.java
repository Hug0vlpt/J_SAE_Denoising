package thresholding;

/**
 * Énumération des types de fonctions de seuillage
 */
public enum ThresholdingFunctionType {
    /**
     * Seuillage dur (Hard Thresholding)
     * Si |x| &lt;= λ alors x = 0, sinon x reste inchangé
     */
    SEUILLAGE_DUR,
    
    /**
     * Seuillage doux (Soft Thresholding)
     * Si |x| &lt;= λ alors x = 0
     * Si x &gt; λ alors x = x - λ
     * Si x &lt; -λ alors x = x + λ
     */
    SEUILLAGE_DOUX;
    
    // Constantes pour la compatibilité avec les codes existants
    public static final ThresholdingFunctionType HARD = SEUILLAGE_DUR;
    public static final ThresholdingFunctionType SOFT = SEUILLAGE_DOUX;
} 