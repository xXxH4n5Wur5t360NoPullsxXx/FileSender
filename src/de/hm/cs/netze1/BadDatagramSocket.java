package de.hm.cs.netze1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class BadDatagramSocket extends DatagramSocket {

  private final int loss_rate;
  private final int corruption_rate;

  public BadDatagramSocket(int loss_rate, int corruption_rate) throws SocketException {
    super(2300);
    this.corruption_rate = corruption_rate;
    this.loss_rate = loss_rate;
  }

  @Override
  public void send(DatagramPacket p) throws IOException {
    if (Math.random() < loss_rate) {
      return;
    } else if (Math.random() < corruption_rate) {
      byte[] bad = new byte[p.getLength()];
      System.arraycopy(p.getData(), 0, bad, 0, bad.length);
      int trenner = (int) (Math.random() * bad.length);
      for (int i = 0; i < trenner; i++) {
        int ran = (int) (Math.random() * bad.length);
        bad[ran] = (byte) (bad[ran] | ran);
      }
      p.setData(bad);
    }

    super.send(p);
  }
}
