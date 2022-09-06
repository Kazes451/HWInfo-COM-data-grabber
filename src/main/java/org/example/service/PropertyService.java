package org.example.service;

import org.example.model.Sensor;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class PropertyService {
	private final Properties properties;
	private final ArrayList<Sensor> sensors;

	private final String registryKeyPath;
	public PropertyService() {
		this.properties = new Properties();
		try (FileInputStream fis = new FileInputStream(System.getProperty("service.config"))) {
			properties.load(fis);
		} catch (Throwable t) {
			throw new RuntimeException("Can't load service properties", t);
		}
		this.registryKeyPath = "Software\\HWiNFO64\\Sensors\\Custom\\"
				.concat(properties.getProperty("service.sensors.device.name"));
		this.sensors = new ArrayList<>();
		int sensorsCount = Integer.parseInt(properties.getProperty("service.sensors.count"));
		for (int i = 0; i < sensorsCount; i++) {
			String[] sensorProperties = properties.getProperty("service.sensors." + i).split(",");
			this.sensors.add(
					new Sensor(
							registryKeyPath.concat(File.separator).concat(sensorProperties[0]),
							sensorProperties[1],
							sensorProperties[2]
					)
			);
		}
	}

	public String getStringValue(String key) {
		return properties.getProperty(key);
	}

	public int getIntValue(String key) {
		return Integer.parseInt(properties.getProperty(key));
	}

	public ArrayList<Sensor> getSensors() {
		return sensors;
	}

	public String getRegistryKeyPath() {
		return registryKeyPath;
	}

	public Sensor getSensor(int i) {
		return sensors.get(i);
	}
}
