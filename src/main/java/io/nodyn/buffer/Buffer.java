/*
 * Copyright 2014-15 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nodyn.buffer;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import java.nio.ByteBuffer;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author Bob McWhirter
 * @author Lance Ball
 */
public class Buffer {

    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final Charset ASCII = StandardCharsets.US_ASCII;
    private static final Charset UCS2 = StandardCharsets.UTF_16LE;
    private static final Charset BINARY = StandardCharsets.ISO_8859_1;

    public static void inject(V8Object object, ByteBuffer buf) {
        if ( object.contains("__rawBuffer__")) {
            throw new RuntimeException( "already has external data" );
        }
		
		V8 v8 = object.getRuntime();
		
		V8Object bufferObj = new V8Object(v8);
		
		bufferObj.add("__javaClass", ByteBuffer.class.getName());
		bufferObj.add("__javaInstance", io.js.J2V8Classes.Utils.registerInstance(buf));
		
		v8.add("__tmp", bufferObj);
		v8.executeScript("ClassHelpers.addJavaFieldsToObj(__tmp)");
		v8.addUndefined("__tmp");

        object.add("__rawBuffer__", bufferObj);
//        object.setIndexedPropertiesToExternalArrayData(buf);
    }

    public static ByteBuffer extract(V8Object object) {
		if (!object.contains("__rawBuffer")) {
			return null;
		}
        return (ByteBuffer) io.js.J2V8Classes.Utils.getInstance(object.getObject("__rawBuffer").getInteger("__javaInstance"));
    }

    public static byte[] extractByteArray(V8Object object) {
        return extractByteArray( extract( object ) );
    }
    
    public static byte[] extractByteArray(ByteBuffer buf) {
        final int pos = buf.limit();
        byte[] bytes = new byte[ pos ];
        buf.position( 0 );
        buf.get( bytes );
        buf.position( pos );
        return bytes;
    }

    
    public static String extractString(V8Object object, int start, int end, Charset charset) {
        ByteBuffer b = extract( object );
        int len = end-start;
        if (len <= 0) { return ""; }
        
        byte[] strBytes = new byte[len];
        int origPosition = b.position();
        b.position(start);
        b.get(strBytes, 0, len);
        b.position(origPosition);
        return new String(strBytes, charset);
    }

    public static long writeStringAsBytes(V8Object object, String str, int offset, int len, Charset encoding) {
        ByteBuffer b = extract( object );
        int origWriter = b.position();
        b.position(offset);
        byte[] bytes = str.getBytes( encoding );
        len = Math.min( bytes.length, Math.min( len, b.limit() - b.position() ) );
        b.put( bytes, 0, len );
        b.position( Math.max( b.position(), origWriter ) );
        return len;
    }


    public static int bufLen(V8Object obj) {
        return extract(obj).capacity();
    }

    // ----------------------------------------
    // ----------------------------------------

    public static Object fill(V8Object obj, Object val, int offset, int end) {
        int byteVal = 0;
        if ( val instanceof Number ) {
            byteVal = ((Number) val).intValue() & 0xFF;
        } else if ( val instanceof String && ! ((String) val).isEmpty() ) {
            byteVal = ((String) val).charAt(0);
        }
        ByteBuffer b = extract(obj);
        for ( int i = offset; i < end; ++i ) {
            b.put(i, (byte) byteVal);
        }
        b.position( Math.max( b.position(), offset + end ) );
        return obj;
    }

    public static long copy(V8Object src, V8Object target, int targetStart, int sourceStart, int sourceEnd) {

        ByteBuffer srcBuf = extract(src);
        ByteBuffer targetBuf = extract(target);

        int origPosition = targetBuf.position();
        targetBuf.position( targetStart );

        int len = Math.min( (sourceEnd - sourceStart), targetBuf.limit() - targetStart );

        targetBuf.put(extractByteArray(src), sourceStart, len);
        targetBuf.position(Math.max(targetBuf.position(), origPosition));

        return len;
    }


    // ----------------------------------------
    // utf8
    // ----------------------------------------

    public static long[] utf8Write(V8Object object, String str, int offset, int len) {
        ByteBuffer b = extract( object );
        int origPosition = b.position();
        byte[] bytes = str.getBytes( UTF8 );
        b.position( offset );
        len = Math.min( bytes.length, Math.min( len, bufLen(object) - offset  ) );
        b.put( bytes, 0, len );
        b.position( Math.max( b.position(), origPosition ) );
        return new long[] { str.length(), len };
        
    }

    public static String utf8Slice(V8Object object, int start, int end) {
        return extractString(object, start, end, UTF8);
    }

    // ----------------------------------------
    // ascii
    // ----------------------------------------

    public static long asciiWrite(V8Object object, String str, int offset, int len) {
        return writeStringAsBytes(object, str, offset, len, ASCII);
    }

    public static String asciiSlice(V8Object object, int start, int end) {
        return extractString(object, start, end, ASCII);
    }

    // ----------------------------------------
    // ucs2
    // ----------------------------------------

    public static long ucs2Write(V8Object object, String str, int offset, int len) {
        return writeStringAsBytes(object, str, offset, len, UCS2);
    }

    public static String ucs2Slice(V8Object object, int start, int end) {
        return extractString(object, start, end, UCS2);
    }

    // ----------------------------------------
    // hex
    // ----------------------------------------

    public static long hexWrite(V8Object object, String str, int offset, int len) {
        ByteBuffer b = extract( object );
        int origWriter = b.position();
        byte[] bytes = Hex.decode(str);
        b.position( offset );
        len = Math.min( bytes.length, Math.min( len, b.capacity() - b.position()) );
        b.put( bytes, 0, len );
        b.position( Math.max( b.position(), origWriter ) );
        return len;
    }

    public static String hexSlice(V8Object object, int start, int end) {
        ByteBuffer b = extract( object );
        byte[] bytes = new byte[ end-start ];
        int originalPosition = b.position();
        b.position(start);
        b.get( bytes, start, end-start );
        b.position(originalPosition);
        return Hex.toHexString( bytes );
    }

    // ----------------------------------------
    // base64
    // ----------------------------------------

    public static long base64Write(V8Object object, String str, int offset, int len) {
        ByteBuffer b = extract( object );
        int origPosition = b.position();
        byte[] bytes = Base64.decode(str);
        b.position( offset );
        len = Math.min( bytes.length, Math.min( len, b.capacity() - b.position() ) );
        b.put(bytes, 0, len);
        b.position(Math.max(b.position(), origPosition));
        return len;
    }

    public static String base64Slice(V8Object object, int start, int end) {
        ByteBuffer b = extract( object );
        byte[] bytes = new byte[ end-start ];
        int originalPosition = b.position();
        b.position(start);
        b.get( bytes, start, end-start );
        b.position(originalPosition);

        return Base64.toBase64String(bytes);
    }

    // ----------------------------------------
    // binary
    // ----------------------------------------

    public static long binaryWrite(V8Object object, String str, int offset, int len) {
        ByteBuffer b = extract( object );
        int origPosition = b.position();
        byte[] bytes = str.getBytes( BINARY );
        len = Math.min( bytes.length, Math.min( len, b.capacity() - b.position() ) );
        b.put( bytes, 0, len );
        b.position( Math.max( b.position(), origPosition ) );
        return len;
    }

    public static String binarySlice(V8Object object, int start, int end) {
        return extractString(object, start, end, BINARY);
    }

    // ----------------------------------------
    // read/write
    // ----------------------------------------

    public static void writeFloatBE(V8Object obj, float value, int offset) {
        extract( obj ).putFloat( offset, value );
    }

    public static float readFloatBE(V8Object obj, int offset) {
        return extract( obj ).getFloat( offset );
    }

    public static void writeFloatLE(V8Object obj, float value, int offset) {
        int bits = Float.floatToIntBits((float) value);
        extract(obj).putInt(offset, Integer.reverseBytes(bits));
    }

    public static float readFloatLE(V8Object obj, int offset) {
        int bits = extract(obj).getInt(offset);
        return Float.intBitsToFloat(Integer.reverseBytes(bits));
    }

    public static void writeDoubleBE(V8Object obj, double value, int offset) {
        extract( obj ).putDouble( offset, value );
    }

    public static double readDoubleBE(V8Object obj, int offset) {
        return extract(obj).getDouble( offset );
    }

    public static void writeDoubleLE(V8Object obj, double value, int offset) {
        long bits = Double.doubleToLongBits(value);
        extract(obj).putLong(offset, Long.reverse(bits));
    }

    public static double readDoubleLE(V8Object obj, int offset) {
        long bits = extract(obj).getLong(offset);
        return Double.longBitsToDouble(Long.reverseBytes(bits));
    }
}
