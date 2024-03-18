package com.mycompany.imagej;

import ij.*;
import ij.io.OpenDialog;
import ij3d.Image3DUniverse;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Vertices;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

import customnode.CustomPointMesh;
import fiji.plugin.trackmate.detection.util.MedianFilter2D;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class DropletsStressGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    //private ImageProcessor processor;
    private OpService opService;

    private JPanel leftPanel;
    //---top left panel---//
    private JPanel topLeftPanel;
    private JPanel originalImage;
    private JButton selectPhotoButton;
    private ImagePlus originalImagePlus;
    //---bottom left panel---//
    private JPanel bottomLeftPanel;
    private SpinnerModel medianRadiusField;
    private SpinnerModel iterationsPSFField;
    private SpinnerModel smoothingSigmaField;
    private SpinnerModel nSmoothingIterationsField;
    private SpinnerModel resamplingLength;
    private SpinnerModel nTracingIterations;
    private SpinnerModel traceLength;
    private SpinnerModel outlierTolerance;
    private JButton previewButton;
    //---right panel---//
    private JPanel rightPanel;
    private JPanel binarizedImage;
    private JPanel marchingCubeImage;
    private JPanel pointCloudImage;
    private JButton finalizeButton;
    private JProgressBar progressBar;


    // Créez un tableau de pixels pour votre image
    int width = 400/* largeur de votre image */;
    int height = 400 /* hauteur de votre image */;

    public DropletsStressGUI(OpService opService) {
        setTitle("Droplets Stress Plugin");
        setSize(1000, 1000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        this.opService = opService;

        // Create the left panel
        leftPanel = new JPanel(new BorderLayout());

        // Create the top left panel with fixed size    
        topLeftPanel = new JPanel();
        topLeftPanel.setPreferredSize(new Dimension(500, 290)); 
        originalImage = new JPanel(new BorderLayout());
        originalImage.setPreferredSize(new Dimension(490, 250)); 
        originalImage.setBackground(Color.WHITE); 
        originalImage.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                "Original Image", TitledBorder.CENTER, TitledBorder.TOP));
        topLeftPanel.add(originalImage);

        // Create and add the "Select Image" button
        selectPhotoButton = new JButton("Select image");
        selectPhotoButton.setPreferredSize(new Dimension(490, 30));
        selectPhotoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectImage();
            }
        });
        topLeftPanel.add(selectPhotoButton);

        // Create the bottom panel with fixed size
        bottomLeftPanel = new JPanel(new GridLayout(0, 1));
        bottomLeftPanel.setPreferredSize(new Dimension(500, 610)); 
        bottomLeftPanel.setBackground(Color.LIGHT_GRAY); 

        medianRadiusField = new SpinnerNumberModel(3, 0, 99, 1); // Default value is "10"
        iterationsPSFField = new SpinnerNumberModel(10, 0, 99, 1); // Default value is "5"
        smoothingSigmaField = new SpinnerNumberModel(1, 0, 99, 1); // Default value is "2.0"
        nSmoothingIterationsField = new SpinnerNumberModel(10, 0, 99, 1); // Default value is "3"
        resamplingLength = new SpinnerNumberModel(2.5, 0, 99, 0.1);
        nTracingIterations = new SpinnerNumberModel(10, 0, 99, 1);
        traceLength = new SpinnerNumberModel(12, 0, 99, 1);
        outlierTolerance = new SpinnerNumberModel(0.5, 0, 99, 0.1);


        // Add parameters
        bottomLeftPanel.setLayout(new GridLayout(9, 2)); 
        bottomLeftPanel.add(new JLabel("Median Radius: "));
        bottomLeftPanel.add(new JSpinner(medianRadiusField));
        bottomLeftPanel.add(new JLabel("Iterations PSF: "));
        bottomLeftPanel.add(new JSpinner(iterationsPSFField));
        bottomLeftPanel.add(new JLabel("Smoothing Sigma: "));
        bottomLeftPanel.add(new JSpinner(smoothingSigmaField));
        bottomLeftPanel.add(new JLabel("N Smoothing Iterations: "));
        bottomLeftPanel.add(new JSpinner(nSmoothingIterationsField));
        bottomLeftPanel.add(new JLabel("Resampling Length: "));
        bottomLeftPanel.add(new JSpinner(resamplingLength));
        bottomLeftPanel.add(new JLabel("N Tracing Iterations: "));
        bottomLeftPanel.add(new JSpinner(nTracingIterations));
        bottomLeftPanel.add(new JLabel("Trace Length: "));
        bottomLeftPanel.add(new JSpinner(traceLength));
        bottomLeftPanel.add(new JLabel("Outlier Tolerance: "));
        bottomLeftPanel.add(new JSpinner(outlierTolerance));

        // Preview button for applying treatments
        previewButton = new JButton("Preview");
        previewButton.setPreferredSize(new Dimension(490, 30));
        previewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("dans actionPerformed de preview button");
                previewButtonActionPerformed(e);
            }
        });


        leftPanel.add(topLeftPanel, BorderLayout.NORTH);
        leftPanel.add(bottomLeftPanel, BorderLayout.CENTER);
        leftPanel.add(previewButton, BorderLayout.SOUTH);

        // Create the right panel
        rightPanel = new JPanel();

        binarizedImage = new JPanel(new BorderLayout());
        binarizedImage.setPreferredSize(new Dimension(490, 200)); 
        binarizedImage.setBackground(Color.WHITE); 
        binarizedImage.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                "Image after surface binarization", TitledBorder.CENTER, TitledBorder.TOP));
        marchingCubeImage = new JPanel(new BorderLayout());
        marchingCubeImage.setPreferredSize(new Dimension(490, 200)); 
        marchingCubeImage.setBackground(Color.WHITE); 
        marchingCubeImage.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                "Image after surface approximation", TitledBorder.CENTER, TitledBorder.TOP));
        pointCloudImage = new JPanel(new BorderLayout());
        pointCloudImage.setPreferredSize(new Dimension(490, 200)); 
        pointCloudImage.setBackground(Color.WHITE); 
        pointCloudImage.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                "Image after surface refinement", TitledBorder.CENTER, TitledBorder.TOP));
        rightPanel.add(binarizedImage);
        rightPanel.add(marchingCubeImage);
        rightPanel.add(pointCloudImage);

        // Create the progress bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true); // Display progress text
        progressBar.setPreferredSize(new Dimension(490, 40)); 

        // Add the progress bar to the bottom of the left panel
        rightPanel.add(progressBar, BorderLayout.SOUTH);

        //Finalize button
        finalizeButton = new JButton("Finalize");
        finalizeButton.setPreferredSize(new Dimension(490, 30));
        finalizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("finalized");
            }
        });
        rightPanel.add(finalizeButton);

        // Add panels to the frame
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

    }

    private void selectImage() {
        OpenDialog openDialog = new OpenDialog("Select Image");
        String directory = openDialog.getDirectory();
        String fileName = openDialog.getFileName();
        if (directory != null && fileName != null) {
            originalImagePlus = new ImagePlus(directory + fileName);
            originalImage.add(getPreviewWindow(originalImagePlus)); // Add preview window with slice slider
            originalImage.validate();
            originalImage.repaint();
        }
    }

    private <T extends RealType<T>> void processImage(ImagePlus imp) {

        // Get parameters from GUI components
        int medianRadius = (int) medianRadiusField.getValue();
        int iterationsPSF = (int) iterationsPSFField.getValue();
        int smoothingSigma = (int) smoothingSigmaField.getValue();
        int n_smoothing_iterations = (int) nSmoothingIterationsField.getValue() ;
        double resampling_length = (double)resamplingLength.getValue() ;
        int n_tracing_iterations = (int) nTracingIterations.getValue() ;
        int trace_length = (int) traceLength.getValue();
        double outlier_tolerance = (double)outlierTolerance.getValue() ;

        RandomAccessibleInterval<T> source = (RandomAccessibleInterval<T>) ImageJFunctions.wrap(imp);
        MedianFilter2D medianFilter = new MedianFilter2D(source, medianRadius);
        medianFilter.process();
        final Img<T> dec = medianFilter.getResult();

        // Sobel edge detection
        RandomAccessibleInterval<T> edge = opService.filter().sobel(dec);
        double[] scalingFactors = new double[]{1.0, 1.0, 1.0};

        // Rescale
        RandomAccessibleInterval<T> rescaled_image = ProcessImage.rescaleImage(edge, scalingFactors);

        // Blurring the image
        RandomAccessibleInterval<T> blurred_image = opService.filter().gauss(rescaled_image, smoothingSigma);

        // Binarization
        IterableInterval<T> iterableBlurredImage = Views.iterable(blurred_image);
        IterableInterval<BitType> binarized_image = opService.threshold().otsu(iterableBlurredImage);
        binarizedImage.removeAll();
        binarizedImage.add(getPreviewWindow(ImageJFunctions.wrap((RandomAccessibleInterval<T>) binarized_image, null)));
        binarizedImage.validate();
        binarizedImage.repaint();

        //Marching cube
        Mesh mesh = opService.geom().marchingCubes((RandomAccessibleInterval<T>) binarized_image, 1);
        Vertices vertices = mesh.vertices();


        // Create a CustomMesh from them
        List<Point3f> custom_mesh = new ArrayList<Point3f>();
        for(int i = 0; i < vertices.size(); i++) {
            custom_mesh.add(new Point3f(vertices.xf(i), vertices.yf(i), vertices.zf(i)));
        }

        CustomPointMesh cm1 = new CustomPointMesh(custom_mesh);
        cm1.setColor(new Color3f(255,255,255));

        // Create a universe
        Image3DUniverse univ1 = new Image3DUniverse();
        univ1.showAttribute(Image3DUniverse.ATTRIBUTE_COORD_SYSTEM, true);

        marchingCubeImage.removeAll();
        marchingCubeImage.add(univ1.getCanvas(), BorderLayout.CENTER);
        // Ajouter le CustomMesh au Image3DUniverse
        univ1.addCustomMesh(cm1, "Mesh");
        marchingCubeImage.validate();
        marchingCubeImage.repaint();

        //Point Cloud 
        List<Point3f> resampled_points = ResamplePointCloud.resamplePointCloud(custom_mesh, resampling_length);

        CustomPointMesh cm2 = new CustomPointMesh(resampled_points);
        cm2.setColor(new Color3f(255,255,255));

        // Create a universe
        Image3DUniverse univ2 = new Image3DUniverse();
        univ2.showAttribute(Image3DUniverse.ATTRIBUTE_COORD_SYSTEM, true);

        pointCloudImage.removeAll();
        pointCloudImage.add(univ2.getCanvas(), BorderLayout.CENTER);
        // Ajouter le CustomMesh au Image3DUniverse
        univ2.addCustomMesh(cm2, "Mesh");
        pointCloudImage.validate();
        pointCloudImage.repaint();



    }


    // ActionListener for the "Preview" button
    private void previewButtonActionPerformed(ActionEvent evt) {
        System.out.println("dans previewButtonActionPerformed de preview button");
        // Perform image processing when the button is clicked

        // Créez une instance de SwingWorker
        ImageProcessingWorker worker = new ImageProcessingWorker();
        // Exécutez le SwingWorker
        worker.execute();
    }

    protected static JPanel getPreviewWindow(ImagePlus imp) {

        if (imp != null) {
            System.out.println("dans getPreviewWindow de preview button");
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

    //    public static void main(String[] args) {
    //        SwingUtilities.invokeLater(new Runnable() {
    //            @Override
    //            public void run() {
    //                new DropletsStressGUI(opService).setVisible(true);
    //            }
    //        });
    //    }

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

    // Classe interne pour le SwingWorker
    private class ImageProcessingWorker extends SwingWorker<Void, Integer> {

        @Override
        protected Void doInBackground() throws Exception {

            // Simulate image processing progress
            int totalProgress = 100;
            int numSteps = 1; // Nombre d'étapes de traitement
            int stepProgress = totalProgress / numSteps;
            int step = 1;

            // Effectuez chaque étape de traitement et publiez la progression
            //for (int step = 1; step <= numSteps; step++) {

            // Effectuez l'étape de traitement
            processImage(originalImagePlus);
            // Calculez et publiez la progression après chaque étape
            int progress = step * stepProgress;
            publish(progress);

            //}

            return null;
        }

        @Override
        protected void process(List<Integer> chunks) {
            // Mettre à jour la barre de progression
            for (Integer progress : chunks) {
                progressBar.setValue(progress);
            }
        }

        @Override
        protected void done() {
            // Traitement terminé, effectuez les actions nécessaires ici
        }
    }
}
