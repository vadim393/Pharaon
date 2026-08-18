package tech.onetap.util.render.providers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public final class ResourceProvider {

	private static final Gson GSON = new Gson();

	private ResourceProvider() {
	}
	
	public static Identifier getShaderIdentifier(String name) {
		return Identifier.of("mre", "core/" + name);
	}

	public static JsonObject toJson(Identifier identifier) {
		return JsonParser.parseString(toString(identifier)).getAsJsonObject();
	}

	public static <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
		return GSON.fromJson(toString(identifier), clazz);
	}

	public static String toString(Identifier identifier) {
		return toString(identifier, "\n");
	}
	
	public static String toString(Identifier identifier, String delimiter) {
		try(InputStream inputStream = openStream(identifier);
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			return reader.lines().collect(Collectors.joining(delimiter));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static InputStream openStream(Identifier identifier) throws IOException {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.getResourceManager() != null) {
			return client.getResourceManager().open(identifier);
		}

		String classpathPath = "assets/" + identifier.getNamespace() + "/" + identifier.getPath();
		InputStream classpathStream = ResourceProvider.class.getClassLoader().getResourceAsStream(classpathPath);
		if (classpathStream != null) {
			return classpathStream;
		}

		throw new IOException("Resource is unavailable before client resource manager init: " + identifier);
	}

}
