Diagramme de cas d'utilisation du programme de (dé)bruitage
Cette section décrit en détail le diagramme de cas d'utilisation présenté à la figure Y. Elle complète l'analyse structurelle (diagramme de classes) en mettant l'accent sur les interactions entre les acteurs et le système.
Vue globale
Le diagramme fait intervenir deux acteurs :
Utilisateur : la personne qui pilote l'application via l'interface graphique.


Système : le noyau logiciel chargé d'exécuter les traitements d'image (ajout de bruit, extraction de patchs, PCA, seuillage, reconstruction, sauvegarde).


L'utilisateur ne communique jamais directement avec les modules internes ; toutes ses actions transitent par des commandes GUI, lesquelles déclenchent des cas d'utilisation côté Système.
Parcours principal
Lancer interface graphique
   → Sélectionner mode (Bruitage ou Débruitage)
      • Bruitage : choisir variance → Bruiter X0 → Sauvegarder Xb
      • Débruitage : choisir paramètres (ACP, seuillage) → Débruiter Xb → Sauvegarder X̂0
Fermer fenêtre

Bruitage
Sélectionner mode bruitage : l'utilisateur active ce scénario.


Sélectionner une image X0 :


soit depuis le dossier "ressources" du projet,


soit depuis un dossier personnel.


Sélectionner variance (σ²) : vérifiée ensuite par le cas d'utilisation Vérifier sigma (condition σ > 0).


Bruiter X0 : le Système génère une image bruitée Xb.


Sauvegarder Xb : optionnel, déclenché si l'utilisateur souhaite conserver l'image bruitée.


Débruitage
Sélectionner mode débruitage.


Sélectionner image Xb (même logique de dossier que précédemment).


Extraire patchs puis Vectoriser patchs.


Effectuer ACP :


ACP locale ou ACP globale selon le paramètre type ACP.


Déterminer variance et Sélectionner seuil (λ) avec validation Vérifier seuil (λ > 0).


Projeter avec seuillage choisi (souple ou dur).


Reconstruire estimation de X0.


Sauvegarde de l'image restaurée.


Fonctions réutilisables via « include »
Cas d'utilisation inclus
Portée
Pourquoi ?
Lister images perso
Sélections d'image
fournit la liste des fichiers disponibles dans le dossier de l'utilisateur.
Vérifier sigma / seuil
Paramétrage
assure que les valeurs saisies sont strictement positives.
Cliquer bouton sauvegarde
Bruitage / Débruitage
factorise la logique de persistance sur disque.
Fermer fenêtre
Tous scénarios
fermeture propre de la session GUI.

Extensions « extend »
Sauvegarder Xb étend Bruiter : la sauvegarde n'est déclenchée que si l'utilisateur clique sur le bouton correspondant.


Sauvegarde de X0̂ étend Débruiter dans la même logique.


Cohérence avec la couche objet
Les cas d'utilisation Bruiter et Débruiter s'appuient directement sur les services exposés par les classes Image, PatchExtractor, PCAEngine et ThreshHolding. Les vérifications de paramètres (σ, λ) et la sélection des options (type ACP, type seuillage) correspondent aux attributs de ImageDenoiser.
En séparant clairement les intentions utilisateur (GUI) et les fonctionnalités coeur (Système), le diagramme offre :
une interface utilisateur riche mais simple, centrée sur le choix des paramètres ;


un noyau algorithmique testable indépendamment de la présentation ;


la possibilité d'ajouter de nouveaux modes (par ex. un débruitage fréquentiel) sans bouleverser l'interface existante.


Conclusion
Le diagramme de cas d'utilisation clarifie le rôle de chaque acteur, décrit les scénarios principaux et détaille les alternatives et validations, offrant ainsi un cadre fonctionnel précis avant toute implémentation.

