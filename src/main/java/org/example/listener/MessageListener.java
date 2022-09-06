package org.example.listener;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import org.example.service.Service;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class MessageListener implements SerialPortMessageListener
{
	private final Logger log = Logger.getLogger(Service.class.getName());
	private Instant lastDataReceivedAt;
	private final Consumer<String[]> dataProcessor;

	public MessageListener(Consumer<String[]> dataProcessor) {
		this.dataProcessor = dataProcessor;
		this.lastDataReceivedAt = Instant.now();
	}

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
		log.fine("Received the following delimited message: " + delimitedMessage);
		dataProcessor.accept(delimitedMessage.split(";"));
	}
}
