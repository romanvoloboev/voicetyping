package com.romanvoloboev.utils.v1;

import org.springframework.stereotype.Component;

import javax.sound.sampled.*;

@Component
public class Microphone {

	private TargetDataLine targetDataLine;

	public enum State {
		BUSY, FREE
	}

	private State state;

	public Microphone() {
		setState(State.FREE);
		initTargetDataLine();
	}

	public State getState() {
		return state;
	}

	private void setState(State state) {
		this.state = state;
	}

	public TargetDataLine getTargetDataLine() {
		return targetDataLine;
	}
	
	private void setTargetDataLine(TargetDataLine targetDataLine) {
		this.targetDataLine = targetDataLine;
	}

	private void initTargetDataLine() {
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, getAudioFormat());
		try {
			setTargetDataLine((TargetDataLine) AudioSystem.getLine(dataLineInfo));
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	private AudioFormat getAudioFormat() {
		float sampleRate = 16000;
		//8000,11025,16000,22050,44100
		int sampleSizeInBits = 16;
		//8,16
		int channels = 1;
		//1,2
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, true, false);
	}

	public void startRecording() {
		if (getTargetDataLine() == null) {
			initTargetDataLine();
		}
		if (!getTargetDataLine().isOpen() && !getTargetDataLine().isRunning() && !getTargetDataLine().isActive()) {
			try {
				setState(State.BUSY);
				getTargetDataLine().open(getAudioFormat());
				getTargetDataLine().start();
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		}
		
	}

	public void stopRecording() {
		if (getState() != State.FREE) {
			getTargetDataLine().stop();
			getTargetDataLine().close();
			setState(State.FREE);
		}
	}
	
}
