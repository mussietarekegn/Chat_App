# 💬 Local Java Swing Chat Application

A lightweight, multi-client desktop chat room built using **Java Swing** (GUI), **Java Sockets** (Networking), and a persistent **MySQL Database** backend. Supports real-time text broadcasting, customized emojis, and interactive base64 image attachments.

---

## ✨ Key Features

* 🔄 **Real-Time Synchronized Chat:** Instantly sends text across all active clients.
* 📷 **Image Sharing:** Attach images via a built-in file chooser, broadcast them instantly, and view them full-size via an **Image Viewer Zoomer**.
* 😀 **Built-in Emoji Picker:** Easily drop clean emojis right into your conversations.
* 🗄️ **Persistent Chat History:** Seamlessly automatically reloads all old text messages and images onto **both** client and server panels upon system restart.
* 🔒 **Secure Local Configurations:** Database credentials are managed completely outside the codebase using an ignored properties file to ensure zero security leaks on GitHub.

---

## 🛠️ Tech Stack & Requirements

* **Frontend UI:** Java Swing (`JFrame`, `JTextPane`, `StyledDocument`)
* **Networking:** Standard Java Sockets (`ServerSocket`, `Socket`)
* **Database Backend:** MySQL 8.x
* **Driver Needed:** MySQL Connector/J JAR Library

---

