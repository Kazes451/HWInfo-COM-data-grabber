package org.example;

import org.example.service.Service;

import java.util.logging.Logger;

public class ServiceLauncher {
	private final static Logger log = Logger.getLogger(ServiceLauncher.class.getName());
	private final static Service service = new Service();
	public static void main(String[] args) {
		{
			log.info("Starting custom HWInfo data service.");
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				log.info("Custom HWInfo data service terminating...");
				service.windowsStop();
				log.info("Custom HWInfo data service is stopped.");
			}));
			service.windowsStart();
		}
	}

	@SuppressWarnings("unused")
	public static void windowsService(String[] args) {
		if (args.length > 0) {
			String cmd = args[0];
			if ("start".equals(cmd)) {
				log.info("Starting custom HWInfo data service.");
				service.windowsStart();
			} else {
				log.info("Stopping custom HWInfo data service.");
				service.windowsStop();
			}
		} else {
			log.severe("No params specified!");
		}
	}
}
