package uk.gov.pay.api.it.support;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A minimal in-process Redis (RESP2) server for the integration tests, replacing the Redis Docker container the
 * Dropwizard tests used. It understands the commands the service's {@code RedisRateLimiter} sends ({@code INCR},
 * {@code EXPIRE}) plus the connection handshake of the Lettuce client; {@code HELLO} is refused so that Lettuce falls
 * back to RESP2; any other command is refused, as a real server refuses unknown commands. {@code EXPIRE} sets a time
 * to live after which a key is gone, as in Redis; {@link #clearCache()} empties the store, like the old rule did.
 * <p>
 * The server starts when the extension is created, so its URL can be used by static application set-up. An instance
 * may be shared by several test classes (and the application they start), so the server stops when the whole test run
 * ends: its socket is registered in the root extension store, which JUnit closes then.
 */
public class InMemoryRedisExtension implements BeforeAllCallback, AfterAllCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryRedisExtension.class);

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final ServerSocket serverSocket;

    public InMemoryRedisExtension() {
        try {
            serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Thread acceptor = new Thread(this::acceptConnections, "in-memory-redis-" + serverSocket.getLocalPort());
        acceptor.setDaemon(true);
        acceptor.start();
    }

    public String getRedisUrl() {
        return "localhost:" + serverSocket.getLocalPort();
    }

    public void clearCache() {
        store.clear();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        clearCache();
        context.getRoot().getStore(ExtensionContext.Namespace.create(InMemoryRedisExtension.class))
                .put(this, (AutoCloseable) serverSocket::close);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        clearCache();
    }

    private void acceptConnections() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                Thread handler = new Thread(() -> serve(socket), "in-memory-redis-connection");
                handler.setDaemon(true);
                handler.start();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    LOGGER.warn("In-memory Redis stopped accepting connections", e);
                }
                return;
            }
        }
    }

    private void serve(Socket socket) {
        try (socket; InputStream in = new BufferedInputStream(socket.getInputStream());
             OutputStream out = socket.getOutputStream()) {
            List<String> command;
            while ((command = readCommand(in)) != null) {
                out.write(execute(command).getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (IOException e) {
            LOGGER.debug("In-memory Redis connection ended", e);
        }
    }

    private String execute(List<String> command) {
        if (command.isEmpty()) {
            return "-ERR empty command\r\n";
        }
        String name = command.getFirst().toUpperCase(Locale.ROOT);
        return switch (name) {
            case "PING" -> "+PONG\r\n";
            case "CLIENT", "SELECT", "AUTH", "QUIT" -> "+OK\r\n";
            case "INCR" -> ":" + store.compute(command.get(1), (key, entry) -> live(entry) ? entry : new Entry())
                    .value.incrementAndGet() + "\r\n";
            case "EXPIRE" -> {
                Entry entry = store.computeIfPresent(command.get(1), (key, existing) -> live(existing) ? existing : null);
                if (entry == null) {
                    yield ":0\r\n";
                }
                entry.expiresAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(Long.parseLong(command.get(2)));
                yield ":1\r\n";
            }
            case "GET" -> {
                Entry entry = store.computeIfPresent(command.get(1), (key, existing) -> live(existing) ? existing : null);
                if (entry == null) {
                    yield "$-1\r\n";
                }
                String text = String.valueOf(entry.value.get());
                yield "$" + text.length() + "\r\n" + text + "\r\n";
            }
            case "FLUSHALL", "FLUSHDB" -> {
                store.clear();
                yield "+OK\r\n";
            }
            default -> "-ERR unknown command '" + command.getFirst() + "'\r\n";
        };
    }

    private static boolean live(Entry entry) {
        return entry != null && (entry.expiresAt == 0 || System.nanoTime() - entry.expiresAt < 0);
    }

    /**
     * A counter with its expiry time (0: none), as the rate limiter uses keys.
     */
    private static final class Entry {
        private final AtomicLong value = new AtomicLong();
        private volatile long expiresAt;
    }

    private static List<String> readCommand(InputStream in) throws IOException {
        String line = readLine(in);
        if (line == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (!line.startsWith("*")) {
            // inline command
            for (String part : line.trim().split("\\s+")) {
                if (!part.isEmpty()) {
                    parts.add(part);
                }
            }
            return parts;
        }
        int count = Integer.parseInt(line.substring(1));
        for (int i = 0; i < count; i++) {
            String header = readLine(in);
            if (header == null || !header.startsWith("$")) {
                throw new IOException("Unexpected RESP element " + header);
            }
            int length = Integer.parseInt(header.substring(1));
            byte[] bytes = in.readNBytes(length);
            in.readNBytes(2);
            parts.add(new String(bytes, StandardCharsets.UTF_8));
        }
        return parts;
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();
        int previous = -1;
        int current;
        while ((current = in.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                builder.setLength(builder.length() - 1);
                return builder.toString();
            }
            builder.append((char) current);
            previous = current;
        }
        return builder.isEmpty() ? null : builder.toString();
    }
}
