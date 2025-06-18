# 🔍 RAT/Remote System Monitor

![Java](https://img.shields.io/badge/Java-8%2B-orange?logo=openjdk)
![License](https://img.shields.io/badge/License-MIT-green)
![Maven](https://img.shields.io/badge/Apache%20Maven-3.8%2B-blue)

Remote monitoring tool with encrypted communication via Discord.

## 🚀 Quick Setup

1. Clone and build:
   ```bash
   git clone https://github.com/Pepeins/Discord-rat.git && cd Discord-rat
   mvn clean package
   ```

2. Configure:
   - Create Discord bot at [Developer Portal](https://discord.com/developers/applications)
   - Update `TOKEN` and `CHANNEL_ID` in `rat.java`

3. Run:
   ```bash
   java -jar target/rat-obfuscated.jar
   ```

## 📋 Commands

| Command              | Function                        |
|----------------------|--------------------------------|
| `!info`             | System information              |
| `!cmd <command>`    | Execute system commands         |
| `!screenshot`       | Capture screenshot              |
| `!record <seconds>` | Record audio (1-30 seconds)    |
| `!download <path>`  | Download files (max 8MB)       |
| `!processes`        | List active processes           |
| `!ip`               | Get public IP address           |
| `!exit`             | Terminate program               |

## 🛠️ Requirements

- Java 8+
- Maven 3.8+
- Discord Bot Token & Channel ID

## ⚠️ Legal Notice

**FOR EDUCATIONAL AND AUTHORIZED USE ONLY.**

Prohibited for unauthorized access or malicious activities. Users are responsible for compliance with applicable laws.

## 📄 License

MIT License - Use responsibly and ethically.
