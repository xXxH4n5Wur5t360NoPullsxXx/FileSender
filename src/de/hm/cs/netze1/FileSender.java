package de.hm.cs.netze1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class FileSender {
	
	private static final int PACKET_SIZE = 1400;
	private static final int TIMEOUT = 10;
	private static final int MAX_WINDOW_SIZE = 100;
	private static AtomicBoolean running = new AtomicBoolean(true);
	
	private static List<FileSendTask> window = new ArrayList<>();
	private static DatagramSocket dgs;
	
	public static void main(String[] args) {
		byte[] paket = new byte[PACKET_SIZE];
		int sequence = (int) (Math.random() * Integer.MAX_VALUE);
		int readCount = 0;
		Timer t = new Timer();
		try {
			dgs = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		new Thread(FileSender::ackReceiver).start();
		
		try (FileInputStream reader = new FileInputStream(args[0])){
			while (readCount != -1) {
				while (window.size() >= MAX_WINDOW_SIZE) {
					window.wait();
				}
				readCount = reader.read(paket);
				if (readCount > 0) {
					Package p = new Package();
					p.setACK(false);
					p.setFACK(false);
					p.setFIN(false);
					p.setPayload(paket, readCount);
					p.setSequenceNumber(sequence);
					FileSendTask fst = new FileSendTask(p, dgs);
					t.schedule(fst, 0, TIMEOUT);
					synchronized(window) {
						window.add(fst);
					}
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}	
		
		running.set(false);
	}
	
	private static void ackReceiver() {
		boolean running = true;
		try {
			while (running) {
				DatagramPacket p = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
				dgs.receive(p);
				Package pit;
				try {
					pit = new Package(p.getData());
				} catch (Exception e) {
					continue;
				}
				
				if (pit.isACK()) {
					synchronized(window) {
						window.stream().filter(x -> x.seq == pit.getSequenceNumber()).peek(x -> x.close()).foreach(x -> window.remove(x));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class FileSendTask extends TimerTask {
	
	private final int sequenznummer;
	private final DatagramPacket paket;
	private final DatagramSocket soc;
	
	public FileSendTask(Package paket, DatagramSocket soc) {
		this.sequenznummer = paket.getSequenceNumber();
		byte[] p = paket.makePaket();
		this.paket = new DatagramPacket(p, p.length);
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
