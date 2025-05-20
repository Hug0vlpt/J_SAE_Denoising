package patch;

import java.util.List;
import ij.process.ImageProcessor;
import image.Image;
import ij.process.ByteProcessor;
import java.util.Arrays;

/**
 * @theory
 * Classe d'utilitaires pour le calcul de métriques
 * Notamment MSE (Mean Squared Error) et PSNR (Peak Signal-to-Noise Ratio)
 */
public class MetricsUtils {
    
    // Constantes pour le calcul du SSIM
    private static final double K1 = 0.01;
    private static final double K2 = 0.03;
    private static final double L = 255.0; // Dynamique des niveaux de gris
    
    /**
     * Calcule l'erreur quadratique moyenne (MSE) entre deux images
     * Version française pour compatibilité
     * @param original Image originale
     * @param compared Image à comparer
     * @return MSE (erreur quadratique moyenne)
     */
    public static double calculerMSE(Image original, Image compared) {
        if (original == null || compared == null) {
            throw new IllegalArgumentException("Les images ne peuvent pas être null");
        }
        
        if (original.getWidth() != compared.getWidth() || original.getHeight() != compared.getHeight()) {
            throw new IllegalArgumentException("Les images doivent avoir les mêmes dimensions");
        }
        
        int width = original.getWidth();
        int height = original.getHeight();
        double sum = 0.0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double diff = original.getPixel(y, x) - compared.getPixel(y, x);
                sum += diff * diff;
            }
        }
        
        return sum / (width * height);
    }
    
    /**
     * Calcule le PSNR (Peak Signal to Noise Ratio) entre deux images
     * Version française pour compatibilité
     * @param original Image originale
     * @param compared Image à comparer
     * @return PSNR en dB
     */
    public static double calculerPSNR(Image original, Image compared) {
        double mse = calculerMSE(original, compared);
        
        if (mse == 0) {
            return 100.0; // Images identiques, PSNR infini (valeur très élevée)
        }
        
        // PSNR = 10 * log10(MAX^2 / MSE)
        return 10.0 * Math.log10(255.0 * 255.0 / mse);
    }
    
    /**
     * Calculates MSE between two images
     * @param original Original image
     * @param compared Compared image
     * @return MSE value
     */
    public static double calculateMSE(Image original, Image compared) {
        return calculerMSE(original, compared);
    }
    
    /**
     * Calculates PSNR from a given MSE value
     * @param mse The MSE value
     * @return PSNR in dB
     */
    public static double calculatePSNR(double mse) {
        if (mse == 0) {
            return 100.0; // Perfect match
        }
        
        // PSNR = 10 * log10(MAX^2 / MSE)
        return 10.0 * Math.log10(255.0 * 255.0 / mse);
    }
    
    /**
     * Calculates SSIM (Structural Similarity Index) between two image processors
     * @param original Original image processor
     * @param processed Processed image processor
     * @return SSIM value (between 0 and 1)
     */
    public static double calculateSSIM(ImageProcessor original, ImageProcessor processed) {
        final int width = original.getWidth();
        final int height = original.getHeight();
        
        if (width != processed.getWidth() || height != processed.getHeight()) {
            throw new IllegalArgumentException("Images must have the same dimensions for SSIM calculation");
        }
        
        // Variables for mean, variance and covariance
        double meanX = 0, meanY = 0;
        double varX = 0, varY = 0, covXY = 0;
        
        // Calculate means
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                meanX += original.getPixelValue(x, y);
                meanY += processed.getPixelValue(x, y);
            }
        }
        
        meanX /= (width * height);
        meanY /= (width * height);
        
        // Calculate variances and covariance
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double px = original.getPixelValue(x, y);
                double py = processed.getPixelValue(x, y);
                
                varX += (px - meanX) * (px - meanX);
                varY += (py - meanY) * (py - meanY);
                covXY += (px - meanX) * (py - meanY);
            }
        }
        
        varX /= (width * height - 1);  // Unbiased estimate
        varY /= (width * height - 1);  // Unbiased estimate
        covXY /= (width * height - 1);  // Unbiased estimate
        
        // Constants for stability
        double C1 = (K1 * L) * (K1 * L);
        double C2 = (K2 * L) * (K2 * L);
        
        // SSIM formula
        double numerator = (2 * meanX * meanY + C1) * (2 * covXY + C2);
        double denominator = (meanX * meanX + meanY * meanY + C1) * (varX + varY + C2);
        
        return numerator / denominator;
    }
    
    /**
     * Calculates SSIM between two images
     * @param original Original image
     * @param processed Processed image
     * @return SSIM value
     */
    public static double calculateSSIM(Image original, Image processed) {
        return calculateSSIM(original.getProcessor(), processed.getProcessor());
    }
    
    /**
     * Calculates MSE between two ByteProcessor objects
     * @param original Original ByteProcessor
     * @param processed Processed ByteProcessor
     * @return MSE value
     */
    public static double calculateMSE(ByteProcessor original, ByteProcessor processed) {
        int width = original.getWidth();
        int height = original.getHeight();
        double sum = 0.0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double diff = original.getPixel(x, y) - processed.getPixel(x, y);
                sum += diff * diff;
            }
        }
        
        return sum / (width * height);
    }
    
    /**
     * Calculates PSNR between two ImageProcessor objects
     * @param original Original ImageProcessor
     * @param processed Processed ImageProcessor
     * @return PSNR value in dB
     */
    public static double calculatePSNR(ImageProcessor original, ImageProcessor processed) {
        double mse = calculateMSE(original, processed);
        if (mse == 0) return 100.0; // Perfect match
        return 10.0 * Math.log10(255.0 * 255.0 / mse);
    }
    
    /**
     * Calculates PSNR between two ByteProcessor objects
     * @param original Original ByteProcessor
     * @param processed Processed ByteProcessor
     * @return PSNR value in dB
     */
    public static double calculatePSNR(ByteProcessor original, ByteProcessor processed) {
        double mse = calculateMSE(original, processed);
        if (mse == 0) return 100.0; // Perfect match
        return 10.0 * Math.log10(255.0 * 255.0 / mse);
    }
    
    /**
     * Creates an Image from an ImageProcessor
     * @param proc The ImageProcessor to convert
     * @return A new Image
     */
    private static Image createImageFromProcessor(ImageProcessor proc) {
        // Create a temporary Image from ImagePlus
        return new Image(new ij.ImagePlus("temp", proc));
    }
    
    /**
     * Calculates MSE between two ImageProcessor objects by converting them to Image
     * @param original The original ImageProcessor
     * @param processed The processed ImageProcessor
     * @return The MSE value
     */
    public static double calculateMSEFromProcessors(ImageProcessor original, ImageProcessor processed) {
        Image img1 = createImageFromProcessor(original);
        Image img2 = createImageFromProcessor(processed);
        return calculateMSE(img1, img2);
    }
    
    /**
     * Calculates PSNR between two ImageProcessor objects by converting them to Image
     * @param original The original ImageProcessor
     * @param processed The processed ImageProcessor
     * @return The PSNR value
     */
    public static double calculatePSNRFromProcessors(ImageProcessor original, ImageProcessor processed) {
        double mse = calculateMSEFromProcessors(original, processed);
        return calculatePSNR(mse);
    }
    
    /**
     * Calculates SSIM between two ImageProcessor objects
     * @param original The original ImageProcessor
     * @param processed The processed ImageProcessor
     * @return The SSIM value
     */
    public static double calculateSSIMFromProcessors(ImageProcessor original, ImageProcessor processed) {
        return calculateSSIM(original, processed);
    }
    
    /**
     * Calculates MSE between two ImageProcessor objects
     * @param original The original ImageProcessor
     * @param processed The processed ImageProcessor
     * @return The MSE value
     */
    public static double calculateMSE(ImageProcessor original, ImageProcessor processed) {
        if (original instanceof ByteProcessor && processed instanceof ByteProcessor) {
            return calculateMSE((ByteProcessor)original, (ByteProcessor)processed);
        }
        
        int width = original.getWidth();
        int height = original.getHeight();
        double sum = 0.0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double diff = original.getPixelValue(x, y) - processed.getPixelValue(x, y);
                sum += diff * diff;
            }
        }
        
        return sum / (width * height);
    }
} 