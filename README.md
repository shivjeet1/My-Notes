## Description

**My Notes** is a beginner-friendly Android application that serves as a simple and elegant note-taking solution. Developed in Java with the native Android SDK, this app demonstrates core Android development concepts including UI handling, memory management, and interaction through Java-based event-driven logic.

The aim is to keep the application lightweight and extensible, suitable both as a personal note manager and as a sandbox project for developers learning Android.

---
## Key Features and Functionality

### Core Features

- ✍️ **Creating Notes**: Users can input and save notes directly via the interface using an `EditText` field and a `Button` tied to a custom event handler using `setOnClickListener()`.
- 📄 **Viewing Notes**: Saved notes are dynamically rendered as `TextView` widgets added programmatically to a vertically scrollable container (`LinearLayout`).

The application currently uses an `ArrayList<String>` to temporarily store note entries in memory during the session. This makes the app lightweight and avoids complex boilerplate in its early stage, while still demonstrating practical coding practices.

Behind the scenes, the `NoteManager` class encapsulates the logic for adding, retrieving, and managing notes. This abstraction helps maintain separation of concerns and prepares the codebase for modular growth (such as swapping in a database later).

---
## UI Design

The current UI uses basic Android Views like `EditText`, `Button`, and `TextView`. It is clean, functional, and easy to navigate. All event handlers are wired via `setOnClickListener()` in Java code. Below are four screenshots from the actual app:

<div style="display: flex; flex-wrap: wrap; gap: 16px;">
  <img src="/static/mynotes/ui-demo1.jpg" width="45%" />
  <img src="/static/mynotes/ui-demo2.jpg" width="45%" />
  <img src="/static/mynotes/ui-demo3.jpg" width="45%" />
  <img src="/static/mynotes/ui-demo4.jpg" width="45%" />
</div>

> All UI elements are designed using XML with a focus on simplicity and readability. The layout is defined in `activity_main.xml`, using a vertical `LinearLayout` with scroll support.

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
## Conclusion

**My Notes** is a clean, minimal Android project with significant educational value. It walks through the Android project lifecycle, UI binding using XML, logic separation in Java, and sets the stage for future enhancements using best practices like MVVM, Room, and Jetpack components.

The project is simple enough to be grasped by beginners, yet structured enough to allow modular upgrades as your understanding of Android grows. As it evolves, it will serve both as a reference and a base for more complex app-building adventures.

[``📱Download APK``](https://github.com/shivjeet1/My-Notes/releases)

---

