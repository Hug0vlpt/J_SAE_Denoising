#!/bin/bash

# Script de génération de la documentation JavaDoc pour le projet SAE-Denoising

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Génération de la documentation JavaDoc ===${NC}"

# 1. Créer le répertoire de documentation s'il n'existe pas
echo "Création du répertoire docs/javadoc si nécessaire..."
mkdir -p ./docs/javadoc

# 2. Générer la documentation
echo "Génération de la documentation JavaDoc..."
javadoc -d ./docs/javadoc -classpath ./bin:./lib/ij.jar -encoding UTF-8 -charset UTF-8 -docencoding UTF-8 \
        -author -version -windowtitle "SAE-Denoising Documentation" \
        -header "Débruitage d'images par ACP" \
        -tag theory:a:"Théorie:" \
        -tag implementation:a:"Implémentation:" \
        -tag complexity:a:"Complexité:" \
        -tag result:a:"Résultat:" \
        -tag reference:a:"Référence:" \
        -noqualifier java.lang:java.util \
        -Xdoclint:none \
        -sourcepath ./src \
        image patch pca thresholding app

# 3. Vérifier si la génération a réussi
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Documentation générée avec succès !${NC}"
    echo -e "Pour consulter la documentation, ouvrez ${YELLOW}./docs/javadoc/index.html${NC} dans un navigateur"
    echo -e "Tags personnalisés supportés: ${YELLOW}@theory${NC}, ${YELLOW}@implementation${NC}, ${YELLOW}@complexity${NC}, ${YELLOW}@result${NC}, ${YELLOW}@reference${NC}"
else
    echo -e "${RED}Erreur lors de la génération de la documentation !${NC}"
    echo -e "${YELLOW}Note: Des avertissements peuvent apparaître mais la documentation a pu être générée${NC}"
    # On ne quitte pas en erreur même si javadoc a retourné une erreur
    exit 0
fi
