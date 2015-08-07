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
	
	public SVGtoSound() {
		this.loadFiles();
	}

	public class InfoArchivo {
		String nombre;
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
			System.out.print(infoArchivo.nombre);
			
			try {

				// Esto crea la info a partir del archivo
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(file);
				doc.getDocumentElement().normalize();

				
				
				System.out.println("Root element :" + doc.getDocumentElement().getAttribute("height"));

				// Recupera la lista de elementos de una categoria (en nuestro caso lineas)
				NodeList nList = doc.getElementsByTagName("line");
				for (int i = 0; i < nList.getLength(); i++) {
					Node nNode = nList.item(i);
					System.out.println(file.getName() + " : " + nNode.getNodeName());
					if (nNode.getNodeType() == Node.ELEMENT_NODE) { // No se bien que significa eso!
						Element eElement = (Element) nNode;
						System.out.println("x1 : " + eElement.getAttribute("x1"));
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
