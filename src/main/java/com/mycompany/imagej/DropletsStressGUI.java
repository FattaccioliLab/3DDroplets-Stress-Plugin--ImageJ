package com.mycompany.imagej;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

import customnode.CustomPointMesh;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij3d.Image3DUniverse;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Vertices;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class DropletsStressGUI extends JFrame {
	
	private static final long serialVersionUID = 1L;


	private OpService opService;
    //---top left panel---//
	 private JPanel leftPanel;
    private JPanel topLeftPanel;
    private JPanel originalImage;
    private JButton selectPhotoButton;
    private ImagePlus originalImagePlus;
    //---bottom left panel---//
    private JPanel bottomLeftPanel;
    private SpinnerModel smoothingSigmaField;
    private SpinnerModel resamplingLength;
    private SpinnerModel maxDegreSH;
    private JButton previewButton;
    //---right panel---//
    private JPanel rightPanel;
    private JPanel binarizedImage;
    private JPanel marchingCubeImage;
    private JPanel pointCloudImage;
    private JPanel spherical_harmonicsImage;
    private JButton finalizeButton;
    private static JProgressBar progressBar;
    //---point display panel---//
	private JPanel pointsDisplayPanel;
	private JTextArea pointsTextArea;
    //---results---//
    private List<Point3f> fitted_points;
    private List<Point3f> resampled_points;
    
    //largeur et hauteur d'image
    int width = 400;
    int height = 400;


	
   
    public DropletsStressGUI(OpService opService) {
        setTitle("Droplets Stress Plugin");
        setSize(1200, 1000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        this.opService = opService;

        // Create the left panel
        leftPanel = new JPanel(new BorderLayout());

        // Create the top left panel with fixed size    
        topLeftPanel = new JPanel();
        topLeftPanel.setPreferredSize(new Dimension(350, 290)); 
        this.originalImage = new JPanel(new BorderLayout());
        this.originalImage.setPreferredSize(new Dimension(340, 250)); 
        this.originalImage.setBackground(Color.WHITE); 
        this.originalImage.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
				"Original Image", TitledBorder.CENTER, TitledBorder.TOP));
        topLeftPanel.add(this.originalImage);
        
        // Create and add the "Select Image" button
        selectPhotoButton = new JButton("Select image");
        selectPhotoButton.setPreferredSize(new Dimension(340, 30));
        selectPhotoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectImage();
            }
        });
        topLeftPanel.add(selectPhotoButton);

        // Create the bottom panel with fixed size
        bottomLeftPanel = new JPanel(new GridLayout(0, 1));
        bottomLeftPanel.setPreferredSize(new Dimension(350, 400)); 
        bottomLeftPanel.setBackground(Color.LIGHT_GRAY); 
        
        smoothingSigmaField = new SpinnerNumberModel(1, 0, 99, 1); // Default value is "1"
        resamplingLength = new SpinnerNumberModel(2.5, 0, 99, 0.1); // Default value is "2.5"
        maxDegreSH = new SpinnerNumberModel(5, 0, 99, 1); // Default value is "5"

        
        // Add parameters
        bottomLeftPanel.setLayout(new GridLayout(3, 2)); 

        bottomLeftPanel.add(new JLabel("Smoothing Sigma: "));
        bottomLeftPanel.add(new JSpinner(smoothingSigmaField));
        bottomLeftPanel.add(new JLabel("Resampling Length: "));
        bottomLeftPanel.add(new JSpinner(resamplingLength));
        bottomLeftPanel.add(new JLabel("Max Degree for Spherical Harmonics: "));
        bottomLeftPanel.add(new JSpinner(maxDegreSH));

        // Preview button for applying treatments
        previewButton = new JButton("Preview");
        previewButton.setPreferredSize(new Dimension(340, 30));
        previewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	previewButtonActionPerformed(e);
            }
        });
      

        leftPanel.add(topLeftPanel, BorderLayout.NORTH);
        leftPanel.add(bottomLeftPanel, BorderLayout.CENTER);
        leftPanel.add(previewButton, BorderLayout.SOUTH);

        // Create the right panel
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setPreferredSize(new Dimension(450, 1000));

        binarizedImage = new JPanel(new BorderLayout());
        binarizedImage.setPreferredSize(new Dimension(400, 300)); 
        binarizedImage.setBackground(Color.WHITE); 
        binarizedImage.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
				"Image after surface binarization", TitledBorder.CENTER, TitledBorder.TOP));
        marchingCubeImage = new JPanel(new BorderLayout());
        marchingCubeImage.setPreferredSize(new Dimension(400, 300)); 
        marchingCubeImage.setBackground(Color.WHITE); 
        marchingCubeImage.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
				"Image after Marching Cube", TitledBorder.CENTER, TitledBorder.TOP));
        pointCloudImage = new JPanel(new BorderLayout());
        pointCloudImage.setPreferredSize(new Dimension(400, 300)); 
        pointCloudImage.setBackground(Color.WHITE); 
        pointCloudImage.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
				"Image after Resample Point Cloud ", TitledBorder.CENTER, TitledBorder.TOP));
        spherical_harmonicsImage = new JPanel(new BorderLayout());
        spherical_harmonicsImage.setPreferredSize(new Dimension(400, 300)); 
        spherical_harmonicsImage.setBackground(Color.WHITE); 
        spherical_harmonicsImage.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
				"Image after spherical Harmonics Expansion", TitledBorder.CENTER, TitledBorder.TOP));
        rightPanel.add(binarizedImage);
        rightPanel.add(marchingCubeImage);
        rightPanel.add(pointCloudImage);
        rightPanel.add(spherical_harmonicsImage);
    
        
        // Create the progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true); // Display progress text
        progressBar.setPreferredSize(new Dimension(450, 30)); 
        
        rightPanel.add(progressBar);
        
        // Create points display panel
        pointsDisplayPanel = new JPanel();
        pointsDisplayPanel.setLayout(new BorderLayout());
        pointsDisplayPanel.setBorder(BorderFactory.createTitledBorder("Points Visualization"));
        
        // Creat JTextArea for pointsDisplayPanel
        this.pointsTextArea = new JTextArea(15, 30);
        this.pointsTextArea.setEditable(false); // Rendre le TextArea non éditable
        JScrollPane scrollPane = new JScrollPane(this.pointsTextArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        pointsDisplayPanel.add(scrollPane, BorderLayout.CENTER);
        
        //Finalize button
        finalizeButton = new JButton("Finalize");
        finalizeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
            	finalizeButtonActionPerformed(e);
            }
        });
        
        pointsDisplayPanel.add(finalizeButton, BorderLayout.SOUTH);

        // Add panels to the frame
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
        add(pointsDisplayPanel, BorderLayout.EAST);
        
    }

    private void selectImage() {
        OpenDialog openDialog = new OpenDialog("Select Image");
        String directory = openDialog.getDirectory();
        String fileName = openDialog.getFileName();
        if (directory != null && fileName != null) {
        	this.originalImagePlus = new ImagePlus(directory + fileName);
            this.originalImage.add(getPreviewWindow(this.originalImagePlus)); // Add preview window with slice slider
            this.originalImage.validate();
            this.originalImage.repaint();
        }
    }
    
    @SuppressWarnings("unchecked")
	private <T extends RealType<T>> void processImage(ImagePlus imp) {
    	
    	setProgressBar(2);
    	// Get parameters from GUI components
        int smoothingSigma = (int) smoothingSigmaField.getValue();
        double resampling_length = (double)resamplingLength.getValue() ;
        int max_degree = (int)maxDegreSH.getValue() ;
        
        setProgressBar(5);
        
        RandomAccessibleInterval<T> source = (RandomAccessibleInterval<T>) ImageJFunctions.wrap(imp);
        ProcessImage.initializeTargetScalingFactor(imp);
        
        setProgressBar(8);
        
        // Rescale
        RandomAccessibleInterval<T> rescaled_image = ProcessImage.rescaleImage(source, ProcessImage.scalingFactor);
        
        setProgressBar(10);
        
        // Blurring the image
        RandomAccessibleInterval<T> blurred_image = opService.filter().gauss(rescaled_image, smoothingSigma);
        
        setProgressBar(15);
        
        // Binarization
        IterableInterval<T> iterableBlurredImage = Views.iterable(blurred_image);
        IterableInterval<BitType> binarized_image = opService.threshold().otsu(iterableBlurredImage);
        
        setProgressBar(20);
        
        binarizedImage.removeAll();
        binarizedImage.add(getPreviewWindow(ImageJFunctions.wrap((RandomAccessibleInterval<T>) binarized_image, null)));
        binarizedImage.validate();
        binarizedImage.repaint();
        
        setProgressBar(25);

        //Marching cube
        Mesh mesh = opService.geom().marchingCubes((RandomAccessibleInterval<T>) binarized_image, 1);
        
        setProgressBar(30);
        
        Vertices vertices = mesh.vertices();

        setProgressBar(32);
        
        // Create a CustomMesh from them
     	List<Point3f> custom_mesh = new ArrayList<Point3f>();
     	for(int i = 0; i < vertices.size(); i++) {
     		custom_mesh.add(new Point3f(vertices.xf(i), vertices.yf(i), vertices.zf(i)));
     	}

     	setProgressBar(35);
     	
     	CustomPointMesh cm1 = new CustomPointMesh(custom_mesh);
     	cm1.setColor(new Color3f(255,255,255));
     	cm1.setPointSize(2);
     	
        setProgressBar(40);
        
        // Create a universe
     	Image3DUniverse univ1 = new Image3DUniverse();
     	univ1.showAttribute(Image3DUniverse.ATTRIBUTE_COORD_SYSTEM, true);
     	
        setProgressBar(45);
        
        marchingCubeImage.removeAll();
        marchingCubeImage.add(univ1.getCanvas(), BorderLayout.CENTER);
        // Ajouter le CustomMesh au Image3DUniverse
     	univ1.addCustomMesh(cm1, "Mesh");
        
        
        setProgressBar(50);
     	
        marchingCubeImage.validate();
        marchingCubeImage.repaint();

     	setProgressBar(55);
     	
    	//Point Cloud 
        this.resampled_points = ResamplePointCloud.resamplePointCloud(custom_mesh, resampling_length);
     	

     	setProgressBar(65);
        
     	CustomPointMesh cm2 = new CustomPointMesh(this.resampled_points);
     	cm2.setColor(new Color3f(255,255,255));
     	cm2.setPointSize(2);
     	
     	// Create a universe
     	Image3DUniverse univ2 = new Image3DUniverse();
     	univ2.showAttribute(Image3DUniverse.ATTRIBUTE_COORD_SYSTEM, true);
     	
     	setProgressBar(70);
     	
     	pointCloudImage.removeAll();
     	pointCloudImage.add(univ2.getCanvas(), BorderLayout.CENTER);
     	
     	setProgressBar(72);
     	
        // Ajouter le CustomMesh au Image3DUniverse
     	univ2.addCustomMesh(cm2, "Resampled Points");
        
        setProgressBar(80);
        
     	pointCloudImage.validate();
     	pointCloudImage.repaint();  
    

     	setProgressBar(85);
     	
        this.fitted_points = new SphericalHarmonicsExpansion(resampled_points, max_degree).expand();
        
        setProgressBar(90);
        
        SphericalHarmonicsExpansion.printPoints3D2(fitted_points, pointsTextArea);
        
     	setProgressBar(93);
     	
     	CustomPointMesh cm3 = new CustomPointMesh(this.fitted_points);
     	cm3.setColor(new Color3f(255,255,255));
     	cm3.setPointSize(2);
     	
     	// Create a universe
     	Image3DUniverse univ3 = new Image3DUniverse();
     	univ3.showAttribute(Image3DUniverse.ATTRIBUTE_COORD_SYSTEM, true);
     	
     	setProgressBar(97);
     	
     	spherical_harmonicsImage.removeAll();
     	spherical_harmonicsImage.add(univ3.getCanvas(), BorderLayout.CENTER);
        univ3.addCustomMesh(cm3, "Fitted Points");
        
        spherical_harmonicsImage.validate();
        spherical_harmonicsImage.repaint();  
     	
        setProgressBar(100);
       
    }
      
    // ActionListener for the "Preview" button
    private void finalizeButtonActionPerformed(ActionEvent evt) {
    	progressBar.setValue(0);
    	SphericalHarmonicsExpansion.writePointsToCSV(this.fitted_points, this.resampled_points);
    	
        // Perform finalize when the button is clicked
    	// Créer une instance de SwingWorker
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
		        try {
					finaliser();
				} catch (Throwable e) {
					e.printStackTrace();
				}
				return null;
			}
		};
		worker.execute();
    }
    
    // ActionListener for the "Preview" button
    private void previewButtonActionPerformed(ActionEvent evt) {
    	progressBar.setValue(0);
    	
        // Perform image processing when the button is clicked
    	// Créer une instance de SwingWorker
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
		        // Effectuez l'étape de traitement
		        processImage(originalImagePlus);
				return null;
			}
		};
		worker.execute();
    }

    protected static JPanel getPreviewWindow(ImagePlus imp) {
    	
        if (imp != null) {
            ImagePlusPanel panel = new ImagePlusPanel(imp);
            panel.setPreferredSize(new Dimension(400, 400));

            JPanel placeholder = new JPanel(null);
            placeholder.setPreferredSize(new Dimension(300, 275));

            JSlider slider = new JSlider(1, imp.getStackSize(), 1);
            slider.setPreferredSize(
                    new Dimension((int) placeholder.getPreferredSize().getWidth(), slider.getPreferredSize().height));

            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int slice = slider.getValue();
                    imp.setSlice(slice);
                    Image img = imp.getProcessor().getBufferedImage();
                    panel.setImage(img);
                }
            });

            JPanel row1 = new JPanel(null);
            row1.setPreferredSize(new Dimension((int) placeholder.getPreferredSize().getWidth(),
                    (int) (0.85 * placeholder.getPreferredSize().getHeight())));
            row1.setLayout(new BorderLayout());
            row1.add(panel, BorderLayout.CENTER);

            JPanel row2 = new JPanel();
            row2.setPreferredSize(new Dimension((int) placeholder.getPreferredSize().getWidth(),
                    (int) (0.15 * placeholder.getPreferredSize().getHeight())));
            row2.setLayout(new BorderLayout());
            row2.add(slider, BorderLayout.SOUTH);

            placeholder.setLayout(new BoxLayout(placeholder, BoxLayout.PAGE_AXIS));
            placeholder.add(row1);
            placeholder.add(row2);

            return placeholder;
        }
        return null;
    }
    
    public void finaliser() {
        // Fermer l'interface graphique
        JFrame topLevelFrame = (JFrame) SwingUtilities.getWindowAncestor(finalizeButton);
        if (topLevelFrame != null) {
            topLevelFrame.dispose();
        }
    }
    
	public static void setProgressBar(int value) {
		progressBar.setValue(value);
	}
    
    
	public static class ImagePlusPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private ImagePlus imp;
		private Image img;

		/*-----------------------------------------------------------------------------------------------------------------------*/
		/**
		 * ImagePlusPanel unique constructor.
		 * 
		 * @param imp images stack
		 */
		public ImagePlusPanel(ImagePlus imp) {
			this.imp = imp;
			this.img = imp.getImage();
			setPreferredSize(new Dimension(imp.getWidth(), imp.getHeight()));
		}

		/*-----------------------------------------------------------------------------------------------------------------------*/
		/**
		 * Method that sets the images stack.
		 * 
		 * @param img images stack
		 */
		public void setImage(Image img) {
			this.img = img;
			repaint();
		}

		/*-----------------------------------------------------------------------------------------------------------------------*/
		/**
		 * Methods used to repaint slice.
		 */
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			double scale = Math.min((double) getWidth() / imp.getWidth(), (double) getHeight() / imp.getHeight());

			int scaledWidth = (int) (imp.getWidth() * scale);
			int scaledHeight = (int) (imp.getHeight() * scale);

			int offsetX = (getWidth() - scaledWidth) / 2;
			int offsetY = (getHeight() - scaledHeight) / 2;

			g.drawImage(img, offsetX, offsetY, scaledWidth, scaledHeight, null);
		}
	}
}
