package net.slipcor.pvparena.ncloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.slipcor.pvparena.ncloader.NCBLoadable.LoadResult;
import net.slipcor.pvparena.ncloader.NCBLoadable.LoadResult.Result;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/*     Copyright (C) 2012  Nodin Chan <nodinchan@live.com>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Loader - Loader base for loading Loadables
 *
 * @author NodinChan
 *
 * @param <T>
 *            A loadable class
 */
@SuppressWarnings("unchecked")
public class NCBLoader<T extends NCBLoadable> implements Listener {

	private final Plugin plugin;

	private final File dir;

	private ClassLoader loader;

	private final Object[] paramTypes;
	private final Class<?>[] ctorParams;

	private final ArrayList<File> files;
	private final List<T> loadables;

	public NCBLoader(Plugin plugin, File dir, Object... paramTypes) {
		this.plugin = plugin;
		this.dir = dir;
		this.paramTypes = paramTypes;
		this.files = new ArrayList<File>();
		this.loadables = new ArrayList<T>();

		for (File f : dir.listFiles(new FileExtensionFilter(".jar"))) {
			files.add(f);
		}

		List<Class<?>> constructorParams = new ArrayList<Class<?>>();

		for (Object paramType : paramTypes)
			constructorParams.add(paramType.getClass());

		this.ctorParams = constructorParams.toArray(new Class<?>[0]);

		List<URL> urls = new ArrayList<URL>();

		for (File file : files)
			try {
				urls.add(file.toURI().toURL());
			} catch (MalformedURLException e) {
			}

		this.loader = URLClassLoader.newInstance(urls.toArray(new URL[0]),
				plugin.getClass().getClassLoader());
	}

	/**
	 * Gets the Logger
	 *
	 * @return The Logger
	 */
	public Logger getLogger() {
		return plugin.getLogger();
	}

	/**
	 * Loads the Loadables
	 *
	 * @return List of loaded loadables
	 */
	public final List<T> load(Class<? extends NCBLoadable> classType) {
		for (File file : files) {
			try {
				final JarFile jarFile = new JarFile(file);
				String mainClass = null;

				if (jarFile.getEntry("path.yml") != null) {
					JarEntry element = jarFile.getJarEntry("path.yml");
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(
									jarFile.getInputStream(element)));
					mainClass = reader.readLine().substring(12);
				}

				if (mainClass != null) {
					Class<?> clazz = Class.forName(mainClass, true, loader);

					if (clazz != null) {
						Class<? extends NCBLoadable> loadableClass = clazz
								.asSubclass(classType);
						Constructor<? extends NCBLoadable> ctor = loadableClass
								.getConstructor(ctorParams);
						T loadable = (T) ctor.newInstance(paramTypes);

						LoadResult result = loadable.init();

						if (result.getResult().equals(Result.SUCCESS)) {
							loadables.add(loadable);

							NCBLoadEvent<T> event = new NCBLoadEvent<T>(plugin,
									loadable, jarFile);
							plugin.getServer().getPluginManager()
									.callEvent(event);
							continue;
						}

						String reason = result.getReason();

						if (reason != null && !reason.isEmpty()) {
							getLogger().log(
									Level.INFO,
									"The JAR file " + file.getName()
											+ " is not initialised: " + reason);
						}
					} else {
						jarFile.close();
						throw new ClassNotFoundException();
					}

				} else {
					jarFile.close();
					throw new ClassNotFoundException();
				}

			} catch (ClassCastException e) {
				getLogger().log(
						Level.WARNING,
						"The JAR file " + file.getPath()
								+ " is in the wrong directory");
				getLogger().log(Level.WARNING,
						"The JAR file " + file.getName() + " failed to load");

			} catch (ClassNotFoundException e) {
				getLogger().log(Level.WARNING, "Invalid path.yml");
				getLogger().log(Level.WARNING,
						"The JAR file " + file.getName() + " failed to load");

			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Unknown cause");
				getLogger().log(Level.WARNING,
						"The JAR file " + file.getName() + " failed to load");
			}
		}

		return loadables;
	}

	/**
	 * Reloads the Loader
	 */
	public List<T> reload(Class<? extends NCBLoadable> classType) {
		unload();

		List<URL> urls = new ArrayList<URL>();
		files.clear();
		for (String loadableFile : dir.list()) {
			if (loadableFile.endsWith(".jar")) {
				File file = new File(dir, loadableFile);
				files.add(file);
				try {
					urls.add(file.toURI().toURL());
				} catch (MalformedURLException e) {
				}
			}
		}

		this.loader = URLClassLoader.newInstance(urls.toArray(new URL[urls
				.size()]), plugin.getClass().getClassLoader());

		return load(classType);
	}

	/**
	 * Unloads the Loader
	 */
	public void unload() {
		loadables.clear();
	}
}