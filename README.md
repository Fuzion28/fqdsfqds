# Sonora 🎧

Lecteur de musique Android inspiré de Spotify, en **Kotlin + Jetpack Compose**, pour lire la musique stockée sur le téléphone.

## Fonctionnalités

- **Accueil** : salutation selon l'heure, accès rapides, carte « lecture aléatoire » animée, écoutés récemment, ajoutés récemment, mix par artiste, albums et artistes.
- **Recherche** : recherche instantanée (sans accents), filtres Titres / Albums / Artistes, grille « Parcourir tout » colorée.
- **Bibliothèque** : titres likés, playlists (créer, renommer, supprimer, ajouter/retirer des titres), albums, artistes ; vue liste ou grille, tri récent / alphabétique.
- **Lecteur plein écran** : fond dégradé aux couleurs de la pochette, pochette qu'on fait glisser pour changer de titre, halo animé, barre de progression interactive, aléatoire / répéter, file d'attente, bouton like animé.
- **Mini-lecteur** qui s'ouvre en plein écran (on le referme en glissant vers le bas), et qu'on fait glisser pour passer au titre suivant ou précédent.
- **Lecture en arrière-plan** (Media3) : notification multimédia, contrôles sur l'écran de verrouillage et depuis le casque.

## Récupérer l'APK

Chaque push lance le workflow GitHub Actions **Build APK** :

- l'APK est disponible dans l'onglet **Actions** → dernier run → artefact `Sonora-apk` ;
- il est aussi publié dans la release **apk-latest** (onglet *Releases*), pratique pour le télécharger directement sur le téléphone.

> L'APK est signé avec une clé de debug générée par la CI. Si l'installation d'une nouvelle version échoue, désinstallez d'abord l'ancienne.

## Compiler en local

Ouvrez le dossier dans Android Studio, ou lancez :

```bash
./gradlew assembleRelease
```

Android 8.0 (API 26) minimum.
