package org.example.listener;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class MessageListener implements SerialPortMessageListener
{
	private final Logger log = Logger.getLogger(MessageListener.class.getName());
	private Instant lastDataReceivedAt;
	private final Consumer<String[]> dataProcessor;
	private final Consumer<SerialPort> disconnectHandler;

	public MessageListener(Consumer<String[]> dataProcessor, Consumer<SerialPort> disconnectHandler) {
		this.dataProcessor = dataProcessor;
		this.disconnectHandler = disconnectHandler;
		this.lastDataReceivedAt = Instant.now();
	}

	public synchronized void setLastDataReceivedAt(Instant lastDataReceivedAt) {
		this.lastDataReceivedAt = lastDataReceivedAt;
	}

	public synchronized Instant getLastDataReceivedAt() {
		return lastDataReceivedAt;
	}
	@Override
	public int getListeningEvents() {
		return SerialPort.LISTENING_EVENT_DATA_RECEIVED | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
	}

	@Override
	public byte[] getMessageDelimiter() { return new byte[] { (byte)0x0a}; }

	@Override
	public boolean delimiterIndicatesEndOfMessage() { return true; }

	@Override
	public void serialEvent(SerialPortEvent event)
	{
		if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
			log.info("Port disconnected.");
			disconnectHandler.accept(event.getSerialPort());
		}
		if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
			setLastDataReceivedAt(Instant.now());
			String delimitedMessage = new String(event.getReceivedData());
			log.fine("Received the following delimited message: " + delimitedMessage);
			dataProcessor.accept(delimitedMessage.split(";"));
		}
	}
}
