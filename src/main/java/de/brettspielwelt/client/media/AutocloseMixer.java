package de.brettspielwelt.client.media;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import java.awt.EventQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AutocloseMixer implements Mixer {
	private static final Info mixerInfo = new MixerInfo();
	private final Queue<WeakReference<LineListener>> listeners;
	private final Queue<AutocloseClip> clips;
	private final Object mixerLock;
	private Mixer upperMixer;
	private boolean open;

	public AutocloseMixer() {
		clips = new ConcurrentLinkedQueue<AutocloseClip>();
		listeners = new ConcurrentLinkedQueue<WeakReference<LineListener>>();
		mixerLock = new Object();

	}

	@Override
	public Info getMixerInfo() {
		return mixerInfo;
	}

	@Override
	public Line.Info[] getSourceLineInfo() {
		return new Line.Info[] {
				new Line.Info(AutocloseClip.class)
		};
	}

	@Override
	public Line.Info[] getTargetLineInfo() {
		return new Line.Info[0];
	}

	@Override
	public Line.Info[] getSourceLineInfo(Line.Info info) {
		if (info.getLineClass().isAssignableFrom(AutocloseClip.class)) {
			return getSourceLineInfo();
		}
		return new Line.Info[0];
	}

	@Override
	public Line.Info[] getTargetLineInfo(Line.Info info) {
		return new Line.Info[0];
	}

	@Override
	public boolean isLineSupported(Line.Info info) {
		return info.getLineClass().isAssignableFrom(AutocloseClip.class);
	}

	@Override
	public Line getLine(Line.Info info) throws LineUnavailableException {
		if (!info.getLineClass().isAssignableFrom(AutocloseClip.class)) {
			throw new LineUnavailableException("This type of line is not available");
		}
		return new AutocloseClip(this);
	}

	@Override
	public int getMaxLines(Line.Info info) {
		return AudioSystem.NOT_SPECIFIED;
	}

	@Override
	public Line[] getSourceLines() {
		return clips.toArray(new AutocloseClip[0]);
	}

	@Override
	public Line[] getTargetLines() {
		return new Line[0];
	}

	@Override
	public void synchronize(Line[] lines, boolean maintainSync) {
	}

	@Override
	public void unsynchronize(Line[] lines) {
	}

	@Override
	public boolean isSynchronizationSupported(Line[] lines, boolean maintainSync) {
		return false;
	}

	@Override
	public Line.Info getLineInfo() {
		return new Line.Info(AutocloseMixer.class);
	}

	@Override
	public void open() throws LineUnavailableException {
		synchronized (mixerLock) {
			if (upperMixer == null) {
				try {
					upperMixer = AudioSystem.getMixer(null);
				} catch (IllegalArgumentException e) {
					throw new LineUnavailableException("System does not have a mixer");
				}
			}
			open = true;
		}
		LineEvent lineEvent = new LineEvent(this, LineEvent.Type.OPEN, 0);
		broadcastEvent(lineEvent);
	}

	@Override
	public void close() {
		synchronized (mixerLock) {
			if (upperMixer != null) {
				upperMixer.close();
				upperMixer = null;
			}
			open = false;
		}
		LineEvent lineEvent = new LineEvent(this, LineEvent.Type.CLOSE, 0);
		broadcastEvent(lineEvent);
	}

	@Override
	public boolean isOpen() {
		synchronized (mixerLock) {
			return open;
		}
	}

	@Override
	public Control[] getControls() {
		synchronized (mixerLock) {
			if (upperMixer == null)  {
				return new Control[0];
			} else {
				return upperMixer.getControls();
			}
		}
	}

	@Override
	public boolean isControlSupported(Control.Type control) {
		synchronized (mixerLock) {
			if (upperMixer == null) {
				return false;
			} else {
				return upperMixer.isControlSupported(control);
			}
		}
	}

	@Override
	public Control getControl(Control.Type control) {
		synchronized (mixerLock) {
			if (upperMixer == null) {
				throw new IllegalArgumentException("Unsupported control");
			} else {
				return upperMixer.getControl(control);
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

	private static final class MixerInfo extends Info {
		public MixerInfo() {
			super("AutoClosing Mixer", "BrettspielWelt",
					"Mixer that autocloses Clips", "1.0");
		}
	}

	protected void addClip(AutocloseClip clip) {
		clips.add(clip);
	}

	protected void removeClip(AutocloseClip clip) {
		clips.remove(clip);
	}

	protected Mixer getUpperMixer() {
		return upperMixer;
	}


}
