package de.hm.cs.netze1;

import java.util.zip.Adler32;

public class Package {
  private byte flags = 0;
  private byte[] payload = new byte[]{0};
  private int sequenceNumber = 0;
  private int acknowledgementNumber = 0;
  private Long checksum = null;
	
	/*
	 * Flags
	 * 0: SYN
	 * 1: ACK
	 * 2: FACK
	 * 3: FIN
	 */

  public Package(byte[] paket) throws Exception {
    checksum = (((paket[9] | 0x0L) << 56) & 0xFF00000000000000L)
        | (((paket[10] | 0x0L) << 48) & 0xFF000000000000L)
        | (((paket[11] | 0x0L) << 40) & 0xFF0000000000L)
        | (((paket[12] | 0x0L) << 32) & 0xFF00000000L)
        | (((paket[13] | 0x0L) << 24) & 0xFF000000L)
        | (((paket[14] | 0x0L) << 16) & 0xFF0000L)
        | (((paket[15] | 0x0L) <<  8) & 0xFF00L)
        | ((paket[16] | 0x0L) & 0xFFL);
    Adler32 chinese = new Adler32();
    chinese.update(paket, 0, 9);
    chinese.update(paket, 17, paket.length - 17);
    if (checksum.compareTo(chinese.getValue()) != 0) {
      throw new Exception("Package defect");
    }
    this.sequenceNumber = ((paket[0] << 24) & 0xFF000000)
        | ((paket[1] << 16) & 0xFF0000)
        | ((paket[2] << 8) & 0xFF00)
        | (paket[3] & 0xFF);
    this.acknowledgementNumber = ((paket[4] << 24) & 0xFF000000)
        | ((paket[5] << 16) & 0xFF0000)
        | ((paket[6] << 8) & 0xFF00)
        | (paket[7] & 0xFF);
    this.flags = paket[8];
    this.payload = new byte[paket.length - 17];
    System.arraycopy(paket, 17, this.payload, 0, this.payload.length);
  }

  public Package() {

  }

  public byte[] makePaket() {
    byte[] paket = new byte[payload.length + 17];
    paket[0] = (byte) (this.sequenceNumber >> 24);
    paket[1] = (byte) (this.sequenceNumber >> 16);
    paket[2] = (byte) (this.sequenceNumber >> 8);
    paket[3] = (byte) this.sequenceNumber;
    paket[4] = (byte) (this.acknowledgementNumber >> 24);
    paket[5] = (byte) (this.acknowledgementNumber >> 16);
    paket[6] = (byte) (this.acknowledgementNumber >> 8);
    paket[7] = (byte) this.acknowledgementNumber;
    paket[8] = this.flags;
    System.arraycopy(this.payload, 0, paket, 17, this.payload.length);
    Adler32 chinese = new Adler32();
    chinese.update(paket, 0, 9);
    chinese.update(paket, 17, paket.length - 17);
    checksum = chinese.getValue();
    paket[9] = (byte) (checksum >> 56L);
    paket[10] = (byte) (checksum >> 48L);
    paket[11] = (byte) (checksum >> 40L);
    paket[12] = (byte) (checksum >> 32L);
    paket[13] = (byte) (checksum >> 24L);
    paket[14] = (byte) (checksum >> 16L);
    paket[15] = (byte) (checksum >> 8L);
    paket[16] = (byte) (checksum >> 0L);
    return paket;
  }

  public void setACK(boolean ack) {
    flags = ack ? (byte) (flags | 2) : (byte) (flags & 253);
  }

  public void setFACK(boolean fack) {
    flags = fack ? (byte) (flags | 4) : (byte) (flags & 251);
  }

  public void setSYN(boolean syn) {
    flags = syn ? (byte) (flags | 1) : (byte) (flags & 254);
  }

  public void setFIN(boolean fin) {
    flags = fin ? (byte) (flags | 8) : (byte) (flags & 247);
  }

  public boolean isSYN() {
    return (flags & 1) != 0;
  }

  public boolean isACK() {
    return (flags & 2) != 0;
  }

  public boolean isFACK() {
    return (flags & 4) != 0;
  }

  public boolean isFIN() {
    return (flags & 8) != 0;
  }

  public void setPayload(byte[] payload, int length) {
    this.payload = new byte[payload.length];
    System.arraycopy(payload, 0, this.payload, 0, length);
  }

  public byte[] getPayload() {
    return this.payload;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public void setSequenceNumber(int sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  public int getAcknowledgementNumber() {
    return acknowledgementNumber;
  }

  public void setAcknowledgementNumber(int acknowledgementNumber) {
    this.acknowledgementNumber = acknowledgementNumber;
  }

  private Long getChecksum() {
    byte[] paket = new byte[payload.length + 9];
    paket[0] = (byte) (this.sequenceNumber >> 24);
    paket[1] = (byte) (this.sequenceNumber >> 16);
    paket[2] = (byte) (this.sequenceNumber >> 8);
    paket[3] = (byte) this.sequenceNumber;
    paket[4] = (byte) (this.acknowledgementNumber >> 24);
    paket[5] = (byte) (this.acknowledgementNumber >> 16);
    paket[6] = (byte) (this.acknowledgementNumber >> 8);
    paket[7] = (byte) this.acknowledgementNumber;
    paket[8] = this.flags;
    System.arraycopy(this.payload, 0, paket, 9, this.payload.length);
    Adler32 chinese = new Adler32();
    chinese.update(paket, 0, paket.length);
    return chinese.getValue();
  }

  @Override
  public int hashCode() {
    return (int) (getChecksum() << 0L);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other.getClass() != getClass()) {
      return false;
    }

    return ((Package)other).getChecksum().compareTo(getChecksum()) == 0;
  }
}
