# 🃏 Poker Game (Java + Swing + TCP Socket)

A simple **Poker Game** built with **Java** and **Swing GUI** for the client-side interface. The project demonstrates the fundamental principles of **TCP socket communication**, **multi-threading**, and **client-server architecture**.

This project is designed for educational purposes, showcasing how networked applications can be developed using Java's core libraries.

---

## 🏗️ Features

- ✅ Multiplayer **Poker** game simulation (basic flow)
- ✅ **Client-Server** model using TCP sockets
- ✅ **GUI interface** using Java Swing
- ✅ Multi-threaded server with **ExecutorService** (Thread Pool)
- ✅ Real-time communication: Server broadcasts updates to clients
- ✅ Dynamic game state updates on the client side
- ✅ Graceful connection handling and threading lifecycle

---

## ⚙️ Core Technologies

- Java SE (JDK 17+)
- Swing (GUI for client)
- TCP Sockets (`Socket`, `ServerSocket`)
- Threading (`Thread`, `ExecutorService`, `ThreadPool`)
- Gson (for JSON serialization/deserialization of game data)
- OOP Design Principles

---

## 🧩 Architecture Overview

### 🖥️ Server

- Listens for incoming client connections on a specific port.
- For each client, accepts the connection (`Socket`) and creates a dedicated thread (via `ExecutorService`) to handle communication.
- Manages the **game state** (e.g., current player, cards, actions) and **broadcasts updates** to all connected clients.

### 👥 Client

- Connects to the server using `Socket` on the specified host/port.
- Has a dedicated thread (`listeningThread`) that continuously listens for messages from the server without blocking the GUI.
- Updates the game state and refreshes the Swing UI when receiving server messages.
- Sends player actions (e.g., "Call", "Fold") to the server.

---

## 🧵 Threading Model

### Server-side

- Uses `ExecutorService` (`newCachedThreadPool()`) for handling multiple clients.
- Each client has its own dedicated thread for communication (`ClientHandler`).

### Client-side

- Uses a single thread (`listeningThread`) to receive messages from the server while keeping the Swing GUI responsive.
- GUI event listeners (e.g., buttons) send messages to the server.

---
