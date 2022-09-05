package org.example;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;

import java.time.Instant;

/**
 * @author Kazes
 */
public class MessageListener implements SerialPortMessageListener
{
	private Instant lastDataReceivedAt = Instant.now();

	public synchronized void setLastDataReceivedAt(Instant lastDataReceivedAt) {
		this.lastDataReceivedAt = lastDataReceivedAt;
	}

	public synchronized Instant getLastDataReceivedAt() {
		return lastDataReceivedAt;
	}
	@Override
	public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }

	@Override
	public byte[] getMessageDelimiter() { return new byte[] { (byte)0x0a}; }

	@Override
	public boolean delimiterIndicatesEndOfMessage() { return true; }

	@Override
	public void serialEvent(SerialPortEvent event)
	{
		setLastDataReceivedAt(Instant.now());
		String delimitedMessage = new String(event.getReceivedData());
		System.out.println("Received the following delimited message: " + delimitedMessage);
		String[] data = delimitedMessage.split(";");
		for (var i = 0; i < data.length; i++) {
			ServiceLauncher.writeToRegistry(ServiceLauncher.REG_KEYS.get(i), data[i]);
		}
	}
}
