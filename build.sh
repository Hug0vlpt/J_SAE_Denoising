#!/bin/bash

# Script de compilation pour le projet SAE-Denoising
# Compile les fichiers Java et les place dans le répertoire bin/

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Compilation du projet SAE-Denoising ===${NC}"

# 1. Supprimer les fichiers .class du répertoire src/
echo "Nettoyage des fichiers .class dans src/..."
find ./src -name "*.class" -type f -delete

# 2. Créer le répertoire bin/ s'il n'existe pas
if [ ! -d "./bin" ]; then
    echo "Création du répertoire bin/..."
    mkdir -p ./bin
fi

# 3. Compiler les fichiers Java
echo "Compilation des fichiers Java..."
javac -d bin -classpath ./bin:./lib/ij.jar:./lib/jama.jar -encoding UTF-8 -sourcepath ./src $(find ./src -name "*.java")

# 4. Vérifier si la compilation a réussi
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Compilation terminée avec succès !${NC}"
    echo -e "Pour lancer le programme de test, utilisez la commande :"
    echo -e "java -cp ./bin:./lib/ij.jar:./lib/jama.jar app.TestDenoising"
else
    echo -e "${RED}Erreur lors de la compilation !${NC}"
    exit 1
fi
