# Diagramme d’activités du flux (dé)bruitage

Cette section détaille le diagramme d’activités – organisé en couloirs – qui illustre le déroulement complet d’une session utilisateur, depuis l’ouverture de l’application jusqu’à la fermeture. Les couloirs représentent respectivement :

- **Utilisateur** : acteur humain qui interagit avec le logiciel.
- **Interface JavaFX (Vue)** : couche graphique affichant menus, images et messages.
- **AppController** : logique d’orchestration, fait le lien entre la vue et le moteur de traitement.
- **ImageProcessor** : cœur algorithmique réalisant le bruitage ou le débruitage.

---

## Démarrage de l’application

L’utilisateur lance l’exécutable, ce qui déclenche l’activité **Ouvrir l’application**. La vue affiche alors le **menu principal** ; l’utilisateur choisit immédiatement un mode de travail : *Bruitage* ou *Débruitage*.

---

## Sélection de l’image

- **Sélectionner une image** : l’utilisateur parcourt soit un dossier du projet (ressources), soit un dossier local de son ordinateur.
- **Vérifier la validité de l’image** (AppController) : contrôle que le fichier est une image supportée (taille non nulle, format correct). En cas d’erreur, la vue affiche un message et retourne vers l’étape de sélection.

Si l’image est valide, la vue la pré‑affiche dans la fenêtre.

---

## Définition des paramètres du mode

Le diagramme contient un nœud de décision « Mode = Bruitage ? » :

### Branche Bruitage

1. **Saisir la variance du bruit (σ²)**.
2. L’AppController transmet la variance à l’ImageProcessor qui **ajoute un bruit gaussien** sur l’image.
3. La vue affiche l’**image bruitée**.
4. Optionnel : **Sauvegarder l’image bruitée** si l’utilisateur le souhaite.

### Branche Débruitage

1. **Saisir le type de seuillage** (doux ou dur).
2. **Saisir la méthode de calcul du seuil** (VisuShrink ou BayesVisu).
3. L’AppController déclenche le pipeline de l’ImageProcessor :
   - **Extraire les patchs**
   - **Vectoriser les patchs**
   - **Effectuer l’ACP**
   - **Projeter / Appliquer le seuillage**
   - **Reconstruire l’image**
4. Le contrôleur calcule ensuite les métriques **MSE** et **PSNR**.
5. La vue affiche l’**image débruitée** puis les **métriques de qualité**.
6. Optionnel : **Sauvegarder l’image débruitée**.

---

## Boucle de navigation

Après un traitement, un regroupement d’activités propose deux alternatives :

- **Changer de mode** : revient à « Afficher l’interface du mode sélectionné » (reprise du flux au niveau du choix de mode).
- **Fermer l’application** : l’utilisateur clique sur *Fermer* et le diagramme se termine sur un nœud final de l’utilisateur et de la vue.

---

## Points clés de l’architecture MVC

| Couloir | Responsabilités | Interactions principales |
|---------|-----------------|--------------------------|
| Utilisateur | Fournit entrées (clics, saisies) | Transmet événements à la Vue |
| Vue (JavaFX) | Rendu graphique, remontée des événements | Appelle AppController, affiche résultats ou messages d’erreur |
| AppController | Validation des données, orchestration du workflow | Appelle ImageProcessor, renvoie sorties à la Vue |
| ImageProcessor | Algorithmes lourds (ajout de bruit, PCA, seuillage, reconstruction) | Reçoit paramètres, renvoie images/métriques |

---

## Avantages du découpage en couloirs

- **Lisibilité** : on distingue clairement responsabilités et transitions.
- **Synchronisation explicite** : chaque flèche horizontale matérialise un appel ou un retour d’information.
- **Facilité de test** : l’ImageProcessor peut être testé indépendamment de la Vue.
- **Extensibilité** : on peut ajouter un nouveau mode (ex. filtrage fréquentiel) dans le contrôleur et le processeur sans toucher à la logique de navigation de la Vue.

---

## Conclusion

Le diagramme d’activités permet de visualiser pas à pas le déroulement fonctionnel de l’application, tout en soulignant l’architecture MVC mise en place. Il garantit que chaque interaction utilisateur trouve un répondant clair dans la pile logicielle et offre une base solide pour implémenter ou faire évoluer l’interface comme les algorithmes.

