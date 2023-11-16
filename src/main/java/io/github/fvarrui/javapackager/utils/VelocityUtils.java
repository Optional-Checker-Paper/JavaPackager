package io.github.fvarrui.javapackager.utils;

import static org.apache.commons.io.FileUtils.writeStringToFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.apache.velocity.util.StringBuilderWriter;

import io.github.fvarrui.javapackager.packagers.Packager;

/**
 * Velocity utils 
 */
public class VelocityUtils {

	private static File assetsDir = new File("assets");
	private static VelocityEngine velocityEngine = null;
	private static List<io.github.fvarrui.javapackager.model.Template> templates;
	
	private VelocityUtils() {}
	
	public static void init(Packager packager) {
		assetsDir = packager.getAssetsDir();
		templates = packager.getTemplates() != null ? packager.getTemplates() : new ArrayList<>();
		// add default template configs
		if (templates.stream().noneMatch(t -> t.getName().equals("windows/iss.vtl"))) {
			templates.add(new io.github.fvarrui.javapackager.model.Template("windows/iss.vtl", true));
		}
	}

	private static VelocityEngine getVelocityEngine() {
		
		if (velocityEngine == null) {
			
			velocityEngine = new VelocityEngine();
			
			// specify resource loaders to use
			velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "file,class");
			
			// for the loader 'file', set the FileResourceLoader as the class to use and use 'assets' directory for templates
			velocityEngine.setProperty("resource.loader.file.class", FileResourceLoader.class.getName());
			velocityEngine.setProperty("resource.loader.file.path", assetsDir.getAbsolutePath());
			
			// for the loader 'class', set the ClasspathResourceLoader as the class to use
			velocityEngine.setProperty("resource.loader.class.class", ClasspathResourceLoader.class.getName());
			
			velocityEngine.init();
			
		}
		
		return velocityEngine;
	}
	
	private static String render(String templatePath, Object info) throws Exception {
		VelocityContext context = new VelocityContext();
		context.put("features", new ArrayList<String>());
		context.put("GUID", UUID.class);
		context.put("StringUtils", org.apache.commons.lang3.StringUtils.class);
		context.put("info", info);
		Template template = getVelocityEngine().getTemplate(templatePath, "UTF-8");
		StringBuilderWriter writer = new StringBuilderWriter();
		template.merge(context, writer);		
		return writer.toString();
	}
	
	private static void render(String templatePath, File output, Object info, boolean includeBom) throws Exception {
		String data = render(templatePath, info);
		data = StringUtils.dosToUnix(data);
		if (!includeBom) {
			writeStringToFile(output, data, "UTF-8");
		} else {
			FileUtils.writeStringToFileWithBOM(output, data);
		}
	}

	@SuppressWarnings("optional:prefer.map.and.orelse") // prefer-map-and-orelse
	public static void render(String templatePath, File output, Object info) throws Exception {
		Optional<io.github.fvarrui.javapackager.model.Template> template = templates.stream().filter(t -> t.getName().equals(templatePath)).findFirst();
		render(templatePath, output, info, template.isPresent() ? template.get().isBom() : false);
	}

}
