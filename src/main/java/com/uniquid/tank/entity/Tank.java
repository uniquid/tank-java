package com.uniquid.tank.entity;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Tank simulates a liquid container that have an input faucet and an output faucet.
 */
public class Tank {

	private static final Logger LOGGER = LoggerFactory.getLogger(Tank.class);

	private static Tank INSTANCE;

	private boolean opened;
	private boolean inputOpen;
	private boolean outputOpen;

	private int level = 0;

	private Object syncObject;

	private Timer inputTimer;
	private Timer outputTimer;

	private Tank() {
		this.opened = false;
		this.syncObject = new Object();
		this.inputTimer = new Timer();
		this.outputTimer = new Timer();
		this.inputOpen = false;
		this.outputOpen = false;
	}

	public static synchronized Tank getInstance() {

		if (INSTANCE == null) {
			INSTANCE = new Tank();
		}

		return INSTANCE;
	}

	public int getLevel() {

		synchronized (syncObject) {

			return level;

		}

	}

	public boolean isInputOpen() {

		synchronized (syncObject) {

			return inputOpen;

		}

	}

	public boolean isOutputOpen() {

		synchronized (syncObject) {

			return outputOpen;

		}

	}

	public void open() {

		synchronized (syncObject) {

			if (!opened) {

				this.opened = true;

			}

		}
	}

	public void close() {

		synchronized (syncObject) {

			if (opened) {

				inputTimer.cancel();
				outputTimer.cancel();

				inputTimer = new Timer();
				outputTimer = new Timer();

				this.opened = false;

			}

		}
	}

	public void openInput() {

		synchronized (syncObject) {

			if (!inputOpen) {

				inputOpen = true;
				inputTimer.schedule(new FillTankTimerTask(), 0, 500);

			}

		}

	}

	public void closeInput() {

		synchronized (syncObject) {

			if (inputOpen) {

				inputOpen = false;
				inputTimer.cancel();
				inputTimer = new Timer();

			}

		}

	}

	public void openOutput() {

		synchronized (syncObject) {

			if (!outputOpen) {

				outputOpen = true;
				outputTimer.schedule(new FlushTankTimerTask(), 0, 500);

			}

		}

	}

	public void closeOutput() {

		synchronized (syncObject) {

			if (outputOpen) {

				outputOpen = false;
				outputTimer.cancel();
				outputTimer = new Timer();

			}

		}

	}

	public class FillTankTimerTask extends TimerTask {

		@Override
		public void run() {

			synchronized (syncObject) {

				level += 1;

				LOGGER.info("Tank level: " + level);

			}

		}

	}

	public class FlushTankTimerTask extends TimerTask {

		@Override
		public void run() {

			synchronized (syncObject) {

				if (level > 2) {
					level -= 2;
				}

				LOGGER.info("Tank level: " + level);
			}

		}

	}

}
