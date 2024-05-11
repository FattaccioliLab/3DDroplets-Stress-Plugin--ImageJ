package com.mycompany.imagej;

import javax.swing.SwingUtilities;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.RealType;

@Plugin(type = Command.class, menuPath = "Plugins>3D Droplets Stress")
public class DropletsStress<T extends RealType<T>> implements Command {

    @Parameter
    private OpService opService;


    @Override
    public void run() {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // passer opService comme argument au constructeur de DropletsStressGUI
                new DropletsStressGUI(opService).setVisible(true);
            }
        });

    }

    public static void main(final String... args) throws Exception {

        final ImageJ ij = new ImageJ();

        // invoke the plugin
        ij.command().run(DropletsStress.class, true);

    }

}