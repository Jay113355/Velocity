package com.velocitypowered.natives.compression;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.OS.LINUX;
import static org.junit.jupiter.api.condition.OS.MAC;

import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Random;
import java.util.function.Supplier;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

class VelocityCompressorTest {

  private static byte[] TEST_DATA = new byte[1 << 14];

  @BeforeAll
  static void checkNatives() throws IOException {
    Natives.compress.getLoadedVariant();
    new Random(1).nextBytes(TEST_DATA);
  }

  @Test
  @EnabledOnOs({MAC, LINUX})
  void sanityCheckNative() {
    assertThrows(IllegalArgumentException.class, () -> Natives.compress.get().create(-42));
  }

  @Test
  @EnabledOnOs({MAC, LINUX})
  void nativeIntegrityCheck() throws DataFormatException {
    VelocityCompressor compressor = Natives.compress.get().create(Deflater.DEFAULT_COMPRESSION);
    if (compressor instanceof JavaVelocityCompressor) {
      compressor.dispose();
      fail("Loaded regular compressor");
    }
    check(compressor, () -> Unpooled.directBuffer(TEST_DATA.length + 32));
  }

  @Test
  void javaIntegrityCheckDirect() throws DataFormatException {
    VelocityCompressor compressor = JavaVelocityCompressor.FACTORY
        .create(Deflater.DEFAULT_COMPRESSION);
    check(compressor, () -> Unpooled.directBuffer(TEST_DATA.length + 32));
  }

  @Test
  void javaIntegrityCheckHeap() throws DataFormatException {
    VelocityCompressor compressor = JavaVelocityCompressor.FACTORY
        .create(Deflater.DEFAULT_COMPRESSION);
    check(compressor, () -> Unpooled.buffer(TEST_DATA.length + 32));
  }

  private void check(VelocityCompressor compressor, Supplier<ByteBuf> bufSupplier)
      throws DataFormatException {
    ByteBuf source = bufSupplier.get();
    ByteBuf dest = bufSupplier.get();
    ByteBuf decompressed = bufSupplier.get();

    source.writeBytes(TEST_DATA);

    try {
      compressor.deflate(source, dest);
      compressor.inflate(dest, decompressed, Integer.MAX_VALUE);
      source.readerIndex(0);
      assertTrue(ByteBufUtil.equals(source, decompressed));
    } finally {
      source.release();
      dest.release();
      decompressed.release();
      compressor.dispose();
    }
  }
}