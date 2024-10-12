package cz.iqlandia.iqplanetarium.starshipoverlay.twinkly;

import cz.iqlandia.iqplanetarium.starshipoverlay.twinkly.data.Mode;
import org.json.JSONObject;

import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Random;

public class Twinkly {
	private final String baseAddress;
	private byte[] token;
	private final HttpClient client;

	public Twinkly(String baseAddress) {
		this.baseAddress = baseAddress;
		client = HttpClient.newHttpClient();
	}

	public boolean login() throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseAddress + "/xled/v1/login"))
				.POST(HttpRequest.BodyPublishers.ofString(new JSONObject().put("challenge", getChallenge()).toString(4)))
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		JSONObject loginResponse = new JSONObject(response.body());

		setToken(loginResponse.getString("authentication_token"));

		request = HttpRequest.newBuilder()
				.uri(URI.create(baseAddress + "/xled/v1/verify"))
				.header("X-Auth-Token", getB64Token())
				.POST(HttpRequest.BodyPublishers.ofString(new JSONObject().put("challenge-response", loginResponse.getString("challenge-response")).toString(4)))
				.build();

		HttpResponse<Void> resp2 = client.send(request, HttpResponse.BodyHandlers.discarding());
		return resp2.statusCode() == 200;
	}

	public boolean change_mode(Mode target) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseAddress + "/xled/v1/led/mode"))
				.header("X-Auth-Token", getB64Token())
				.POST(HttpRequest.BodyPublishers.ofString(new JSONObject().put("mode", target.name().toLowerCase()).toString(4)))
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return (response.statusCode() == 200) && new JSONObject(response.body()).getInt("code") == 1000;
	}

	public boolean change_mode(Mode target, int effect_id) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseAddress + "/xled/v1/led/mode"))
				.header("X-Auth-Token", getB64Token())
				.POST(HttpRequest.BodyPublishers.ofString(new JSONObject().put("mode", target.name().toLowerCase()).put("effect_id", effect_id).toString(4)))
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return (response.statusCode() == 200) && new JSONObject(response.body()).getInt("code") == 1000;
	}

	public Mode getMode() throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseAddress + "/xled/v1/led/mode"))
				.header("X-Auth-Token", getB64Token())
				.GET()
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if((response.statusCode() != 200) || new JSONObject(response.body()).getInt("code") != 1000) {
			throw new IOException("The device has returned the code " + new JSONObject(response.body()).getInt("code"));
		}
		return Mode.valueOf(new JSONObject(response).getString("mode"));
	}

	public Color getColor() throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseAddress + "/xled/v1/led/mode"))
				.header("X-Auth-Token", getB64Token())
				.GET()
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if((response.statusCode() != 200) || new JSONObject(response.body()).getInt("code") != 1000) {
			throw new IOException("The device has returned the code " + new JSONObject(response.body()).getInt("code"));
		}
		JSONObject o = new JSONObject(response.body());
		return new Color(o.getInt("red"), o.getInt("green"), o.getInt("blue"));
	}

	public boolean setColor(Color c) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseAddress + "/xled/v1/led/mode"))
				.header("X-Auth-Token", getB64Token())
				.POST(HttpRequest.BodyPublishers.ofString(new JSONObject().put("red", c.getRed()).put("green", c.getGreen()).put("blue", c.getBlue()).toString(4)))
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return (response.statusCode() == 200) && new JSONObject(response.body()).getInt("code") == 1000;
	}

	private void setToken(String b64token) {
		token = Base64.getDecoder().decode(b64token);
	}

	private String getB64Token() {
		return Base64.getEncoder().encodeToString(token);
	}

	private byte[] getBinaryToken() {
		return token;
	}

	private String getChallenge() {
		Random r = new Random();
		byte [] ctoken = new byte[32];
		r.nextBytes(ctoken);
		return Base64.getEncoder().encodeToString(ctoken);
	}
}