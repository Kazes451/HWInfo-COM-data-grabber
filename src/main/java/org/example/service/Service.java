package org.example.service;

import com.fazecast.jSerialComm.SerialPort;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.example.listener.MessageListener;
import org.example.model.Sensor;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

public class Service {
	private final Logger log = Logger.getLogger(Service.class.getName());
	private final SerialPort comPort;
	private final MessageListener listener;
	private final String comPortName;
	private final PropertyService propertyService;

	public Service() {
		this.propertyService = new PropertyService();
		this.comPortName = propertyService.getStringValue("service.listened.port");
		this.comPort = SerialPort.getCommPort(comPortName);
		this.listener = new MessageListener(this::processData);
	}

	public void windowsStart() {
		createRegistryStructure();
		initialize();
		int timeout = propertyService.getIntValue("service.read.timeout");
		while (comPort.isOpen()) {
			synchronized(this) {
				try {
					this.wait(timeout);
					Duration lastDataReceived = Duration.between(listener.getLastDataReceivedAt(), Instant.now());
					if (lastDataReceived.toMillis() > timeout) {
						log.severe("No data received from port " + comPortName + " for " + lastDataReceived.toSeconds() + " seconds.");
					}
				} catch(InterruptedException ie){
					terminate();
				}
			}
		}
		log.info("Service stopped.");
	}

	public void windowsStop() {
		terminate();
		synchronized(this) {
			this.notify();
		}
	}

	private void initialize() {
		try {
			comPort.openPort(propertyService.getIntValue("service.listened.port.safety-sleep-time"));
			log.info("Trying to open " + comPortName);
			if (comPort.isOpen()) {
				log.info("Listening port " + comPortName);
				comPort.addDataListener(listener);
			} else {
				log.info("Unable to listen port " + comPortName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void terminate() {
		if (comPort.isOpen()) {
			comPort.closePort();
			log.info("Port " + comPort.getSystemPortName() + " closed");
			comPort.removeDataListener();
		}
		deleteRegistryStructure();
	}
	private void createKey(Sensor sensor) {
		Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, sensor.registryPath());
		Advapi32Util.registrySetStringValue(
				WinReg.HKEY_CURRENT_USER,
				sensor.registryPath(),
				"Name",
				sensor.name()
		);
		Advapi32Util.registrySetStringValue(
				WinReg.HKEY_CURRENT_USER,
				sensor.registryPath(),
				"Value",
				""
		);
		Advapi32Util.registrySetStringValue(
				WinReg.HKEY_CURRENT_USER,
				sensor.registryPath(),
				"Unit",
				sensor.units()
		);
	}

	public void processData(String [] data) {
		for (var i = 0; i < data.length; i++) {
			writeToRegistry(propertyService.getSensor(i).registryPath(), data[i]);
		}
	}
	private void writeToRegistry(String keyPath, String value) {
		log.finer("Writing " + keyPath + ", value=" + value);
		Advapi32Util.registrySetStringValue(
				WinReg.HKEY_CURRENT_USER,
				keyPath,
				"Value",
				value
		);
	}

	private void createRegistryStructure() {
		propertyService.getSensors().stream()
				.filter(s -> !Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, s.registryPath()))
				.forEach(this::createKey);
	}

	private void deleteRegistryStructure() {
		if (Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, propertyService.getRegistryKeyPath())) {
			log.finer("Deleting " + propertyService.getRegistryKeyPath());
			propertyService.getSensors().stream()
					.filter(s -> Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, s.registryPath()))
					.forEach(s -> Advapi32Util.registryDeleteKey(WinReg.HKEY_CURRENT_USER, s.registryPath()));
			Advapi32Util.registryDeleteKey(WinReg.HKEY_CURRENT_USER, propertyService.getRegistryKeyPath());
		}
		log.info("Registry records deleted.");
	}
}
