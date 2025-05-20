#!/bin/bash

# Script pour exécuter le projet SAE-Denoising

# Couleurs pour les messages
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Exécution du projet SAE-Denoising ===${NC}"

# 1. Vérifier que le projet est compilé
if [ ! -d "./bin" ] || [ -z "$(ls -A ./bin)" ]; then
    echo -e "${RED}Erreur: Le projet n'est pas compilé.${NC}"
    echo -e "Veuillez d'abord lancer le script de compilation avec ./build.sh"
    exit 1
fi

# 2. Exécuter le programme de test
echo -e "Lancement du programme de test..."
java -cp ./bin:./lib/ij.jar:./lib/jama.jar app.TestDenoising

# 3. Vérifier si l'exécution a réussi
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Exécution terminée avec succès !${NC}"
else
    echo -e "${RED}Erreur lors de l'exécution !${NC}"
    exit 1
fi 