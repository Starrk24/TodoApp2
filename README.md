# Moja Lista Zadań (To-Do App)

Aplikacja mobilna na system Android służąca do zarządzania codziennymi zadaniami. Projekt został przygotowany jako zadanie zaliczeniowe przy użyciu nowoczesnych narzędzi programistycznych.

## 🚀 Funkcjonalności
- **Ekran Powitalny (Splash Screen):** Profesjonalne wejście do aplikacji z weryfikacją statusu logowania.
- **System Użytkowników:** Pełna rejestracja i logowanie przy użyciu Firebase Authentication.
- **Interaktywne Menu Kołowe:** Autorski komponent graficzny (Custom Canvas) pozwalający na wybór kategorii zadań (Dom, Praca, Sport, Hobby).
- **Zarządzanie Zadaniami (CRUD):** Dodawanie, edytowanie, usuwanie oraz oznaczanie zadań jako ukończone.
- **Kategoryzacja:** Możliwość filtrowania zadań według przypisanej kategorii.
- **Trwałość danych:** Automatyczny zapis listy zadań w pamięci urządzenia (SharedPreferences + JSON).
- **Responsywność:** Pełna obsługa obrotu ekranu bez utraty wprowadzonych danych.

## 🛠 Technologie
- **Język:** Kotlin
- **UI:** Jetpack Compose (Material Design 3)
- **Nawigacja:** Jetpack Navigation
- **Backend/Auth:** Firebase Authentication
- **Przechowywanie danych:** SharedPreferences

## 📋 Konfiguracja Firebase
Aplikacja wymaga połączenia z usługą Firebase, aby system logowania działał poprawnie:
1. Utwórz projekt w [Firebase Console](https://console.firebase.google.com/).
2. Dodaj aplikację Android z nazwą pakietu: `com.example.todoapp`.
3. Pobierz plik `google-services.json` i umieść go w folderze `app/` swojego projektu.
4. W sekcji **Build -> Authentication** włącz metodę logowania **Email/Password**.
5. W pliku `build.gradle` (Project) oraz `build.gradle` (Module) dodaj niezbędne wtyczki Google Services (skonfigurowane w projekcie).

## 🔨 Instrukcja uruchomienia
1. Pobierz kod źródłowy i otwórz go w **Android Studio**.
2. Upewnij się, że plik `google-services.json` znajduje się w folderze `/app`.
3. Kliknij ikonę słonika (**Sync Project with Gradle Files**) w prawym górnym rogu.
4. Wybierz emulator (minimum API 24) lub podłącz fizyczne urządzenie.
5. Kliknij zielony przycisk **Run**.

## 📦 Generowanie APK
Aby wygenerować plik instalacyjny:
1. Wejdź w menu `Build -> Build Bundle(s) / APK(s) -> Build APK(s)`.
2. Po zakończeniu procesu kliknij `locate` w powiadomieniu, aby znaleźć plik `app-debug.apk`.