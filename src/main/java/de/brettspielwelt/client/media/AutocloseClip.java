package de.brettspielwelt.client.media;

import de.brettspielwelt.shared.tools.JavaVersionChecker;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class AutocloseClip implements Clip {
	private final AutocloseMixer mixer;
	private final Object clipLock;
	private final Queue<WeakReference<LineListener>> listeners;

	private byte[] sampleBuffer;
	private AudioFormat sampleFormat;
	private int bufferSize;

	private SourceDataLine baseClip;
	private Feeder feeder;

	private boolean storePosOnClose;
	private boolean open;
	private boolean running;
	private int currentPosition;

	private int storedPosition;
	private int loopStart;
	private int loopEnd;
	private int loopsToGo;

	private static final int IDLE_MILLIS = 5000;

	public AutocloseClip(AutocloseMixer mixer) {
		this.mixer = mixer;
		clipLock = new Object();
		listeners = new ConcurrentLinkedQueue<WeakReference<LineListener>>();
		loopStart = 0;
		loopEnd = 0;
	}

	@Override
	public void open(AudioFormat format, byte[] data, int offset, int bufferSize) throws LineUnavailableException {
		AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(data), format,
				data.length / format.getFrameSize());
		try {
			open(ais);
		} catch (IOException e) {
			throw new LineUnavailableException("Cannot read sound data");
		}
	}

	@Override
	public void open(AudioInputStream stream) throws LineUnavailableException, IOException {
		stream = convertToSupportedStream(stream);
		AudioFormat format = stream.getFormat();
		ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
		byte[] transferBuffer = new byte[8192];
		int len;
		while  ((len = stream.read(transferBuffer)) != -1) {
			bufferStream.write(transferBuffer, 0, len);
		}
		synchronized (clipLock) {
			if (open) {
				close();
			}
			sampleBuffer = bufferStream.toByteArray();
			sampleFormat = format;
			bufferSize = (int)(sampleFormat.getSampleRate() / 10) * sampleFormat.getFrameSize();
			loopStart = 0;
			loopEnd = (sampleBuffer.length / sampleFormat.getFrameSize()) -1;
		}
		open();
	}

	@Override
	public int getFrameLength() {
		synchronized (clipLock) {
			if (sampleFormat != null) {
				return sampleBuffer.length / sampleFormat.getFrameSize();
			} else {
				return 0;
			}
		}
	}

	@Override
	public long getMicrosecondLength() {
		synchronized (clipLock) {
			if (sampleFormat != null) {
				int frames = sampleBuffer.length / sampleFormat.getFrameSize();
				return (long) (frames * 1000000L / sampleFormat.getSampleRate());
			} else {
				return 0;
			}
		}
	}

	@Override
	public void setFramePosition(int frames) {
		synchronized (clipLock) {
			storedPosition = frames;
			storePosOnClose = false;
		}
	}

	@Override
	public void setMicrosecondPosition(long microseconds) {
		synchronized (clipLock) {
			if (sampleFormat != null && sampleBuffer != null) {
				storedPosition = (int) (microseconds * sampleFormat.getSampleRate() / 1000000.0);
				storePosOnClose = false;
			}
		}
	}

	@Override
	public void setLoopPoints(int start, int end) {
		synchronized (clipLock) {
			if (sampleBuffer == null || sampleFormat == null) {
				return;
			}
			if (loopEnd == -1) {
				end = (sampleBuffer.length / sampleFormat.getFrameSize()) - 1;
			}
			if (start > end) {
				start = end;
			}
			loopStart = start;
			loopEnd = end;
		}
	}

	@Override
	public void loop(int count) {
		LineEvent event = null;
		synchronized (clipLock) {
			try {
				if (count < 0 && count != Clip.LOOP_CONTINUOUSLY) {
					throw new IllegalArgumentException("Invalid loop count");
				}
				ensureOpen();

				int pos = getFramePosition();

				if (pos < loopEnd) {
					loopsToGo = count;
				} else {
					loopsToGo = 0;
				}
				if (!running) {
					storePosOnClose = true;
					currentPosition = storedPosition;
					if (feeder != null) {
						feeder.markRun();
					}
					event = new LineEvent(this, LineEvent.Type.START, pos);
				}
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		}
		if (event != null) {
			broadcastEvent(event);
		}
	}

	@Override
	public void drain() {
		synchronized (clipLock) {
			if (baseClip != null) {
				baseClip.drain();
			}
		}
	}

	@Override
	public void flush() {
		synchronized (clipLock) {
			if (baseClip != null) {
				baseClip.flush();
			}
		}
	}

	@Override
	public void start() {
		synchronized (clipLock) {
			try {
				ensureOpen();
				if (!running) {
					storePosOnClose = true;
					currentPosition = storedPosition;
					feeder.markRun();
				}
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		synchronized (clipLock) {
			if (feeder != null) {
				feeder.markStop();
			}
		}
	}

	@Override
	public boolean isRunning() {
		synchronized (clipLock) {
			return running;
		}
	}

	@Override
	public boolean isActive() {
		synchronized (clipLock) {
			if (baseClip != null) {
				return baseClip.isActive();
			} else {
				return false;
			}
		}
	}

	@Override
	public AudioFormat getFormat() {
		synchronized (clipLock) {
			return sampleFormat;
		}
	}

	@Override
	public int getBufferSize() {
		synchronized (clipLock) {
			return bufferSize;
		}
	}

	@Override
	public int available() {
		synchronized (clipLock) {
			if (baseClip != null) {
				return baseClip.available();
			} else {
				return 0;
			}
		}
	}

	@Override
	public int getFramePosition() {
		synchronized (clipLock) {
			if (running) {
				return currentPosition;
			} else {
				return storedPosition;
			}
		}
	}

	@Override
	public long getLongFramePosition() {
		synchronized (clipLock) {
			if (running) {
				return currentPosition;
			} else {
				return storedPosition;
			}
		}
	}

	@Override
	public long getMicrosecondPosition() {
		synchronized (clipLock) {
			if (sampleFormat == null) {
				return 0;
			}
			long pos;
			if (running) {
				pos = currentPosition;
			} else {
				pos = storedPosition;
			}
			return (int) (  pos * sampleFormat.getSampleRate() / 1000000.0);
		}
	}

	@Override
	public float getLevel() {
		synchronized (clipLock) {
			if (baseClip != null) {
				return baseClip.getLevel();
			} else {
				return AudioSystem.NOT_SPECIFIED;
			}
		}
	}

	@Override
	public Line.Info getLineInfo() {
		return new Line.Info(AutocloseClip.class);
	}

	@Override
	public void open() throws LineUnavailableException {
		int pos = 0;
		boolean opened = false;
		synchronized (clipLock) {
			if (sampleFormat == null || sampleBuffer == null) {
				throw new IllegalArgumentException("Cannot open without source");
			}
			if (!open) {
				open = true;
				mixer.addClip(this);
				opened = true;
				pos = storedPosition;
			}
		}
		if (opened) {
			LineEvent event = new LineEvent(this, LineEvent.Type.OPEN, pos);
			broadcastEvent(event);
		}
	}

	@Override
	public void close() {
		int pos = 0;
		boolean closed = false;
		synchronized (clipLock) {
			if (open) {
				if (running) {
					storedPosition = currentPosition;
				}
				pos = storedPosition;
				idleClose();
				mixer.removeClip(this);
				open = false;
				closed = true;
			}
		}
		if (closed) {
			LineEvent event = new LineEvent(this, LineEvent.Type.CLOSE, pos);
			broadcastEvent(event);
		}
	}

	@Override
	public boolean isOpen() {
		synchronized (clipLock) {
			return open;
		}
	}

	@Override
	public Control[] getControls() {
		synchronized (clipLock) {
			try {
				ensureOpen();
				return baseClip.getControls();
			} catch (LineUnavailableException e) {
				e.printStackTrace();
				return new Control[0];
			}
		}
	}

	@Override
	public boolean isControlSupported(Control.Type control) {
		synchronized (clipLock) {
			try {
				ensureOpen();
				return baseClip.isControlSupported(control);
			} catch (LineUnavailableException e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	@Override
	public Control getControl(Control.Type control) {
		synchronized (clipLock) {
			try {
				ensureOpen();
				return baseClip.getControl(control);
			} catch (LineUnavailableException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	@Override
	public void addLineListener(LineListener listener) {
		listeners.add(new WeakReference<LineListener>(listener));
	}

	@Override
	public void removeLineListener(LineListener listener) {
		Iterator<WeakReference<LineListener>> it = listeners.iterator();
		while (it.hasNext()) {
			LineListener itListener = it.next().get();
			if (itListener == null) {
				it.remove();
			} else if (itListener == listener) {
				it.remove();
			}
		}
	}

	protected void broadcastEvent(final LineEvent event) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				for (WeakReference<LineListener> listener : listeners) {
					LineListener itListener = listener.get();
					if (itListener != null) {
						itListener.update(event);
					}
				}
			}
		});
	}

	protected void ensureOpen() throws LineUnavailableException {
		synchronized (clipLock) {
			if (feeder != null) {
				feeder.ping();
				if (feeder.isClosing()) {
					while (feeder != null) {
						try {
							clipLock.wait();
						} catch (InterruptedException ignored) {
						}
					}
				}
			}

			if (sampleFormat == null || sampleBuffer == null) {
				throw new LineUnavailableException("No sample data loaded in clip");
			}

			if (baseClip == null) {
				baseClip = (SourceDataLine) mixer.getUpperMixer().getLine(new Line.Info(SourceDataLine.class));
				try {
					baseClip.open(sampleFormat, bufferSize);
				} catch (LineUnavailableException e){
					baseClip = null;
					throw e;
				}
			}

			if (feeder == null) {
				feeder = new Feeder(baseClip, bufferSize, sampleBuffer, sampleFormat);
				Thread closerThread = new Thread(feeder, "AudioClip player");
				closerThread.setPriority(Thread.MAX_PRIORITY);
				closerThread.setDaemon(true);
				closerThread.start();
			}

		}
	}

	protected void idleClose() {
		synchronized (clipLock) {
			if (feeder != null) {
				feeder.terminate();
				feeder = null;
			}
			if (baseClip != null && baseClip.isOpen()) {
				if (baseClip.isRunning()) {
					baseClip.stop();
					baseClip.flush();
				}
				baseClip.close();
				baseClip = null;
			}
			clipLock.notifyAll();
		}
	}

	private class Feeder implements Runnable {

		private final SourceDataLine baseClip;
		private final int bufferSize;
		private final byte[] sampleBuffer;
		private final byte[] buffer;

		private final int frameSize;


		private boolean started;
		private boolean terminate;
		private boolean closing;

		private long lastActivity;

		public Feeder(SourceDataLine baseClip, int bufferSize, byte[] sampleBuffer, AudioFormat sampleFormat) {
			this.baseClip = baseClip;
			this.bufferSize = bufferSize;
			this.sampleBuffer = sampleBuffer;
			this.frameSize = sampleFormat.getFrameSize();
			this.buffer = new byte[bufferSize];
			this.lastActivity = System.nanoTime();
		}

		public void terminate() {
			synchronized (this) {
				terminate = true;
				notifyAll();
			}
		}

		public void markRun() {
			synchronized (clipLock) {
				running = true;
			}
			synchronized (this) {
				notifyAll();
			}
		}

		public void markStop() {
			synchronized (clipLock) {
				running = false;
			}
			synchronized (this) {
				notifyAll();
			}
		}

		public boolean isClosing() {
			synchronized (this) {
				return closing;
			}
		}

		public void ping() {
			synchronized (this) {
				lastActivity = System.nanoTime();
			}
		}

		public void run() {
			while (!terminate && !isClosing()) {
				int writtenBytes;
				synchronized (clipLock) {
					int currentEnd = loopsToGo > 0 ? loopEnd : sampleBuffer.length / frameSize;
					int writeBatch = bufferSize / frameSize;
					int writtenFrames = 0;
					while (writtenFrames < writeBatch && running) {
						int chunkLength = currentEnd - currentPosition;
						if (chunkLength <= 0) {
							if (loopsToGo == 0) {
								break;
							}
							if (loopsToGo != Clip.LOOP_CONTINUOUSLY) {
								loopsToGo--;
							}
							currentPosition = loopStart;
							if (loopsToGo == 0) {
								currentEnd = sampleBuffer.length / frameSize;
							}
							continue;
						}
						if (chunkLength > (writeBatch - writtenFrames)) {
							chunkLength = writeBatch - writtenFrames;
						}
						System.arraycopy(sampleBuffer, currentPosition * frameSize,
								buffer, writtenFrames * frameSize, chunkLength * frameSize);
						writtenFrames += chunkLength;
						currentPosition += chunkLength;
					}
					writtenBytes = writtenFrames * frameSize;
				}
				if (writtenBytes > 0 && !terminate) {
					if (!started) {
						started = true;
						baseClip.start();
					}
					baseClip.write(buffer, 0, writtenBytes);
				} else {
					synchronized (clipLock) {
						running = false;
						if (started) {
							baseClip.drain();
							baseClip.stop();
							started = false;
						}
						LineEvent event = new LineEvent(AutocloseClip.this, LineEvent.Type.STOP, currentPosition);
						broadcastEvent(event);
					}
					synchronized (this) {
						lastActivity = System.nanoTime();
						long waitTime;
						while (!running && !closing && !terminate) {
							long currentNano = System.nanoTime();
							waitTime = IDLE_MILLIS - (currentNano - lastActivity) / 1000000L;
							if (waitTime <= 0) {
								closing = true;
							} else {
								try {
									wait(waitTime);
								} catch (InterruptedException ignored) {
								}
							}
						}
					}
				}
			}

			if (closing) {
				synchronized (clipLock) {
					if (storePosOnClose) {
						storedPosition = currentPosition;
					}
				}
				idleClose();
			}
		}
	}

	private AudioInputStream convertToSupportedStream(AudioInputStream stream) throws IOException {
		boolean mac = JavaVersionChecker.jvmVersion().isMac();
		AudioFormat sourceFormat = stream.getFormat();
		Mixer mixer = this.mixer.getUpperMixer();
		Line.Info[] infos = mixer.getSourceLineInfo(new Line.Info(SourceDataLine.class));
		if (!mac && supportedAudio(sourceFormat, infos)) {
			return stream;
		}
		AudioFormat pcmFormat = new AudioFormat(sourceFormat.getSampleRate(), 16,
				sourceFormat.getChannels(), true, false);

		short[] pcm;
		try {
			byte[] sampleBytes = AudioResampler.readStreamToBytes(stream);
			pcm = AudioResampler.transformToPcm(sampleBytes, sourceFormat);

		} catch (UnsupportedAudioFileException e) {
			throw new IOException("Cannot convert audio " + sourceFormat + " to " + pcmFormat, e);
		}
		if (!mac && supportedAudio(pcmFormat, infos)) {
			return AudioResampler.shortsToOutputStream(pcmFormat, pcm);
		}
		AudioFormat cdFormat = new AudioFormat(44100.0f, 16,
				sourceFormat.getChannels(), true, false);
		if (!supportedAudio(cdFormat, infos)) {
			throw new IOException("Cannot support format - fallback to 44,1kHz not possible: " + sourceFormat);
		}
		short[] outShorts = AudioResampler.resampleAudio(pcm, cdFormat.getChannels(), pcmFormat.getSampleRate(),
				cdFormat.getSampleRate());
		if (!isMonoBuggyMac() || cdFormat.getChannels() > 1) {
			return AudioResampler.shortsToOutputStream(cdFormat, outShorts);
		} else {
			short[] stereoShorts = new short[outShorts.length*2];
			for (int i=0;i<outShorts.length;++i) {
				stereoShorts[i*2] = outShorts[i];
				stereoShorts[i*2+1] = outShorts[i];
			}
			AudioFormat stereoFormat = new AudioFormat(44100.0f, 16, 2, true, false);
			return AudioResampler.shortsToOutputStream(stereoFormat, stereoShorts);
		}
	}

	private boolean supportedAudio(AudioFormat format, Line.Info[] infos) {
		for (Line.Info info : infos) {
			if (info instanceof DataLine.Info) {
				DataLine.Info lineInfo = (DataLine.Info) info;
				if (lineInfo.isFormatSupported(format)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isMonoBuggyMac() {
		if (!JavaVersionChecker.jvmVersion().isMac()) {
			return false;
		}
		return JavaVersionChecker.jvmVersion().getMajor() == 1 &&
				JavaVersionChecker.jvmVersion().getMinor() == 6;
 	}

}
