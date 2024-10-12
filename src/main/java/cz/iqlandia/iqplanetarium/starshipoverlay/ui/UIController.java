package cz.iqlandia.iqplanetarium.starshipoverlay.ui;

import com.github.weisj.darklaf.LafManager;
import static com.github.weisj.darklaf.LafManager.getPreferredThemeStyle;
import com.github.weisj.darklaf.theme.DarculaTheme;
import cz.iqlandia.iqplanetarium.starshipoverlay.Config;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
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
	private final JFrame frame;
	private ZonedDateTime t0Time;
	private final Config cfg;
	private final List<RemAlerts> tRemAlert = new ArrayList<>(2);

	public UIController(ZonedDateTime defaultT0Time, Config cfg) {
		t0Time = defaultT0Time;
		this.cfg = cfg;
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

		frame.add(panel);
		frame.pack();
		frame.setSize(400, (int) frame.getSize().getHeight());
		frame.setVisible(true);
	}

	private JPanel generateStreamDelayController() {
		JPanel panel = new JPanel(new GridLayout(1, 5));
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
}
