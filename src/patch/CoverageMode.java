package patch;

/**
 * Énumération des modes de couverture pour l'extraction des patchs
 */
public enum CoverageMode {
    /**
     * Extrait exactement N patchs aléatoirement répartis dans l'image
     */
    EXACT_N,
    
    /**
     * Extrait des patchs avec chevauchement pour couvrir toute l'image
     */
    OVERLAP_FULL,
    
    /**
     * Extrait des patchs sans chevauchement
     */
    NO_OVERLAP
} 