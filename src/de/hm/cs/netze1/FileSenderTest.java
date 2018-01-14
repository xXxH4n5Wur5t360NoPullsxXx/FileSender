package de.hm.cs.netze1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.junit.Test;

/*
  Neumann
 */
public class FileSenderTest {

  @Test
  public void test() throws Exception {
    DatagramSocket socket = new DatagramSocket(2000);
    new Thread() {
      @Override
      public void run() {
        DatagramPacket p;
        System.out.println("Waiting for package...");
        try {
          while (true) {
            p = new DatagramPacket(new byte[1400], 1400);
            socket.receive(p);
            Package paket;
            try {
              byte[] input = new byte[p.getLength()];
              p.getData();
              p.getLength();
              p.getOffset();
              System.arraycopy(p.getData(), p.getOffset(), input, 0, p.getLength());
              paket = new Package(input);
            } catch (Exception e) {
              System.out.println("Received defect package");
              continue;
            }
            Package returnThis = new Package();
            returnThis.setACK(true);
            returnThis.setAcknowledgementNumber(paket.getSequenceNumber());
            byte[] bytep = returnThis.makePaket();
            DatagramPacket dgp = new DatagramPacket(bytep, bytep.length);
            dgp.setPort(p.getPort());
            dgp.setAddress(InetAddress.getLoopbackAddress());
            socket.send(dgp);
          }
        } catch (IOException e) {}
      }
    }.start();

    FileSender.main(new String[] {"src\\de\\hm\\cs\\netze1\\FileSender.java", "127.0.0.1", "2000", "0.9", "0.9"});

  }
}
