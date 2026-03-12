package de.studio3.huaweimonitor;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ModbusTcpClient {
    private static final AtomicInteger NEXT_TRANSACTION_ID = new AtomicInteger(1);

    public int[] readHoldingRegisters(String host, int port, int unitId, int startAddress, int quantity, int timeoutMs)
            throws IOException {
        if (quantity <= 0 || quantity > 125) {
            throw new IllegalArgumentException("Ungueltige Registeranzahl: " + quantity);
        }

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);
        socket.setSoTimeout(timeoutMs);

        try {
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            int transactionId = NEXT_TRANSACTION_ID.getAndIncrement() & 0xFFFF;

            byte[] request = new byte[12];
            request[0] = (byte) ((transactionId >> 8) & 0xFF);
            request[1] = (byte) (transactionId & 0xFF);
            request[2] = 0;
            request[3] = 0;
            request[4] = 0;
            request[5] = 6;
            request[6] = (byte) (unitId & 0xFF);
            request[7] = 3;
            request[8] = (byte) ((startAddress >> 8) & 0xFF);
            request[9] = (byte) (startAddress & 0xFF);
            request[10] = (byte) ((quantity >> 8) & 0xFF);
            request[11] = (byte) (quantity & 0xFF);

            outputStream.write(request);
            outputStream.flush();

            byte[] header = new byte[7];
            readFully(inputStream, header);
            int responseTransactionId = ((header[0] & 0xFF) << 8) | (header[1] & 0xFF);
            int protocolId = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
            int remainingLength = ((header[4] & 0xFF) << 8) | (header[5] & 0xFF);
            int responseUnitId = header[6] & 0xFF;

            if (responseTransactionId != transactionId) {
                throw new IOException("Unerwartete Transaktions-ID: " + responseTransactionId);
            }
            if (protocolId != 0) {
                throw new IOException("Ungueltige Protokoll-ID: " + protocolId);
            }
            if (responseUnitId != (unitId & 0xFF)) {
                throw new IOException("Unerwartete Unit-ID: " + responseUnitId);
            }
            if (remainingLength < 3) {
                throw new IOException("Ungueltige Modbus-Antwortlaenge: " + remainingLength);
            }

            byte[] pdu = new byte[remainingLength - 1];
            readFully(inputStream, pdu);

            int functionCode = pdu[0] & 0xFF;
            if ((functionCode & 0x80) != 0) {
                int exceptionCode = pdu.length > 1 ? (pdu[1] & 0xFF) : -1;
                throw new IOException("Modbus-Exception " + exceptionCode);
            }
            if (functionCode != 3) {
                throw new IOException("Unerwarteter Function Code: " + functionCode);
            }

            int byteCount = pdu[1] & 0xFF;
            if (byteCount != quantity * 2) {
                throw new IOException("Unerwartete Byte-Anzahl: " + byteCount);
            }

            int[] registers = new int[quantity];
            for (int i = 0; i < quantity; i++) {
                int index = 2 + (i * 2);
                registers[i] = ((pdu[index] & 0xFF) << 8) | (pdu[index + 1] & 0xFF);
            }
            return registers;
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public int readSignedInt32(String host, int port, int unitId, int startAddress, int timeoutMs) throws IOException {
        int[] registers = readHoldingRegisters(host, port, unitId, startAddress, 2, timeoutMs);
        return toSignedInt32(registers, 0);
    }

    public static int toSignedInt32(int[] registers, int offset) {
        long value = ((long) (registers[offset] & 0xFFFF) << 16) | (long) (registers[offset + 1] & 0xFFFF);
        if ((value & 0x80000000L) != 0) {
            value -= 0x1_0000_0000L;
        }
        return (int) value;
    }

    public static long toSignedInt64(int[] registers, int offset) {
        long highHigh = (long) (registers[offset] & 0xFFFF);
        long highLow = (long) (registers[offset + 1] & 0xFFFF);
        long lowHigh = (long) (registers[offset + 2] & 0xFFFF);
        long lowLow = (long) (registers[offset + 3] & 0xFFFF);
        return (highHigh << 48) | (highLow << 32) | (lowHigh << 16) | lowLow;
    }

    public static long toUnsignedInt32(int[] registers, int offset) {
        return ((long) (registers[offset] & 0xFFFF) << 16) | (long) (registers[offset + 1] & 0xFFFF);
    }

    public static int toSignedInt16(int[] registers, int offset) {
        int value = registers[offset] & 0xFFFF;
        if ((value & 0x8000) != 0) {
            value -= 0x10000;
        }
        return value;
    }

    private static void readFully(InputStream inputStream, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int count = inputStream.read(buffer, offset, buffer.length - offset);
            if (count < 0) {
                throw new EOFException("Antwort unerwartet beendet");
            }
            offset += count;
        }
    }
}
