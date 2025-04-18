---
title: "My Notes"
date: 2025-04-06
summary: "A Java-based note-taking application focusing on simplicity and extensibility."
tags: ["Java", "Android-app", "My Notes"]
images: ["projects/mynotes/ui-demo.png", "projects/mynotes/code-snapshot.png"]
type: page 
ShowPostNavLinks: true
showTableOfContents: true
draft: false
weight: 26
---

## Description

**My Notes** is a beginner-friendly Android application that serves as a simple and elegant note-taking solution. Developed in Java with the native Android SDK, this app demonstrates core Android development concepts including UI handling, memory management, and interaction through Java-based event-driven logic.

The aim is to keep the application lightweight and extensible, suitable both as a personal note manager and as a sandbox project for developers learning Android.

---

## Introduction

**My Notes** is a lightweight Android application designed to simplify note management. Built with Java and native Android SDK, it allows users to create and view notes through a minimalistic user interface. The project is currently in its early development stages, featuring a clean design and intuitive workflow.

This project is aimed at showcasing foundational Android development concepts such as activity lifecycle management, event listeners, UI inflation from XML, and Java-based logic separation. It's a solid starting point for any Android beginner.

🔗 GitHub Repository: [My-Notes by shivjeet1](https://github.com/shivjeet1/My-Notes)

---

## Key Features and Functionality

### Core Features

- ✍️ **Creating Notes**: Users can input and save notes directly via the interface using an `EditText` field and a `Button` tied to a custom event handler using `setOnClickListener()`.
- 📄 **Viewing Notes**: Saved notes are dynamically rendered as `TextView` widgets added programmatically to a vertically scrollable container (`LinearLayout`).

The application currently uses an `ArrayList<String>` to temporarily store note entries in memory during the session. This makes the app lightweight and avoids complex boilerplate in its early stage, while still demonstrating practical coding practices.

Behind the scenes, the `NoteManager` class encapsulates the logic for adding, retrieving, and managing notes. This abstraction helps maintain separation of concerns and prepares the codebase for modular growth (such as swapping in a database later).

---

## Project Structure

```plaintext
My-Notes/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── example/
│   │       │           └── mynotes/
│   │       │               ├── MainActivity.java         ← Handles UI & event listeners
│   │       │               └── NoteManager.java           ← Manages logic for notes (add/retrieve)
│   │       └── res/
│   │           ├── layout/
│   │           │   └── activity_main.xml                  ← UI layout defined in XML
│   │           └── values/
│   │               └── strings.xml                       ← String resources for i18n & reuse
├── build.gradle.kts
├── gradle/
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
```

---

## UI Design

The current UI uses basic Android Views like `EditText`, `Button`, and `TextView`. It is clean, functional, and easy to navigate. All event handlers are wired via `setOnClickListener()` in Java code. Below are four screenshots from the actual app:

<div style="display: flex; flex-wrap: wrap; gap: 16px;">
  <img src="/images/mynotes/ui-demo1.png" width="45%" />
  <img src="/images/mynotes/ui-demo2.png" width="45%" />
  <img src="/images/mynotes/ui-demo3.png" width="45%" />
  <img src="/images/mynotes/ui-demo4.png" width="45%" />
</div>

> All UI elements are designed using XML with a focus on simplicity and readability. The layout is defined in `activity_main.xml`, using a vertical `LinearLayout` with scroll support.

---

## Future Improvements

### Persistent Storage

Currently, notes exist temporarily in memory. Future versions aim to support saving notes using file or database-backed storage. Here’s a snippet using basic file storage in Java:

```java
try (BufferedWriter writer = new BufferedWriter(new FileWriter("notes.txt", true))) {
    writer.write(note + "\n");
} catch (IOException e) {
    e.printStackTrace();
}
```

While functional, this approach will be upgraded to Room — a modern SQLite wrapper for Android — to ensure data persistence, schema migrations, and query flexibility.

### Planned Features

- 🗃️ File/database-backed storage using Room DB with LiveData and ViewModel integration
- 🏷️ Note tagging, filtering, and text-based search using queryable schema
- 📤 Export/share notes to other apps via Android Intent system
- 💻 JavaFX desktop version (exploratory, to reuse core logic via shared module)
- 🎨 Enhanced dark/light themes using `AppCompatDelegate` and `themes.xml`
- ☁️ Cloud sync via Firebase Realtime Database or Google Drive API

---

## Conclusion

**My Notes** is a clean, minimal Android project with significant educational value. It walks through the Android project lifecycle, UI binding using XML, logic separation in Java, and sets the stage for future enhancements using best practices like MVVM, Room, and Jetpack components.

The project is simple enough to be grasped by beginners, yet structured enough to allow modular upgrades as your understanding of Android grows. As it evolves, it will serve both as a reference and a base for more complex app-building adventures.

📱 **Download APK** — _Coming Soon!_

---

## Summary

If you're starting out with Android and want a solid, clean example that doesn't overwhelm you with libraries or architecture, **My Notes** is a great place to begin.

You’ll get to:

- Understand how Java integrates with Android Views and Activities
- Learn how to structure your first app properly
- Get hands-on with the basics of user interaction and UI logic
- And most importantly — _build something real that works!_

This app is designed to be more than a toy project — it's a gateway into the real Android ecosystem.

