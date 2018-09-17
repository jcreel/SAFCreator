package edu.tamu.di.SAFCreator;

import edu.tamu.di.SAFCreator.controller.UserInterfaceController;
import edu.tamu.di.SAFCreator.view.UserInterfaceView;

/**
 *
 */
public class App {
    private static ImportDataProcessor processor = new ImportDataProcessorImpl();

    private static UserInterfaceView view = new UserInterfaceView();

    @SuppressWarnings("unused")
    private static UserInterfaceController controller = new UserInterfaceController(processor, view);


    public static void main(String[] args) {
        view.setSize(800, 640);
        view.setResizable(false);
        view.setVisible(true);
    }
}
