package de.hm.cs.netze1;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileSender {

  private static final int PACKET_SIZE = 1400;
  private static final int TIMEOUT = 10;
  private static final int TERMINATE_TIMEOUT = 10000;
  private static final int MAX_WINDOW_SIZE = 50;
  private static AtomicBoolean running = new AtomicBoolean(true);

  private static List<FileSendTask> window = new ArrayList<>();
  private static DatagramSocket dgs;

  /**
   * 0 File
   * 1 Ip
   * 2 Port
   * 3 Loss rate
   * 4 Corruption rate
   */
  public static void main(String[] args) {
    byte[] paket = new byte[PACKET_SIZE - 50];
    int sequence = (int) (Math.random() * Integer.MAX_VALUE);
    int readCount = 0;

    String ip = args[1];
    int port = Integer.parseInt(args[2]);

    long start = System.currentTimeMillis();

    Timer t = new Timer();
    try {
      dgs = new BadDatagramSocket(Double.parseDouble(args[3]), Double.parseDouble(args[4]));
      dgs.setSoTimeout(TERMINATE_TIMEOUT);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    new Thread(FileSender::ackReceiver).start();

    try (FileInputStream reader = new FileInputStream(args[0])){
      FileSendTask fst;
      Package p = new Package();
      p.setSYN(true);
      p.setSequenceNumber(sequence);
      p.setACK(false);
      p.setFIN(false);

      fst = new FileSendTask(p, dgs, ip, port);
      synchronized(window) {
        window.add(fst);
      }
      t.schedule(fst, 0, TIMEOUT);
      sequence++;

      p.setSYN(false);

      synchronized (window) {
        window.wait(TERMINATE_TIMEOUT);
      }

      if (!window.isEmpty()) {
        System.exit(-1);
      }

      while (readCount != -1) {
        synchronized (window) {
          while (window.size() >= MAX_WINDOW_SIZE) {
            window.wait();
          }
        }
        readCount = reader.read(paket);
        if (readCount > 0) {
          p.setPayload(paket, readCount);
          p.setSequenceNumber(sequence);
          fst = new FileSendTask(p, dgs, ip, port);
          synchronized(window) {
            window.add(fst);
          }
          t.schedule(fst, 0, TIMEOUT);
          sequence += readCount;
        }
      }

      p.setACK(false);
      p.setFIN(true);
      p.setSequenceNumber(sequence + 1);
      fst = new FileSendTask(p, dgs, ip, port);
      synchronized (window) {
        window.add(fst);
      }
      t.schedule(fst, 0, TIMEOUT);

      synchronized (window) {
        while (!window.isEmpty()) {
          window.wait();
        }
      }

      long stop = System.currentTimeMillis();

      System.out.printf("Transmission took %d ms%n", stop - start);

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

    dgs.close();
    t.cancel();
  }

  private static void ackReceiver() {
    try {
      while (true) {
        DatagramPacket p = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
        dgs.receive(p);
        Package pit;
        try {
          byte[] input = new byte[p.getLength()];
          System.arraycopy(p.getData(), p.getOffset(), input, 0, p.getLength());
          pit = new Package(input);
        }  catch (Exception e) {
          continue;
        }
        if (pit.isACK()) {
          synchronized(window) {
            window.stream().filter(x -> x.seq() == pit.getAcknowledgementNumber()).findFirst()
                .ifPresent(x -> { x.cancel(); window.remove(x); });
            window.notifyAll();
          }
        }
      }
    } catch (SocketTimeoutException e) {
      System.out.println("Fuck you, fuck you and fuck you all, I AM SOCK OF THIS SHIT, I AM OUTTA HERE");
      System.exit(-1);
      return;
    } catch (Exception e) {
      System.out.println("Stopped waiting for packages");
    }
  }
}

class FileSendTask extends TimerTask {

  private final int sequenznummer;
  private final DatagramPacket paket;
  private final DatagramSocket soc;

  public FileSendTask(Package paket, DatagramSocket soc, String ip, int port) throws UnknownHostException {
    this.sequenznummer = paket.getSequenceNumber();
    byte[] p = paket.makePaket();
    this.paket = new DatagramPacket(p, p.length, InetAddress.getByName(ip), port);
    this.soc = soc;
  }

  @Override
  public void run() {
    try {
      soc.send(paket);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public int seq() {
    return sequenznummer;
  }

  @Override
  public int hashCode() {
    return sequenznummer;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null)
      return false;
    if (o.getClass() != FileSendTask.class)
      return false;
    FileSendTask fst = (FileSendTask) o;
    return sequenznummer == fst.seq();
  }
}
