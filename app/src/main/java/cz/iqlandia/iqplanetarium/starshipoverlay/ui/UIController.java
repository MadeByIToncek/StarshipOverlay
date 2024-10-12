package cz.iqlandia.iqplanetarium.starshipoverlay.ui;

import com.github.weisj.darklaf.LafManager;
import static com.github.weisj.darklaf.LafManager.getPreferredThemeStyle;
import com.github.weisj.darklaf.theme.DarculaTheme;
import cz.iqlandia.iqplanetarium.starshipoverlay.Config;
import cz.iqlandia.iqplanetarium.starshipoverlay.twinkly.Twinkly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class UIController implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(UIController.class);
	private final JFrame frame;
	private ZonedDateTime t0Time;
	private final Config cfg;
	private final Twinkly twinkly;
	private final List<RemAlerts> tRemAlert = new ArrayList<>(2);

	public UIController(ZonedDateTime defaultT0Time, Config cfg, Twinkly twinkly) {
		t0Time = defaultT0Time;
		this.cfg = cfg;
		this.twinkly = twinkly;
		frame = new JFrame("StarshipOverlay");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setResizable(false);
		// Directly install theme
		LafManager.install(new DarculaTheme());
		LafManager.themeForPreferredStyle(getPreferredThemeStyle());

		JPanel panel = new JPanel(new GridLayout(0, 1));
		panel.add(generateTitleCard());
		panel.add(generateStreamDelayController());
		panel.add(generateT0Controller());
		panel.add(generateColorController());

		frame.add(panel);
		frame.pack();
		frame.setSize(400, (int) frame.getSize().getHeight());
		frame.setVisible(true);
	}

		private JPanel generateColorController() {
			JPanel panel = new JPanel(new GridLayout(1, State.values().length));
			panel.setBorder(BorderFactory.createTitledBorder("Color"));
			for (State value : State.values()) {
				JButton state = new JButton();
				state.setText(value.name());
				state.setActionCommand(value.name());
				state.addActionListener(e -> {
					try {
						twinkly.setColor(value.twinklyColor);
					} catch (IOException | InterruptedException ex) {
						log.warn("Unable to talk to twinkly!");
					}
				});
				state.setBackground(value.uiColor);
				if(value == State.RUD) {
					state.setForeground(Color.WHITE);
				}
				panel.add(state);
			}
		return panel;
	}

	private JPanel generateStreamDelayController() {
		JPanel panel = new JPanel(new GridLayout(1, 2));
		panel.setBorder(BorderFactory.createTitledBorder("Incoming stream delay"));
		SpinnerNumberModel model = new SpinnerNumberModel();
		model.setValue(cfg.streamOffsetMillis);
		JSpinner spinner = new JSpinner(model);

		JComponent comp = spinner.getEditor();
		JFormattedTextField field = (JFormattedTextField) comp.getComponent(0);
		DefaultFormatter formatter = (DefaultFormatter) field.getFormatter();
		formatter.setCommitsOnValidEdit(true);
		spinner.addChangeListener(e -> {
			cfg.streamOffsetMillis = model.getNumber().longValue();
		});

		panel.add(spinner);

		return panel;
	}

	private Component generateTitleCard() {
		JPanel panel = new JPanel(new GridLayout(2, 1));
		JLabel tRem = new JLabel("Loading...", SwingConstants.CENTER);
		tRem.setFont(tRem.getFont().deriveFont(Font.BOLD,30f));
		JLabel time = new JLabel("Loading...", SwingConstants.CENTER);
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask() {
			int alertRemCycles = 0;
			@Override
			public void run() {
				ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Prague"));
				if(tRemAlert.isEmpty() && alertRemCycles == 0) {
					Duration d = Duration.between(t0Time, now.plusNanos(cfg.streamOffsetMillis * 1000000));
					boolean pos = d.isPositive();
					d = d.abs();
					tRem.setText((pos?"T+ ":"T- ") + d.toHours() + ":" + d.toMinutesPart() + ":" + d.toSecondsPart());
				} else if (alertRemCycles > 0) {
					alertRemCycles--;
				} else if(!tRemAlert.isEmpty()) {
					RemAlerts str = tRemAlert.getFirst();
					tRemAlert.remove(str);
					tRem.setText(str.message);
					alertRemCycles = str.alertcycles;
				}

				ZonedDateTime tx = now.withZoneSameInstant(ZoneId.of("America/Chicago"));

				time.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " CZ // " + tx.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " TX");
			}
		}, 0, 100);

		panel.add(tRem);
		panel.add(time);
		return panel;
	}

	private JPanel generateT0Controller() {
		JPanel panel = new JPanel(new GridLayout(1, 5));
		panel.setBorder(BorderFactory.createTitledBorder("T0 time setup"));
		JButton submit = new JButton("Submit T0");
		JTextField time = new JTextField(t0Time.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss XXXX")));

		panel.add(time);
		panel.add(submit);

		submit.addActionListener(e -> {
			t0Time = ZonedDateTime.parse(time.getText(), DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss XXXX"));
			tRemAlert.add(RemAlerts.T_ZERO_CHANGED);
		});
		return panel;
	}

	public ZonedDateTime getLaunchT0() {
		return t0Time.minusNanos(cfg.streamOffsetMillis * 1000000);
	}

	@Override
	public void close() throws IOException {
		cfg.t0 = t0Time;
	}

	private enum RemAlerts {
		T_ZERO_CHANGED("T0 has changed!", 10);

		public final String message;
		public final int alertcycles;

		RemAlerts(String message, int alertcycles) {
			this.message = message;
			this.alertcycles = alertcycles;
		}
	}

	public enum State {
		NOMINAL(new Color(0, 163, 224), new Color(0, 187, 255)),
		GO(new Color(4, 173, 4),new Color(0, 255, 0)),
		HOLD(new Color(204, 109, 1),new Color(255, 153, 0)),
		ABORT(new Color(175, 20, 0),new Color(255, 0, 0)),
		RUD(new Color(69, 81, 84),new Color(97, 97, 97));

		private final Color uiColor;
		private final Color twinklyColor;

		State(Color uiColor, Color twinklyColor) {
			this.uiColor = uiColor;
			this.twinklyColor = twinklyColor;
		}
	}
}
