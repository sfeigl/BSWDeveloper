package de.brettspielwelt.client.media;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;


public class AudioResampler {
	private static final int SINC_OFFSET_STEPS = 1024;
	private static final int SINC_TABLE_SIZE = 30;
	private static final int SINC_TABLE_CENTER = SINC_TABLE_SIZE / 2;

	private static final int ALOW_AMI_MASK = 0x55;

	private static final byte ULAW_SIGN_BIT = (byte) 0x80;
	private static final int ULAW_QUANT_MASK = 0xf;
	private static final int ULAW_SEG_SHIFT = 4;
	private static final byte ULAW_SEG_MASK = (byte) 0x70;
	private static final int ULAW_BIAS = 0x84;

	public static byte[] readStreamToBytes(AudioInputStream stream) throws IOException {
		ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
		byte[] transferBuffer = new byte[8192];
		int len;
		while  ((len = stream.read(transferBuffer)) != -1) {
			bufferStream.write(transferBuffer, 0, len);
		}
		return bufferStream.toByteArray();
	}

	public static short[] transformToPcm(byte[] sampleBytes, AudioFormat sourceFormat) throws UnsupportedAudioFileException {
		int samples = sampleBytes.length / (sourceFormat.getSampleSizeInBits() / 8);
		short[] out = new short[samples];
		ByteBuffer sampleBuffer = ByteBuffer.wrap(sampleBytes);
		if (AudioFormat.Encoding.ALAW.equals(sourceFormat.getEncoding())) {
			for (int i=0;i<samples;++i) {
				byte alaw = sampleBuffer.get();
				out[i] = aLawToPcm(alaw);
			}
		} else if (AudioFormat.Encoding.ULAW.equals(sourceFormat.getEncoding())) {
			for (int i=0;i<samples;++i) {
				byte ulaw = sampleBuffer.get();
				out[i] = uLawToPcm(ulaw);
			}
		} else if (AudioFormat.Encoding.PCM_SIGNED.equals(sourceFormat.getEncoding())) {
			if (sourceFormat.getSampleSizeInBits() == 8) {
				for (int i=0;i<samples;++i) {
					short sample = byteToShort(sampleBuffer.get());
					out[i] = sample;
				}
			} else if (sourceFormat.getSampleSizeInBits() == 16) {
				ShortBuffer shorts = sampleBuffer.order(
						sourceFormat.isBigEndian()?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN).asShortBuffer();
				for (int i=0;i<samples;++i) {
					out[i] = shorts.get();
				}
			} else {
				throw new UnsupportedAudioFileException("Does not support audio file with PCM size " +
						sourceFormat.getSampleSizeInBits());
			}
		} else if (AudioFormat.Encoding.PCM_UNSIGNED.equals(sourceFormat.getEncoding())) {
			if (sourceFormat.getSampleSizeInBits() == 8) {
				for (int i=0;i<samples;++i) {
					int sample = sampleBuffer.get();
					sample -= 128;
					short aligned = byteToShort((byte) sample);
					out[i] = aligned;
				}
			} else if (sourceFormat.getSampleSizeInBits() == 16) {
				ShortBuffer shorts = sampleBuffer.order(
						sourceFormat.isBigEndian()?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN).asShortBuffer();
				for (int i=0;i<samples;++i) {
					out[i] = (short) (shorts.get() - 32768);
				}
			} else {
				throw new UnsupportedAudioFileException("Does not support audio file with PCM size " +
						sourceFormat.getSampleSizeInBits());
			}
		} else {
			throw new UnsupportedAudioFileException("This audio format is not supported: " +  sourceFormat);
		}
		return out;
	}

	public static AudioInputStream shortsToOutputStream(AudioFormat format, short[] outShorts) throws IOException {
		ByteBuffer pcmArray = ByteBuffer.allocate(outShorts.length * 2);
		ShortBuffer pcmShort = pcmArray.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
		for (short data : outShorts) {
			pcmShort.put(data);
		}
		ByteArrayInputStream inStream = new ByteArrayInputStream(pcmArray.array());
		try {
			return new AudioInputStream(inStream, format, outShorts.length /
					format.getFrameSize() * 2L);
		} catch (IllegalArgumentException e) {
			throw new IOException("Cannot create auduo stream with format " + format, e);
		}
	}


	public static short[] resampleAudio(short[] in, int channels, float inRate, float outRate) {

		@SuppressWarnings("IntegerDivisionInFloatingPointContext")
		double inputEnd = in.length / channels;
		@SuppressWarnings("IntegerDivisionInFloatingPointContext")
		int outputEnd = (int) Math.ceil((in.length / channels) * outRate / inRate);
		short[] out = new short[outputEnd * channels];
		double inStep = inRate / outRate;
		double[][] sincTables = buildSincTableForRatio(inStep);
		double inputOffset = 0.0;
		int outputOffset = 0;
		while (inputOffset < inputEnd && outputOffset < outputEnd) {
			int integerInOffset = (int) inputOffset;
			double[] sincForOffset =
					sincTables[(int) ((inputOffset - integerInOffset) * SINC_OFFSET_STEPS)];
			for (int channel = 0; channel < channels; ++channel) {
				double outValue = 0;
				for (int inPos = (integerInOffset - SINC_TABLE_CENTER) * channels, i = 0; i < SINC_TABLE_SIZE; i++, inPos +=
						channels) {
					if (inPos >= 0 && inPos < in.length) {
						outValue += (in[inPos] / 32768f) * sincForOffset[i];
					}
				}
				int intOut = (int) (outValue * 32768f);
				if (intOut > 32767) {
					intOut = 32767;
				}
				if (intOut < -32768) {
					intOut = -32768;
				}
				out[outputOffset*channels+channel] = (short) intOut;
			}
			outputOffset++;
			inputOffset += inStep;
		}
		return out;
	}

	private static double[][] buildSincTableForRatio(double ratio) {
		if (ratio < 1.0) {
			ratio = 1.0;
		}
		ratio = (1.0 / (1.0 + Math.pow(((ratio-1)*10.0), 1.1) / 10.0));
		double[][] sincTable = new double[SINC_OFFSET_STEPS][];
		for (int i = 0; i < SINC_OFFSET_STEPS; i++) {
			sincTable[i] = buildSincTableForOffset(-i / ((double) SINC_OFFSET_STEPS), ratio);
		}
		return sincTable;
	}
	private static double[] buildSincTableForOffset(double offset, double ratio) {
		double[] sincTable = new double[SINC_TABLE_SIZE];
		for (int i = 0; i < SINC_TABLE_SIZE; i++) {
			sincTable[i] = (-0.5 * Math.cos(2.0 * Math.PI * (i + offset) / (double) SINC_TABLE_SIZE) + 0.5);
		}
		for (int i = 0; i < SINC_TABLE_SIZE; i++)
			sincTable[i] *= normalizedSinc((-SINC_TABLE_CENTER + i + offset) * ratio) * ratio;
		return sincTable;
	}

	private static double normalizedSinc(double x) {
		return (x == 0.0) ? 1.0 : Math.sin(Math.PI * x) / (Math.PI * x);
	}

	private static short uLawToPcm(byte u_val_b) {
		int t;
		int u_val = ~u_val_b;
		t = ((u_val & ULAW_QUANT_MASK) << 3) + ULAW_BIAS;
		t <<= (u_val & ULAW_SEG_MASK) >> ULAW_SEG_SHIFT;
		return (short) ((u_val & ULAW_SIGN_BIT) != 0 ? (ULAW_BIAS - t) : (t - ULAW_BIAS));
	}

	private static short aLawToPcm(byte signedChar) {
		int alaw = ((int) signedChar) & 0xff;
		int i;
		int seg;

		alaw ^= ALOW_AMI_MASK;
		i = ((alaw & 0x0F) << 4);
		seg = ((alaw & 0x70) >> 4);
		if (seg != 0) {
			i = (i + 0x100) << (seg - 1);
		}
		return (short) (((alaw & 0x80) != 0) ? i : -i);
	}

	private static short byteToShort(byte sampleByte) {
		int baseShort = (short) sampleByte;
		return (short) (baseShort * 256);
	}
}


