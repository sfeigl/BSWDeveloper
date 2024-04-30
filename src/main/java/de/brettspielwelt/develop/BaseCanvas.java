package de.brettspielwelt.develop;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.FileInputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import de.brettspielwelt.game.Board;

public class BaseCanvas extends JPanel  {

	private final HTMLWrapper game;
	private final JPanel bottomComponent;
	private final Timer timer;

	public BaseCanvas(int nr) {
		game=new Board();
		game.addMouseListener(game);
		game.addMouseMotionListener(game);
		try {
			game.props.load(new FileInputStream("assets/Text.string"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		game.init();
		game.spielerNr=nr;
		Main.info.setBoard(game);
		
		timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                game.run();
            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000/24);
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(game,BorderLayout.CENTER);
        
        bottomComponent=new JPanel();
		bottomComponent.add( new JButton("Console"));
        add(bottomComponent,BorderLayout.SOUTH);
	}

	public void removeNotify() {
		Main.info.removeBoard(game);
		timer.cancel();
	}

	public Component getBottomComponent() {
		return bottomComponent;
	}

}
