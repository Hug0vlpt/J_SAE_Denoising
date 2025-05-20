package patch;

import java.util.List;
import image.Image;
import image.Imagette;
import image.Patch;

/**
 * Interface définissant les opérations sur les patches
 * Implémente les méthodes nécessaires aux approches globale et locale
 */
public interface IPatchOperation {
    
    /**
     * Extrait des patches d'une image selon l'approche globale
     * 
     * @param Xb Image bruitée
     * @param s Taille des patches (s×s)
     * @return Liste des patches extraits
     */
    List<Patch> ExtractPatchs(Image Xb, int s);
    
    /**
     * Découpe une image en imagettes selon l'approche locale
     * 
     * @param X Image à découper
     * @param W Taille des imagettes (W×W)
     * @param n Nombre d'imagettes souhaité
     * @return Liste des imagettes extraites
     */
    List<Imagette> DecoupeImage(Image X, int W, int n);
    
    /**
     * Découpe une image en imagettes avec un mode de couverture spécifique
     * @param X Image à découper
     * @param W Taille des imagettes (W×W)
     * @param n Nombre d'imagettes souhaité (si fullCoverage=false)
     * @param fullCoverage Si true, couvre toute l'image avec chevauchement, sinon extrait n imagettes
     * @return Liste des imagettes extraites
     */
    List<Imagette> DecoupeImage(Image X, int W, int n, boolean fullCoverage);
    
    /**
     * Découpe une image en imagettes avec un mode de couverture spécifique et contrôle du chevauchement
     * @param X Image à découper
     * @param W Taille des imagettes (W×W)
     * @param n Nombre d'imagettes souhaité (si coverageMode=EXACT_N)
     * @param coverageMode Mode de couverture: 0=EXACT_N, 1=OVERLAP_FULL, 2=NO_OVERLAP
     * @return Liste des imagettes extraites
     */
    List<Imagette> DecoupeImage(Image X, int W, int n, int coverageMode);
    
    /**
     * Vectorise une liste de patches
     * 
     * @param Y_patchs Liste des patches à vectoriser
     * @return Liste des vecteurs correspondants
     */
    List<double[]> VectorPatchs(List<Patch> Y_patchs);
    
    /**
     * Reconstruit une image à partir d'une liste de patches
     * 
     * @param Y_patchs Liste des patches
     * @param l Nombre de lignes de l'image (hauteur)
     * @param c Nombre de colonnes de l'image (largeur)
     * @return Image reconstruite
     */
    Image ReconstructPatchs(List<Patch> Y_patchs, int l, int c);
    
    /**
     * Dévectorisation d'une collection de vecteurs en patchs
     * @param vecteurs Liste des vecteurs à dévectoriser
     * @param positions Liste des positions des patchs d'origine
     * @param s Taille des patchs (s×s)
     * @return Liste des patchs reconstruits
     */
    List<Patch> DevectorisePatchs(List<double[]> vecteurs, List<int[]> positions, int s);
} 