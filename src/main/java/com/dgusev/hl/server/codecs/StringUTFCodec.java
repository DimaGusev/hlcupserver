package com.dgusev.hl.server.codecs;

import com.dgusev.hl.server.stat.Statistics;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by dgusev on 21.08.2017.
 */
public class StringUTFCodec {

    public static int decode(InputStream inputStream, char[] out, byte[] buf) throws IOException {
        int count = 0;
        int size = inputStream.read(buf);
        for (int i = 0; i < size; i++) {
            if ((buf[i] & 0xff) < 0x7f) {
                out[count] = (char) buf[i];
                count++;
            } else {
                byte f = buf[i];
                byte s = buf[i + 1];
                char res = 0;
                res |= ((f << 3) & 0xff) << 3;
                res |= (s & 0x3F);
                out[count] = res;
                i++;
                count++;
            }
        }
        return count;
    }

    public static int decode(ByteBuf buffer,byte[] buf, char[] out) throws IOException {
        int count = 0;
        int size = buffer.writerIndex();
        buffer.readBytes(buf, 0, size);
        for (int i = 0; i < size; i++) {
            byte b = buf[i];
            if ((b & 0xff) < 0x7f) {
                out[count] = (char) b;
                count++;
            } else {
                byte f = b;
                byte s = buf[i+1];
                char res = 0;
                res |= ((f << 3) & 0xff) << 3;
                res |= (s & 0x3F);
                out[count] = res;
                i++;
                count++;
            }
        }
        return count;
    }

    public static int encode(char[] buf, int size, byte[] out, int index) {
        int count = 0;
        for (int i = 0; i<size;i++) {
            char ch = buf[i];
            if (ch < 0x7f) {
                out[index + count] = (byte)ch;
                count++;
            } else {
                byte f = (byte)((ch >> 6) | 0xC0);
                byte s = (byte)((ch & 0x3F) | 0x80);
                out[index + count] = f;
                out[index + count + 1] = s;
                count+=2;
            }
        }
        return count;
    }


    public static int encode(char[] buf, int size, ByteBuf out) {
        int count = 0;
        for (int i = 0; i<size;i++) {
            char ch = buf[i];
            if (ch < 0x7f) {
                out.writeByte((byte)ch);
                count++;
            } else {
                byte f = (byte)((ch >> 6) | 0xC0);
                byte s = (byte)((ch & 0x3F) | 0x80);
                out.writeByte(f);
                out.writeByte(s);
                count+=2;
            }
        }
        return count;
    }


}
