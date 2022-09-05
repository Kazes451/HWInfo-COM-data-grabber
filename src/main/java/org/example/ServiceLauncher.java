package org.example;

import com.fazecast.jSerialComm.SerialPort;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * @author Kazes
 */
public class ServiceLauncher implements Daemon {
	private static final ServiceLauncher engineLauncherInstance = new ServiceLauncher();

	private static String comPortName = null;

	private static final String REGISTRY_KEY_PATH = "Software\\HWiNFO64\\Sensors\\Custom\\Chiller";

	private static final String REGISTRY_KEY_TEMP = REGISTRY_KEY_PATH + "\\Temp0";
	private static final String REGISTRY_KEY_TEMP_DEW_POINT = REGISTRY_KEY_PATH + "\\Temp1";
	private static final String REGISTRY_KEY_HUMIDITY = REGISTRY_KEY_PATH + "\\Other0";
	private static final String REGISTRY_KEY_PRESSURE = REGISTRY_KEY_PATH + "\\Other1";
	private static final String REGISTRY_KEY_ALT = REGISTRY_KEY_PATH + "\\Other2";
	public static final List<String> REG_KEYS = List.of(REGISTRY_KEY_TEMP, REGISTRY_KEY_PRESSURE,
			REGISTRY_KEY_ALT, REGISTRY_KEY_HUMIDITY, REGISTRY_KEY_TEMP_DEW_POINT);
	private static SerialPort comPort;
	private final MessageListener listener = new MessageListener();

	public static void main(String[] args) {
		{
			System.out.println("Custom HWInfo data service is running!");
			comPortName = args[1];
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Custom HWInfo data service terminating...");
				engineLauncherInstance.terminate();
				System.out.println("Custom HWInfo data service is stopped.");
			}));
			engineLauncherInstance.windowsStart();
		}
	}

	@SuppressWarnings("unused")
	public static void windowsService(String[] args) {
		String cmd = "stop";
		if (args.length > 0) {
			cmd = args[0];
			comPortName = args[1];
		}

		if ("start".equals(cmd)) {
			engineLauncherInstance.windowsStart();
		}
		else {
			engineLauncherInstance.windowsStop();
		}
	}

	public void windowsStart() {
		createRegistryStructure();
		initialize();
		while (comPort.isOpen()) {
			// don't return until stopped
			synchronized(this) {
				try {
					this.wait(10000); // check every 10 seconds if sensor is alive
					if (Duration.between(listener.getLastDataReceivedAt(), Instant.now()).getSeconds() > 10) {
						System.err.println("No data received from port " + comPortName + " for 10 seconds.");
					}
				} catch(InterruptedException ie){
					terminate();
					ie.printStackTrace();
				}
			}
		}
	}

	public void windowsStop() {
		terminate();
		synchronized(this) {
			// stop the start loop
			this.notify();
		}
	}

	// Implementing the Daemon interface is not required for Windows but is for Linux
	@Override
	public void init(DaemonContext arg0) {
		System.out.println("Daemon init");
	}

	@Override
	public void start() {
		System.out.println("Daemon start");
		initialize();
	}

	@Override
	public void stop() {
		System.out.println("Daemon stop");
		terminate();
	}

	@Override
	public void destroy() {
		System.out.println("Daemon destroy");
	}

	private void initialize() {
		if (comPortName == null) {
			System.err.println("COM port name isn't specified!");
		}
		try {
			comPort = SerialPort.getCommPort(comPortName);
			comPort.openPort(2000);
			System.out.println("Trying to open " + comPortName);
			if (comPort.isOpen()) {
				System.out.println("Listening port " + comPortName);
				comPort.addDataListener(listener);
			} else {
				System.out.println("Port isn't listening");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void terminate() {
		if (comPort != null) {
			System.out.println("Closing port " + comPort.getSystemPortName());
			comPort.closePort();
			System.out.println("Port closed");
			comPort.removeDataListener();
		}
		deleteRegistryStructure();
	}

	private void createKey(String keyPath, String name, String unit) {
		Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, keyPath);
		Advapi32Util.registrySetStringValue(
				WinReg.HKEY_CURRENT_USER,
				keyPath,
				"Name",
				name
		);
		Advapi32Util.registrySetStringValue(
				WinReg.HKEY_CURRENT_USER,
				keyPath,
				"Value",
				""
		);
		Advapi32Util.registrySetStringValue(
				WinReg.HKEY_CURRENT_USER,
				keyPath,
				"Unit",
				unit
		);
	}

	public static void writeToRegistry(String keyPath, String value) {
		System.out.println("Writing " + keyPath + ", value=" + value);
		Advapi32Util.registrySetStringValue(
				WinReg.HKEY_CURRENT_USER,
				keyPath,
				"Value",
				value
		);
	}

	private static void createRegistryStructure() {
		REG_KEYS.forEach(k -> {
			if (!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, k)) {
				switch (k) {
					case REGISTRY_KEY_TEMP -> engineLauncherInstance.createKey(k, "Temperature", "Celsius");
					case REGISTRY_KEY_ALT -> engineLauncherInstance.createKey(k, "Approx altitude",  "Meters");
					case REGISTRY_KEY_PRESSURE -> engineLauncherInstance.createKey(k, "Pressure",  "KPa");
					case REGISTRY_KEY_HUMIDITY -> engineLauncherInstance.createKey(k, "Humidity",  "%");
					case REGISTRY_KEY_TEMP_DEW_POINT -> engineLauncherInstance.createKey(k, "Temperature", "DewPoint");
				}
			}
		});
	}

	private void deleteRegistryStructure() {
		if (Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, REGISTRY_KEY_PATH)) {
			System.out.println("Deleting " + REGISTRY_KEY_PATH);
			REG_KEYS.forEach(k -> {
				if (Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, k)) {
					Advapi32Util.registryDeleteKey(WinReg.HKEY_CURRENT_USER, k);
					System.out.println("Deleting " + k);
				}
			});
			Advapi32Util.registryDeleteKey(WinReg.HKEY_CURRENT_USER, REGISTRY_KEY_PATH);
		}

	}

}
