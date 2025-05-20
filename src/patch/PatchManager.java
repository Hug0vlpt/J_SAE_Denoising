package patch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import image.Image;
import image.Imagette;
import image.Patch;

/**
 * Classe de gestion des patchs et des imagettes
 * Implémente les approches globale et locale
 * Optimisée avec le traitement parallèle via les streams Java
 */
public class PatchManager implements IPatchOperation {
    
    // Constantes pour les modes de couverture
    public static final int EXACT_N = 0;          // Extraction d'exactement N imagettes
    public static final int OVERLAP_FULL = 1;     // Couverture complète avec chevauchement
    public static final int NO_OVERLAP = 2;       // Couverture complète sans chevauchement
    
    // Enum pour les modes de couverture plus lisible
    public enum CoverageMode {
        EXACT_N,        // Extraction d'exactement N imagettes
        OVERLAP_FULL,   // Couverture complète avec chevauchement
        NO_OVERLAP      // Couverture complète sans chevauchement
    }
    
    // Flag pour activer/désactiver le traitement parallèle
    private boolean useParallelProcessing = true;
    
    /**
     * Définit si le traitement parallèle doit être utilisé
     * @param useParallel true pour activer le parallélisme, false sinon
     */
    public void setUseParallelProcessing(boolean useParallel) {
        this.useParallelProcessing = useParallel;
    }
    
    /**
     * Indique si le traitement parallèle est activé
     * @return true si le parallélisme est activé, false sinon
     */
    public boolean isUseParallelProcessing() {
        return useParallelProcessing;
    }

    /**
     * Extraction de patchs selon l'approche globale
     * @param Xb Image bruitée
     * @param s Taille des patchs (s×s)
     * @return Liste des patchs extraits avec leurs positions
     */
    @Override
    public List<Patch> ExtractPatchs(Image Xb, int s) {
        // Extraction avec 50% de chevauchement par défaut
        return ExtractPatchs(Xb, s, OVERLAP_FULL, 50, false);
    }

    /**
     * Extraction de patchs avec configuration enum
     * @param Xb Image bruitée
     * @param s Taille des patchs (s×s)
     * @param coverageMode Mode de couverture
     * @param overlapPercentage Pourcentage de chevauchement (0-100)
     * @param mirrorEdges Si true, utilise l'effet miroir pour les bords
     * @return Liste des patchs extraits
     */
    public List<Patch> ExtractPatchs(Image Xb, int s, CoverageMode coverageMode, double overlapPercentage, boolean mirrorEdges) {
        int mode;
        switch (coverageMode) {
            case EXACT_N: mode = EXACT_N; break;
            case OVERLAP_FULL: mode = OVERLAP_FULL; break;
            case NO_OVERLAP: mode = NO_OVERLAP; break;
            default: mode = OVERLAP_FULL;
        }
        return ExtractPatchs(Xb, s, mode, (int)overlapPercentage, mirrorEdges);
    }
    
    /**
     * Extraction de patchs avec contrôle du chevauchement et de la gestion des bords
     * @param Xb Image bruitée
     * @param s Taille des patchs (s×s)
     * @param coverageMode Mode de couverture (OVERLAP_FULL, NO_OVERLAP)
     * @param overlapPercentage Pourcentage de chevauchement (utilisé si coverageMode=OVERLAP_FULL)
     * @param mirrorEdges Si true, utilise l'effet miroir pour les bords, sinon recul
     * @return Liste des patchs extraits avec leurs positions
     */
    public List<Patch> ExtractPatchs(Image Xb, int s, int coverageMode, int overlapPercentage, boolean mirrorEdges) {
        if (useParallelProcessing) {
            return ExtractPatchsParallel(Xb, s, coverageMode, overlapPercentage, mirrorEdges);
        } else {
            // Création d'une liste pour stocker les patchs
            List<Patch> patches = new ArrayList<>();
            
            int height = Xb.getHeight();
            int width = Xb.getWidth();
            
            switch (coverageMode) {
                case OVERLAP_FULL:
                    // Calculer le stride en fonction du pourcentage de chevauchement
                    double stride = s * (1 - overlapPercentage / 100.0);
                    int strideInt = Math.max(1, (int) Math.round(stride)); // Au moins 1 pixel
                    
                    // Extraction avec chevauchement contrôlé en partant du bas de l'image
                    for (int i = height - s; i >= 0; i -= strideInt) {
                        // Ajuster la position pour le dernier patch si nécessaire
                        if (!mirrorEdges && i < height - s && i > 0 && i < strideInt) {
                            i = 0; // Ajuster pour le dernier patch en bas
                        }
                        
                        for (int j = 0; j <= width - s; j += strideInt) {
                            // Ajuster la position pour le dernier patch si nécessaire
                            if (!mirrorEdges && j > width - s - strideInt && j != width - s) {
                                j = width - s; // Ajuster pour le dernier patch à droite
                            }
                            
                            extractPatchAtPosition(Xb, s, i, j, patches, mirrorEdges);
                        }
                    }
                    break;
                    
                case NO_OVERLAP:
                    // Extraction sans chevauchement en partant du bas de l'image
                    for (int i = height - s; i >= 0; i -= s) {
                        for (int j = 0; j <= width - s; j += s) {
                            extractPatchAtPosition(Xb, s, i, j, patches, mirrorEdges);
                        }
                    }
                    break;
                    
                default:
                    throw new IllegalArgumentException("Mode de couverture non reconnu pour l'extraction de patchs");
            }
            
            return patches;
        }
    }
    
    /**
     * Version optimisée de l'extraction de patchs avec traitement parallèle
     * @param Xb Image bruitée
     * @param s Taille des patchs (s×s)
     * @param coverageMode Mode de couverture
     * @param overlapPercentage Pourcentage de chevauchement (utilisé si coverageMode=OVERLAP_FULL)
     * @param mirrorEdges Si true, utilise l'effet miroir pour les bords, sinon recul
     * @return Liste des patchs extraits avec leurs positions
     */
    public List<Patch> ExtractPatchsParallel(Image Xb, int s, int coverageMode, int overlapPercentage, boolean mirrorEdges) {
        if (Xb == null) {
            throw new IllegalArgumentException("L'image ne peut pas être null");
        }
        if (s <= 0) {
            throw new IllegalArgumentException("La taille des patchs doit être positive");
        }
        
        int height = Xb.getHeight();
        int width = Xb.getWidth();
        
        if (s > height || s > width) {
            throw new IllegalArgumentException("La taille des patchs ne peut pas dépasser celle de l'image");
        }
        
        List<int[]> positions = new ArrayList<>();
        
        switch (coverageMode) {
            case OVERLAP_FULL:
                // Calculer le stride en fonction du pourcentage de chevauchement
                double stride = s * (1 - overlapPercentage / 100.0);
                int strideInt = Math.max(1, (int) Math.round(stride)); // Au moins 1 pixel
                
                // Générer toutes les positions de patchs
                for (int i = height - s; i >= 0; i -= strideInt) {
                    // Ajuster pour le dernier patch en bas si nécessaire
                    if (!mirrorEdges && i < height - s && i > 0 && i < strideInt) {
                        i = 0;
                    }
                    
                    for (int j = 0; j <= width - s; j += strideInt) {
                        // Ajuster pour le dernier patch à droite si nécessaire
                        if (!mirrorEdges && j > width - s - strideInt && j != width - s) {
                            j = width - s;
                        }
                        
                        positions.add(new int[]{i, j});
                    }
                }
                break;
                
            case NO_OVERLAP:
                // Extraction sans chevauchement
                for (int i = height - s; i >= 0; i -= s) {
                    for (int j = 0; j <= width - s; j += s) {
                        positions.add(new int[]{i, j});
                    }
                }
                break;
                
            default:
                throw new IllegalArgumentException("Mode de couverture non reconnu");
        }
        
        // Extraction parallèle des patchs à partir des positions
        IntStream positionStream = IntStream.range(0, positions.size());
        if (useParallelProcessing) {
            positionStream = positionStream.parallel();
        }
        
        return positionStream
                .mapToObj(idx -> {
                    int[] pos = positions.get(idx);
                    return extractPatchAtPositionAsReturn(Xb, s, pos[0], pos[1], mirrorEdges);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Extrait un patch à la position spécifiée et le retourne
     * Version adaptée pour le traitement parallèle
     */
    private Patch extractPatchAtPositionAsReturn(Image Xb, int s, int i, int j, boolean mirrorEdges) {
        double[][] patchData = new double[s][s];
        
        // Copie des données de l'image dans le patch
        for (int k = 0; k < s; k++) {
            for (int l = 0; l < s; l++) {
                if (mirrorEdges) {
                    // Utiliser l'effet miroir pour les pixels hors limite
                    int row = i + k;
                    int col = j + l;
                    
                    // Miroir pour les lignes
                    if (row < 0) {
                        row = -row;
                    } else if (row >= Xb.getHeight()) {
                        row = 2 * Xb.getHeight() - row - 2;
                    }
                    
                    // Miroir pour les colonnes
                    if (col < 0) {
                        col = -col;
                    } else if (col >= Xb.getWidth()) {
                        col = 2 * Xb.getWidth() - col - 2;
                    }
                    
                    patchData[k][l] = Xb.getPixel(row, col);
                } else {
                    // Pas d'effet miroir, juste vérifier les limites
                    if (i + k < Xb.getHeight() && j + l < Xb.getWidth()) {
                        patchData[k][l] = Xb.getPixel(i + k, j + l);
                    } else {
                        patchData[k][l] = 0;
                    }
                }
            }
        }
        
        int[] position = {i, j};
        return new Patch(patchData, position, s);
    }
    
    /**
     * Extrait un patch à la position spécifiée
     * @param Xb Image source
     * @param s Taille du patch
     * @param i Position verticale
     * @param j Position horizontale
     * @param patches Liste de patchs à laquelle ajouter le patch
     * @param mirrorEdges Si true, utilise l'effet miroir pour les bords
     */
    private void extractPatchAtPosition(Image Xb, int s, int i, int j, List<Patch> patches, boolean mirrorEdges) {
        double[][] patchData = new double[s][s];
        
        // Copie des données de l'image dans le patch
        for (int k = 0; k < s; k++) {
            for (int l = 0; l < s; l++) {
                if (mirrorEdges) {
                    // Utiliser l'effet miroir pour les pixels hors limite
                    int row = i + k;
                    int col = j + l;
                    
                    // Miroir pour les lignes
                    if (row < 0) {
                        row = -row;
                    } else if (row >= Xb.getHeight()) {
                        row = 2 * Xb.getHeight() - row - 2;
                    }
                    
                    // Miroir pour les colonnes
                    if (col < 0) {
                        col = -col;
                    } else if (col >= Xb.getWidth()) {
                        col = 2 * Xb.getWidth() - col - 2;
                    }
                    
                    patchData[k][l] = Xb.getPixel(row, col);
                } else {
                    // Pas d'effet miroir, juste vérifier les limites
                    if (i + k < Xb.getHeight() && j + l < Xb.getWidth()) {
                        patchData[k][l] = Xb.getPixel(i + k, j + l);
                    } else {
                        // Gérer le cas où le patch dépasse l'image (ne devrait pas arriver)
                        patchData[k][l] = 0;
                    }
                }
            }
        }
        
        int[] position = {i, j};
        Patch patch = new Patch(patchData, position, s);
        patches.add(patch);
    }
    
    /**
     * Reconstruction d'une image à partir d'une collection de patchs
     * @param Y_patchs Liste des patchs
     * @param l Nombre de lignes
     * @param c Nombre de colonnes
     * @return Image reconstruite
     */
    @Override
    public Image ReconstructPatchs(List<Patch> Y_patchs, int l, int c) {
        if (useParallelProcessing) {
            return ReconstructPatchsParallel(Y_patchs, l, c);
        } else {
            // Implémentation séquentielle originale
            if (Y_patchs == null || Y_patchs.isEmpty()) {
                throw new IllegalArgumentException("La liste de patchs ne peut pas être vide");
            }
            
            // Créer une nouvelle image avec des valeurs à 0
            Image reconstructedImage = new Image(c, l);
            
            // Compteurs pour calculer la moyenne des pixels qui se chevauchent
            int[][] overlapCount = new int[l][c];
            
            // Pour chaque patch
            for (Patch patch : Y_patchs) {
                int[] position = patch.getPosition();
                int patchSize = patch.getS();
                double[][] patchData = patch.getData();
                
                // Ajouter les valeurs du patch à l'image
                for (int i = 0; i < patchSize; i++) {
                    for (int j = 0; j < patchSize; j++) {
                        int imageI = position[0] + i;
                        int imageJ = position[1] + j;
                        
                        if (imageI < l && imageJ < c) {
                            double currentValue = reconstructedImage.getPixel(imageI, imageJ);
                            reconstructedImage.setPixel(imageI, imageJ, currentValue + patchData[i][j]);
                            overlapCount[imageI][imageJ]++;
                        }
                    }
                }
            }
            
            // Normaliser en divisant par le nombre de patchs qui se chevauchent
            for (int i = 0; i < l; i++) {
                for (int j = 0; j < c; j++) {
                    if (overlapCount[i][j] > 0) {
                        double currentValue = reconstructedImage.getPixel(i, j);
                        reconstructedImage.setPixel(i, j, currentValue / overlapCount[i][j]);
                    }
                }
            }
            
            return reconstructedImage;
        }
    }
    
    /**
     * Version optimisée de la reconstruction d'image avec traitement parallèle
     */
    public Image ReconstructPatchsParallel(List<Patch> Y_patchs, int l, int c) {
        if (Y_patchs == null || Y_patchs.isEmpty()) {
            throw new IllegalArgumentException("La liste de patchs ne peut pas être vide");
        }
        
        // Créer une nouvelle image avec des valeurs à 0
        Image reconstructedImage = new Image(c, l);
        
        // Utiliser des structures thread-safe pour les compteurs
        ConcurrentHashMap<Integer, AtomicInteger> overlapCountMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, AtomicInteger> pixelValueMap = new ConcurrentHashMap<>();
        
        // Traitement parallèle des patchs
        IntStream patchStream = IntStream.range(0, Y_patchs.size());
        if (useParallelProcessing) {
            patchStream = patchStream.parallel();
        }
        
        patchStream.forEach(patchIdx -> {
            Patch patch = Y_patchs.get(patchIdx);
            int[] position = patch.getPosition();
            int patchSize = patch.getS();
            double[][] patchData = patch.getData();
            
            // Ajouter les valeurs du patch à l'image
            for (int i = 0; i < patchSize; i++) {
                for (int j = 0; j < patchSize; j++) {
                    int imageI = position[0] + i;
                    int imageJ = position[1] + j;
                    
                    if (imageI < l && imageJ < c) {
                        int pixelKey = imageI * c + imageJ; // Clé unique pour chaque pixel
                        
                        // Incrémenter le compteur de chevauchement pour ce pixel
                        overlapCountMap.computeIfAbsent(pixelKey, k -> new AtomicInteger(0)).incrementAndGet();
                        
                        // Ajouter la valeur du patch au pixel
                        int pixelValue = (int)(patchData[i][j] * 1000); // Mise à l'échelle pour éviter les erreurs d'arrondi
                        pixelValueMap.computeIfAbsent(pixelKey, k -> new AtomicInteger(0)).addAndGet(pixelValue);
                    }
                }
            }
        });
        
        // Normaliser les pixels en divisant par le nombre de chevauchements
        for (int i = 0; i < l; i++) {
            for (int j = 0; j < c; j++) {
                int pixelKey = i * c + j;
                
                AtomicInteger overlapCount = overlapCountMap.get(pixelKey);
                AtomicInteger pixelValue = pixelValueMap.get(pixelKey);
                
                if (overlapCount != null && overlapCount.get() > 0) {
                    double normalizedValue = (double)pixelValue.get() / (overlapCount.get() * 1000.0);
                    reconstructedImage.setPixel(i, j, normalizedValue);
                }
            }
        }
        
        return reconstructedImage;
    }
    
    /**
     * Découpe une image en imagettes selon l'approche locale
     * @param X Image à découper
     * @param W Taille des imagettes (W×W)
     * @param n Nombre approximatif d'imagettes souhaité (sera ajusté pour couvrir l'image)
     * @return Liste des imagettes extraites
     */
    @Override
    public List<Imagette> DecoupeImage(Image X, int W, int n) {
        // Par défaut, on utilise le mode de couverture complète avec chevauchement
        return DecoupeImage(X, W, n, OVERLAP_FULL);
    }
    
    /**
     * Découpe une image en imagettes avec un mode de couverture spécifique
     * @param X Image à découper
     * @param W Taille des imagettes (W×W)
     * @param n Nombre d'imagettes souhaité (si coverageMode=EXACT_N)
     * @param fullCoverage Si true, couvre toute l'image avec chevauchement, sinon extrait n imagettes
     * @return Liste des imagettes extraites
     */
    @Override
    public List<Imagette> DecoupeImage(Image X, int W, int n, boolean fullCoverage) {
        // Conversion du booléen en mode de couverture
        int coverageMode = fullCoverage ? OVERLAP_FULL : EXACT_N;
        return DecoupeImage(X, W, n, coverageMode);
    }
    
    /**
     * Découpe une image en imagettes avec un mode de couverture spécifique
     * @param X Image à découper
     * @param W Taille des imagettes (W×W)
     * @param n Nombre d'imagettes souhaité (si coverageMode=EXACT_N)
     * @param coverageMode Mode de couverture: 0=EXACT_N, 1=OVERLAP_FULL, 2=NO_OVERLAP
     * @return Liste des imagettes extraites
     */
    @Override
    public List<Imagette> DecoupeImage(Image X, int W, int n, int coverageMode) {
        // Appel avec les paramètres par défaut
        return DecoupeImage(X, W, n, coverageMode, 50, false);
    }
    
    /**
     * Découpe une image en imagettes avec un mode de couverture spécifique,
     * contrôle du chevauchement et gestion des bords
     * @param X Image à découper
     * @param W Taille des imagettes (W×W)
     * @param n Nombre d'imagettes souhaité (si coverageMode=EXACT_N)
     * @param coverageMode Mode de couverture: 0=EXACT_N, 1=OVERLAP_FULL, 2=NO_OVERLAP
     * @param overlapPercentage Pourcentage de chevauchement (utilisé si coverageMode=OVERLAP_FULL)
     * @param mirrorEdges Si true, utilise l'effet miroir pour les bords
     * @return Liste des imagettes extraites
     */
    public List<Imagette> DecoupeImage(Image X, int W, int n, int coverageMode, int overlapPercentage, boolean mirrorEdges) {
        if (X == null) {
            throw new IllegalArgumentException("L'image ne peut pas être null");
        }
        if (W <= 0) {
            throw new IllegalArgumentException("La taille des imagettes doit être positive");
        }
        if (coverageMode == EXACT_N && n <= 0) {
            throw new IllegalArgumentException("Le nombre d'imagettes doit être positif");
        }
        
        int height = X.getHeight();
        int width = X.getWidth();
        
        if (W > height || W > width) {
            throw new IllegalArgumentException("La taille des imagettes ne peut pas dépasser celle de l'image");
        }
        
        List<Imagette> imagettes = new ArrayList<>();
        
        switch (coverageMode) {
            case OVERLAP_FULL:
                // Calculer le stride en fonction du pourcentage de chevauchement
                double stride = W * (1 - overlapPercentage / 100.0);
                int strideInt = Math.max(1, (int) Math.round(stride)); // Au moins 1 pixel
                
                // Extraction en partant du bas de l'image
                for (int i = height - W; i >= 0; i -= strideInt) {
                    // Si on ne fait pas d'effet miroir, ajuster la position pour les bords
                    if (!mirrorEdges && i < height - W && i > 0 && i < strideInt) {
                        i = 0; // S'assurer de couvrir le bord supérieur
                    }
                    
                    for (int j = 0; j <= width - W; j += strideInt) {
                        // Ajuster pour le dernier patch à droite
                        if (!mirrorEdges && j > width - W - strideInt && j != width - W) {
                            j = width - W;
                        }
                        
                        extractImagette(X, W, i, j, imagettes, mirrorEdges);
                    }
                }
                
                System.out.println("Extraction d'imagettes terminée en mode couverture avec chevauchement " + 
                                  overlapPercentage + "% : " + imagettes.size() + " imagettes extraites.");
                break;
                
            case NO_OVERLAP:
                // Couverture complète sans chevauchement
                for (int i = height - W; i >= 0; i -= W) {
                    for (int j = 0; j <= width - W; j += W) {
                        extractImagette(X, W, i, j, imagettes, mirrorEdges);
                    }
                }
                
                System.out.println("Extraction d'imagettes terminée en mode sans chevauchement : " + 
                                  imagettes.size() + " imagettes extraites.");
                break;
                
            case EXACT_N:
                // Extraction d'exactement n imagettes aléatoires
                Random random = new Random();
                int maxI = height - W;
                int maxJ = width - W;
                
                for (int k = 0; k < n; k++) {
                    int i = random.nextInt(maxI + 1);
                    int j = random.nextInt(maxJ + 1);
                    
                    extractImagette(X, W, i, j, imagettes, mirrorEdges);
                }
                
                System.out.println("Extraction d'imagettes terminée en mode exact " + n + 
                                  " imagettes : " + imagettes.size() + " imagettes extraites.");
                break;
                
            default:
                throw new IllegalArgumentException("Mode de couverture non reconnu");
        }
        
        return imagettes;
    }
    
    /**
     * Extrait une imagette à la position spécifiée
     * @param X Image source
     * @param W Taille de l'imagette
     * @param i Position verticale
     * @param j Position horizontale
     * @param imagettes Liste d'imagettes à laquelle ajouter l'imagette
     * @param mirrorEdges Si true, utilise l'effet miroir pour les bords
     */
    private void extractImagette(Image X, int W, int i, int j, List<Imagette> imagettes, boolean mirrorEdges) {
        double[][] data = new double[W][W];
        
        // Copie des données de l'image dans l'imagette
        for (int k = 0; k < W; k++) {
            for (int l = 0; l < W; l++) {
                if (mirrorEdges) {
                    // Utiliser l'effet miroir pour les pixels hors limite
                    int row = i + k;
                    int col = j + l;
                    
                    // Miroir pour les lignes
                    if (row < 0) {
                        row = -row;
                    } else if (row >= X.getHeight()) {
                        row = 2 * X.getHeight() - row - 2;
                    }
                    
                    // Miroir pour les colonnes
                    if (col < 0) {
                        col = -col;
                    } else if (col >= X.getWidth()) {
                        col = 2 * X.getWidth() - col - 2;
                    }
                    
                    data[k][l] = X.getPixel(row, col);
                } else {
                    // Pas d'effet miroir, juste vérifier les limites
                    if (i + k < X.getHeight() && j + l < X.getWidth()) {
                        data[k][l] = X.getPixel(i + k, j + l);
                    } else {
                        // Gérer le cas où l'imagette dépasse l'image (ne devrait pas arriver)
                        data[k][l] = 0;
                    }
                }
            }
        }
        
        int[] position = {i, j};
        imagettes.add(new Imagette(data, position));
    }
    
    @Override
    public List<double[]> VectorPatchs(List<Patch> Y_patchs) {
        if (useParallelProcessing) {
            return VectorPatchsParallel(Y_patchs);
        } else {
            // Implémentation séquentielle originale
            List<double[]> vecteurs = new ArrayList<>();
            
            for (Patch patch : Y_patchs) {
                double[][] data = patch.getData();
                int s = patch.getS();
                double[] vecteur = new double[s * s];
                
                // Vectorisation du patch
                for (int i = 0; i < s; i++) {
                    for (int j = 0; j < s; j++) {
                        vecteur[i * s + j] = data[i][j];
                    }
                }
                
                vecteurs.add(vecteur);
            }
            
            return vecteurs;
        }
    }
    
    /**
     * Version parallèle pour la vectorisation des patchs
     */
    public List<double[]> VectorPatchsParallel(List<Patch> Y_patchs) {
        IntStream patchStream = IntStream.range(0, Y_patchs.size());
        if (useParallelProcessing) {
            patchStream = patchStream.parallel();
        }
        
        return patchStream
            .mapToObj(i -> {
                Patch patch = Y_patchs.get(i);
                double[][] data = patch.getData();
                int s = patch.getS();
                double[] vecteur = new double[s * s];
                
                // Vectorisation du patch
                for (int k = 0; k < s; k++) {
                    for (int l = 0; l < s; l++) {
                        vecteur[k * s + l] = data[k][l];
                    }
                }
                
                return vecteur;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Patch> DevectorisePatchs(List<double[]> vecteurs, List<int[]> positions, int s) {
        if (vecteurs == null || vecteurs.isEmpty()) {
            throw new IllegalArgumentException("La liste de vecteurs ne peut pas être vide");
        }
        if (positions == null || positions.isEmpty()) {
            throw new IllegalArgumentException("La liste de positions ne peut pas être vide");
        }
        if (vecteurs.size() != positions.size()) {
            throw new IllegalArgumentException("Le nombre de vecteurs et de positions doit être identique");
        }
        
        List<Patch> patches = new ArrayList<>();
        
        for (int i = 0; i < vecteurs.size(); i++) {
            double[] vecteur = vecteurs.get(i);
            int[] position = positions.get(i);
            
            Patch patch = Patch.fromVector(vecteur, s, position);
            patches.add(patch);
        }
        
        return patches;
    }
    
    // Méthode de démonstration pour compter le nombre d'imagettes nécessaires
    public int countRequiredImagettes(int imageWidth, int imageHeight, int imagetteSize, int overlapPercentage) {
        // Calculer le stride en fonction du pourcentage de chevauchement
        double stride = imagetteSize * (1 - overlapPercentage / 100.0);
        int strideInt = Math.max(1, (int) Math.round(stride));
        
        // Calculer le nombre d'imagettes dans chaque dimension
        int numRows = (int) Math.ceil((double) imageHeight / strideInt);
        int numCols = (int) Math.ceil((double) imageWidth / strideInt);
        
        return numRows * numCols;
    }
} 