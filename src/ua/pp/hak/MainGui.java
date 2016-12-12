package ua.pp.hak;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

public class MainGui extends JFrame {

	private static final long serialVersionUID = -4115751417721782411L;
	private Thread worker;
	private final String root = "update/";

	private JTextArea outText;
	private JButton cancle;
	private JButton launch;
	private JScrollPane sp;
	private JPanel pan1;
	private JPanel pan2;

	public MainGui() {
		initComponents();
		outText.setText("Contacting Download Server...");
		download();
	}

	private void initComponents() {
		setTitle("Update");
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setIconImage(new ImageIcon(getClass().getResource("/image/templex-updater-big.png")).getImage());
		pan1 = new JPanel();
		pan1.setLayout(new BorderLayout());

		pan2 = new JPanel();
		pan2.setLayout(new FlowLayout());

		outText = new JTextArea();
		Font font = new Font("Consolas", Font.PLAIN, 14);
		outText.setFont(font);
		outText.setEditable(false);
		sp = new JScrollPane();
		sp.setViewportView(outText);

		launch = new JButton("Launch App");
		launch.setEnabled(false);
		launch.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				launch();
			}
		});
		pan2.add(launch);

		cancle = new JButton("Cancel Update");
		cancle.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		pan2.add(cancle);
		pan1.add(sp, BorderLayout.CENTER);
		pan1.add(pan2, BorderLayout.SOUTH);
		add(pan1);
		pack();
		this.setSize(500, 400);
		setLocationRelativeTo(null);
	}

	private void download() {
		worker = new Thread(new Runnable() {
			public void run() {
				try {
					downloadFile(getDownloadLinkFromHost());
					unzip();
					copyFiles(new File(root), new File("..").getAbsolutePath());
					cleanup();
					launch.setEnabled(true);
					cancle.setText("Close Window");
					outText.setText(outText.getText() + "\nUpdate Finished!");
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(null, "An error occured while preforming update!");
				}
			}
		});
		worker.start();
	}

	private void launch() {
		// String[] run = { "java", "-jar", "update app.jar" };
		String[] cmdArray = new String[] { "../JobsParser.exe" };
		try {
			Runtime.getRuntime().exec(cmdArray);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.exit(0);
	}

	private void cleanup() {
		outText.setText(outText.getText() + "\nPreforming clean up...");
		File f = new File("update.zip");
		f.delete();
		remove(new File(root));
		new File(root).delete();
	}

	private void remove(File f) {
		File[] files = f.listFiles();
		for (File ff : files) {
			if (ff.isDirectory()) {
				remove(ff);
				ff.delete();
			} else {
				ff.delete();
			}
		}
	}

	private void copyFiles(File f, String dir) throws IOException {
		File[] files = f.listFiles();
		for (File ff : files) {
			if (ff.isDirectory()) {
				new File(dir + "/" + ff.getName()).mkdir();
				copyFiles(ff, dir + "/" + ff.getName());
			} else {
				copy(ff.getAbsolutePath(), dir + "/" + ff.getName());
			}

		}
	}

	public void copy(String srFile, String dtFile) throws FileNotFoundException, IOException {

		File f1 = new File(srFile);
		File f2 = new File(dtFile);

		InputStream in = new FileInputStream(f1);

		OutputStream out = new FileOutputStream(f2);

		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	private void unzip() throws IOException {
		int BUFFER = 2048;
		BufferedOutputStream dest = null;
		BufferedInputStream is = null;
		ZipEntry entry;
		ZipFile zipfile = new ZipFile("update.zip");
		Enumeration<? extends ZipEntry> e = zipfile.entries();
		(new File(root)).mkdir();
		while (e.hasMoreElements()) {
			entry = (ZipEntry) e.nextElement();
			outText.setText(outText.getText() + "\nExtracting: " + entry);
			if (entry.isDirectory())
				(new File(root + entry.getName())).mkdir();
			else {
				(new File(root + entry.getName())).createNewFile();
				is = new BufferedInputStream(zipfile.getInputStream(entry));
				int count;
				byte data[] = new byte[BUFFER];
				FileOutputStream fos = new FileOutputStream(root + entry.getName());
				dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = is.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
				is.close();
			}
		}
		zipfile.close();

	}

	private void downloadFile(String link) throws MalformedURLException, IOException {
		URL url = new URL(link);
		URLConnection conn = url.openConnection();
		InputStream is = conn.getInputStream();
		long max = conn.getContentLength();
		outText.setText(outText.getText() + "\n" + "Downloding file...\nUpdate Size(compressed): " + max + " Bytes");
		BufferedOutputStream fOut = new BufferedOutputStream(new FileOutputStream(new File("update.zip")));
		byte[] buffer = new byte[32 * 1024];
		int bytesRead = 0;
		// int in = 0;
		while ((bytesRead = is.read(buffer)) != -1) {
			// in += bytesRead;
			fOut.write(buffer, 0, bytesRead);
		}
		fOut.flush();
		fOut.close();
		is.close();
		outText.setText(outText.getText() + "\nDownload Complete!");

	}

	private String getDownloadLinkFromHost() throws MalformedURLException, IOException {
		String path = "http://tpad.hak.pp.ua/url.html";
		URL url = new URL(path);

		InputStream html = null;

		html = url.openStream();

		int c = 0;
		StringBuilder buffer = new StringBuilder("");

		while (c != -1) {
			c = html.read();
			buffer.append((char) c);

		}
		return buffer.substring(buffer.indexOf("[url]") + 5, buffer.indexOf("[/url]"));
	}

	public static void main(String args[]) {

		if (args.length != 1 || !args[0].equals("-update")) {
			System.out.println("Usage:\n" + "ua.pp.hak.MainGui update\n");
			return;
		}

		// change look and feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new MainGui().setVisible(true);
			}
		});
	}

}
