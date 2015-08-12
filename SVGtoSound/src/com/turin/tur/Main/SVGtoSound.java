package com.turin.tur.Main;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.turin.tur.wave.WavFile;
import com.turin.tur.wave.WavFileException;

public class SVGtoSound {

	// Define algunas constantes
	public String path = "./testFiles/";
	public File[] archivosOriginales;
	public ArrayList<InfoArchivo> archivos = new ArrayList<InfoArchivo>();
	public final boolean logScale = true;
	public final boolean fixScale = true;
	public final float maxHeigth = 100;
	public final float frecMax = 8000;
	public final float frecMin = 100;
	public final float time = 5; // in secs
	public final boolean fixedTime = true; // indicate if the length of sound must be the variable time
	public final float secByPix = 5 / 100f; // indicate how many sec are represented by pixel.
	public final int fs = 44100; // hz of the sound
	public final float base = 10; // base of the log scale

	public SVGtoSound() {
		this.loadFiles();
		this.createSounds();
	}

	/**
	 * Create a sound that change in frecuencie in logaritmic form
	 * 
	 * 
	 * @param freci
	 *            Inicial value of frecuence
	 * @param frecf
	 *            Final value of frecuence
	 * @param T
	 *            Time (in sec) of sound
	 * @param fs
	 *            samples per second of the sound
	 * @return
	 * 
	 */
	private double[] createMusicRamp(float freci, float frecf, float ti, float tf) {
		double dt = 1 / (double) fs; // es el dt que transcurre entre sample y sample
		int N = Math.round((tf - ti) * fs); // El numero de samples que hay que crear
		double[] frec = logspacelog(freci, frecf, N, base); // Crea una escala logaritmica en base 10 que va de la frecuencia inicial a la final
		for (int i = 0; i < frec.length; i++) { // Lo multiplica x 2pi para trabajar con la fase
			frec[i] = frec[i] * 2 * Math.PI;
		}
		// Integra las freciencias instantaneas
		for (int i = 1; i < frec.length; i++) { 
			frec[i] = frec[i - 1] + frec[i] * dt; // El primero lo deja tal cual y despues suma hasta el ultimo
		}
		// ahora frec es la fase instante a instante

		// Vamos a hacer el coseno de la fase
		for (int i = 0; i < frec.length; i++) {
			frec[i] = Math.cos(frec[i]);
		}
		// Ya esta creado el sonido

		frec = tukeywin(frec, 0.02);

		return frec;

	}

	/**
	 * Aplica una funcion tipo tukeywin (que suaviza los extremos)
	 * 
	 * @param frec
	 *            Es el array de datos de entrada
	 * @param d
	 *            es el parametro de cuanto suavizar. Si es un numero menor que uno asume que es el porcentaje (por unidad), si es mayor que es numero de frames
	 *            a suavizar
	 * @return Devuelve el input suavizado
	 */
	private double[] tukeywin(double[] frec, double d) {
		int framesMaximos;

		if (d >= 1) { // recupera cuantos frames tiene que suavizar
			framesMaximos = (int) d;
		} else {
			framesMaximos = (int) ((double) frec.length * d);
		}

		if (framesMaximos != 0) {

			// suaviza los frames del inicio
			for (int i = 0; i < framesMaximos; i++) {
				double fase = (double) i / framesMaximos * Math.PI;
				double factor = (-Math.cos(fase) + 1) / 2;
				frec[i] = frec[i] * factor;
			}

			// suaviza los frames del final
			for (int i = 0; i < framesMaximos; i++) {
				double fase = (double) i / framesMaximos * Math.PI;
				double factor = (-Math.cos(fase) + 1) / 2;
				frec[frec.length - 1 - i] = frec[frec.length - 1 - i] * factor;
			}
		}
		return frec;
	}

	/**
	 * log in any base
	 * 
	 * @param base
	 *            The base of log
	 * @param num
	 *            The argument of log
	 * @return
	 */
	public double logOfBase(double base, double num) {
		return Math.log(num) / Math.log(base);
	}

	/**
	 * generates n logarithmically-spaced points between d1 and d2 using the provided base. d1 and d2 are the expected start and end.
	 * 
	 * @param d1
	 *            The min value expected
	 * @param d2
	 *            The max value expected
	 * @param n
	 *            The number of points to generated
	 * @param base
	 *            the logarithmic base to use
	 * @return an array of lineraly space points.
	 */

	public strictfp double[] logspacelog(double d1, double d2, int n, double base) {
		double[] y = new double[n];
		double[] p = linspace(logOfBase(base, d1), logOfBase(base, d2), n);
		for (int i = 0; i < y.length - 1; i++) {
			y[i] = Math.pow(base, p[i]);
		}
		y[y.length - 1] = Math.pow(base, logOfBase(base, d2));
		return y;
	}

	/**
	 * generates n linearly-spaced points between d1 and d2.
	 * 
	 * @param d1
	 *            The min value
	 * @param d2
	 *            The max value
	 * @param n
	 *            The number of points to generated
	 * @return an array of lineraly space points.
	 */
	public static strictfp double[] linspace(double d1, double d2, int n) {

		double[] y = new double[n];
		double dy = (d2 - d1) / (n - 1);
		for (int i = 0; i < n; i++) {
			y[i] = d1 + (dy * i);
		}

		return y;

	}

	/**
	 * Create the secuence of mp3 from the info in files
	 */
	public void createSounds() {

		if (archivos != null) {
			for (InfoArchivo archivo : archivos) {

				float timefactor;
				if (fixedTime) {
					timefactor = time / (secByPix * archivo.ancho);
				} else {
					timefactor = 1;
				}

				// create the base full length secuence for file
				int N = (int) (secByPix * archivo.ancho * timefactor * fs);
				double[] secuence = new double[N];

				for (Linea linea : archivo.lineas) {

					double[] rampa = createMusicRamp(linea.freci, linea.frecf, linea.ti, linea.tf);
					for (int i = 0; i < rampa.length; i++) {
						rampa[i] = rampa[i] / archivo.lineas.size();
					}
					int frameInicial = (int) linea.ti * fs;
					for (int i = 0; i < rampa.length; i++) {
						secuence[i + frameInicial] = secuence[i + frameInicial] + rampa[i];
					}
				}

				File file = new File(path, archivo.nombre + ".wav");
				// Create a wav file with the name specified as the first argument
				try {
					WavFile wavFile = WavFile.newWavFile(file, 1, secuence.length, 16, fs);
					wavFile.writeFrames(secuence, secuence.length);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (WavFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	private void findParameters(Linea linea, float ancho, float alto) {

		// se fija que la linea vaya de izquierda a derecha y sino lo corrige
		if (linea.xi > linea.xf) {
			float temp = linea.xi;
			linea.xi = linea.xf;
			linea.xf = temp;
			temp = linea.yi;
			linea.yi = linea.yf;
			linea.yf = temp;
		}

		// get scale correction for file
		float timefactor;
		if (fixedTime) {
			timefactor = time / (secByPix * ancho);
		} else {
			timefactor = 1;
		}
		float frecfactor;
		if (fixScale) {
			frecfactor = maxHeigth / alto;
		} else {
			frecfactor = 1;
		}

		float xi = linea.xi * timefactor;
		float xf = linea.xf * timefactor;
		// get the initial and final frec based in conditions
		float yi = linea.yi * frecfactor;
		float yf = linea.yf * frecfactor;

		// corrige que el SVG toma el cero arriba
		yi = maxHeigth - yi;
		yf = maxHeigth - yf;

		linea.ti = xi * secByPix;
		linea.tf = xf * secByPix;

		if (logScale) {
			/*
			 * partimos de la forma frec = A*exp(y) + B y sabemos que frecMax = A*base^(maxHeigth) + B y que frecMin = A * base^(0) + B
			 * 
			 * haciendo cuentas queda que:
			 * 
			 * A = (FrecM - frecm) / (base^M-base^m) B = [ (frecM + frecm) - A (base^M + base^m) ] / 2
			 * 
			 * el problema es que se hacen numeros muy grandes de manejar por lo tanto vamos a reescalar el problema de manera que la yMax sea 1 de esta forma
			 * nos aseguramos que nunca va a dar resultados inmanejables.
			 */

			double A = (frecMax - frecMin) / (Math.pow(base, 1) - Math.pow(base, 0));
			double B = ((frecMax + frecMin) - A * (Math.pow(base, 1) + Math.pow(base, 0))) / 2;
			linea.freci = (float) (A * Math.pow(base, yi / maxHeigth) + B);
			linea.frecf = (float) (A * Math.pow(base, yf / maxHeigth) + B);
		} else {
			linea.freci = frecMin + yi * (frecMax - frecMin) / maxHeigth;
			linea.frecf = frecMin + yf * (frecMax - frecMin) / maxHeigth;
		}
	}

	public class InfoArchivo {
		String nombre;
		float ancho;
		float alto;

		ArrayList<Linea> lineas = new ArrayList<Linea>();
	}

	public class Linea {
		// Parametros que se leen desde cada linea del SVG
		float xi;
		float xf;
		float yi;
		float yf;
		// Parametros que se calculan en funcion de parametros generales de la imagen y del programa
		float freci;
		float frecf;
		float ti;
		float tf;

	}

	private void loadFiles() {

		// Primero busca la lista de archivos de interes
		File dir = new File(path);
		archivosOriginales = dir.listFiles(new SvgFileFilter());

		for (File file : archivosOriginales) { // El loop por cada archivo
			// Crea la entrada perteneciente al archivo
			InfoArchivo infoArchivo = new InfoArchivo();
			infoArchivo.nombre = file.getName().replaceFirst("[.][^.]+$", "");

			try {

				// Esto crea la info a partir del archivo
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(file);
				doc.getDocumentElement().normalize();

				// Recupera el ancho y el alto
				infoArchivo.alto = Float.parseFloat(doc.getDocumentElement().getAttribute("height"));
				infoArchivo.ancho = Float.parseFloat(doc.getDocumentElement().getAttribute("width"));

				// Recupera la lista de elementos de una categoria (en nuestro caso lineas)
				NodeList nList = doc.getElementsByTagName("line");
				for (int i = 0; i < nList.getLength(); i++) { // itera sobre cada linea
					if (nList.item(i).getNodeType() == Node.ELEMENT_NODE) { // Se fija que el nodo sea un elemento (si no entendi mal eso deferencia de los atributos, etc)
						Linea linea = new Linea();
						Element eElement = (Element) nList.item(i);

						linea.xi = Float.parseFloat(eElement.getAttribute("x1"));
						linea.xf = Float.parseFloat(eElement.getAttribute("x2"));
						linea.yi = Float.parseFloat(eElement.getAttribute("y1"));
						linea.yf = Float.parseFloat(eElement.getAttribute("y2"));

						findParameters(linea, infoArchivo.ancho, infoArchivo.alto);
						infoArchivo.lineas.add(linea);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Agrega la info del archivo a la lista general
			archivos.add(infoArchivo);
		}
	}

	public class SvgFileFilter implements FileFilter
	{
		private final String[] okFileExtensions =
				new String[] { "svg" };

		public boolean accept(File file)
		{
			for (String extension : okFileExtensions)
			{
				if (file.getName().toLowerCase().endsWith(extension))
				{
					return true;
				}
			}
			return false;
		}
	}

}
