package com.turin.tur.Main;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SVGtoSound {

	// Define algunas constantes
	public String path = "E:/Ioni/Dropbox/RieraPerez/svg";
	public File[] archivosOriginales;
	public ArrayList<InfoArchivo> archivos = new ArrayList<InfoArchivo>();
	public final boolean logScale=true;
	public final boolean fixScale=true;
	public final float maxHeigth=100;
	public final float frecMax = 8000;
	public final float frecMin = 100;
	
	
	public SVGtoSound() {
		this.loadFiles();
		this.createSounds();
	}

	/**
	 * Create a sound that change in frecuencie in logaritmic form
	 * 
	 *  
	 * @param freci Inicial value of frecuence
	 * @param frecf Final value of frecuence
	 * @param T Time (in sec) of sound
	 * @param fs samples per second of the sound 
	 * 
	 */
	private void createMusicRamp (float freci, float frecf, float T, float fs) {
		double dt = 1 / fs; // es el dt que transcurre entre sample y sample
		int N = Math.round(T * fs); // El numero de samples que hay que crear
		double[] frec = logspacelog(freci, frecf, N, 10); // Crea una escala logaritmica en base 10 que va de la frecuencia inicial a la final
		for(int i=0 ; i<frec.length ; i++) { // Lo multiplica x 2pi para trabajar con la fase
			frec[i] = frec[i]*2*Math.PI;
		}
		// Integra las freciencias instantaneas
		for(int i=1 ; i<frec.length ; i++) { // Lo multiplica x 2pi para trabajar con la fase
			frec[i] = frec[i]+frec[i-1]*dt; // El primero lo deja tal cual y despues suma hasta el ultimo
		} 
		// ahora frec es la fase instante a instante
		
		// Vamos a hacer el coseno de la fase
		for(int i=1 ; i<frec.length ; i++) {
			frec[i] = Math.cos(frec[i]); 
		} 
		
		// ACA hay que agregar la rampoa tukeywin
		
		// Ahora vamos a grabarla a un archivo
		// WavFile wavFile = WavFile.newWavFile(new File(args[0]), 2, numFrames, 16, sampleRate);
		
	}

	/**
	 * log in any base
	 * 
	 * @param base The base of log
	 * @param num The argument of log
	 * @return
	 */
	public double logOfBase(double base, double num) {
	    return Math.log(num) / Math.log(base);
	}
	
	/**
     * generates n logarithmically-spaced points between d1 and d2 using the
     * provided base. d1 and d2 are the expected start and end.
     * 
     * @param d1 The min value expected
     * @param d2 The max value expected
     * @param n The number of points to generated
     * @param base the logarithmic base to use
     * @return an array of lineraly space points.
     */
	
    public strictfp double[] logspacelog(double d1, double d2, int n, double base) {
        double[] y = new double[n];
        double[] p = linspace(logOfBase(base,d1), logOfBase(base,d2), n);
        for(int i = 0; i < y.length - 1; i++) {
            y[i] = Math.pow(base, p[i]);
        }
        y[y.length - 1] = Math.pow(base, logOfBase(base,d2));
        return y;
    }

	/**
     * generates n logarithmically-spaced points between d1 and d2 using the
     * provided base.
     * 
     * Nota! Esto parece estar muy mal!
     * 
     * @param d1 The min value
     * @param d2 The max value
     * @param n The number of points to generated
     * @param base the logarithmic base to use
     * @return an array of lineraly space points.
     */
	
    public strictfp double[] logspace(double d1, double d2, int n, double base) {
        double[] y = new double[n];
        double[] p = linspace(d1, d2, n);
        for(int i = 0; i < y.length - 1; i++) {
            y[i] = Math.pow(base, p[i]);
        }
        y[y.length - 1] = Math.pow(base, d2);
        return y;
    }

    /**
     * generates n linearly-spaced points between d1 and d2.
     * @param d1 The min value
     * @param d2 The max value
     * @param n The number of points to generated
     * @return an array of lineraly space points.
     */
    public static strictfp double[] linspace(double d1, double d2, int n) {

        double[] y = new double[n];
        double dy = (d2 - d1) / (n - 1);
        for(int i = 0; i < n; i++) {
            y[i] = d1 + (dy * i);
        }

        return y;

    }
    
	private void createSounds() {
		double[] escala = logspacelog (100,8000,10,10);
		System.out.println("empieza en: " + escala[0]);
		System.out.println("medio en: " + escala[5]);
		System.out.println("termina en: " + escala[escala.length-1]);
	}

	public class InfoArchivo {
		String nombre;
		float ancho;
		float alto;
		
		ArrayList<Linea> lineas = new ArrayList<Linea>();
	}
	
	public class Linea {
		float xi;
		float xf;
		float yi;
		float yf;
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
				for (int i = 0; i < nList.getLength(); i++) { 					// itera sobre cada linea
					if (nList.item(i).getNodeType() == Node.ELEMENT_NODE) { // Se fija que el nodo sea un elemento (si no entendi mal eso deferencia de los atributos, etc)
						Linea linea = new Linea();
						Element eElement = (Element) nList.item(i);
						
						linea.xi = Float.parseFloat(eElement.getAttribute("x1"));
						linea.xf = Float.parseFloat(eElement.getAttribute("x2"));
						linea.yi = Float.parseFloat(eElement.getAttribute("y1"));
						linea.yf = Float.parseFloat(eElement.getAttribute("y2"));
						
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
