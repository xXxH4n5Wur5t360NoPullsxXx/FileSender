package de.hm.cs.netze1;

import static org.junit.Assert.*;

import org.junit.Test;

public class PackageTests {

  @Test
  public void test() throws Exception {
    Package p = new Package();
    p.setACK(true);
    assertTrue(p.isACK());
    p.setACK(false);
    assertFalse(p.isACK());
    p.setSYN(true);
    assertTrue(p.isSYN());
    p.setSYN(false);
    assertFalse(p.isSYN());
    p.setFIN(true);
    assertTrue(p.isFIN());
    p.setFIN(false);
    assertFalse(p.isFIN());
    p.setPayload(new byte[] {1, 2, 3, 4, 5}, 5);
    p.setSequenceNumber(16909320);
    p.setAcknowledgementNumber(134480385);
    assertEquals(134480385, p.getAcknowledgementNumber());
    assertEquals(16909320, p.getSequenceNumber());

    Package p2 = new Package(p.makePaket());

    assertTrue(p.equals(p2));

    p.setPayload(new byte[0], 0);
    p.setSequenceNumber(-16909320);
    p.setAcknowledgementNumber(-134480385);
    p2 = new Package(p.makePaket());

    assertTrue(p.equals(p2));

    p.setPayload(new byte[] {1}, 1);
    p2 = new Package(p.makePaket());

    assertTrue(p.equals(p2));
  }

}