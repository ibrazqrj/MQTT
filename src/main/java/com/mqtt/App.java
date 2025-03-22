package com.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import com.sun.net.httpserver.HttpServer;

public class App {
    private static double latestValue = 0.0; // Letzter gesendeter Wert für Prometheus

    public static void main(String[] args) {
        String server = System.getenv("MQTT_SERVER");
        final String broker = "tcp://" + server + ":1883";
        final String clientId = UUID.randomUUID().toString();

        String pubTopic = args.length > 0 ? args[0] : System.getenv("MQTT_PUB_TOPIC");
        String subTopic = args.length > 1 ? args[1] : System.getenv("MQTT_SUB_TOPIC");

        if (pubTopic == null) pubTopic = "sensoren/java1";
        if (subTopic == null) subTopic = "feedback/java1";

        System.out.println("Senden auf: " + pubTopic);
        System.out.println("Abonniert auf: " + subTopic);

        // Starte HTTP-Server für Prometheus
        startHttpServer();

        try {
            MqttClient client = new MqttClient(broker, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);

            client.subscribe(subTopic, (topic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("Feedback erhalten: " + payload);
                if ("stop".equalsIgnoreCase(payload.trim())) {
                    System.out.println("Stop-Befehl empfangen. Beende das Programm.");
                    client.disconnect();
                    System.exit(0);
                }
            });

            double counter = Math.PI / 2;
            while (true) {
                double sinValue = Math.sin(counter) * 10;
                latestValue = sinValue; // Speichert den aktuellen Wert für Prometheus
                String payload = String.format("%.2f", sinValue);
                MqttMessage msg = new MqttMessage(payload.getBytes());
                msg.setQos(0);
                msg.setRetained(false);

                client.publish(pubTopic, msg);
                System.out.println("Gesendet an [" + pubTopic + "]: " + payload);

                counter += 0.1;
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/metrics", exchange -> {
                String response = "sensor_value " + latestValue + "\n";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            });
            server.setExecutor(null);
            server.start();
            System.out.println("Prometheus HTTP-Server läuft auf Port 8000...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
