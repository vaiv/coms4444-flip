package flip.sim;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.nio.channels.*;

class HTTPServer {

    private ServerSocket socket = null;

    private Socket connection = null;

    private byte[] buffer = new byte[8192];

    public HTTPServer() throws IOException {
        socket = new ServerSocket();
        socket.bind(null);
        if (!socket.isBound())
            throw new NotYetBoundException();
    }

    public int port() {
        return socket == null ? -1 : socket.getLocalPort();
    }
    
    public String addr() {
    	return socket == null ? "NA" : socket.getLocalSocketAddress().toString();
    }

    public void close() throws IOException {
        if (connection != null)
            throw new ConnectionPendingException();
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    public String request() throws IOException {
        if (connection != null)
            throw new ConnectionPendingException();
        connection = socket.accept();
        InputStream in = connection.getInputStream();
        int ch, i = 0;
        do {
            try {
                ch = in.read();
            } catch (IOException e1) {
                try {
                    connection.close();
                } catch (IOException e2) {
                }
                connection = null;
                throw e1;
            }
            if (ch < 0 || i == buffer.length) {
                try {
                    connection.close();
                } catch (IOException e) {
                }
                connection = null;
                throw new UnknownServiceException("Incomplete HTTP request");
            }
            buffer[i++] = (byte) ch;
        } while (i < 4 || buffer[i - 1] != '\n' || buffer[i - 2] != '\r'
                || buffer[i - 3] != '\n' || buffer[i - 4] != '\r');
        for (i = 0; buffer[i] != '\r'; ++i) ;
        String line = new String(buffer, 0, i);
        String[] parts = line.split(" ");
        if (parts.length != 3 || !parts[0].equals("GET")
                || !parts[1].startsWith("/")
                || !parts[2].equals("HTTP/1.1")) {
            try {
                connection.close();
            } catch (IOException e) {
            }
            connection = null;
            throw new UnknownServiceException("Invalid HTTP request: " + line);
        }
        return parts[1].substring(1);
    }

    public void reply(File file) throws IOException {
        if (connection == null)
            throw new NoConnectionPendingException();
        FileInputStream in = new FileInputStream(file);
        OutputStream out = connection.getOutputStream();
        String date = ZonedDateTime.now(ZoneId.of("GMT")).format(
                DateTimeFormatter.RFC_1123_DATE_TIME);
        String header = "HTTP/1.1 200 OK\r\n";
        long length = file.length();
        header += "Content-Length: " + length + "\r\n";
        header += "Cache-Control: no-cache, no-store\r\n";
        header += "Date: " + date + "\r\n\r\n";
        try {
            out.write(header.getBytes());
            int bytes;
            while ((bytes = in.read(buffer)) >= 0) {
                length -= bytes;
                if (length < 0) break;
                out.write(buffer, 0, bytes);
            }
            if (length != 0)
                throw new IOException("File modified during send");
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
            try {
                connection.close();
            } catch (IOException e) {
            }
            connection = null;
        }
    }

    public void reply(String content) throws IOException {
        if (connection == null)
            throw new NoConnectionPendingException();
        OutputStream out = connection.getOutputStream();
        String date = ZonedDateTime.now(ZoneId.of("GMT")).format(
                DateTimeFormatter.RFC_1123_DATE_TIME);
        String header = "HTTP/1.1 200 OK\r\n";
        header += "Content-Length: " + content.length() + "\r\n";
        header += "Cache-Control: no-cache, no-store\r\n";
        header += "Date: " + date + "\r\n\r\n";
        try {
            out.write(header.getBytes());
            out.write(content.getBytes());
        } finally {
            try {
                connection.close();
            } catch (IOException e) {
            }
            connection = null;
        }
    }
}