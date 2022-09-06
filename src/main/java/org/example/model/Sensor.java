package org.example.model;

import java.util.Objects;

public record Sensor(
		String registryPath,
		String name,
		String units
) {
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Sensor sensor = (Sensor) o;

		if (!name.equals(sensor.name)) return false;
		if (!registryPath.equals(sensor.registryPath)) return false;
		return Objects.equals(units, sensor.units);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + registryPath.hashCode();
		result = 31 * result + (units != null ? units.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Sensor{" +
				"registryPath='" + registryPath + '\'' +
				", name='" + name + '\'' +
				", units='" + units + '\'' +
				'}';
	}
}
