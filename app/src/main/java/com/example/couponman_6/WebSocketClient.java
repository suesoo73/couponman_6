package com.example.couponman_6;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Random;

public class WebSocketClient {
    private static final String TAG = "WebSocketClient";
    
    public interface WebSocketListener {
        void onConnected();
        void onMessage(String message);
        void onDisconnected();
        void onError(String error);
    }
    
    private Socket socket;
    private BufferedReader reader;
    private OutputStream writer;
    private Thread readThread;
    private boolean isConnected = false;
    private WebSocketListener listener;
    private Handler mainHandler;
    private String uri;
    
    public WebSocketClient() {
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void setListener(WebSocketListener listener) {
        this.listener = listener;
    }
    
    public void connect(String uri) {
        this.uri = uri;
        Log.d(TAG, "Starting connection to: " + uri);
        
        new Thread(() -> {
            try {
                URI parsedUri = new URI(uri);
                String host = parsedUri.getHost();
                int port = parsedUri.getPort();
                if (port == -1) {
                    port = parsedUri.getScheme().equals("wss") ? 443 : 80;
                }
                String path = parsedUri.getPath();
                if (path == null || path.isEmpty()) {
                    path = "/";
                }
                String query = parsedUri.getQuery();
                if (query != null && !query.isEmpty()) {
                    path += "?" + query;
                }
                
                Log.d(TAG, "Connecting to host: " + host + ", port: " + port + ", path: " + path);
                
                // Connect to server
                Log.d(TAG, "Creating socket connection...");
                socket = new Socket(host, port);
                socket.setSoTimeout(0); // No timeout
                socket.setKeepAlive(true);
                
                Log.d(TAG, "Socket connected: " + socket.isConnected());
                
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = socket.getOutputStream();
                
                // Send WebSocket handshake
                String key = generateWebSocketKey();
                String handshake = "GET " + path + " HTTP/1.1\r\n" +
                        "Host: " + host + ":" + port + "\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Key: " + key + "\r\n" +
                        "Sec-WebSocket-Version: 13\r\n" +
                        "Origin: http://" + host + "\r\n" +
                        "\r\n";
                
                Log.d(TAG, "Sending handshake:\n" + handshake);
                writer.write(handshake.getBytes());
                writer.flush();
                
                // Read handshake response
                Log.d(TAG, "Reading handshake response...");
                String line;
                boolean validHandshake = false;
                StringBuilder responseHeaders = new StringBuilder();
                
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    responseHeaders.append(line).append("\n");
                    Log.d(TAG, "Response header: " + line);
                    
                    if (line.contains("HTTP/1.1 101")) {
                        validHandshake = true;
                        Log.d(TAG, "Received 101 Switching Protocols");
                    }
                }
                
                Log.d(TAG, "Full response headers:\n" + responseHeaders.toString());
                
                if (validHandshake) {
                    isConnected = true;
                    Log.d(TAG, "WebSocket handshake successful, connection established");
                    notifyConnected();
                    
                    // Send initial message after connection
                    sendInitialMessage();
                    
                    startReading();
                } else {
                    Log.e(TAG, "Invalid handshake response - did not receive 101 status");
                    throw new IOException("Invalid handshake response");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Connection error: " + e.getMessage(), e);
                e.printStackTrace();
                notifyError("연결 실패: " + e.getMessage());
                disconnect();
            }
        }).start();
    }
    
    private String generateWebSocketKey() {
        byte[] key = new byte[16];
        new Random().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    private String getCloseCodeDescription(int code) {
        switch (code) {
            case 1000: return "Normal Closure";
            case 1001: return "Going Away";
            case 1002: return "Protocol Error";
            case 1003: return "Unsupported Data";
            case 1007: return "Invalid frame payload data";
            case 1008: return "Policy Violation";
            case 1009: return "Message Too Big";
            case 1010: return "Mandatory Extension";
            case 1011: return "Internal Server Error";
            default: return "Unknown";
        }
    }
    
    private void sendInitialMessage() {
        // Send JSON message similar to the JavaScript example
        String jsonMessage = "{\n" +
                "    \"type\": \"message\",\n" +
                "    \"content\": \"Android 클라이언트에서 서버로 보내는 메시지\"\n" +
                "}";
        Log.d(TAG, "Sending initial message: " + jsonMessage);
        sendMessage(jsonMessage);
    }
    
    private void startReading() {
        Log.d(TAG, "Starting read thread...");
        readThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (isConnected && socket != null && !socket.isClosed()) {
                    int bytesRead = socket.getInputStream().read(buffer);
                    Log.d(TAG, "Read " + bytesRead + " bytes from socket");
                    
                    if (bytesRead > 0) {
                        // Log raw bytes for debugging
                        StringBuilder hexDump = new StringBuilder();
                        for (int i = 0; i < Math.min(bytesRead, 50); i++) {
                            hexDump.append(String.format("%02X ", buffer[i]));
                        }
                        Log.d(TAG, "Raw frame bytes: " + hexDump.toString());
                        // Simple frame parsing (for text frames only)
                        int opcode = buffer[0] & 0x0F;
                        boolean fin = (buffer[0] & 0x80) != 0;
                        
                        Log.d(TAG, "Frame - FIN: " + fin + ", Opcode: " + opcode);
                        
                        if (opcode == 0x08) { // Close frame
                            int closePayloadLength = buffer[1] & 0x7F;
                            int closeCode = -1;
                            String closeReason = "";
                            
                            if (closePayloadLength >= 2) {
                                closeCode = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
                                if (closePayloadLength > 2) {
                                    closeReason = new String(buffer, 4, closePayloadLength - 2);
                                }
                            }
                            
                            Log.d(TAG, "Received close frame from server - Code: " + closeCode + 
                                      " (" + getCloseCodeDescription(closeCode) + ")" +
                                      (closeReason.isEmpty() ? "" : ", Reason: " + closeReason));
                            break;
                        } else if (opcode == 0x09) { // Ping frame
                            int pingPayloadLength = buffer[1] & 0x7F;
                            int pingOffset = 2;
                            boolean pingMasked = (buffer[1] & 0x80) != 0;
                            
                            if (pingPayloadLength == 126) {
                                pingPayloadLength = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
                                pingOffset = 4;
                            }
                            
                            byte[] maskKey = null;
                            if (pingMasked) {
                                maskKey = new byte[4];
                                System.arraycopy(buffer, pingOffset, maskKey, 0, 4);
                                pingOffset += 4;
                            }
                            
                            byte[] pingPayload = new byte[pingPayloadLength];
                            System.arraycopy(buffer, pingOffset, pingPayload, 0, pingPayloadLength);
                            
                            // Unmask payload if needed
                            if (pingMasked && maskKey != null) {
                                for (int i = 0; i < pingPayloadLength; i++) {
                                    pingPayload[i] ^= maskKey[i % 4];
                                }
                            }
                            
                            Log.d(TAG, "Received ping frame with " + pingPayloadLength + " bytes payload, sending pong");
                            sendPong(pingPayload, pingPayloadLength);
                        } else if (opcode == 0x0A) { // Pong frame
                            Log.d(TAG, "Received pong frame");
                        } else if (opcode == 0x01) { // Text frame
                            int payloadLength = buffer[1] & 0x7F;
                            int offset = 2;
                            
                            Log.d(TAG, "Text frame - Payload length: " + payloadLength);
                            
                            if (payloadLength == 126) {
                                payloadLength = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
                                offset = 4;
                            } else if (payloadLength == 127) {
                                // Skip large frames for simplicity
                                Log.w(TAG, "Skipping large frame (length > 65535)");
                                continue;
                            }
                            
                            if ((buffer[1] & 0x80) != 0) { // Masked
                                Log.d(TAG, "Frame is masked (unusual for server->client)");
                                offset += 4; // Skip mask key
                            }
                            
                            String message = new String(buffer, offset, Math.min(payloadLength, bytesRead - offset));
                            Log.d(TAG, "Received text message: " + message);
                            notifyMessage(message);
                        } else {
                            Log.w(TAG, "Unknown opcode: " + opcode);
                        }
                    } else if (bytesRead == -1) {
                        Log.d(TAG, "Socket closed by server (EOF)");
                        break;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Read error: " + e.getMessage(), e);
                e.printStackTrace();
            }
            
            Log.d(TAG, "Read thread ending, disconnecting...");
            disconnect();
        });
        readThread.start();
    }
    
    private void sendPong(byte[] pingPayload, int payloadLength) {
        if (!isConnected || socket == null || socket.isClosed()) {
            return;
        }
        
        try {
            java.io.ByteArrayOutputStream frame = new java.io.ByteArrayOutputStream();
            frame.write(0x8A); // FIN = 1, opcode = 10 (pong)
            
            // Client must mask the frame
            if (payloadLength <= 125) {
                frame.write(payloadLength | 0x80); // Masked
            } else {
                Log.w(TAG, "Ping payload too large for pong response");
                return;
            }
            
            // Add mask key
            byte[] mask = new byte[4];
            new Random().nextBytes(mask);
            frame.write(mask);
            
            // Add masked payload (echo the ping payload)
            for (int i = 0; i < payloadLength; i++) {
                frame.write(pingPayload[i] ^ mask[i % 4]);
            }
            
            writer.write(frame.toByteArray());
            writer.flush();
            Log.d(TAG, "Sent pong frame with " + payloadLength + " bytes payload");
        } catch (IOException e) {
            Log.e(TAG, "Failed to send pong: " + e.getMessage());
        }
    }
    
    public void sendMessage(String message) {
        if (!isConnected || socket == null || socket.isClosed()) {
            Log.w(TAG, "Cannot send message - not connected");
            return;
        }
        
        new Thread(() -> {
            try {
                byte[] messageBytes = message.getBytes("UTF-8");
                int length = messageBytes.length;
                
                Log.d(TAG, "Sending message (" + length + " bytes): " + message);
                
                // Create WebSocket frame
                java.io.ByteArrayOutputStream frame = new java.io.ByteArrayOutputStream();
                frame.write(0x81); // FIN = 1, opcode = 1 (text)
                
                if (length <= 125) {
                    frame.write(length | 0x80); // Masked
                } else if (length <= 65535) {
                    frame.write(126 | 0x80); // Masked
                    frame.write((length >> 8) & 0xFF);
                    frame.write(length & 0xFF);
                } else {
                    // Skip large messages for simplicity
                    return;
                }
                
                // Add mask key
                byte[] mask = new byte[4];
                new Random().nextBytes(mask);
                frame.write(mask);
                
                // Add masked payload
                for (int i = 0; i < messageBytes.length; i++) {
                    frame.write(messageBytes[i] ^ mask[i % 4]);
                }
                
                byte[] frameBytes = frame.toByteArray();
                writer.write(frameBytes);
                writer.flush();
                
                // Log sent frame for debugging
                StringBuilder hexDump = new StringBuilder();
                for (int i = 0; i < Math.min(frameBytes.length, 50); i++) {
                    hexDump.append(String.format("%02X ", frameBytes[i]));
                }
                Log.d(TAG, "Sent frame bytes: " + hexDump.toString());
                
            } catch (Exception e) {
                Log.e(TAG, "Send error: " + e.getMessage(), e);
                e.printStackTrace();
            }
        }).start();
    }
    
    public void disconnect() {
        Log.d(TAG, "Disconnect called, current state - isConnected: " + isConnected);
        
        isConnected = false;
        
        try {
            if (readThread != null) {
                Log.d(TAG, "Interrupting read thread");
                readThread.interrupt();
            }
            
            if (writer != null && socket != null && !socket.isClosed()) {
                try {
                    // Send close frame
                    Log.d(TAG, "Sending close frame");
                    writer.write(new byte[]{(byte)0x88, 0x00});
                    writer.flush();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send close frame: " + e.getMessage());
                }
            }
            
            if (socket != null && !socket.isClosed()) {
                Log.d(TAG, "Closing socket");
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Disconnect error: " + e.getMessage(), e);
        }
        
        socket = null;
        reader = null;
        writer = null;
        readThread = null;
        
        Log.d(TAG, "Disconnect complete, notifying listeners");
        notifyDisconnected();
    }
    
    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }
    
    private void notifyConnected() {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onConnected();
            }
        });
    }
    
    private void notifyMessage(String message) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onMessage(message);
            }
        });
    }
    
    private void notifyDisconnected() {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onDisconnected();
            }
        });
    }
    
    private void notifyError(String error) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onError(error);
            }
        });
    }
    
}