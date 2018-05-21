import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class BGMus {
	//Init audio
	private static File audioFile = null;
	private static Clip audioClip;
	private AudioInputStream audioStream;
	
	public BGMus(String audio) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
		audioFile = new File(audio);
		
		audioStream = AudioSystem.getAudioInputStream(audioFile);
		AudioFormat format = audioStream.getFormat();
		DataLine.Info info = new DataLine.Info(Clip.class, format);
		audioClip = (Clip) AudioSystem.getLine(info);
		
	}
	
	public void start() throws LineUnavailableException, IOException {
		audioClip.open(audioStream);
		audioClip.loop(Clip.LOOP_CONTINUOUSLY);
	}
	public void stop() {
		audioClip.close();
	}

}
